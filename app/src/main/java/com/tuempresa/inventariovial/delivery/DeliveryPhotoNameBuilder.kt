package com.tuempresa.inventariovial.delivery

import com.tuempresa.inventariovial.road.OrderedAsset
import java.util.Locale
import java.math.BigDecimal

enum class DeliveryNamingPolicy { FOLDER_COMPACT, LEGACY_COMPATIBLE }

object FileNameComplianceValidator {
    const val MAX_LENGTH = 20
    fun warning(name: String): String? = when {
        !safeComponent(name) -> "Nombre digital no seguro: $name"
        name.length > MAX_LENGTH -> "$name tiene ${name.length} caracteres; supera el límite de 20. Usa FOLDER_COMPACT."
        else -> null
    }
    fun safeComponent(name: String) = name.isNotBlank() && name == name.trim() && !name.contains("..") &&
        name.none { it == '/' || it == '\\' || it == ':' || it.code < 32 }
}

object DeliveryPhotoNameBuilder {
    fun build(sequence: Int, photoIndex: Int, asset: OrderedAsset, policy: DeliveryNamingPolicy): String {
        require(sequence > 0 && photoIndex > 0)
        return when (policy) {
            DeliveryNamingPolicy.FOLDER_COMPACT -> String.format(Locale.ROOT, "%03d_F%02d.jpg", sequence, photoIndex)
            DeliveryNamingPolicy.LEGACY_COMPATIBLE -> {
                val r = asset.item.record
                val distance = BigDecimal.valueOf(r.startDistanceM).stripTrailingZeros().toPlainString()
                listOfNotNull(sequence.toString(), asset.routeCode, asset.assetFamily.abbreviation,
                    r.sideCode?.takeIf { it.isNotBlank() }, "${r.startPrCode}+$distance").joinToString("_") + ".jpg"
            }
        }
    }
}
