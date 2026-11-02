package com.tuempresa.inventariovial.localfiles

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.tuempresa.inventariovial.core.AutomaticDelivery
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.media.VideoDatabase
import com.tuempresa.inventariovial.media.VideoNames
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/** Archive the active-list entry and release public duplicates. Room, originals and remote data remain. */
object ConfirmedLocalCleanup {
    const val PREFIX="CONFIRMED_LOCAL_ARCHIVE/"
    data class Candidate(val key:String,val revision:Long)
    data class Backup(val candidates:List<Candidate>,val files:List<LocalSharedFile>,val zip:File,val bytes:Long,val sha256:String)
    fun archived(choices:List<DeviceDeliveryChoice>,key:String,revision:Long)=choices.firstOrNull{it.key==PREFIX+key}
        ?.destination?.let{runCatching{JSONObject(it)}.getOrNull()}?.let{it.optString("state")=="ARCHIVED"&&it.optLong("revision")==revision}==true
    private suspend fun validate(c:Context,db:InventoryDatabase,item:Candidate):Pair<JSONObject,LocalExportJob> {
        val kind=item.key.substringBefore('/');val id=item.key.substringAfter('/')
        require(DeliveryDestination.revision(db,kind,id)==item.revision){"Una ficha cambió; revisa la selección."}
        val intent=requireNotNull(db.automaticSyncDao().get(item.key)){"Falta la entrega confirmada."}
        require(intent.state=="SYNCED"&&intent.revision==item.revision&&intent.syncedRevision==item.revision){"Hay cambios o una entrega pendiente; registro protegido."}
        val prepared=JSONObject(requireNotNull(intent.preparedJson));val ack=JSONObject(requireNotNull(intent.receiptJson))
        require(AutomaticDelivery.validAck(prepared,ack)&&prepared.getString("key")==item.key&&prepared.getLong("revision")==item.revision){"Falta un ACK íntegro de la revisión actual."}
        val jobs=db.localFilesDao().jobs().filter{it.recordKey==item.key}
        require(jobs.none{it.state in setOf("QUEUED","RUNNING")}){"Hay una publicación local en curso; espera antes de limpiar."}
        val full=requireNotNull(jobs.filter{it.revision==item.revision&&it.scope.startsWith("FULL")&&it.state in setOf("READY","CLEANED")&&it.manifestJson!=null}.maxByOrNull{it.createdAt}){"Genera primero una copia documental completa de la revisión actual."}
        require(LocalPlan.parse(requireNotNull(full.manifestJson)).problems.isEmpty()){"La copia local está incompleta; registro protegido."}
        for(file in db.localFilesDao().filesForRecord(item.key).filter{it.state=="READY"&&it.uri!=null}) {
            require(db.localFilesDao().otherFileReferences(item.key,requireNotNull(file.uri),file.relativePath,file.name)==0){
                "Esta copia está referenciada por otra ficha; se conserva el archivo compartido."
            }
        }
        if(kind=="SCAP")require(VideoDatabase.get(c).videos().all().filter{it.parentKey==item.key&&!it.retired}.all{VideoNames.confirmedRelativePath(c,it)!=null}){"Hay videos sin confirmación propia; registro protegido."}
        return ack to full
    }
    suspend fun prepare(c:Context,db:InventoryDatabase,items:List<Candidate>,storage:SharedDocumentStore=AndroidSharedDocumentStore(c)):Backup=withContext(Dispatchers.IO){
        require(items.isNotEmpty()&&items.distinctBy{it.key}.size==items.size){"Selecciona registros entregados distintos."}
        val sources=mutableListOf<VerifiedZipArchive.Source>();val public=mutableListOf<LocalSharedFile>();val records=JSONArray()
        for((n,item)in items.withIndex()){
            val (ack,job)=db.withTransaction{validate(c,db,item)}
            val plan=LocalPlan.parse(requireNotNull(job.manifestJson))
            records.put(JSONObject().put("key",item.key).put("revision",item.revision).put("ack",ack).put("documentaryPlan",JSONObject(job.manifestJson)))
            val copies=db.localFilesDao().filesForRecord(item.key).filter{it.state in setOf("READY","CLEANED")&&it.uri!=null}
            val prior=db.localFilesDao().choice(PREFIX+item.key)?.destination?.let(::JSONObject)
            val interrupted=prior?.takeIf{it.optString("state")=="CLEANING"&&it.optLong("revision")==item.revision}
            val priorVerified=interrupted?.let{j->runCatching{streamDigest(requireNotNull(c.contentResolver.openInputStream(Uri.parse(j.getString("backupUri")))))==(j.getLong("backupBytes") to j.getString("backupSha256"))}.getOrDefault(false)}==true
            for(f in copies.filter{it.state=="READY"}){
                val digest=runCatching{storage.digest(Uri.parse(f.uri))}
                val frozen=interrupted?.optJSONArray("files")?.let{a->(0 until a.length()).map{a.getJSONObject(it)}.any{it.optString("id")==f.id&&it.optString("uri")==f.uri&&it.optString("sha256")==f.sha256&&it.optLong("size")==f.size}}==true
                require(digest.getOrNull()==(f.size to f.sha256)||digest.isFailure&&priorVerified&&frozen){"Una copia pública cambió o falta sin un respaldo de limpieza verificable."}
            }
            public+=copies
            val all=(plan.files+copies).distinctBy{it.source to it.sha256}
            for((i,f)in all.withIndex()){
                val file=File(f.source);require(fileDigest(file)==(f.size to f.sha256)){"Falta un original del respaldo; no se limpiará."}
                sources+=VerifiedZipArchive.Source("REGISTROS/$n/$i/${f.name}",f.size,f.sha256){file.inputStream()}
            }
        }
        val metadata=JSONObject().put("format","GBO_CLEANUP_BACKUP_V1").put("records",records).put("originalsRetainedInGbo",true).toString().toByteArray()
        sources+=VerifiedZipArchive.Source("RESPALDO.json",metadata.size.toLong(),com.tuempresa.inventariovial.delivery.sha256(metadata)){metadata.inputStream()}
        val zip=File(c.cacheDir,"cleanup-${UUID.randomUUID()}.zip")
        val result=VerifiedZipArchive.write(zip,sources)
        Backup(items,public.distinctBy{it.id},zip,result.bytes,result.sha256)
    }
    /** The external backup is read back before any removal; every file also rechecks the live revision. */
    suspend fun clean(c:Context,db:InventoryDatabase,backup:Backup,uri:Uri,userId:String,storage:SharedDocumentStore=AndroidSharedDocumentStore(c)):Int=withContext(Dispatchers.IO){
        require(backup.files.none{it.uri==uri.toString()}){"El respaldo debe ser un documento nuevo."}
        require(streamDigest(requireNotNull(c.contentResolver.openInputStream(uri)))==(backup.bytes to backup.sha256)){"El respaldo externo no se verificó; no se limpió ningún archivo."}
        for(item in backup.candidates)db.withTransaction{
            validate(c,db,item)
            db.localFilesDao().saveChoice(DeviceDeliveryChoice(PREFIX+item.key,JSONObject().put("revision",item.revision).put("state","CLEANING").put("backupUri",uri.toString()).put("backupSha256",backup.sha256).put("backupBytes",backup.bytes)
                .put("files",JSONArray(backup.files.filter{it.recordKey==item.key}.map{JSONObject().put("id",it.id).put("uri",it.uri).put("sha256",it.sha256).put("size",it.size)})).toString(),userId,System.currentTimeMillis()))
        }
        var removed=0
        for(file in backup.files)db.withTransaction {
            val item=backup.candidates.single{it.key==file.recordKey};validate(c,db,item)
            val current=requireNotNull(db.localFilesDao().files(file.jobId).find{it.id==file.id})
            require(current.uri==file.uri&&current.sha256==file.sha256&&current.size==file.size){"La copia cambió durante la limpieza."}
            if(current.state=="READY"){if(storage.removeOwned(current))removed++;db.localFilesDao().saveFile(current.copy(state="CLEANED"))}
        }
        db.withTransaction{
            for(item in backup.candidates){
                validate(c,db,item)
                val choice=requireNotNull(db.localFilesDao().choice(PREFIX+item.key));db.localFilesDao().saveChoice(choice.copy(destination=JSONObject(choice.destination).put("state","ARCHIVED").toString()))
                db.localFilesDao().jobs().filter{it.recordKey==item.key&&it.state=="READY"}.forEach{db.localFilesDao().saveJob(it.copy(state="CLEANED",message="Copia retirada tras verificar respaldo; originales e historial conservados."))}
            }
        }
        removed
    }
    suspend fun restore(db:InventoryDatabase,key:String)=db.withTransaction{
        val old=requireNotNull(db.localFilesDao().choice(PREFIX+key));val journal=JSONObject(old.destination)
        require(journal.getString("state") in setOf("ARCHIVED","CLEANING")){"El registro no está archivado ni tiene una limpieza interrumpida."}
        // Restore from our retained originals, never trust a user-selected ZIP to overwrite Room or media.
        val jobs=db.localFilesDao().jobs().filter{it.recordKey==key&&it.revision==journal.getLong("revision")&&it.manifestJson!=null}
        require(jobs.isNotEmpty()){"No se encontró el historial documental; conserva el ZIP y solicita recuperación asistida."}
        for(file in jobs.flatMap{LocalPlan.parse(requireNotNull(it.manifestJson)).files}.distinctBy{it.source})
            require(fileDigest(File(file.source))==(file.size to file.sha256)){"Un original no se verificó; conserva el respaldo externo y no continúes."}
        db.localFilesDao().saveChoice(old.copy(destination=journal.put("state","RESTORED").toString(),selectedAt=System.currentTimeMillis()))
    }
}
