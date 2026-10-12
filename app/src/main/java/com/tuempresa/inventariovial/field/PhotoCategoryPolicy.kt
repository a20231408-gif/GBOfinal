package com.tuempresa.inventariovial.field

/** Category metadata for CORE; existing path and category keys are preserved. */
object PhotoCategoryPolicy {
    const val GENERAL = "GENERAL"
    data class Category(val key: String, val label: String, val contractualName: String, val multipleAllowed: Boolean = true)
    private val general = listOf(Category(GENERAL, "General", GENERAL))
    val culvert = listOf(Category("CULVERT_PANORAMIC", "Panorámica", "PANORAMICA"),
        Category("CULVERT_INLET", "Entrada", "ENTRADA"), Category("CULVERT_OUTLET", "Salida", "SALIDA"))

    val linear = listOf(Category("INICIO", "Inicio", "INICIO"), Category("FIN", "Fin", "FIN"),
        Category("OBSERVACION", "Observación", "OBSERVACION"))
    val vertical = listOf(Category("PANORAMICA", "Panorámica", "PANORAMICA"), Category("ELEMENTO", "Elemento", "ELEMENTO"))
    fun categories(sicCode: String, assetType: String = "", classCode: String = "", typeCode: String = "", hasEnds: Boolean = true): List<Category> = when {
        sicCode in setOf("SIC-18", "SIC-18A") -> culvert
        sicCode == "SIC-22" -> vertical
        sicCode == "SIC-17" -> com.tuempresa.inventariovial.scap.domain.ScapPhotoCategories.labels.map { Category(it.key,it.value,com.tuempresa.inventariovial.core.DriveInventorySchema.category(it.key)) }
        sicCode == "SIC-20" && classCode == "12" -> linear // Physical entrance/exit of a ford, even with one contractual PR.
        hasEnds && (sicCode == "SIC-19" || sicCode == "SIC-20" || sicCode == "SIC-23" && classCode in setOf("21","22") || sicCode == "SIC-21" &&
            (classCode in setOf("18", "20") || classCode == "19" && typeCode == "1" || assetType == "HORIZONTAL_STUDS")) -> linear
        else -> general
    }

    /** Null / historic metadata falls back to GENERAL; never infer from a filename. */
    fun category(sicCode: String, assetType: String = "", stored: String?): String =
        categories(sicCode,assetType).firstOrNull {it.key == stored || it.contractualName == stored}?.key ?: GENERAL
}
