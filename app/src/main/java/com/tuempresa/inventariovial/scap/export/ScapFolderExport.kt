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
    suspend fun remembered(db:InventoryDatabase,id:String):Uri?=(db.localFilesDao().choice("SCAP_EXPORT_TREE/"+id) ?: db.localFilesDao().choice("SCAP_EXPORT_AUTHORIZED_BASE"))?.destination?.let(Uri::parse)
    suspend fun folderUri(c:Context,db:InventoryDatabase,id:String):Uri? {
        val tree=remembered(db,id) ?: return null
        val s=requireNotNull(db.scapDao().snapshot(id));val parent=DriveMirrorPlan.ROOT+ScapDocumentNames.route(s)+"/PUENTES/"
        val name=PublicLocalFolderNames.reserve(db,"SCAP/$id",parent,ScapDocumentNames.folder(s),c)
        return AndroidSharedDocumentStore(c,tree).directory(parent+name+"/")
    }
    suspend fun save(c:Context,db:InventoryDatabase,s:ScapInspectionSnapshot,tree:Uri,excelName:String=ScapDocumentNames.excel(s)):List<LocalSharedFile> = withContext(Dispatchers.IO) {
        val latest=requireNotNull(db.scapDao().snapshot(s.inspection.id))
        require(latest==s){"La ficha cambió; vuelve a exportar su versión guardada."}
        ScapDocumentNames.folder(latest)
        require(tree.scheme=="content")
        c.contentResolver.takePersistableUriPermission(tree,Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        db.localFilesDao().saveChoice(DeviceDeliveryChoice("SCAP_EXPORT_TREE/"+s.inspection.id,tree.toString(),"SCAP_SAF_TREE",System.currentTimeMillis()))
        db.localFilesDao().saveChoice(DeviceDeliveryChoice("SCAP_EXPORT_AUTHORIZED_BASE",tree.toString(),"SCAP_SAF_BASE",System.currentTimeMillis()))
        val storage=AndroidSharedDocumentStore(c,tree)
        val id=sha256(("SCAP_FOLDER/"+tree+"/"+s.inspection.id+"/"+s.inspection.updatedAt+"/"+excelName).toByteArray())
        val existing=db.localFilesDao().job(id)
        val job=existing ?: LocalExportJob(id,"SCAP/${s.inspection.id}","SCAP",s.inspection.id,s.inspection.updatedAt,scope="SAF_TREE")
        val plan=existing?.manifestJson?.let(LocalPlan::parse) ?: LocalExportBuilder.build(c,db,job)
        require(excelName.endsWith(".xlsx",true)&&excelName.none{it=='/'||it=='\\'||it.code<32})
        val public=plan.files.filter{it.mimeType!= "application/json"&&RecordDocuments.sicFormat(it)==null}
            .map{if(it.name==ScapDocumentNames.excel(s))it.copy(name=excelName)else it}
        require(public.any{it.name==excelName}){"No se preparó el Excel SCAP. Los originales permanecen en GBO."}
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
