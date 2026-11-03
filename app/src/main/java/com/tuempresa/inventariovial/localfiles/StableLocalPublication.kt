package com.tuempresa.inventariovial.localfiles

import android.content.Context
import android.net.Uri
import android.util.AtomicFile
import androidx.room.withTransaction
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.delivery.sha256
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Local publication only. No ACK, delivery, historical-directory migration or destructive cleanup.
 * Each name has a durable private journal and immutable bytes. A verified pending copy is promoted
 * by rename; only a previous copy created by THIS publisher may be renamed into controlled history.
 * Old public layouts and unknown/externally edited destinations are never overwritten or adopted.
 */
object StableLocalPublication {
    private val mutex=Mutex()
    private fun read(file:File):JSONObject? = if(file.exists()||File(file.path+".bak").exists())
        JSONObject(AtomicFile(file).openRead().use{it.readBytes().toString(Charsets.UTF_8)}) else null
    private fun atomic(file:File,write:(java.io.OutputStream)->Unit) {
        check(file.parentFile!!.isDirectory||file.parentFile!!.mkdirs())
        val a=AtomicFile(file);val out=a.startWrite()
        try{write(out);a.finishWrite(out)}catch(e:Exception){a.failWrite(out);throw e}
    }
    private fun save(file:File,j:JSONObject)=atomic(file){it.write(j.toString().toByteArray())}
    private fun encode(file:LocalSharedFile)=JSONObject(LocalPlan(listOf(file),emptyList()).json())
        .getJSONArray("files").getJSONObject(0).put("uri",file.uri ?: JSONObject.NULL)
    private fun decode(j:JSONObject):LocalSharedFile=LocalPlan.parse(JSONObject().put("files",JSONArray().put(j))
        .put("problems",JSONArray()).toString()).files.single().copy(uri=j.optString("uri").takeIf{it.isNotBlank()&&it!="null"})
    private fun historyName(file:LocalSharedFile):String {
        val stem=PublicLocalFolderNames.component(file.name.substringBeforeLast('.',file.name)).take(24)
        val ext=file.name.substringAfterLast('.',"").takeIf{it.isNotBlank()}?.let{".$it"}.orEmpty()
        return ".Historial - $stem - ${file.sha256}-${sha256((file.uri ?: file.id).toByteArray()).take(8)}$ext"
    }
    private fun freeze(dir:File,file:LocalSharedFile):LocalSharedFile {
        require(file.name.isNotBlank() && file.name !in setOf(".","..") && file.name.none{it=='/'||it=='\\'||it.code<32})
        require(file.sha256.matches(Regex("[a-f0-9]{64}")))
        val frozen=File(dir,"versions/${file.sha256}/${file.name}")
        if(!frozen.isFile)atomic(frozen){out->File(file.source).inputStream().use{it.copyTo(out)}}
        require(fileDigest(frozen)==(file.size to file.sha256)){"El historial privado no coincide; se conserva la copia pública."}
        return file.copy(source=frozen.path)
    }
    /** Recreate a missing owned copy from verified private bytes, never overwrite a published edit. */
    private suspend fun verifiedCopy(db:InventoryDatabase,storage:SharedDocumentStore,file:LocalSharedFile):LocalSharedFile {
        require(fileDigest(File(file.source))==(file.size to file.sha256))
        val slot=storage.reserve(file,file.uri?.let(Uri::parse));storage.checkSlot(slot,file)
        if(!slot.published){storage.write(slot.uri,File(file.source));require(storage.digest(slot.uri)==(file.size to file.sha256))}
        val uri=storage.publish(slot,file)
        require(storage.digest(uri)==(file.size to file.sha256)){"La copia fue modificada fuera de GBO; no se sobrescribió."}
        if(file.uri!=null && uri.toString()!=file.uri)db.withTransaction {
            db.localFilesDao().filesForRecord(file.recordKey).filter{
                it.uri==file.uri&&it.relativePath==file.relativePath&&it.name==file.name&&it.sha256==file.sha256
            }.forEach{db.localFilesDao().saveFile(it.copy(uri=uri.toString()))}
        }
        return file.copy(uri=uri.toString())
    }
    private suspend fun recover(dir:File,db:InventoryDatabase,storage:SharedDocumentStore) {
        val journalFile=File(dir,"journal.json");val j=read(journalFile) ?: return
        if(j.getString("phase")=="DONE")return
        val next=j.getJSONObject("next");val target=decode(next.getJSONObject("file"))
        require(fileDigest(File(target.source))==(target.size to target.sha256)){"Preparación local dañada; versiones anteriores conservadas."}
        val staging=target.copy(name=".Preparacion - ${target.sha256.take(16)} - ${target.name}")
        if(j.getString("phase")=="PREPARED") {
            val slot=storage.reserve(staging,j.optString("stageUri").takeIf{it.isNotBlank()}?.let(Uri::parse))
            j.put("stageUri",slot.uri.toString());save(journalFile,j)
            storage.checkSlot(slot,staging)
            if(!slot.published){
                storage.write(slot.uri,File(target.source))
                require(storage.digest(slot.uri)==(target.size to target.sha256)){"Escritura incompleta; versión anterior conservada."}
            }
            val uri=storage.publish(slot,staging)
            require(storage.digest(uri)==(target.size to target.sha256)){"No se verificó la preparación; versión anterior conservada."}
            j.put("stageUri",uri.toString()).put("phase","ARCHIVE");save(journalFile,j)
        }
        if(j.getString("phase")=="ARCHIVE") {
            j.optJSONObject("previous")?.let{previous->
                val old=decode(previous.getJSONObject("file"))
                // This descriptor was recorded by this publisher, never inferred from a legacy URI.
                require(old.recordKey==target.recordKey)
                require(fileDigest(File(old.source))==(old.size to old.sha256)){"Falta el historial privado anterior; no se sustituyó."}
                require(db.localFilesDao().otherFileReferences(old.recordKey,requireNotNull(old.uri),old.relativePath,old.name)==0){"La copia está compartida; no se cambió su nombre."}
                previous.put("file",encode(verifiedCopy(db,storage,old)))
            }
            // Persist the exact source URI BEFORE a rename that may change it or lose its result.
            j.put("phase","ARCHIVE_RENAME");save(journalFile,j)
        }
        if(j.getString("phase")=="ARCHIVE_RENAME") {
            j.optJSONObject("previous")?.let{previous->
                val old=decode(previous.getJSONObject("file"))
                require(db.localFilesDao().otherFileReferences(old.recordKey,requireNotNull(old.uri),old.relativePath,old.name)==0){"La copia está compartida; no se cambió su nombre."}
                val archived=old.copy(name=historyName(old))
                val slot=storage.renameOwned(SharedDocumentStore.Slot(Uri.parse(old.uri),true),old,archived)
                require(storage.digest(slot.uri)==(old.size to old.sha256))
                db.withTransaction {
                    db.localFilesDao().filesForRecord(old.recordKey).filter{
                        it.uri==old.uri && it.relativePath==old.relativePath && it.name==old.name && it.sha256==old.sha256
                    }.forEach{db.localFilesDao().saveFile(it.copy(name=archived.name,uri=slot.uri.toString()))}
                }
                save(File(dir,"history/${old.sha256}-${sha256(requireNotNull(old.uri).toByteArray()).take(8)}.json"),previous.put("file",encode(archived.copy(uri=slot.uri.toString()))))
            }
            j.put("phase","PROMOTE");save(journalFile,j)
        }
        // renameOwned reconciles a lost rename response (including SAF documentId changes).
        val final=storage.renameOwned(SharedDocumentStore.Slot(Uri.parse(j.getString("stageUri")),true),staging,target)
        storage.checkSlot(final,target)
        require(storage.digest(final.uri)==(target.size to target.sha256)){"No se verificó la copia actual; historial conservado."}
        next.put("file",encode(target.copy(uri=final.uri.toString())))
        save(File(dir,"current.json"),next)
        db.localFilesDao().saveFile(target.copy(uri=final.uri.toString(),state="READY"))
        j.put("phase","DONE");save(journalFile,j)
    }
    suspend fun publish(c:Context,db:InventoryDatabase,job:LocalExportJob,input:LocalSharedFile,storage:SharedDocumentStore):LocalSharedFile=mutex.withLock {
        require(input.recordKey==job.recordKey && input.jobId==job.id)
        // A completed document replaces its provisional/draft presentation in the SAME slot.
        // Its previous label and bytes remain in history instead of appearing as another current file.
        val documentName=input.name.replace(Regex("_(PROVISIONAL|BORRADOR)(?=\\.[^.]+$)"),"")
        val dir=File(c.filesDir,"local_publications_v5/"+sha256((input.relativePath+"\u0000"+documentName).toByteArray()))
        check(dir.isDirectory||dir.mkdirs())
        recover(dir,db,storage)
        val current=read(File(dir,"current.json"))
        val old=current?.getJSONObject("file")?.let(::decode)
        require(old==null||old.recordKey==input.recordKey){"Ese destino pertenece a otra ficha; no se sobrescribió."}
        val frozen=freeze(dir,input)
        // Reopening/retrying an older job must never replace a more recent visible document.
        val older=current!=null && (job.revision<current.getLong("revision") ||
            (job.revision==current.getLong("revision")&&job.createdAt<current.getLong("createdAt")))
        if(old!=null && old.name==frozen.name && old.sha256==frozen.sha256) {
            val verified=verifiedCopy(db,storage,old)
            if(!older)save(File(dir,"current.json"),JSONObject().put("file",encode(frozen.copy(uri=verified.uri)))
                .put("revision",job.revision).put("createdAt",job.createdAt))
            return@withLock frozen.copy(uri=verified.uri,state="READY")
        }
        if(older) {
            val archived=frozen.copy(name=historyName(frozen))
            val slot=storage.reserve(archived);storage.checkSlot(slot,archived)
            if(!slot.published){storage.write(slot.uri,File(archived.source));require(storage.digest(slot.uri)==(archived.size to archived.sha256))}
            val uri=storage.publish(slot,archived)
            require(storage.digest(uri)==(archived.size to archived.sha256))
            return@withLock archived.copy(uri=uri.toString(),state="READY")
        }
        val next=JSONObject().put("file",encode(frozen)).put("revision",job.revision).put("createdAt",job.createdAt)
        save(File(dir,"journal.json"),JSONObject().put("phase","PREPARED").put("next",next).put("previous",current ?: JSONObject.NULL))
        recover(dir,db,storage)
        decode(requireNotNull(read(File(dir,"current.json"))).getJSONObject("file")).copy(state="READY")
    }
}
