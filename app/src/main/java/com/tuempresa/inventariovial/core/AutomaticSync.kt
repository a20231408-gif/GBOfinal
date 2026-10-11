package com.tuempresa.inventariovial.core

import android.content.Context
import androidx.room.withTransaction
import androidx.work.*
import com.tuempresa.inventariovial.DriveUploadPolicy
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import kotlinx.coroutines.*
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
            old?.lastSyncedAt,receiptJson=old?.receiptJson))
        db.syncContext?.let { wake(it) }
    }
    suspend fun draft(db:InventoryDatabase,kind:String,id:String) {
        val old=db.automaticSyncDao().get("$kind/$id") ?: return
        db.automaticSyncDao().put(old.copy(state=if(old.syncedRevision>0) "UPDATE_PENDING" else "DRAFT",preparedJson=null,error=null))
    }
    /** Persistent drain also repairs an interruption between the Room commit and WorkManager enqueue. */
    fun wake(context:Context) {
        val c=context.applicationContext
        scope.launch {
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
            require(old.state!="DRAFT") {"Valida primero la ficha."}
            db.automaticSyncDao().put(old.copy(state="QUEUED",error=null,attempts=0,origin="MANUAL_APP"))
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
        val db=InventoryDatabase.getInstance(applicationContext);val dao=db.automaticSyncDao()
        var retry=false
        val pending=dao.all().filter {it.state in setOf("QUEUED","UPDATE_PENDING","UPLOADING","ERROR","UNCONFIGURED","PENDING_VALIDATION")}
        val ordered=pending.map {it to AutomaticDelivery.order(applicationContext,db,it)}.sortedWith(compareBy({it.second.first},{it.second.second})).map {it.first}
        for(intent in ordered) {
            if(!AutomaticDelivery.validated(db,intent))continue
            val configError=DriveUploadPolicy.configurationError()
            if(configError!=null) {dao.state(intent.key,intent.revision,"UNCONFIGURED",configError);continue}
            try {
                val transport=AutomaticDriveHttp()
                if(!transport.supportsV3()) {dao.state(intent.key,intent.revision,"UNCONFIGURED","El servicio requiere syncProtocol 3. Se comprobará automáticamente de nuevo; la copia anterior se conserva.");continue}
                val prepared=AutomaticDelivery.prepare(applicationContext,db,intent)
                dao.prepared(intent.key,intent.revision,prepared.toString())
                dao.state(intent.key,intent.revision,"UPLOADING",null)
                if(!AutomaticDelivery.current(db,intent,prepared))continue
                val ack=transport.post(prepared)
                if(!ack.optBoolean("ok"))throw SyncRejection(ack.optString("code"),ack.optBoolean("retryable"))
                require(AutomaticDelivery.validAck(prepared,ack)) {"La confirmación no acredita todos los archivos, carpeta y revisión."}
                db.withTransaction {
                    if(AutomaticDelivery.current(db,intent,prepared)) {
                        dao.acknowledge(intent.key,intent.revision,System.currentTimeMillis(),ack.toString())
                        val files=ack.getJSONArray("files")
                        for(n in 0 until files.length()) {
                            val file=files.getJSONObject(n)
                            if(file.getString("key").startsWith("photo/")) {
                                val id=file.getString("key").removePrefix("photo/")
                                db.inventoryDao().photoById(id)?.let {photo->db.inventoryDao().updatePhotoMetadata(photo.copy(syncStatus="SYNCED",driveFileId=file.getString("fileId"),driveFolderId=file.getString("folderId")))}
                            }
                        }
                        if(intent.kind=="SCAP")db.scapDao().inspection(intent.recordId)?.let {s->db.scapDao().syncState(s.id,s.updatedAt,"SYNCED")}
                    }
                }
            }catch(e:CancellationException){throw e}
            catch(e:SyncPending){dao.state(intent.key,intent.revision,"PENDING_VALIDATION",e.message)}
            catch(e:SyncRejection){
                val pending=e.code=="ORDERING_REQUIRED"
                val state=if(pending)"PENDING_VALIDATION" else if(e.retryable)"ERROR" else "BLOCKED"
                dao.state(intent.key,intent.revision,state,if(pending)"Pendiente de validación/ordenamiento. Se conservó la entrega anterior." else "El servidor rechazó la revisión (${e.code}). La evidencia local se conserva.")
                retry=retry || e.retryable && runAttemptCount<4
            }catch(_:IllegalArgumentException){
                dao.state(intent.key,intent.revision,"BLOCKED","La ficha o los archivos requieren corrección antes de enviar. Revisa la validación del formato.")
            }catch(_:Exception){
                dao.state(intent.key,intent.revision,"ERROR","No se confirmó la entrega. Archivos locales conservados; reintenta la comunicación.")
                retry=retry || runAttemptCount<4
            }
        }
        if(retry)Result.retry() else Result.success()
    }
}
class SyncPending(message:String):Exception(message)
class SyncRejection(val code:String,val retryable:Boolean):Exception(code)
