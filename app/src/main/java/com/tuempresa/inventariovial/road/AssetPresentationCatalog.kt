package com.tuempresa.inventariovial.road

import com.tuempresa.inventariovial.data.entity.InventoryRecordWithPhotos
import java.text.Normalizer
import java.util.Locale

enum class AssetFamily(val abbreviation: String, val label: String, val folderLabel: String,
    val mapFolder: String, val rgb: Int, val sicGroup: String) {
    PUENTE("P", "Puente", "PUENTE", "PUENTES", 0x228B22, "SIC-17"),
    ALCANTARILLA("ALC", "Alcantarilla", "ALCANTARILLA", "ALCANTARILLAS", 0x0066FF, "SIC-18"),
    CUNETA("CUN", "Cuneta", "CUNETA", "CUNETAS", 0xFF8800, "SIC-19"),
    BADEN("BAD", "Badén", "BADEN", "BADENES", 0x9933CC, "SIC-20"),
    TUNEL("TUN", "Túnel", "TUNEL", "OTROS", 0x9933CC, "SIC-20"),
    MURO("MUR", "Muro", "MURO", "OTROS", 0x9933CC, "SIC-20"),
    SENALIZACION_VERTICAL("SV", "Señalización vertical", "SEÑALIZACION VERTICAL", "SEÑALIZACIÓN", 0xFF0000, "SIC-22"),
    CANAL("CAN", "Canal", "CANAL", "OTROS", 0xFF8800, "SIC-19"),
    BAJADA_AGUA("BAJ", "Bajada de agua", "BAJADA DE AGUA", "OTROS", 0xFF8800, "SIC-19"),
    ZANJA_DRENAJE("ZD", "Zanja de drenaje", "ZANJA DE DRENAJE", "OTROS", 0xFF8800, "SIC-19"),
    SENALIZACION_HORIZONTAL("SH", "Señalización horizontal", "SEÑALIZACION HORIZONTAL", "SEÑALIZACIÓN", 0x00A6A6, "SIC-21"),
    DERECHO_DE_VIA("DV", "Derecho de vía", "DERECHO DE VIA", "OTROS", 0x8B5A2B, "SIC-23"),
    CALZADA_FAJAS("FAJ", "Calzada y fajas", "FAJA", "CALZADA_FAJAS", 0x167D8D, "SIC-08"),
    PUNTOS_CRITICOS("PC", "Puntos críticos", "PUNTO CRITICO", "PUNTOS_CRITICOS", 0xE25C2A, "SIC-13"),
    ACCIDENTES("ACC", "Accidentes", "ACCIDENTE", "ACCIDENTES", 0xB32835, "SIC-34"),
    OTROS("ELM", "Otros/no clasificados", "ELEMENTO", "OTROS", 0x808080, "OTROS");

    val kmlColor: String get() = String.format(Locale.ROOT, "ff%02x%02x%02x", rgb and 255, (rgb shr 8) and 255, (rgb shr 16) and 255)
}

/** Single presentation catalog for ordering, UI, folder plans and GIS. Detail classes take precedence. */
object AssetPresentationCatalog {
    // Una familia representativa por grupo SIC para la leyenda del mapa.
    val legend = listOf(
        AssetFamily.PUENTE,
        AssetFamily.ALCANTARILLA,
        AssetFamily.CUNETA,
        AssetFamily.BADEN,
        AssetFamily.SENALIZACION_HORIZONTAL,
        AssetFamily.SENALIZACION_VERTICAL,
        AssetFamily.DERECHO_DE_VIA,
        AssetFamily.OTROS
    )

    /** Grupo visual por SIC. No depende de relaciones opcionales y es seguro para registros legacy. */
    fun sicGroup(item: InventoryRecordWithPhotos): String = when (item.record.sicCode) {
        "SIC-08", "SIC-09", "SIC-13", "SIC-34", "SIC-17", "SIC-18", "SIC-19", "SIC-20", "SIC-21", "SIC-22", "SIC-23" -> item.record.sicCode
        else -> resolve(item).sicGroup
    }

