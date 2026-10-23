package com.tuempresa.inventariovial.localfiles

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import androidx.work.*
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.delivery.sha256
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.concurrent.TimeUnit

object LocalCopySettings {
    fun enabled(c:Context)=c.getSharedPreferences("gbo_local_copies",Context.MODE_PRIVATE).getBoolean("automatic",true)
    suspend fun enabled(c:Context,value:Boolean){
        withContext(Dispatchers.IO){
            val db=InventoryDatabase.getInstance(c)
            db.withTransaction{db.localFilesDao().saveChoice(DeviceDeliveryChoice("LOCAL_AUTOMATIC",if(value)"ON"else"OFF","DEVICE_SETTING",System.currentTimeMillis()))}
            check(c.getSharedPreferences("gbo_local_copies",Context.MODE_PRIVATE).edit().putBoolean("automatic",value).commit())
        }
    }
}
object LocalCopies {
    /** Runs in the existing closure transaction: a crash cannot lose a committed local request. */
    suspend fun onClosed(db:InventoryDatabase,kind:String,id:String,revision:Long,origin:String){
        val c=db.syncContext ?: return
        if(origin=="AUTOMATIC" && (LocalCopySettings.enabled(c)||DeliveryDestination.deviceOnly(db,"$kind/$id")))request(c,db,kind,id,revision,automatic=true)
    }
    suspend fun request(c:Context,db:InventoryDatabase,kind:String,id:String,revision:Long,scope:String="FULL",automatic:Boolean=false):LocalExportJob = db.withTransaction {
        val key="$kind/$id";val jobId=sha256((key+"/"+revision+"/"+scope).toByteArray())
        val dao=db.localFilesDao();val old=dao.job(jobId) ?: dao.jobForRevision(key,revision,scope)
        if(!automatic&&scope in setOf("FULL","PHOTOS","ZIP")&&old?.manifestJson!=null&&old.state !in setOf("QUEUED","RUNNING")&&
            LocalPlan.parse(old.manifestJson).publicNamingVersion<PublicLocalFolderNames.VERSION) {
            // Explicit regeneration only: retain the historical job, URIs, bytes and frozen paths.
            return@withTransaction request(c,db,kind,id,revision,scope+"_READABLE_V2",automatic=false)
        }
        val job=old ?: LocalExportJob(jobId,key,kind,id,revision,scope,automatic=automatic)
        if(old==null)dao.saveJob(job)
        else if(!automatic&&old.state !in setOf("QUEUED","RUNNING"))dao.saveJob(old.copy(state="QUEUED",message="Revisando la copia local…"))
        wake(c);job
    }
    suspend fun requestCurrent(c:Context,db:InventoryDatabase,kind:String,id:String,scope:String="FULL",automatic:Boolean=false){
        if(kind=="SIC" && db.inventoryDao().recordById(id)?.sicCode=="SCAP")return
        val revision=requireNotNull(DeliveryDestination.revision(db,kind,id)){"Guarda primero la ficha dentro de GBO."}
        // Provisional local documents deliberately do not require closure, PR, Internet or ACK.
        db.withTransaction{request(c,db,kind,id,revision,scope,automatic=automatic)}
    }
    suspend fun retry(c:Context,db:InventoryDatabase,id:String)=db.withTransaction {
        db.localFilesDao().job(id)?.let{
            if(it.state in setOf("QUEUED","RUNNING"))return@withTransaction
            val current=DeliveryDestination.revision(db,it.kind,it.recordId)
            if(it.manifestJson==null && current!=null && current!=it.revision)request(c,db,it.kind,it.recordId,current,it.scope)
            else db.localFilesDao().saveJob(it.copy(state="QUEUED",message="Reintentando copia local…"))
            wake(c)
        };Unit
    }
    /** Manual prepared document: freeze its bytes outside cache before scheduling. */
    suspend fun document(c:Context,db:InventoryDatabase,file:File,kind:String,mime:String,route:String="EXPORTACIONES_MANUALES")=withContext(Dispatchers.IO){
        val digest=fileDigest(file);require(digest.first>0);LocalExportBuilder.space(c,digest.first)
        val id=sha256((kind+"/"+file.name+"/"+digest.second).toByteArray());val key="$kind/EXPORT_$id"
        val job=LocalExportJob(id,key,kind,"EXPORT_$id",file.lastModified(),scope="DOCUMENT")
        val target=File(LocalExportBuilder.work(c,job),LocalExportNames.safe(file.name));if(!target.isFile){
            val a=android.util.AtomicFile(target);val out=a.startWrite();try{file.inputStream().use{it.copyTo(out,64*1024)};a.finishWrite(out)}catch(e:Exception){a.failWrite(out);throw e}
        }
        require(fileDigest(target)==digest){"El documento cambió durante la preparación."}
        val parent="Download/GBO_Ruta_Clara/${if(kind=="PAVEMENT")"PAVIMENTOS" else kind}/${LocalExportNames.safe(route)}/EXPORTACIONES_MANUALES/"
        val folder=PublicLocalFolderNames.reserve(db,key,parent,"Exportación - ${file.nameWithoutExtension}")
        val entry=LocalSharedFile(id,id,key,parent+folder+"/",file.name,mime,digest.first,digest.second,target.path)
        db.withTransaction{val old=db.localFilesDao().job(id);if(old==null)db.localFilesDao().saveJob(job.copy(manifestJson=LocalPlan(listOf(entry),emptyList(),PublicLocalFolderNames.VERSION).json()))else db.localFilesDao().saveJob(old.copy(state="QUEUED"))};wake(c)
    }
    suspend fun photo(c:Context,db:InventoryDatabase,file:File)=withContext(Dispatchers.IO){
        val digest=fileDigest(file);val id=sha256(file.canonicalPath.toByteArray());val key="PHOTO/$id";val rev=file.lastModified();val jobId=sha256((key+"/"+rev+"/"+digest.second).toByteArray())
        val mime=photoMime(file);val suffix=when(mime){"image/jpeg"->"jpg";"image/png"->"png";"image/webp"->"webp";"image/heic"->"heic";else->file.extension.ifBlank{"bin"}}
        val parent="Download/GBO_Ruta_Clara/FOTOGRAFIAS/INDIVIDUALES/"
        val folder=PublicLocalFolderNames.reserve(db,key,parent,"Fotografía individual")
        val job=LocalExportJob(jobId,key,"PHOTO",id,rev,scope="DOCUMENT");val entry=LocalSharedFile(jobId,jobId,key,parent+folder+"/R_${rev}_${digest.second.take(8)}/",LocalExportNames.safe(file.nameWithoutExtension)+".$suffix",mime,digest.first,digest.second,file.path)
        db.withTransaction{val old=db.localFilesDao().job(jobId);db.localFilesDao().saveJob((old?:job).copy(state="QUEUED",manifestJson=old?.manifestJson ?: LocalPlan(listOf(entry),emptyList(),PublicLocalFolderNames.VERSION).json()))};wake(c)
    }
    fun photoMime(file:File):String {
        val b=file.inputStream().use{val x=ByteArray(16);x.take(maxOf(0,it.read(x))).toByteArray()}
        return when{
            b.size>=2&&b[0]==0xff.toByte()&&b[1]==0xd8.toByte()->"image/jpeg"
            b.size>=4&&b[0]==0x89.toByte()&&String(b,1,3)=="PNG"->"image/png"
            b.size>=12&&String(b,0,4)=="RIFF"&&String(b,8,4)=="WEBP"->"image/webp"
            b.size>=12&&String(b,4,4)=="ftyp"->"image/heic"
            else->when(file.extension.lowercase()){ "jpg","jpeg"->"image/jpeg";"png"->"image/png";"webp"->"image/webp";"heic","heif"->"image/heic";else->"application/octet-stream"}
        }
    }
    suspend fun rememberSaf(c:Context,db:InventoryDatabase,source:File,kind:String,mime:String,uri:Uri)=withContext(Dispatchers.IO){
        require(AndroidSharedDocumentStore(c).digest(uri)==fileDigest(source)){"No se verificó el archivo guardado; conserva el original y reintenta."}
        runCatching{c.contentResolver.takePersistableUriPermission(uri,android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION)}
        val id=sha256(("SAF/"+uri).toByteArray());val job=LocalExportJob(id,"$kind/EXPORT_$id",kind,"EXPORT_$id",source.lastModified(),scope="SAF",state="READY",completed=1,total=1,message="Archivo verificado en la ubicación elegida con Android.")
        LocalExportBuilder.space(c,source.length());val copy=File(LocalExportBuilder.work(c,job),LocalExportNames.safe(source.name))
        val a=android.util.AtomicFile(copy);val out=a.startWrite();try{source.inputStream().use{it.copyTo(out)};a.finishWrite(out)}catch(e:Exception){a.failWrite(out);throw e}
        val digest=fileDigest(copy);val display=runCatching{c.contentResolver.query(uri,arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),null,null,null)?.use{if(it.moveToFirst())it.getString(0)else null}}.getOrNull() ?: source.name
        db.withTransaction{db.localFilesDao().saveJob(job);db.localFilesDao().saveFile(LocalSharedFile(id,id,job.recordKey,"Ubicación elegida con Android/",display,mime,digest.first,digest.second,copy.path,uri.toString(),"READY"))}
    }
    /** Existing records are queued only after the explicit UI estimate/confirmation. */
    suspend fun historyCandidates(db:InventoryDatabase):List<Triple<String,String,Long>> {
        val existing=db.localFilesDao().requestedRevisions().toSet()
        return buildList {
            db.inventoryDao().activeSnapshots().filter{it.record.sicCode!="SCAP"}.forEach{add(Triple("SIC",it.record.id,it.record.updatedAt))}
            db.scapDao().allSnapshots().filter{it.inspection.status=="COMPLETE"}.forEach{add(Triple("SCAP",it.inspection.id,it.inspection.updatedAt))}
            db.pavementDao().observeSamples().first().filter{it.status=="CLOSED"}.forEach{add(Triple("PAVEMENT",it.id,it.updatedAt))}
        }.filter{"${it.first}/${it.second}/${it.third}" !in existing}
    }
    fun wake(context:Context){
        runCatching {
            val manager=WorkManager.getInstance(context.applicationContext)
            manager.enqueueUniquePeriodicWork("gbo-local-copy-recovery",ExistingPeriodicWorkPolicy.KEEP,PeriodicWorkRequestBuilder<LocalCopyWorker>(15,TimeUnit.MINUTES).build())
            manager.enqueueUniqueWork("gbo-local-copy-signal",ExistingWorkPolicy.REPLACE,OneTimeWorkRequestBuilder<LocalCopySignalWorker>().setInitialDelay(5,TimeUnit.SECONDS).build())
        } // Intent remains in Room even when Android cannot schedule yet.
    }
}

