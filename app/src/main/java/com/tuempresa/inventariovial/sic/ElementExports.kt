package com.tuempresa.inventariovial.sic

import android.content.Context
import android.util.AtomicFile
import com.tuempresa.inventariovial.core.GeneratedFile
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.export.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Separate contractual files, one element and one action. No change to workbook contracts. */
object ElementExports {
    suspend fun prepare(context:Context,db:InventoryDatabase,id:String,heading:ExportHeading):List<GeneratedFile> = withContext(Dispatchers.IO) {
        val parent=requireNotNull(db.inventoryDao().recordById(id))
        require(parent.status=="ACTIVE") {"Finaliza la ficha antes de exportar"}
        val rows=db.inventoryDao().recordsForExport(null,null).filter {it.record.id==id}
        val formats=InventoryElements.exportFormats(rows)
        require(formats.isNotEmpty()) {"No hay formatos completos para exportar"}
        // Validate all requested worksheets before touching existing generated files.
        formats.forEach {(format,selected)->selected.forEach {format.values(it)}}
        val dir=File(context.cacheDir,"sic_exports").apply {check(isDirectory || mkdirs())}
        val generated=formats.map {(format,selected)->
            val target=File(dir,"${format.code}_${id}.xlsx")
            val atomic=AtomicFile(target);val stream=atomic.startWrite()
            try {SicExcelWriter.write(stream,selected,format,heading);atomic.finishWrite(stream)}
            catch(e:Exception) {atomic.failWrite(stream);throw e}
            GeneratedFile("${format.code}_$id",id,parent.assetType,format.code,target.absolutePath,SicExcelWriter.XLSX_MIME,target.lastModified(),"LOCAL")
        }
        if(!parent.observations.isNullOrBlank()) {
            val report=File(dir,"${parent.sicCode}_${id}_observaciones.html")
            report.writeText(ObservationReport.render(listOf(parent)))
            generated+GeneratedFile("${parent.sicCode}_${id}_observaciones",id,parent.assetType,"OBSERVACIONES",report.absolutePath,"text/html",report.lastModified(),"LOCAL")
        } else generated
    }
}
