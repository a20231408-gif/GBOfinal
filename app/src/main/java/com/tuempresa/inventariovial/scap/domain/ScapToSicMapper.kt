package com.tuempresa.inventariovial.scap.domain

import com.tuempresa.inventariovial.scap.data.ScapInspectionSnapshot

enum class ScapMappingStatus { DIRECT, TRANSFORM, NO_MAPPING, PENDING_CLIENT_CONFIRMATION }
data class ScapMappedField(val scapField:String,val sicField:String,val value:String,val status:ScapMappingStatus,val notes:String="")
data class ScapSicMapping(val format:String,val roadRecordId:String,val fields:List<ScapMappedField>,val pending:List<String>)

/** Safe projections. Does not write SIC tables, alter catalog codes or schedule Drive. */
object ScapToSicMapper {
    private fun direct(s:ScapInspectionSnapshot,pairs:List<Pair<String,String>>)=pairs.filterNot {it.first=="singularityName" && s.values()["nameContractVersion"]=="1"}.mapNotNull {(from,to)->
        (if(ScapBridgeType.from(s)!=null && from=="sidewalkWidthM") s.values()[from].orEmpty() else ScapFieldPolicy.exportValue(from,"inspection",s.values()))
            .takeIf {it.isNotBlank()}?.let {ScapMappedField(from,to,it,ScapMappingStatus.DIRECT)}
    }
    fun mapToSic17(s:ScapInspectionSnapshot):ScapSicMapping {
        val fields=direct(s,listOf("bridgeCode" to "bridgeCode","route" to "routeCode","lastInspection" to "surveyDate","totalLengthM" to "dimension1LengthM",
            "lowerClearanceM" to "dimension2LowerHeightM","upperClearanceM" to "dimension3UpperHeightM","singularityName" to "singularityName"))+
            (if(s.spans.isEmpty()) emptyList() else listOf(ScapMappedField("spans.count","spans",s.spans.size.toString(),ScapMappingStatus.TRANSFORM,"Cuenta tramos registrados; requiere revisión al confirmar la ficha.")))
        return ScapSicMapping("SIC-17",s.inspection.roadRecordId,BridgeSicContract.withManual(s,"SIC-17",fields.filter{it.sicField!="routeCode"}+location(s)+ScapSicSupplement.fields(s).filter{it.sicField!="roadbedCode"}),emptyList())
    }
    fun mapToSic17A(s:ScapInspectionSnapshot):ScapSicMapping {
        val coordinateFields=if(ScapBridgeType.from(s)!=null) direct(s,listOf("latitude" to "latitude","longitude" to "longitude")) else emptyList()
        val fields=direct(s,listOf("bridgeName" to "bridgeName","bridgeCode" to "bridgeCode","constructionYear" to "constructionYear",
            "politicalDepartment" to "department","province" to "province","district" to "district","nearbyTown" to "nearbyTown",
            "altitudeM" to "altitude","roadwayWidthM" to "roadwayWidthM","sidewalkWidthM" to "sidewalkWidthM"))+
            coordinateFields+listOfNotNull(s.inspection.latitude?.takeIf{coordinateFields.none{it.sicField=="latitude"}}?.let {ScapMappedField("GNSS.latitude","latitude",it.toString(),ScapMappingStatus.DIRECT)},
                s.inspection.longitude?.takeIf{coordinateFields.none{it.sicField=="longitude"}}?.let {ScapMappedField("GNSS.longitude","longitude",it.toString(),ScapMappingStatus.DIRECT)})
        return ScapSicMapping("SIC-17A",s.inspection.roadRecordId,BridgeSicContract.withManual(s,"SIC-17A",fields),emptyList())
    }
    fun mapToSic17B(s:ScapInspectionSnapshot)=ScapSicMapping("SIC-17B",s.inspection.roadRecordId,
        BridgeSicContract.withManual(s,"SIC-17B",direct(s,listOf("bridgeCode" to "bridgeCode","designLoad" to "designLoad","mainSpanM" to "mainSpanM"))),
        BridgeSicContract.unresolved("SIC-17B").values.toList())
    private fun location(s:ScapInspectionSnapshot):List<ScapMappedField> {
        val r=s.roadRecord ?: return emptyList()
        val values=linkedMapOf("routeCode" to r.routeCode,"roadbedCode" to r.roadbedCode.ifBlank{s.values()["sic17.roadbedCode"].orEmpty()})
        if(r.startPrCode.isNotBlank() && r.startDistanceM.isFinite() && r.startDistanceM>=0) {
            values["startPrCode"]=r.startPrCode;values["startDistanceM"]=r.startDistanceM.toString()
        }
        if(!r.endPrCode.isNullOrBlank() && r.endDistanceM!=null && r.endDistanceM.isFinite() && r.endDistanceM>=0) {
            values["endPrCode"]=r.endPrCode;values["endDistanceM"]=r.endDistanceM.toString()
        }
        return values.filterValues{it.isNotBlank()}.map {(key,value)->ScapMappedField("InventoryRecordEntity.$key",key,value,ScapMappingStatus.DIRECT,"Ubicación contractual vinculada por roadRecordId; ${r.locationSource}.")}
    }
}