object LocalCopyDrain {
    private val mutex=Mutex()
    suspend fun run(c:Context,db:InventoryDatabase,storage:SharedDocumentStore=AndroidSharedDocumentStore(c),limit:Int=3,
        prepare:suspend (Context,InventoryDatabase,LocalExportJob)->LocalPlan=LocalExportBuilder::build)=withContext(Dispatchers.IO){mutex.withLock {
        val dao=db.localFilesDao();val pending=db.withTransaction {dao.pending(limit)}
        for(start in pending){var job=start
            if(job.automatic && System.currentTimeMillis()-job.createdAt<AutomaticLocalDocuments.QUIET_MS)continue
            if(job.automatic && job.manifestJson==null && DeliveryDestination.revision(db,job.kind,job.recordId)!=job.revision){dao.saveJob(job.copy(state="SUPERSEDED",message="Agrupada en la revisión actual"));continue}
            try {
                job=job.copy(state="RUNNING",message="Preparando copia local…");dao.saveJob(job)
                val plan=job.manifestJson?.let(LocalPlan::parse) ?: prepare(c,db,job).also{
                    // The producer and consumer both enforce the revision before freezing a plan.
                    require(DeliveryDestination.revision(db,job.kind,job.recordId)==job.revision){"La ficha cambió durante la preparación. Reintenta la revisión actual."}
                    job=job.copy(manifestJson=it.json(),total=it.files.size);dao.saveJob(job)
                }
                require(plan.files.isNotEmpty()){"No hay archivos válidos para exportar."}
                LocalExportBuilder.space(c,plan.files.sumOf{it.size})
                var done=0;val failures=plan.problems.toMutableList()
                val priorFiles=dao.files(job.id).associateBy{it.id}
                for(source in plan.files){
                    try {
                        require(fileDigest(File(source.source))==(source.size to source.sha256)){"El archivo privado cambió. No se sobrescribió la revisión exportada."}
                        val stored=priorFiles[source.id]
                        val retained=stored?.state=="READY"&&stored.uri?.let{uri->runCatching{storage.digest(Uri.parse(uri))==(source.size to source.sha256)}.getOrDefault(false)}==true
                        val slot=if(retained)SharedDocumentStore.Slot(Uri.parse(stored!!.uri),true)else storage.reserve(source,stored?.uri?.let(Uri::parse))
                        dao.saveFile(source.copy(uri=slot.uri.toString(),state="WRITING"))
                        storage.checkSlot(slot,source)
                        if(slot.published)require(storage.digest(slot.uri)==(source.size to source.sha256)){"Existe una copia distinta de esa revisión. No se sobrescribió."}
                        else {storage.write(slot.uri,File(source.source));require(storage.digest(slot.uri)==(source.size to source.sha256)){"La escritura quedó incompleta; reintenta."}}
                        val published=storage.publish(slot,source);require(storage.digest(published)==(source.size to source.sha256)){"No se pudo verificar el documento publicado."}
                        dao.saveFile(source.copy(uri=published.toString(),state="READY"));done++
                    }catch(e:CancellationException){throw e}catch(e:Exception){failures+="${source.name}: ${publicError(e)}"}
                    dao.saveJob(job.copy(completed=done,total=plan.files.size,message="Copia local: $done/${plan.files.size} archivos verificados"))
                }
                dao.saveJob(job.copy(state=if(failures.isEmpty())"READY"else"PARTIAL",completed=done,total=plan.files.size,
                    message=if(failures.isEmpty())"Copia local completada: $done archivos verificados en Descargas."else "Copia local parcial ($done/${plan.files.size}). "+failures.distinct().joinToString(" · ")))
            }catch(e:CancellationException){throw e}catch(e:Exception){dao.saveJob(job.copy(state="FAILED",message="Copia local fallida: ${publicError(e)}. La ficha y los originales se conservan; puedes reintentar."))}
        }
        dao.pendingCount()>0
    }}
    fun publicError(e:Exception):String {
        if(e is SecurityException)return "Android denegó el acceso; autoriza de nuevo el destino"
        if(e is java.io.IOException)return "no se pudo leer o escribir; comprueba espacio y acceso"
        return e.message?.takeIf{!it.contains("content://")&&!it.contains("/data/")&&!it.contains("http") }?.take(300) ?: "no se pudo completar la copia"
    }
}
/** Replacing only this tiny delayed signal never cancels a running export. Append closes the finish/enqueue race. */
class LocalCopySignalWorker(context:Context,parameters:WorkerParameters):CoroutineWorker(context,parameters){
    override suspend fun doWork():Result {
        WorkManager.getInstance(applicationContext).enqueueUniqueWork("gbo-local-copy-drain",ExistingWorkPolicy.APPEND_OR_REPLACE,
            OneTimeWorkRequestBuilder<LocalCopyWorker>().setBackoffCriteria(BackoffPolicy.LINEAR,10,TimeUnit.SECONDS).build()).result.get()
        return Result.success()
    }
}
class LocalCopyWorker(context:Context,parameters:WorkerParameters):CoroutineWorker(context,parameters){
    override suspend fun doWork():Result {
        try {VideoDocumentOutbox.transfer(applicationContext)}catch(e:CancellationException){throw e}catch(e:Exception){return Result.retry()}
        return if(LocalCopyDrain.run(applicationContext,InventoryDatabase.getInstance(applicationContext)))Result.retry()else Result.success()
    }
}
