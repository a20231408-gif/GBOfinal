package com.tuempresa.inventariovial.export

import com.tuempresa.inventariovial.data.entity.InventoryRecordForExport
import com.tuempresa.inventariovial.road.RoadPr
import com.tuempresa.inventariovial.validation.*
import org.json.JSONObject
import java.io.OutputStream

/** Companion evidence; the official SIC column order stays unchanged. No PR-code kilometer fallback. */
object TechnicalLengthExport {
    fun applies(item:com.tuempresa.inventariovial.data.entity.InventoryRecordWithPhotos) = item.sic19!=null || item.sic20?.classCode in setOf("12","14") ||
        item.sic21?.let {it.classCode=="20" || it.classCode=="19" && it.typeCode=="1"}==true
    fun applies(item:InventoryRecordForExport) = item.sic19!=null || item.sic20?.classCode in setOf("12","14") ||
        item.sic21?.let {it.classCode=="20" || it.classCode=="19" && it.typeCode=="1"}==true
    fun metadata(item:InventoryRecordForExport,prs:List<RoadPr>):JSONObject {
        val calculation=ContractualLength.calculate(item.record,prs)
        val measured=item.sic20?.takeIf {it.classCode=="14"}?.wallLengthMeters
        val auto=item.sicExtension?.values()?.get("_lengthMode")=="AUTO"
        return JSONObject().put("calculatedM",calculation.metres ?: JSONObject.NULL).put("calculationSource",calculation.source)
            .put("measuredM",if(auto)JSONObject.NULL else measured ?: JSONObject.NULL)
            .put("applicableM",if(auto)calculation.metres ?: JSONObject.NULL else measured ?: calculation.metres ?: JSONObject.NULL)
            .put("applicableSource",if(!auto && measured!=null) "MEASURED_OR_HISTORICAL" else calculation.source)
            .put("warning",calculation.warning ?: JSONObject.NULL).put("unit","m")
    }
    fun write(out:OutputStream,item:InventoryRecordForExport,prs:List<RoadPr>) {
        require(applies(item))
        val m=metadata(item,prs)
        val columns=listOf(SicColumn("UUID",CellKind.TEXT),SicColumn("SIC",CellKind.TEXT),
            SicColumn("Ruta",CellKind.TEXT),SicColumn("Calzada",CellKind.TEXT),SicColumn("PR inicio",CellKind.TEXT),
            SicColumn("Distancia inicio (m)",CellKind.DECIMAL),SicColumn("PR fin",CellKind.TEXT),SicColumn("Distancia fin (m)",CellKind.DECIMAL),
            SicColumn("Longitud calculada (m)",CellKind.DECIMAL),SicColumn("Longitud medida / histórica (m)",CellKind.DECIMAL),
            SicColumn("Longitud aplicable (m)",CellKind.DECIMAL),SicColumn("Origen",CellKind.TEXT),SicColumn("Advertencia",CellKind.TEXT,60.0))
        fun value(key:String):Any?=m.opt(key).takeUnless {it==JSONObject.NULL}
        SicExcelWriter.writeMappedRow(out,"LONGITUD TECNICA",item.record.routeCode,columns,
            listOf(item.record.id,item.record.sicCode,item.record.routeCode,item.record.roadbedCode,item.record.startPrCode,item.record.startDistanceM,
                item.record.endPrCode,item.record.endDistanceM,value("calculatedM"),value("measuredM"),value("applicableM"),value("applicableSource"),value("warning")))
    }
}
