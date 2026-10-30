package com.tuempresa.inventariovial.localfiles

import com.tuempresa.inventariovial.core.AutomaticDelivery
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.delivery.sha256
import org.json.JSONObject
import java.io.File
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.tuempresa.inventariovial.media.VideoDatabase
import com.tuempresa.inventariovial.media.VideoNames

/** Copies only exact prepared bytes accredited by the existing full ACK validator. No HTTP. */
object DriveMirrorPlan {
    const val ROOT="Download/GBO Inventario Vial/"
    const val SCOPE="DRIVE_MIRROR_V3"
    fun relative(path:String):String {
        require(path.isNotBlank() && path.split('/').all{it.isNotBlank() && it !in setOf(".","..") &&
            it.none{c->c=='\\' || c.code<32 || c in "<>:\"|?*"}}){"La ruta confirmada no es publicable de forma segura en Android."}
        return ROOT+path
    }
    fun build(job:LocalExportJob,prepared:JSONObject,ack:JSONObject):LocalPlan {
        require(prepared.getString("key")==job.recordKey && prepared.getLong("revision")==job.revision &&
            AutomaticDelivery.validAck(prepared,ack)){"Falta una confirmación completa para esta revisión. La copia provisional se conserva."}
        val receipts=ack.getJSONArray("files")
        val files=prepared.getJSONArray("files")
        val entries=(0 until files.length()).map {n->
            val f=files.getJSONObject(n);val key=f.getString("key")
            val r=(0 until receipts.length()).map{receipts.getJSONObject(it)}.single{it.getString("key")==key}
            val path=relative(r.getString("logicalPath"));val source=File(f.getString("localFile"))
            val digest=fileDigest(source)
            require(digest.second==r.getString("sha256")){"La preparación cambió; no se publicó como copia de Drive."}
            LocalSharedFile(sha256((job.id+"/"+key).toByteArray()),job.id,job.recordKey,
                path.substringBeforeLast('/')+"/",path.substringAfterLast('/'),f.getString("mimeType"),digest.first,digest.second,source.path)
        }
        return LocalPlan(entries,listOfNotNull(
            if(ack.optJSONArray("sicFiles")?.length()?.let{it>0}==true)
                "El consolidado EXCEL_SIC del servidor contiene otras tablets: no se descargó ni se sustituyó por el Excel de una ficha." else null,
            if(job.kind=="SCAP") "Los videos se reconcilian solo con su propia confirmación; el recibo padre no acredita su nombre remoto." else null),3)
    }
    fun preservePrevious(plan:LocalPlan,existing:List<LocalSharedFile>,revision:Long):LocalPlan {
        val conflict=plan.files.any{f->existing.any{old->old.state=="READY" && old.relativePath==f.relativePath && old.name==f.name && old.sha256!=f.sha256}}
        if(!conflict)return plan
        return plan.copy(files=plan.files.map{f->f.copy(relativePath=ROOT+"_REVISIONES_CONFIRMADAS/R_$revision/"+f.relativePath.removePrefix(ROOT))},
            problems=plan.problems+"La copia anterior se conserva. La revisión nueva se publica y verifica por separado en _REVISIONES_CONFIRMADAS; no se trasladaron ni sobrescribieron carpetas.")
    }
    suspend fun request(c:android.content.Context,db:InventoryDatabase,kind:String,id:String)=withContext(Dispatchers.IO) {
        val i=requireNotNull(db.automaticSyncDao().get("$kind/$id")) {"Esta ficha aún no tiene entrega Drive."}
        require(i.syncedRevision==i.revision && i.state=="SYNCED") {"Espera la entrega confirmada de la revisión actual."}
        require(DeliveryDestination.revision(db,kind,id)==i.revision){"La ficha tiene cambios locales posteriores; se conserva su copia provisional."}
        val job=LocalExportJob(sha256((i.key+"/"+i.revision+"/"+SCOPE).toByteArray()),i.key,kind,id,i.revision,SCOPE)
        val ack=JSONObject(requireNotNull(i.receiptJson))
        var generated=build(job,JSONObject(requireNotNull(i.preparedJson)),ack)
        if(kind=="SCAP") {
            val videos=VideoDatabase.get(c).videos().all().filter{it.parentKey==i.key && !it.retired}
            val confirmed=videos.mapNotNull{v->
                val name=VideoNames.confirmedRelativePath(c,v) ?: return@mapNotNull null
                val path=relative(ack.getString("logicalPath")+"/"+name);val source=File(v.localPath)
                require(fileDigest(source)==(v.sizeBytes to v.sha256)){"El original de video cambió; se conserva sin publicar otra identidad."}
                LocalSharedFile(sha256((job.id+"/video/"+v.id).toByteArray()),job.id,i.key,path.substringBeforeLast('/')+"/",path.substringAfterLast('/'),v.mimeType,v.sizeBytes,v.sha256,source.path)
            }
            generated=generated.copy(files=generated.files+confirmed,problems=generated.problems.filterNot{it.startsWith("Los videos se reconcilian") }+
                if(confirmed.size<videos.size)listOf("Hay videos pendientes o sin nombre remoto acreditado; sus copias provisionales se conservan.")else emptyList())
        }
        val plan=preservePrevious(generated,db.localFilesDao().filesForRecord(i.key),i.revision)
        db.withTransaction {
            val old=db.localFilesDao().job(job.id)
            if(old==null)db.localFilesDao().saveJob(job.copy(manifestJson=plan.json()))
            else if(old.state !in setOf("QUEUED","RUNNING"))db.localFilesDao().saveJob(old.copy(state="QUEUED"))
        }
        LocalCopies.wake(c)
    }
}
