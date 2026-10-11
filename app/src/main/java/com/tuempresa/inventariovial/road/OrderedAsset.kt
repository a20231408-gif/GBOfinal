package com.tuempresa.inventariovial.road

import com.tuempresa.inventariovial.DriveFolderRouter
import com.tuempresa.inventariovial.data.entity.InventoryRecordWithPhotos

enum class AssetOrderSource { GNSS_MAP_MATCH, MANUAL_CHAINAGE_FALLBACK, CREATED_AT_FALLBACK }

data class OrderedAsset(
    val item: InventoryRecordWithPhotos,
    val routeCode: String,
    val sibCode: String?,
    val assetFamily: AssetFamily,
    val axisMeasureM: Double?,
    val sequence: Int,
    val orderSource: AssetOrderSource,
    val fallbackPosition: RoadPosition,
    val warnings: List<String>,
    val remoteLogicalPath:String? = null
) {
    val recordId get() = item.record.id
    val shortCode get() = remoteLogicalPath?.substringAfterLast(' ')?.toIntOrNull()?.let {"${assetFamily.abbreviation}-$it"} ?: "${assetFamily.abbreviation}-${recordId.take(8)}"
    val folderName get() = remoteLogicalPath?.substringAfterLast('/') ?: "${assetFamily.folderLabel} UUID_$recordId"
}

internal fun orderedAssets(records: List<InventoryRecordWithPhotos>, calculator: ChainageCalculator): List<OrderedAsset> {
    val rows = records.filter { it.record.status == "ACTIVE" && it.record.sicCode != "SCAP" }.map { item ->
        val r = item.record
        val family = AssetPresentationCatalog.resolve(item)
        val sib: String? = null // Retained DTO field for historical consumers; never used for V4 routing.
        val axis = r.axisMeasureM?.takeIf { it.isFinite() && it >= 0 }
        val fallback = calculator.calculate(r)
        OrderedAsset(item, normalizedRoadCode(r.routeCode), sib, family, axis, 0,
            when { axis != null -> AssetOrderSource.GNSS_MAP_MATCH
                fallback.chainageM?.isFinite() == true -> AssetOrderSource.MANUAL_CHAINAGE_FALLBACK
                else -> AssetOrderSource.CREATED_AT_FALLBACK }, fallback,
            buildList {
                if (family == AssetFamily.OTROS) add("Familia sin abreviatura configurada: ${r.sicCode} / ${r.assetType}; se usa ELM.")
                if (r.latitude == null || r.longitude == null) add("Sin coordenadas verificadas: permanece en el inventario ordenado, sin punto geográfico.")
                if (r.routeCode.isBlank()) add("Ruta no seleccionada.")
            })
    }.sortedWith { a,b -> InventoryTechnicalOrder.comparator.compare(a.item.record,b.item.record) }
    val counters = mutableMapOf<Triple<String, String?, AssetFamily>, Int>()
    return rows.map { row ->
        val key = Triple(row.routeCode, row.sibCode, row.assetFamily)
        val sequence = (counters[key] ?: 0) + 1
        counters[key] = sequence
        row.copy(sequence = sequence)
    }
}
