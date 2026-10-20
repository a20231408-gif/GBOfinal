package com.tuempresa.inventariovial.scap.export

import android.content.Context
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.InventoryRecordEntity
import com.tuempresa.inventariovial.data.entity.InventoryRecordWithPhotos
import com.tuempresa.inventariovial.delivery.DeliveryNamingPolicy
import com.tuempresa.inventariovial.delivery.DeliveryPhotoNameBuilder
import com.tuempresa.inventariovial.road.*
import com.tuempresa.inventariovial.scap.data.ScapInspectionSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class ScapBridgeRow(val snapshot: ScapInspectionSnapshot, val record: InventoryRecordEntity)

/** Presentation only: neither UUID nor contractual bridgeCode is replaced by P-N. */
data class ScapHumanLayout(val routeCode: String, val elementFolder: String, val shortCode: String,
    val bridgeIdentity: String, val photoPaths: Map<String, String>, val sketchPaths: Map<String, String>) {
    val sibCode get() = "SIB-08" // Historical parser only; V4 never uses this in paths.
    val path get() = "$routeCode/PUENTES/$elementFolder"
    fun json() = JSONObject().put("routeCode", routeCode).put("sibCode", sibCode)
        .put("elementFolder", elementFolder).put("shortCode", shortCode).put("bridgeIdentity", bridgeIdentity)
        .put("photoPaths", JSONObject(photoPaths.toSortedMap())).put("sketchPaths", JSONObject(sketchPaths.toSortedMap()))
    companion object {
        fun saved(s: ScapInspectionSnapshot) = s.values("delivery")["layout"]?.let { parse(JSONObject(it)) }
        fun parse(j: JSONObject): ScapHumanLayout {
            fun paths(key: String): Map<String, String> = j.getJSONObject(key).let { obj ->
                obj.keys().asSequence().associateWith { obj.getString(it) }
            }
            require(j.getString("sibCode") == "SIB-08")
            return ScapHumanLayout(j.getString("routeCode"), j.getString("elementFolder"), j.getString("shortCode"),
                j.getString("bridgeIdentity"), paths("photoPaths"), paths("sketchPaths"))
        }
    }
}

object ScapHumanDelivery {
    private fun identity(s: ScapInspectionSnapshot) = ScapDrivePayload.bridgeIdentity(s.inspection.bridgeCode, s.inspection.id)

    fun plan(rows: List<ScapBridgeRow>): Map<String, ScapHumanLayout> = buildMap {
        for ((route, routeRows) in rows.groupBy { normalizedRoadCode(it.snapshot.values()["route"].orEmpty()) }) {
            val bridges = routeRows.groupBy { identity(it.snapshot) }
            val latest = bridges.mapValues { (_, inspections) -> inspections.maxWith(
                compareBy<ScapBridgeRow> { it.snapshot.inspection.updatedAt }.thenBy { it.snapshot.inspection.id }) }
            // Adapt only the ordering view; never persist SIC/status changes to the SCAP parent.
            val ordered = RoadOrdering.orderAssetsForRoute(latest.values.map { row ->
                InventoryRecordWithPhotos(row.record.copy(routeCode = route, sicCode = "SIC-17", assetType = "PUENTE", status = "ACTIVE"), emptyList())
            })
            var photoOffset = 0
            for (asset in ordered) {
                val key = latest.entries.single { it.value.record.id == asset.recordId }.key
                val inspections = bridges.getValue(key)
                inspections.forEach { row ->
                    val s = row.snapshot
                    put(s.inspection.id, ScapHumanLayout(route, asset.folderName, asset.shortCode, key,
                        s.photos.filter{it.localPath.isNotBlank()}.sortedBy { it.photoIndex }.mapIndexed { index, photo -> photo.id to
                            ("fotos/" + DeliveryPhotoNameBuilder.build(photoOffset + index + 1, index + 1, asset,
                                DeliveryNamingPolicy.FOLDER_COMPACT)) }.toMap(),
                        s.sketches.sortedBy { it.id }.mapIndexed { index, sketch -> sketch.id to "croquis/CROQUIS_${index + 1}.png" }.toMap()))
                }
                photoOffset += inspections.maxOf { it.snapshot.photos.count{photo->photo.localPath.isNotBlank()} }
            }
        }
    }

    suspend fun prepare(context: Context, db: InventoryDatabase, snapshot: ScapInspectionSnapshot): ScapHumanLayout = withContext(Dispatchers.IO) {
        val locationIssues=ScapDriveLocation.issues(context,snapshot)
        require(locationIssues.isEmpty()){ScapDriveLocation.MESSAGE+" "+locationIssues.joinToString(" · ")}
        val route = ProjectRouteCatalog.load(context).requireRoute(snapshot.values()["route"].orEmpty()).routeCode
        val positioner = RoadAxisPositioner(RoadReferenceRepository(context).loadFromAssets(),
            com.tuempresa.inventariovial.field.FieldSettings.load(context).matchConfig())
        val snapshots = db.scapDao().allSnapshots().filter { normalizedRoadCode(it.values()["route"].orEmpty()) == route }
        val rows = snapshots.map { s ->
            val record = requireNotNull(db.inventoryDao().recordById(s.inspection.roadRecordId))
            val positioned = positioner.position(record.copy(routeCode = route,
                latitude = s.inspection.latitude ?: record.latitude, longitude = s.inspection.longitude ?: record.longitude,
                gpsAccuracyM = s.inspection.gpsAccuracyM.takeIf { s.inspection.latitude != null && s.inspection.longitude != null }))
            ScapBridgeRow(s, positioned)
        }
        val current = rows.single { it.snapshot.inspection.id == snapshot.inspection.id }
        require(current.snapshot.inspection.updatedAt == snapshot.inspection.updatedAt) { "La ficha cambió; prepara nuevamente la entrega." }
        require(ChainageCalculator().calculate(current.record).chainageM != null) { "Completa PR y distancia para la entrega técnica." }
        plan(rows).getValue(snapshot.inspection.id)
    }
}
