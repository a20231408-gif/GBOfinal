package com.tuempresa.inventariovial.scap.export

import com.tuempresa.inventariovial.scap.data.*
import com.tuempresa.inventariovial.data.entity.*
import org.json.JSONObject

/** Explicit property access and stable JSON labels. Safe when R8 renames JVM fields. */
internal object ScapCaptureRows {
    fun row(value:Any):JSONObject {
        val fields:Map<String,Any?> = when(value){
            is ScapInspectionEntity -> mapOf(
                "id" to value.id,
                "roadRecordId" to value.roadRecordId,
                "bridgeName" to value.bridgeName,
                "bridgeCode" to value.bridgeCode,
                "createdAt" to value.createdAt,
                "updatedAt" to value.updatedAt,
                "createdBy" to value.createdBy,
                "deviceId" to value.deviceId,
                "status" to value.status,
                "syncStatus" to value.syncStatus,
                "latitude" to value.latitude,
                "longitude" to value.longitude,
                "gpsAccuracyM" to value.gpsAccuracyM,
                "gpsTimestamp" to value.gpsTimestamp,
                "locationSource" to value.locationSource
            )
            is ScapFieldValueEntity -> mapOf(
                "inspectionId" to value.inspectionId,
                "ownerId" to value.ownerId,
                "key" to value.key,
                "value" to value.value,
                "source" to value.source
            )
            is ScapSpanEntity -> mapOf(
                "id" to value.id,
                "inspectionId" to value.inspectionId,
                "spanIndex" to value.spanIndex,
                "lengthM" to value.lengthM,
                "category" to value.category,
                "type" to value.type,
                "secondaryCharacteristic" to value.secondaryCharacteristic,
                "edgeCondition" to value.edgeCondition,
                "predominantMaterial" to value.predominantMaterial
            )
            is ScapSubstructureEntity -> mapOf(
                "id" to value.id,
                "inspectionId" to value.inspectionId,
                "kind" to value.kind,
                "elementIndex" to value.elementIndex,
                "elevationType" to value.elevationType,
                "elevationMaterial" to value.elevationMaterial,
                "foundationType" to value.foundationType,
                "foundationMaterial" to value.foundationMaterial,
                "soil" to value.soil
            )
            is ScapSupportEntity -> mapOf(
                "id" to value.id,
                "inspectionId" to value.inspectionId,
                "supportIndex" to value.supportIndex,
                "type" to value.type,
                "material" to value.material,
                "location" to value.location,
                "number" to value.number
            )
            is ScapJointEntity -> mapOf(
                "id" to value.id,
                "inspectionId" to value.inspectionId,
                "jointIndex" to value.jointIndex,
                "type" to value.type,
                "material" to value.material
            )
            is ScapElementEntity -> mapOf(
                "id" to value.id,
                "inspectionId" to value.inspectionId,
                "elementCode" to value.elementCode,
                "description" to value.description,
                "quantity" to value.quantity,
                "unit" to value.unit,
                "importanceFactor" to value.importanceFactor,
                "group" to value.group,
                "isPresent" to value.isPresent
            )
            is ScapElementConditionEntity -> mapOf(
                "id" to value.id,
                "scapElementId" to value.scapElementId,
                "percent0" to value.percent0,
                "percent1" to value.percent1,
                "percent2" to value.percent2,
                "percent3" to value.percent3,
                "percent4" to value.percent4,
                "percent5" to value.percent5
            )
            is ScapDefectEntity -> mapOf(
                "id" to value.id,
                "inspectionId" to value.inspectionId,
                "elementCode" to value.elementCode,
                "description" to value.description,
                "locationDescription" to value.locationDescription,
                "photoId" to value.photoId,
                "aiSuggested" to value.aiSuggested,
                "validatedByUser" to value.validatedByUser,
                "createdAt" to value.createdAt
            )
            is ScapProfilePointEntity -> mapOf(
                "id" to value.id,
                "inspectionId" to value.inspectionId,
                "pointIndex" to value.pointIndex,
                "distanceM" to value.distanceM,
                "downstreamM" to value.downstreamM,
                "upstreamM" to value.upstreamM,
                "axisM" to value.axisM
            )
            is ScapSketchEntity -> mapOf(
                "id" to value.id,
                "inspectionId" to value.inspectionId,
                "type" to value.type,
                "localUri" to value.localUri
            )
            is PhotoEntity -> mapOf(
                "id" to value.id,
                "recordId" to value.recordId,
                "photoIndex" to value.photoIndex,
                "localPath" to value.localPath,
                "isPrimary" to value.isPrimary,
                "generatedFileName" to value.generatedFileName,
                "driveFolderId" to value.driveFolderId,
                "driveFileId" to value.driveFileId,
                "syncStatus" to value.syncStatus,
                "createdAt" to value.createdAt,
                "originalPath" to value.originalPath,
                "stampedPath" to value.stampedPath,
                "scapInspectionId" to value.scapInspectionId,
                "scapElementCode" to value.scapElementCode,
                "photoCategory" to value.photoCategory,
                "description" to value.description,
                "photoLatitude" to value.photoLatitude,
                "photoLongitude" to value.photoLongitude,
                "photoAltitudeM" to value.photoAltitudeM,
                "photoGpsAccuracyM" to value.photoGpsAccuracyM,
                "photoGpsTimestamp" to value.photoGpsTimestamp,
                "photoGnssProvider" to value.photoGnssProvider,
                "photoSatellites" to value.photoSatellites,
                "photoHdop" to value.photoHdop,
                "photoIsRtkFixed" to value.photoIsRtkFixed,
                "capturedAt" to value.capturedAt,
                "photoLocationSource" to value.photoLocationSource,
                "sha256" to value.sha256
            )
            is InventoryRecordEntity -> mapOf(
                "id" to value.id,
                "sicCode" to value.sicCode,
                "assetType" to value.assetType,
                "routeCode" to value.routeCode,
                "roadbedCode" to value.roadbedCode,
                "startPrCode" to value.startPrCode,
                "startDistanceM" to value.startDistanceM,
                "endPrCode" to value.endPrCode,
                "endDistanceM" to value.endDistanceM,
                "sideCode" to value.sideCode,
                "latitude" to value.latitude,
                "longitude" to value.longitude,
                "altitudeM" to value.altitudeM,
                "gpsAccuracyM" to value.gpsAccuracyM,
                "surveyDate" to value.surveyDate,
                "observations" to value.observations,
                "status" to value.status,
                "photoSyncStatus" to value.photoSyncStatus,
                "excelSyncStatus" to value.excelSyncStatus,
                "createdAt" to value.createdAt,
                "updatedAt" to value.updatedAt,
                "endLatitude" to value.endLatitude,
                "endLongitude" to value.endLongitude,
                "endGpsAccuracyM" to value.endGpsAccuracyM,
                "locationSource" to value.locationSource,
                "sideSource" to value.sideSource,
                "sessionId" to value.sessionId,
                "gpsTimestamp" to value.gpsTimestamp,
                "endGpsTimestamp" to value.endGpsTimestamp,
                "gnssProvider" to value.gnssProvider,
                "gnssFixType" to value.gnssFixType,
                "verticalAccuracyM" to value.verticalAccuracyM,
                "satellites" to value.satellites,
                "hdop" to value.hdop,
                "correctionAge" to value.correctionAge,
                "isRtkFixed" to value.isRtkFixed,
                "serverSyncStatus" to value.serverSyncStatus,
                "serverSyncError" to value.serverSyncError,
                "segment" to value.segment,
                "surveyDirection" to value.surveyDirection,
                "axisMeasureM" to value.axisMeasureM,
                "distanceToRoadAxisM" to value.distanceToRoadAxisM,
                "roadMatchConfidence" to value.roadMatchConfidence,
                "matchedSegmentId" to value.matchedSegmentId,
                "projectedLatitude" to value.projectedLatitude,
                "projectedLongitude" to value.projectedLongitude,
                "endLocationSource" to value.endLocationSource,
                "routeSource" to value.routeSource,
                "suggestedRouteCode" to value.suggestedRouteCode,
                "suggestedPrCode" to value.suggestedPrCode,
                "suggestedDistanceFromPrM" to value.suggestedDistanceFromPrM,
                "routeMatchDistanceM" to value.routeMatchDistanceM,
                "routeMatchConfidence" to value.routeMatchConfidence,
                "routeMatchAmbiguous" to value.routeMatchAmbiguous,
                "endSuggestedPrCode" to value.endSuggestedPrCode,
                "endSuggestedDistanceFromPrM" to value.endSuggestedDistanceFromPrM,
                "endAxisMeasureM" to value.endAxisMeasureM,
                "endDistanceToRoadAxisM" to value.endDistanceToRoadAxisM,
                "endMatchedSegmentId" to value.endMatchedSegmentId,
                "endProjectedLatitude" to value.endProjectedLatitude,
                "endProjectedLongitude" to value.endProjectedLongitude
            )
            is Sic17AEntity -> mapOf(
                "recordId" to value.recordId,
                "bridgeName" to value.bridgeName,
                "bridgeCode" to value.bridgeCode,
                "constructionYear" to value.constructionYear,
                "department" to value.department,
                "province" to value.province,
                "district" to value.district,
                "nearbyTown" to value.nearbyTown,
                "latitude" to value.latitude,
                "longitude" to value.longitude,
                "altitude" to value.altitude,
                "lanes" to value.lanes,
                "roadwayWidthM" to value.roadwayWidthM,
                "sidewalkWidthM" to value.sidewalkWidthM,
                "deckWidthM" to value.deckWidthM,
                "superstructureWidthM" to value.superstructureWidthM,
                "alignmentCode" to value.alignmentCode
            )
            is Sic17BEntity -> mapOf(
                "recordId" to value.recordId,
                "bridgeCode" to value.bridgeCode,
                "designLoad" to value.designLoad,
                "maximumCapacity" to value.maximumCapacity,
                "wearingSurface" to value.wearingSurface,
                "vehicleRestraintCode" to value.vehicleRestraintCode,
                "mainSpanM" to value.mainSpanM,
                "boundaryCode" to value.boundaryCode,
                "crossSectionCode" to value.crossSectionCode,
                "beams" to value.beams,
                "slabMaterialCode" to value.slabMaterialCode,
                "beamMaterialCode" to value.beamMaterialCode,
                "abutmentElevationCode" to value.abutmentElevationCode,
                "abutmentMaterialCode" to value.abutmentMaterialCode,
                "abutmentFoundationCode" to value.abutmentFoundationCode,
                "pierElevationCode" to value.pierElevationCode,
                "pierMaterialCode" to value.pierMaterialCode,
                "pierFoundationCode" to value.pierFoundationCode,
                "comments" to value.comments
            )
            else -> error("Entidad no admitida en captura SCAP")
        }
        return JSONObject().apply {fields.forEach{(key,v)->put(key,if(v is Double&&!v.isFinite()||v is Float&&!v.isFinite())JSONObject.NULL else v ?: JSONObject.NULL)}}
    }
}
