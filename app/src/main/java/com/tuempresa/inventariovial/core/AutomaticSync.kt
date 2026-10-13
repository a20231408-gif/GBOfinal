package com.tuempresa.inventariovial.core

import android.content.Context
import androidx.room.withTransaction
import androidx.work.*
import com.tuempresa.inventariovial.DriveUploadPolicy
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object AutomaticSync {
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
    /** Called in the same Room transaction as validation. No HTTP/config dependency. */
    suspend fun request(db:InventoryDatabase,kind:String,id:String,revision:Long,warnings:Boolean=false,origin:String="AUTOMATIC") {
        val key="$kind/$id";val dao=db.automaticSyncDao();val old=dao.get(key)
        if(old!=null && old.revision==revision && old.state in setOf("SYNCED","QUEUED","UPLOADING"))return
        val next=maxOf(revision,(old?.revision ?: 0)+1)
        dao.put(DriveSyncIntent(key,kind,id,next,old?.syncedRevision ?: 0,
            if((old?.syncedRevision ?: 0)>0) "UPDATE_PENDING" else "QUEUED",origin,warnings,
            old?.lastSyncedAt,receiptJson=old?.receiptJson,preparedJson=old?.preparedJson?.takeIf {JSONObject(it).optLong("revision")>old.syncedRevision},
            error=old?.error?.takeIf {old.state=="CONFLICT"}))
        if(old?.state=="CONFLICT")dao.state(key,next,"CONFLICT",old.error)
        db.syncContext?.let { wake(it) }
    }
    suspend fun draft(db:InventoryDatabase,kind:String,id:String) {
        val old=db.automaticSyncDao().get("$kind/$id") ?: return
        db.automaticSyncDao().put(old.copy(error=old.error?.takeIf {old.state=="CONFLICT"},state=if(old.state=="CONFLICT") "CONFLICT" else if(old.syncedRevision>0) "UPDATE_PENDING" else "DRAFT"))
    }
    /** Preserve identity/revision/receipts; recover only the previous GPS-only gate. */
    suspend fun recoverGpsBlocked(db:InventoryDatabase):Int=db.withTransaction {
        var count=0
        for(old in db.automaticSyncDao().all()) {
            if(old.state !in setOf("PENDING_VALIDATION","BLOCKED","ERROR") ||
                old.error?.startsWith("Entrega pendiente de georreferenciación externa")!=true ||
                !AutomaticDelivery.validated(db,old))continue
            db.automaticSyncDao().put(old.copy(state=if(old.syncedRevision>0) "UPDATE_PENDING" else "QUEUED",
                error=null,preparedJson=null,attempts=0))
            count++
        }
        count
    }
    /** Persistent drain also repairs an interruption between the Room commit and WorkManager enqueue. */
    suspend fun recoverJpegBlocked(context:Context,db:InventoryDatabase):Int=db.withTransaction {
        var count=0
        for(old in db.automaticSyncDao().all()) {
            if(old.state!="BLOCKED" || old.error?.contains("(NOT_JPEG)")!=true || old.syncedRevision>=old.revision || !AutomaticDelivery.validated(db,old))continue
            // Only a definitively rejected current revision can be rebuilt. Lost ACKs and predecessors are replayed unchanged.
            val prepared=old.preparedJson?.let(::JSONObject)
            if(prepared!=null && prepared.optLong("revision")!=old.revision)continue
            val dir=java.io.File(context.filesDir,"automatic_drive/${com.tuempresa.inventariovial.delivery.sha256(old.key.toByteArray())}/${old.revision}")
            if(dir.exists())check(dir.renameTo(java.io.File(dir.parentFile,"${old.revision}_NOT_JPEG_${java.util.UUID.randomUUID()}"))) {"No se pudo conservar la preparación rechazada."}
            db.automaticSyncDao().put(old.copy(state=if(old.syncedRevision>0)"UPDATE_PENDING" else "QUEUED",error=null,preparedJson=null,attempts=0))
            count++
        }
        count
    }

    fun wake(context:Context) {
        val c=context.applicationContext
        scope.launch {
            recoverGpsBlocked(InventoryDatabase.getInstance(c))
            recoverJpegBlocked(c,InventoryDatabase.getInstance(c))
            // Periodic work is a recovery mechanism; the immediate work handles ordinary closure.
            val periodic=PeriodicWorkRequestBuilder<AutomaticSyncWorker>(15,TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build()
            WorkManager.getInstance(c).enqueueUniquePeriodicWork("automatic-drive-recovery",ExistingPeriodicWorkPolicy.KEEP,periodic)
            enqueue(c)
        }
    }
    fun enqueue(context:Context) {
        val job=OneTimeWorkRequestBuilder<AutomaticSyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL,30,TimeUnit.SECONDS).build()
        WorkManager.getInstance(context).enqueueUniqueWork("automatic-drive-drain",ExistingWorkPolicy.APPEND_OR_REPLACE,job)
    }
    suspend fun retry(context:Context,key:String) {
        val db=InventoryDatabase.getInstance(context)
        db.withTransaction {
            val old=db.automaticSyncDao().get(key) ?: return@withTransaction
            require(old.state!="CONFLICT") {"Conflicto de revisiones: conserva la edición local y concilia con el responsable antes de reenviar."}
            require(old.state!="DRAFT") {"Valida primero la ficha."}
            if(old.state=="BLOCKED" && old.error?.contains("(NOT_JPEG)")==true) {
                recoverJpegBlocked(context,db)
            } else db.automaticSyncDao().put(old.copy(state="QUEUED",error=null,attempts=0,origin="MANUAL_APP"))
        }
        enqueue(context)
    }
    suspend fun manual(context:Context,kind:String,id:String,warnings:Boolean=false) {
        val db=InventoryDatabase.getInstance(context)
        val existing=db.automaticSyncDao().get("$kind/$id")
        val revision=when(kind) {
            "SIC"->requireNotNull(db.inventoryDao().recordById(id)).let {require(it.status=="ACTIVE") {"Valida primero la ficha."};it.updatedAt}
            "SCAP"->requireNotNull(db.scapDao().inspection(id)).let {require(it.status=="COMPLETE") {"Confirma primero el cierre SCAP."};it.updatedAt}
            else->requireNotNull(db.pavementDao().sample(id)).let {require(it.status=="CLOSED") {"Cierra primero la muestra."};it.updatedAt}
        }
        if(existing!=null && existing.revision>=revision) {
            if(existing.state!="SYNCED")retry(context,existing.key)
        }else db.withTransaction {request(db,kind,id,revision,warnings,"MANUAL_APP")}
    }
}

