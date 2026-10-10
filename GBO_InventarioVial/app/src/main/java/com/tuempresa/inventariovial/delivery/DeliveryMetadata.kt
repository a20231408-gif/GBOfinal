package com.tuempresa.inventariovial.delivery

import com.tuempresa.inventariovial.PlannedAssetFolder
import com.tuempresa.inventariovial.PlannedPhoto
import com.tuempresa.inventariovial.data.entity.PhotoEntity
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
internal fun json(vararg values: Pair<String, Any?>) = JSONObject().apply { values.forEach { (key, value) -> put(key, value ?: JSONObject.NULL) } }

object DeliveryMetadata {
    fun photo(photo: PhotoEntity, plan: PlannedPhoto, hash: String) = json(
        "photoId" to photo.id, "photoIndex" to photo.photoIndex, "deliveryPhotoSequence" to plan.deliveryPhotoSequence,
        "fileName" to plan.photoName, "capturedAt" to photo.capturedAt,
        "latitude" to photo.photoLatitude, "longitude" to photo.photoLongitude, "altitudeM" to photo.photoAltitudeM,
        "accuracyM" to photo.photoGpsAccuracyM, "gpsTimestamp" to photo.photoGpsTimestamp, "provider" to photo.photoGnssProvider,
        "photoLocationSource" to photo.photoLocationSource, "satellites" to photo.photoSatellites,
        "hdop" to photo.photoHdop, "isRtkFixed" to photo.photoIsRtkFixed,
        "category" to photo.photoCategory, "description" to photo.description, "sha256" to hash)

    fun element(folder: PlannedAssetFolder, photos: List<JSONObject>): JSONObject {
        val asset = folder.asset; val r = asset.item.record
        return json("recordId" to r.id, "shortCode" to asset.shortCode, "displayName" to asset.folderName,
            "assetFamily" to asset.assetFamily.name, "sicCode" to r.sicCode, "sibCode" to asset.sibCode,
            "routeCode" to r.routeCode, "roadbedCode" to r.roadbedCode,
            "location" to json("confirmedRoute" to r.routeCode, "confirmedPr" to r.startPrCode,
                "confirmedDistance" to r.startDistanceM, "locationSource" to r.locationSource, "routeSource" to r.routeSource,
                "confirmedEndPr" to r.endPrCode, "confirmedEndDistance" to r.endDistanceM, "endLocationSource" to r.endLocationSource),
            "suggestion" to json("suggestedRoute" to r.suggestedRouteCode, "suggestedPr" to r.suggestedPrCode,
                "suggestedDistance" to r.suggestedDistanceFromPrM, "suggestedEndPr" to r.endSuggestedPrCode,
                "suggestedEndDistance" to r.endSuggestedDistanceFromPrM, "routeMatchAmbiguous" to r.routeMatchAmbiguous,
                "routeMatchConfidence" to r.routeMatchConfidence, "routeMatchDistanceM" to r.routeMatchDistanceM),
            "geometricLocation" to json("axisMeasureM" to r.axisMeasureM, "distanceToRoadAxisM" to r.distanceToRoadAxisM,
                "projectedLatitude" to r.projectedLatitude, "projectedLongitude" to r.projectedLongitude,
                "matchedSegmentId" to r.matchedSegmentId, "endAxisMeasureM" to r.endAxisMeasureM,
                "endProjectedLatitude" to r.endProjectedLatitude, "endProjectedLongitude" to r.endProjectedLongitude),
            "recordLocation" to json("latitude" to r.latitude, "longitude" to r.longitude,
                "altitude" to r.altitudeM, "accuracy" to r.gpsAccuracyM),
            "photos" to JSONArray(photos))
    }

    fun photoPayload(folder: PlannedAssetFolder, photo: PhotoEntity, plan: PlannedPhoto): JSONObject {
        val a = folder.asset; val r = a.item.record
        return json("recordId" to r.id, "photoId" to photo.id, "photoIndex" to photo.photoIndex,
            "deliveryPhotoSequence" to plan.deliveryPhotoSequence, "routeCode" to r.routeCode,
            "sibCode" to a.sibCode, "sicCode" to r.sicCode, "assetFamily" to a.assetFamily.name,
            "shortCode" to a.shortCode, "elementFolder" to a.folderName, "fileName" to plan.photoName,
            "mimeType" to "image/jpeg", "photoLatitude" to photo.photoLatitude, "photoLongitude" to photo.photoLongitude,
            "photoAltitudeM" to photo.photoAltitudeM, "photoGpsAccuracyM" to photo.photoGpsAccuracyM,
            "photoGpsTimestamp" to photo.photoGpsTimestamp, "photoGnssProvider" to photo.photoGnssProvider,
            "confirmedPrCode" to r.startPrCode, "confirmedDistanceM" to r.startDistanceM,
            "suggestedPrCode" to r.suggestedPrCode, "suggestedDistanceM" to r.suggestedDistanceFromPrM,
            "axisMeasureM" to r.axisMeasureM, "distanceToRoadAxisM" to r.distanceToRoadAxisM,
            "photoCategory" to photo.photoCategory, "description" to photo.description)
    }

    fun exifDescription(folder: PlannedAssetFolder, photoId: String): String {
        val a = folder.asset; val r = a.item.record
        return "Ruta=${r.routeCode};SIB=${a.sibCode};SIC=${r.sicCode};Elemento=${a.shortCode};PR=${r.startPrCode};DistPR=${r.startDistanceM};Axis=${r.axisMeasureM ?: ""};RecordId=${r.id};PhotoId=$photoId"
    }
}
