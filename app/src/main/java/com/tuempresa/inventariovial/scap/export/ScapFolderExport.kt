package com.tuempresa.inventariovial.scap.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.delivery.sha256
import com.tuempresa.inventariovial.localfiles.*
import com.tuempresa.inventariovial.scap.data.ScapInspectionSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Explicit SAF tree export. The destination never becomes the automatic-copy preference. */
object ScapFolderExport {
    suspend fun save(c:Context,db:InventoryDatabase,s:ScapInspectionSnapshot,tree:Uri):List<LocalSharedFile> = withContext(Dispatchers.IO) {
        require(tree.scheme=="content")
        c.contentResolver.takePersistableUriPermission(tree,Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        val storage=AndroidSharedDocumentStore(c,tree)
        val id=sha256(("SCAP_FOLDER/"+tree+"/"+s.inspection.id+"/"+s.inspection.updatedAt).toByteArray())
        val existing=db.localFilesDao().job(id)
        val job=existing ?: LocalExportJob(id,"SCAP/${s.inspection.id}","SCAP",s.inspection.id,s.inspection.updatedAt,scope="SAF_TREE")
        val plan=existing?.manifestJson?.let(LocalPlan::parse) ?: LocalExportBuilder.build(c,db,job)
        val public=plan.files.filter{it.mimeType!= "application/json"&&RecordDocuments.sicFormat(it)==null}
        require(public.any{it.name==ScapDocumentNames.excel(s)}){"No se preparó el Excel SCAP. Los originales permanecen en GBO."}
        // This uses the same verified, journaled publication contract as automatic local copies,
        // isolated by tree URI so choosing a second folder cannot retarget an existing publication.
        db.localFilesDao().saveJob(job.copy(state="RUNNING",manifestJson=plan.json(),total=public.size))
        val saved=mutableListOf<LocalSharedFile>()
        try {
            for(file in public) {
                require(fileDigest(File(file.source))==(file.size to file.sha256))
                val result=StableLocalPublication.publish(c,db,job,file,storage,namespace=tree.toString())
                db.localFilesDao().saveFile(result);saved+=result
                db.localFilesDao().saveJob(job.copy(state="RUNNING",manifestJson=plan.json(),total=public.size,completed=saved.size))
            }
            require(plan.problems.isEmpty()){plan.problems.joinToString(" · ")}
            db.localFilesDao().saveJob(job.copy(state="READY",manifestJson=plan.json(),total=public.size,completed=saved.size,
                message="Carpeta del puente exportada y verificada. ${saved.size} archivos; no confirma Drive."))
            saved
        }catch(e:Exception){
            db.localFilesDao().saveJob(job.copy(state="PARTIAL",manifestJson=plan.json(),total=public.size,completed=saved.size,
                message="Carpeta parcial: ${LocalCopyDrain.publicError(e)}. Originales e historial conservados."))
            throw e
        }
    }
}
