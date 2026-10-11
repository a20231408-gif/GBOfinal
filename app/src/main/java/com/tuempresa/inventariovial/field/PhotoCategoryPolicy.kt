package com.tuempresa.inventariovial.field

/** Category metadata for CORE; existing path and category keys are preserved. */
object PhotoCategoryPolicy {
    const val GENERAL = "GENERAL"
    data class Category(val key: String, val label: String, val contractualName: String, val multipleAllowed: Boolean = true)
    private val general = listOf(Category(GENERAL, "General", GENERAL))
    val culvert = listOf(Category("CULVERT_PANORAMIC", "Panorámica", "PANORAMICA"),
        Category("CULVERT_INLET", "Entrada", "ENTRADA"), Category("CULVERT_OUTLET", "Salida", "SALIDA"))

    fun categories(sicCode: String, assetType: String = ""): List<Category> =
        if (sicCode == "SIC-18" || assetType in setOf("CULVERT", "ALCANTARILLA")) culvert else general

    /** Null / historic metadata falls back to GENERAL; never infer from a filename. */
    fun category(sicCode: String, assetType: String = "", stored: String?): String =
        categories(sicCode,assetType).firstOrNull {it.key == stored || it.contractualName == stored}?.key ?: GENERAL
}
