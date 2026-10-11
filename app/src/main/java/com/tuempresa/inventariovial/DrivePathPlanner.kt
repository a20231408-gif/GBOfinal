package com.tuempresa.inventariovial

import com.tuempresa.inventariovial.data.entity.InventoryRecordWithPhotos
import com.tuempresa.inventariovial.road.*
import com.tuempresa.inventariovial.delivery.*

data class PlannedPhoto(val photoId: String, val recordId: String, val photoIndex: Int,
    val localPath: String, val photoName: String, val plannedPath: String, val deliveryPhotoSequence: Int = 0)
data class PlannedAssetFolder(val asset: OrderedAsset, val plannedPath: String?, val photos: List<PlannedPhoto>)
data class DrivePathPlan(val folders: List<PlannedAssetFolder>, val warnings: List<String>)

/** Pure plan only: never creates folders, moves files, or enqueues uploads. */
object DrivePathPlanner {
    fun plan(records: List<InventoryRecordWithPhotos>, calculator: ChainageCalculator = ChainageCalculator(), policy: DeliveryNamingPolicy = DeliveryNamingPolicy.FOLDER_COMPACT): DrivePathPlan {
        val warnings = mutableListOf<String>()
        val ordered = RoadOrdering.orderAssetsForRoute(records, calculator)
        val sequenceByPhoto = mutableMapOf<String, Int>()
        ordered.groupBy { it.routeCode }.values.forEach { routeAssets ->
            var sequence = 0
            routeAssets.sortedWith(compareBy<OrderedAsset> { it.axisMeasureM == null }
                .thenBy { it.axisMeasureM ?: it.fallbackPosition.chainageM ?: Double.POSITIVE_INFINITY }
                .thenBy { it.item.record.roadbedCode }.thenBy { it.assetFamily.name }.thenBy { it.recordId }).forEach { asset ->
                asset.item.photos.filter { it.scapInspectionId == null }.sortedWith(compareBy({ it.photoIndex }, { it.id })).forEach {
                    sequenceByPhoto[it.id] = ++sequence
                }
            }
        }
        val folders = ordered.map { asset ->
            warnings += asset.warnings.map { "${asset.recordId}: $it" }
            val route = asset.routeCode
            val path = if (FileNameComplianceValidator.safeComponent(route))
                "$route/${com.tuempresa.inventariovial.core.DriveInventorySchema.familyPath(asset.assetFamily)}/${com.tuempresa.inventariovial.core.DriveInventorySchema.elementLabel(asset.assetFamily)} UUID_${asset.recordId}" else null
            if (path == null) warnings += "${asset.recordId}: ruta de carpetas no planificable."
            val photos = if (path == null) emptyList() else asset.item.photos.filter { it.scapInspectionId == null }.sortedWith(compareBy({ it.photoIndex }, { it.id }))
                .map { photo ->
                    require(photo.recordId == asset.recordId) { "Fotografía asociada a otro registro: ${photo.id}" }
                    val sequence = sequenceByPhoto.getValue(photo.id)
                    val name = DeliveryPhotoNameBuilder.build(sequence, photo.photoIndex, asset, policy)
                    FileNameComplianceValidator.warning(name)?.let { warnings += it }
                    PlannedPhoto(photo.id, photo.recordId, photo.photoIndex, photo.localPath, name, "$path/${com.tuempresa.inventariovial.core.DriveInventorySchema.category(photo.photoCategory)}/$name", sequence)
                }
            PlannedAssetFolder(asset, path, photos)
        }
        return DrivePathPlan(folders, warnings)
    }
}