class AutomaticSyncWorker(context:Context,params:WorkerParameters):CoroutineWorker(context,params) {
    override suspend fun doWork():Result=withContext(Dispatchers.IO) {
        val retry=AutomaticSyncDrain.run(applicationContext,InventoryDatabase.getInstance(applicationContext),attempt=runAttemptCount)
        if(retry)Result.retry() else Result.success()
    }
}

/** Same drain for the WorkManager worker and focused transport/ACK regression tests. */
object AutomaticSyncDrain {
    private val mutex=Mutex()
    suspend fun run(context:Context,db:InventoryDatabase,
        transportFactory:()->AutomaticDriveTransport={AutomaticDriveHttp()},
        configurationError:String?=DriveUploadPolicy.configurationError(),attempt:Int=0):Boolean = mutex.withLock {
        drain(context,db,transportFactory,configurationError,attempt)
    }
    private suspend fun drain(context:Context,db:InventoryDatabase,
        transportFactory:()->AutomaticDriveTransport={AutomaticDriveHttp()},
        configurationError:String?=DriveUploadPolicy.configurationError(),attempt:Int=0):Boolean {
        AutomaticSync.recoverGpsBlocked(db)
        AutomaticSync.recoverJpegBlocked(context,db)
        val dao=db.automaticSyncDao()
        var retry=false
        val pending=dao.all().filter {it.state in setOf("QUEUED","UPDATE_PENDING","UPLOADING","ERROR","UNCONFIGURED","PENDING_VALIDATION") && AutomaticDelivery.validated(db,it)}
        val ordered=pending.map {it to AutomaticDelivery.order(context,db,it)}.sortedWith(compareBy({it.second.first},{it.second.second},{it.first.key})).map {it.first}
        for(queued in ordered) {
            var intent=dao.get(queued.key) ?: continue
            if(intent.state=="CONFLICT" || !AutomaticDelivery.validated(db,intent))continue
            if(configurationError!=null) {dao.state(intent.key,intent.revision,"UNCONFIGURED",configurationError);continue}
            try {
                val transport=transportFactory()
                if(!transport.supportsV3()) {dao.state(intent.key,intent.revision,"UNCONFIGURED","El servicio requiere syncProtocol 3. Se comprobará automáticamente de nuevo; la copia anterior se conserva.");continue}
                if(!transport.supportsFamilyPaths()) {
                    dao.state(intent.key,intent.revision,"UNCONFIGURED","El backend requiere pathVersion 4, stableFamilyNumbering=true y photoCategoryVersion=1. No se envió contenido; la cola se conserva.")
                    continue
                }
                // An unconfirmed predecessor may have committed before its response was lost.
                // Replay its exact snapshot; never rebase against another operator's revision.
                val predecessor=intent.preparedJson?.let(::JSONObject)?.takeIf {it.getLong("revision")<intent.revision && it.getLong("revision")>intent.syncedRevision}
                if(predecessor!=null) {
                    if(predecessor.optInt("technicalDeliveryVersion")==1 && !transport.supportsTechnicalDeliveryWithoutGps()) {dao.state(intent.key,intent.revision,"UNCONFIGURED","El backend no acredita entrega técnica sin GPS. Cola conservada.");continue}
                    val previousAck=transport.post(predecessor)
                    if(!previousAck.optBoolean("ok"))throw SyncRejection(previousAck.optString("code"),previousAck.optBoolean("retryable"),previousAck.optLong("remoteRevision").takeIf {it>0})
                    if(!AutomaticDelivery.validAck(predecessor,previousAck,intent.receiptJson?.let(::JSONObject)))throw java.io.IOException("Confirmación anterior incompleta.")
                    db.withTransaction {
                        DriveInventorySchema.remember(context,predecessor,previousAck)
                        dao.confirmBase(intent.key,predecessor.getLong("revision"),System.currentTimeMillis(),previousAck.toString())
                    }
                    intent=dao.get(intent.key) ?: continue
                }
                val cached=java.io.File(context.filesDir,"automatic_drive/${com.tuempresa.inventariovial.delivery.sha256(intent.key.toByteArray())}/${intent.revision}/prepared-path4.json")
                if(com.tuempresa.inventariovial.BuildConfig.GBO_PHOTO_NAMES_V2_ENABLED && intent.preparedJson==null && !cached.isFile) {
                    if(!transport.supportsReadablePhotoNames())throw SyncPending("Nombres representativos pendientes de activar readablePhotoNamingVersion=2 en Apps Script.")
                    val preview=AutomaticDelivery.prepare(context,db,intent,readable=false,persist=false)
                    val reservation=transport.reservePhotoNames(preview)
                    if(!reservation.optBoolean("ok"))throw SyncRejection(reservation.optString("code"),reservation.optBoolean("retryable"))
                    ReadablePhotoNames.remember(context,preview,reservation)
                }
                val prepared=AutomaticDelivery.prepare(context,db,intent)
                dao.prepared(intent.key,intent.revision,prepared.toString())
                if(prepared.optInt("technicalDeliveryVersion")==1 && !transport.supportsTechnicalDeliveryWithoutGps()) {
                    dao.state(intent.key,intent.revision,"UNCONFIGURED","El backend de Drive debe anunciar technicalDeliveryWithoutGps=true. Ficha y fotos conservadas; se comprobará automáticamente de nuevo.")
                    continue
                }
                dao.state(intent.key,intent.revision,"UPLOADING",null)
                if(!AutomaticDelivery.current(db,intent,prepared))continue
                val ack=transport.post(prepared)
                if(!ack.optBoolean("ok"))throw SyncRejection(ack.optString("code"),ack.optBoolean("retryable"),ack.optLong("remoteRevision").takeIf {it>0})
                if(!AutomaticDelivery.validAck(prepared,ack,intent.receiptJson?.let(::JSONObject)))throw java.io.IOException("La confirmación no acredita todos los archivos, carpeta y revisión.")
                db.withTransaction {
                    val matches=AutomaticDelivery.current(db,intent,prepared)
                    DriveInventorySchema.remember(context,prepared,ack)
                    dao.confirmBase(intent.key,intent.revision,System.currentTimeMillis(),ack.toString())
                    if(matches) {
                        dao.acknowledge(intent.key,intent.revision,System.currentTimeMillis(),ack.toString())
                        val files=ack.getJSONArray("files")
                        for(n in 0 until files.length()) {
                            val file=files.getJSONObject(n)
                            if(file.getString("key").startsWith("photo/")) {
                                val id=file.getString("key").removePrefix("photo/")
                                db.inventoryDao().photoById(id)?.let {photo->db.inventoryDao().updatePhotoMetadata(photo.copy(syncStatus="SYNCED",driveFileId=file.getString("fileId"),driveFolderId=file.getString("folderId")))}
                            }
                        }
                        if(intent.kind=="SIC") {
                            db.inventoryDao().refreshRecordPhotoStatus(intent.recordId)
                            if(prepared.getJSONArray("sicRows").length()>0)db.inventoryDao().markTechnicalExcelSynced(intent.recordId)
                        }
                        if(intent.kind=="SCAP")db.scapDao().inspection(intent.recordId)?.let {s->db.scapDao().syncState(s.id,s.updatedAt,"SYNCED")}
                    }
                }
            com.tuempresa.inventariovial.media.VideoSync.enqueue(context)
            }catch(e:CancellationException){throw e}
            catch(e:SyncPending){dao.state(intent.key,intent.revision,"PENDING_VALIDATION",e.message)}
            catch(e:SyncRejection){
                val pending=e.code=="ORDERING_REQUIRED"
                val conflict=e.code in setOf("REVISION_CONFLICT","STALE_REVISION","BASE_REVISION_CONFLICT")
                val state=if(conflict) "CONFLICT" else if(pending)"PENDING_VALIDATION" else if(e.retryable)"ERROR" else "BLOCKED"
                val message=if(conflict) "Conflicto de revisiones (${e.code}; base ${intent.syncedRevision}; remota ${e.remoteRevision ?: "no informada"}). Edición local conservada; no se sobrescribió el servidor. Requiere conciliación antes de reenviar." else if(pending)"Pendiente de validación/ordenamiento. Se conservó la entrega anterior." else "El servidor rechazó la revisión (${e.code}). La evidencia local se conserva."
                if(conflict)db.withTransaction {
                    val latest=dao.get(intent.key)
                    if(latest!=null && latest.syncedRevision==intent.syncedRevision)dao.state(latest.key,latest.revision,state,message)
                }else dao.state(intent.key,intent.revision,state,message)
                retry=retry || e.retryable && attempt<4
            }catch(_:IllegalArgumentException){
                dao.state(intent.key,intent.revision,"BLOCKED","La ficha o los archivos requieren corrección antes de enviar. Revisa la validación del formato.")
            }catch(_:Exception){
                dao.state(intent.key,intent.revision,"ERROR","No se confirmó la entrega. Archivos locales conservados; reintenta la comunicación.")
                retry=retry || attempt<4
            }
        }
        return retry
    }
}
class SyncPending(message:String):Exception(message)
class SyncRejection(val code:String,val retryable:Boolean,val remoteRevision:Long?=null):Exception(code)
