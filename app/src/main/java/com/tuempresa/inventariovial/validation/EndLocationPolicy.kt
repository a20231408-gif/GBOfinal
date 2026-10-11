package com.tuempresa.inventariovial.validation

import com.tuempresa.inventariovial.model.form.InventorySaveRequest
import com.tuempresa.inventariovial.model.form.SicFormDetail

enum class InventoryLocationMode { POINT, LINEAR }

/** Per-format matrix. SIC-17 follows the source workbook's start/end columns. */
fun locationMode(sicCode:String,assetType:String="",sic20Class:String?=null,sic23Class:String?=null):InventoryLocationMode =
    if (sicCode != "SIC-22" && (sicCode in setOf("SIC-17","SIC-19","SIC-21") || assetType in setOf("MURO","TUNEL") ||
        sic20Class in setOf("13","14") || (sicCode=="SIC-23" && sic23Class !in setOf("23","24")))) InventoryLocationMode.LINEAR else InventoryLocationMode.POINT

fun requiresEndLocation(sicCode:String,assetType:String="",sic20Class:String?=null,sic23Class:String?=null):Boolean =
    locationMode(sicCode,assetType,sic20Class,sic23Class) == InventoryLocationMode.LINEAR

fun requiresEndLocation(request:InventorySaveRequest)=requiresEndLocation(request.sicCode,request.assetType,
    (request.detail as? SicFormDetail.Sic20)?.state?.classCode,(request.detail as? SicFormDetail.Sic23)?.state?.classCode)

/** New point captures ignore stale end fields; historical entities are never rewritten here. */
fun pointCapture(request:InventorySaveRequest):InventorySaveRequest = if(request.sicCode!="SIC-22") request else request.copy(
    endPrCode=null,endDistanceM=null,endLatitude=null,endLongitude=null,endGpsAccuracyM=null,
    endLocation=null,endSuggestion=null,endLocationSource="MANUAL")

/** Saving an edited point must not erase the old, now-hidden end-location audit. */
fun preservePointHistory(captured: com.tuempresa.inventariovial.data.entity.InventoryRecordEntity,
                         previous: com.tuempresa.inventariovial.data.entity.InventoryRecordEntity?): com.tuempresa.inventariovial.data.entity.InventoryRecordEntity =
    if (captured.sicCode != "SIC-22" || previous == null) captured else captured.copy(
        endPrCode=previous.endPrCode,endDistanceM=previous.endDistanceM,endLatitude=previous.endLatitude,
        endLongitude=previous.endLongitude,endGpsAccuracyM=previous.endGpsAccuracyM,endGpsTimestamp=previous.endGpsTimestamp,
        endLocationSource=previous.endLocationSource,endSuggestedPrCode=previous.endSuggestedPrCode,
        endSuggestedDistanceFromPrM=previous.endSuggestedDistanceFromPrM,endAxisMeasureM=previous.endAxisMeasureM,
        endDistanceToRoadAxisM=previous.endDistanceToRoadAxisM,endMatchedSegmentId=previous.endMatchedSegmentId,
        endProjectedLatitude=previous.endProjectedLatitude,endProjectedLongitude=previous.endProjectedLongitude)