    /** Color del mapa por familia contractual SIC, como en VIDEO_SHOWCASE. */
    fun colorFamily(item: InventoryRecordWithPhotos): AssetFamily = when (sicGroup(item)) {
        "SIC-08", "SIC-09" -> AssetFamily.CALZADA_FAJAS
        "SIC-13" -> AssetFamily.PUNTOS_CRITICOS
        "SIC-34" -> AssetFamily.ACCIDENTES
        "SIC-17" -> AssetFamily.PUENTE
        "SIC-18" -> AssetFamily.ALCANTARILLA
        "SIC-19" -> AssetFamily.CUNETA
        "SIC-20" -> AssetFamily.BADEN
        "SIC-21" -> AssetFamily.SENALIZACION_HORIZONTAL
        "SIC-22" -> AssetFamily.SENALIZACION_VERTICAL
        "SIC-23" -> AssetFamily.DERECHO_DE_VIA
        else -> AssetFamily.OTROS
    }

    fun resolve(item: InventoryRecordWithPhotos): AssetFamily {
        val r = item.record
        when (r.sicCode) {
            "SIC-08", "SIC-09" -> return AssetFamily.CALZADA_FAJAS
            "SIC-13" -> return AssetFamily.PUNTOS_CRITICOS
            "SIC-34" -> return AssetFamily.ACCIDENTES
            "SIC-17" -> return AssetFamily.PUENTE
            "SIC-18" -> return AssetFamily.ALCANTARILLA
            "SIC-22" -> return AssetFamily.SENALIZACION_VERTICAL
            "SIC-19" -> item.sic19?.let { return when (it.classCode) {
                "08", "13" -> AssetFamily.CUNETA
                "09" -> AssetFamily.CANAL
                "10" -> AssetFamily.BAJADA_AGUA
                "11" -> AssetFamily.ZANJA_DRENAJE
                else -> AssetFamily.OTROS
            } }
            "SIC-20" -> item.sic20?.let { return when (it.classCode) {
                "12" -> AssetFamily.BADEN
                "13" -> AssetFamily.TUNEL
                "14" -> AssetFamily.MURO
                else -> AssetFamily.OTROS
            } }
            "SIC-23" -> item.sic23?.let { return if (it.classCode == "21") AssetFamily.DERECHO_DE_VIA else AssetFamily.OTROS }
        }
        val type = Normalizer.normalize(r.assetType, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "").trim().uppercase(Locale.ROOT).replace(' ', '_')
        return when (type) {
            "BRIDGE", "PUENTE" -> AssetFamily.PUENTE
            "CULVERT", "ALCANTARILLA" -> AssetFamily.ALCANTARILLA
            "DITCH", "CUNETA" -> AssetFamily.CUNETA
            "FORD", "BADEN" -> AssetFamily.BADEN
            "TUNNEL", "TUNEL" -> AssetFamily.TUNEL
            "WALL", "MURO" -> AssetFamily.MURO
            "VERTICAL_SIGN", "SENALIZACION_VERTICAL" -> AssetFamily.SENALIZACION_VERTICAL
            "CANAL" -> AssetFamily.CANAL
            "BAJADA_AGUA", "BAJADA_DE_AGUA" -> AssetFamily.BAJADA_AGUA
            "ZANJA_DRENAJE", "ZANJA_DE_DRENAJE" -> AssetFamily.ZANJA_DRENAJE
            "HORIZONTAL_MARKS", "HORIZONTAL_STUDS", "SAFETY", "SENALIZACION_HORIZONTAL" -> AssetFamily.SENALIZACION_HORIZONTAL
            "RIGHT_OF_WAY", "DERECHO_VIA", "DERECHO_DE_VIA" -> AssetFamily.DERECHO_DE_VIA
            else -> AssetFamily.OTROS
        }
    }
}
