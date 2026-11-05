package com.tuempresa.inventariovial.scap.export

import android.content.Context
import com.tuempresa.inventariovial.export.ProvisionalSicExcel
import com.tuempresa.inventariovial.export.SicExcelWriter
import com.tuempresa.inventariovial.localfiles.*
import com.tuempresa.inventariovial.scap.catalog.ScapCatalog
import com.tuempresa.inventariovial.scap.data.ScapInspectionSnapshot
import com.tuempresa.inventariovial.scap.domain.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object ScapDocumentNames {
    fun route(s:ScapInspectionSnapshot)=s.roadRecord?.routeCode?.takeIf{it.isNotBlank()}
        ?: s.values()["route"].orEmpty().ifBlank{"SIN CARRETERA"}
    fun confirmedName(s:ScapInspectionSnapshot):String? {
        val v=s.values()
        val name=if(v.containsKey("singularityName"))v["singularityName"] else s.inspection.bridgeName.takeIf{it.isNotBlank()} ?: v["bridgeName"]
        return name?.trim()?.takeIf{it.isNotBlank()&&it.lowercase(java.util.Locale.ROOT) !in setOf("puente","sin nombre","puente sin nombre")}
    }
    fun bridge(s:ScapInspectionSnapshot)=confirmedName(s) ?: "Puente sin nombre"
    fun excel(s:ScapInspectionSnapshot)=PublicLocalFolderNames.component("SCAP - ${route(s)} - ${bridge(s)}")+".xlsx"
    fun folder(s:ScapInspectionSnapshot)=PublicLocalFolderNames.component("Puente ${requireNotNull(confirmedName(s)){"Asigna y guarda el nombre del puente antes de crear su carpeta pública. El borrador se conserva en GBO."}}")

}

/** The contractual template remains unchanged. Incomplete captures carry a visible draft marker inside XLSX. */
object ScapLocalDocuments {
    data class Workbook(val file:File,val pending:Int,val draft:Boolean,val reason:String?)
    suspend fun workbook(c:Context,s:ScapInspectionSnapshot,directory:File):Workbook=withContext(Dispatchers.IO) {
        check(directory.isDirectory||directory.mkdirs())
        val completion=ScapCompletion.assess(s,ScapCatalog.load(c).forSnapshot(s))
        val draft=s.inspection.status!="COMPLETE"||completion.pending.isNotEmpty()||
            s.values("completion")["status"]!="COMPLETE"
        val raw=File(directory,"plantilla.generating.xlsx");var reason:String?=null
        val target=File(directory,ScapDocumentNames.excel(s))
        try {
            val bounded=if(s.photos.size>32)s.copy(photos=ScapPhotoPanel.ordered(s).take(32))else s
            ScapDelivery.writeExcel(c,bounded,raw)
            target.outputStream().use {out->
                if(!draft)raw.inputStream().use{it.copyTo(out)}
                else ProvisionalSicExcel.decorate(out,raw.readBytes(),s.inspection.id,s.inspection.createdBy,
                    s.inspection.updatedAt,s.inspection.createdAt,
                    completion.pending.map{ProvisionalSicExcel.Pending(it.field,it.section,"Dato pendiente de captura")},markerCell="C4")
            }
        }catch(e:CancellationException){throw e}catch(e:Exception){
            reason=LocalCopyDrain.publicError(e)
            target.outputStream().use{ProvisionalScapWorkbook.write(it,s,reason)}
        }
        if(draft||reason!=null) {
            val marked=target.inputStream().use{TemplateWorkbook(it)}
            marked.set(1,"C4","BORRADOR — ${ScapDocumentNames.bridge(s)} — ${completion.pending.size} datos pendientes")
            target.outputStream().use{marked.writeCopy(it)}
        }
        ManualDocumentExport.verifyXlsx(target)
        Workbook(target,completion.pending.size,draft||reason!=null,reason)
    }
    suspend fun prepare(c:Context,s:ScapInspectionSnapshot):ManualDocument=withContext(Dispatchers.IO) {
        val dir=File(c.filesDir,"scap_manual/${java.util.UUID.randomUUID()}")
        val w=workbook(c,s,dir)
        val notice=if(w.draft)"BORRADOR · ${w.pending} datos pendientes. Copia de trabajo; no confirma Drive." else "SCAP completo · copia local; no confirma Drive."
        ManualDocumentExport.freeze(c,w.file,"SCAP",SicExcelWriter.XLSX_MIME,ScapDocumentNames.excel(s),s.inspection.updatedAt,
            notice+(w.reason?.let{" Plantilla pendiente: $it. Se conserva la captura en una hoja de trabajo."}.orEmpty()),ownerKey="SCAP/${s.inspection.id}")
    }
}
