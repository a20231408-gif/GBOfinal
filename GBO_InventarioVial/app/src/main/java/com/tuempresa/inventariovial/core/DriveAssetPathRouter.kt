package com.tuempresa.inventariovial.core

import java.text.Normalizer
import java.util.Locale

/** Logical paths ONLY. No remote mutations; existing SIB delivery folders remain untouched. */
data class AssetPathRequest(
    val route: String,
    val assetFamily: String,
    val assetSequence: String,
    val photoCategory: PhotoCategory? = null,
    val documentType: String? = null,
    val pavementSurface: String? = null
)

@JvmInline
value class PhotoCategory(val code: String) {
    companion object {
        val GENERAL = PhotoCategory("GENERAL")
        val PANORAMICA = PhotoCategory("PANORAMICA")
        val ENTRADA = PhotoCategory("ENTRADA")
        val SALIDA = PhotoCategory("SALIDA")
        /** Explicit field from capture metadata, NEVER guessed from a file name. */
        fun fromStored(value: String?): PhotoCategory = PhotoCategory(value?.takeIf { it.isNotBlank() } ?: "GENERAL")
    }
}

object DriveAssetPathRouter {
    private val illegal = Regex("[^A-Z0-9_-]+")
    /** Canonicalize labels; reject empty/relative components instead of hiding path traversal. */
    fun component(raw: String): String {
        require(raw.isNotBlank() && !raw.contains("..") && '/' !in raw && '\\' !in raw && '\u0000' !in raw) {
            "Componente de ruta inválido"
        }
        val normalized = Normalizer.normalize(raw.trim(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .uppercase(Locale.ROOT)
            .replace(Regex("\\s+"), "_")
            .replace(illegal, "_").trim('_')
        require(normalized.isNotBlank() && normalized.length <= 80) { "Componente de ruta inválido" }
        return normalized
    }

    fun path(request: AssetPathRequest): String = buildList {
        add(component(request.route))
        add(component(request.assetFamily))
        request.pavementSurface?.let { add(component(it)) }
        add(component(request.assetSequence))
        request.photoCategory?.let { add(component(it.code)) }
        request.documentType?.let { add(component(it)) }
    }.joinToString("/")

    fun photoFolder(route: String, family: String, sequence: String, category: PhotoCategory? = null,
        pavementSurface: String? = null): String = path(AssetPathRequest(route, family, sequence,
        category ?: PhotoCategory.GENERAL, pavementSurface = pavementSurface))
}
