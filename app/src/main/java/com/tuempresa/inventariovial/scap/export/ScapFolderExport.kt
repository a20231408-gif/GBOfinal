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
    private const val VERIFIED_BASE="SCAP_EXPORT_VERIFIED_BASE_V1"
    // Legacy global selections may be an individual bridge folder. Never promote them to a shared base.
    suspend fun remembered(db:InventoryDatabase,id:String):Uri?=(db.localFilesDao().choice("SCAP_EXPORT_TREE/"+id)?.takeIf{it.selectedBy==VERIFIED_BASE}
        ?: db.localFilesDao().choice(VERIFIED_BASE))?.destination?.let(Uri::parse)
    fun baseName(c:Context,tree:Uri):String {
        require(tree.scheme=="content"&&android.provider.DocumentsContract.isTreeUri(tree)){"Elige una carpeta base mediante Android."}
        val doc=android.provider.DocumentsContract.buildDocumentUriUsingTree(tree,android.provider.DocumentsContract.getTreeDocumentId(tree))
        return c.contentResolver.query(doc,arrayOf(android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME,android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE),android.os.Bundle.EMPTY,null)?.use{q->
            require(q.moveToFirst()&&q.getString(1)==android.provider.DocumentsContract.Document.MIME_TYPE_DIR){"La base autorizada no es una carpeta disponible."}
            q.getString(0).also{name->
                require(name.isNotBlank()&&!name.startsWith("Puente",true)&&!name.matches(Regex("(?i)(PE[- ]?)?[0-9]{1,3}[A-Z]{0,3}"))&&name.uppercase(java.util.Locale.ROOT) !in setOf("FOTOS","FOTOGRAFIAS","FOTOGRAFÍAS","CROQUIS","REVISIONES","EXCEL_SIC")){
                    "Elige una base común, como Documentos; no una carpeta de puente, carretera o fotografías. Los archivos existentes se conservan."
                }
            }
        } ?: error("Android no permitió verificar la carpeta base.")
    }
    suspend fun authorized(c:Context,db:InventoryDatabase,id:String):Uri? = remembered(db,id)?.takeIf{uri->
        c.contentResolver.persistedUriPermissions.any{it.uri==uri&&it.isReadPermission&&it.isWritePermission}&&runCatching{baseName(c,uri)}.isSuccess}
    /** Read-only suggestion for secondary Save As; never reserves, renames or creates a folder. */
    suspend fun folderUri(c:Context,db:InventoryDatabase,id:String):Uri? {
        val tree=authorized(c,db,id) ?: return null
        val jobs=db.localFilesDao().jobs().filter{it.recordKey=="SCAP/$id"&&it.scope=="SAF_TREE"}.associateBy{it.id}
        val file=db.localFilesDao().filesForRecord("SCAP/$id").filter {
            val uri=it.uri?.let(Uri::parse)
            it.state=="READY"&&it.jobId in jobs&&!it.name.startsWith(".Historial - ")&&it.name.endsWith(".xlsx",true)&&RecordDocuments.sicFormat(it)==null&&
                uri!=null&&uri.authority==tree.authority&&android.provider.DocumentsContract.isTreeUri(uri)&&
                android.provider.DocumentsContract.getTreeDocumentId(uri)==android.provider.DocumentsContract.getTreeDocumentId(tree)
        }.maxWithOrNull(compareBy<LocalSharedFile>{jobs.getValue(it.jobId).revision}.thenBy{jobs.getValue(it.jobId).createdAt}) ?: return null
        return runCatching{AndroidSharedDocumentStore(c,tree).directory(file.relativePath,false)}.getOrNull()
    }
    suspend fun save(c:Context,db:InventoryDatabase,s:ScapInspectionSnapshot,tree:Uri,excelName:String=ScapDocumentNames.excel(s)):List<LocalSharedFile> = withContext(Dispatchers.IO) {
        val latest=requireNotNull(db.scapDao().snapshot(s.inspection.id))
        require(latest==s){"La ficha cambió; vuelve a exportar su versión guardada."}
        ScapDocumentNames.folder(latest)
        require(excelName.endsWith(".xlsx",true)&&excelName.none{it=='/'||it=='\\'||it.code<32})
        baseName(c,tree)
        c.contentResolver.takePersistableUriPermission(tree,Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        db.localFilesDao().saveChoice(DeviceDeliveryChoice("SCAP_EXPORT_TREE/"+s.inspection.id,tree.toString(),VERIFIED_BASE,System.currentTimeMillis()))
        db.localFilesDao().saveChoice(DeviceDeliveryChoice(VERIFIED_BASE,tree.toString(),"SCAP_SAF_VERIFIED_BASE",System.currentTimeMillis()))
        PublicLocalFolderNames.reserve(db,"SCAP/${s.inspection.id}",DriveMirrorPlan.ROOT+ScapDocumentNames.route(latest)+"/PUENTES/",ScapDocumentNames.folder(latest),c)
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
