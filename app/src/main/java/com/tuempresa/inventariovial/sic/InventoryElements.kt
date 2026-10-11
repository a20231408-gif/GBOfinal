package com.tuempresa.inventariovial.sic

import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.catalog.EngineeringConditions
import com.tuempresa.inventariovial.export.SicExportFormat
import com.tuempresa.inventariovial.model.RoadAssetType

data class InventoryElementEntry(val title: String, val subtitle: String, val family: String, val format: SicFieldFormat)
data class InventoryElement(val record: InventoryRecordEntity, val children: List<InventoryRecordEntity>)
data class InventoryDestination(val title:String,val subtitle:String,val family:String,val asset:RoadAssetType?=null,val format:SicFieldFormat?=null)

/** Presentation aggregation uses explicit UUID relationships, never proximity or labels. */
object InventoryElements {
    val dashboard = listOf(
        InventoryElementEntry("Fajas de las calzadas", "SIC-08", "CALZADA Y FAJAS", SicFieldFormat.SIC08),
        InventoryElementEntry("Ancho de las fajas", "SIC-09", "CALZADA Y FAJAS", SicFieldFormat.SIC09),
        InventoryElementEntry("Puntos críticos", "SIC-13", "SEGURIDAD VIAL", SicFieldFormat.SIC13),
        InventoryElementEntry("Accidentes", "SIC-34", "SEGURIDAD VIAL", SicFieldFormat.SIC34))
    val home:List<InventoryDestination> = listOf(
        InventoryDestination("Fajas de las calzadas","SIC-08","CALZADA Y FAJAS",format=SicFieldFormat.SIC08),
        InventoryDestination("Ancho de las fajas","SIC-09","CALZADA Y FAJAS",format=SicFieldFormat.SIC09),
        InventoryDestination("Señalización y seguridad","SIC-21 · SIC-22","SEGURIDAD VIAL",asset=RoadAssetType.SIGNALIZATION),
        InventoryDestination("Puntos críticos","SIC-13","SEGURIDAD VIAL",format=SicFieldFormat.SIC13),
        InventoryDestination("Accidentes","SIC-34","SEGURIDAD VIAL",format=SicFieldFormat.SIC34),
        InventoryDestination("Alcantarillas","SIC-18 · SIC-18A cuando corresponde","OBRAS DE ARTE Y DRENAJE",asset=RoadAssetType.CULVERT),
        InventoryDestination("Cunetas / drenaje","SIC-19","OBRAS DE ARTE Y DRENAJE",asset=RoadAssetType.DITCH),
        InventoryDestination("Badenes","SIC-20","OBRAS DE ARTE Y DRENAJE",asset=RoadAssetType.FORD),
        InventoryDestination("Muros","SIC-20","OBRAS DE ARTE Y DRENAJE",asset=RoadAssetType.WALL),
        InventoryDestination("Túneles","SIC-20","OBRAS DE ARTE Y DRENAJE",asset=RoadAssetType.TUNNEL),
        InventoryDestination("Derecho de vía","SIC-23","DERECHO DE VÍA",asset=RoadAssetType.RIGHT_OF_WAY),
        InventoryDestination("Puentes","SIC-17 · SCAP","PUENTES",asset=RoadAssetType.BRIDGE))
    fun title(code: String) = when(code) {
        "SIC-08" -> "Fajas de las calzadas"
        "SIC-09" -> "Ancho de las fajas"
        "SIC-13" -> "Punto crítico"
        "SIC-34" -> "Accidente / zona de accidentabilidad"
        "SIC-18", "SIC-18A" -> "Alcantarilla"
        else -> code
    }
    fun parentId(record: InventoryRecordEntity, extensions: List<SicExtensionEntity>, records: List<InventoryRecordEntity>): String {
        val source = extensions.find { it.recordId == record.id && it.formatCode == "SIC-09" }?.sourceRecordId
        return source?.takeIf { id -> record.sicCode == "SIC-09" && records.any {
            it.id == id && it.sicCode == "SIC-08" && (it.status == record.status || it.status == "ACTIVE" && record.status == "DRAFT")
        } } ?: record.id
    }
    fun group(records: List<InventoryRecordEntity>, extensions: List<SicExtensionEntity>): List<InventoryElement> {
        val groups = records.groupBy { parentId(it, extensions, records) }
        return groups.map { (id, rows) -> InventoryElement(rows.first { it.id == id }, rows.filter { it.id != id }) }
    }
    fun requiresCulvertDetail(structural: String, functional: String) = EngineeringConditions.badCulvert(structural, functional)
    fun culvertDetailStatus(parent: Sic18Entity, detail: Sic18AEntity?, extension: SicExtensionEntity?): String {
        if (!requiresCulvertDetail(parent.structuralConditionCode, parent.functionalConditionCode))
            return if (detail != null || extension != null) "No requerido · antecedente conservado" else "No requerido"
        return if (detail == null || extension?.status == "DRAFT") "Pendiente" else "Completo"
    }
    fun exportFormats(rows: List<InventoryRecordForExport>): List<Pair<SicExportFormat, List<InventoryRecordForExport>>> {
        val output = mutableListOf<Pair<SicExportFormat,List<InventoryRecordForExport>>>()
        for (format in SicExportFormat.entries) {
            val selected = rows.filter { row ->
                row.record.status == "ACTIVE" && format.appliesTo(row) &&
                    (format.fieldFormat == null || row.sicExtension?.status != "DRAFT") &&
                    (format != SicExportFormat.SIC18A || row.sic18?.let { requiresCulvertDetail(it.structuralConditionCode,it.functionalConditionCode) } == true)
            }
            if (selected.isNotEmpty()) output += format to selected
        }
        return output
    }
}
