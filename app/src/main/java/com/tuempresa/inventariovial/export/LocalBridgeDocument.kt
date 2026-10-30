package com.tuempresa.inventariovial.export

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.localfiles.*
import com.tuempresa.inventariovial.scap.data.ScapInspectionSnapshot
import com.tuempresa.inventariovial.scap.export.ScapSicExporter
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Missing location never becomes a row of somebody else's route workbook. */
object LocalBridgeDocument {
    fun located(s:ScapInspectionSnapshot)=s.roadRecord?.let{r->r.routeCode.matches(Regex("[A-Za-z0-9][A-Za-z0-9_-]{0,63}"))&&
        r.startPrCode.toIntOrNull()!=null&&r.startDistanceM.isFinite()&&r.startDistanceM>=0&&
        !com.tuempresa.inventariovial.scap.domain.ScapSicLocation.needsReview(s)}==true
    suspend fun provisional(c:Context,db:InventoryDatabase,s:ScapInspectionSnapshot,format:String,
        storage:SharedDocumentStore=AndroidSharedDocumentStore(c)):Uri=withContext(Dispatchers.IO){
        val id=com.tuempresa.inventariovial.delivery.sha256("LOCAL_SIC/${s.inspection.id}/${s.inspection.updatedAt}/$format".toByteArray())
        val job=LocalExportJob(id,"SCAP/${s.inspection.id}","SCAP",s.inspection.id,s.inspection.updatedAt,scope="SIC_PROVISIONAL_$format")
        val raw=ByteArrayOutputStream();ScapSicExporter.writeLocal(raw,s,format)
        val p=ScapSicExporter.previews(s).single{it.format==format}
        val bytes=LocalSicWorkbookPolicy.status(raw.toByteArray(),true,p.missingRequired.size+1)
        val source=File(LocalExportBuilder.work(c,job),"${format}_PROVISIONAL.xlsx")
        if(!source.exists()){
            val atomic=android.util.AtomicFile(source);val out=atomic.startWrite()
            try{out.write(bytes);atomic.finishWrite(out)}catch(e:Exception){atomic.failWrite(out);throw e}
        }
        val digest=fileDigest(source);require(digest.first==bytes.size.toLong()&&digest.second==com.tuempresa.inventariovial.delivery.sha256(bytes)){"La copia anterior se conserva; revisa la revisión guardada."}
        val parent=DriveMirrorPlan.ROOT+"_PROVISIONALES/SIC_POR_IDENTIFICAR/"
        val folder=PublicLocalFolderNames.reserve(db,job.recordKey,parent,PublicLocalFolderNames.describe(db,job,"Puente"))
        val f=LocalSharedFile(id,id,job.recordKey,"$parent$folder/REVISIONES/R_${job.revision}/",source.name,SicExcelWriter.XLSX_MIME,digest.first,digest.second,source.path)
        val slot=storage.reserve(f);storage.checkSlot(slot,f)
        if(!slot.published){storage.write(slot.uri,source);require(storage.digest(slot.uri)==digest)}
        val uri=storage.publish(slot,f);require(storage.digest(uri)==digest)
        db.withTransaction{db.localFilesDao().saveJob(job.copy(state="READY",completed=1,total=1,message="ARCHIVO GUARDADO · PROVISIONAL: ubicación pendiente; fuera del consolidado de ruta."));db.localFilesDao().saveFile(f.copy(uri=uri.toString(),state="READY"))}
        uri
    }
}
