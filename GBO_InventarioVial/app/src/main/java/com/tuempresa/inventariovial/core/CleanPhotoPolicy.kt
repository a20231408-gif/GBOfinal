package com.tuempresa.inventariovial.core

import java.io.File

/** New delivery adapter: use untouched capture bytes and separate metadata.
 * Existing server delivery is deliberately NOT migrated until its watermark protocol is updated.
 */
data class PhotoForDelivery(val id: String, val originalPath: String, val category: PhotoCategory,
    val metadataJson: String)

object CleanPhotoPolicy {
    fun original(photo: PhotoForDelivery): File {
        val file = File(photo.originalPath)
        require(file.isFile && file.canRead() && file.length() > 0) { "Fotografía original no disponible" }
        return file // Do not stamp, recompress or mutate photo pixels.
    }
    fun byCategory(photos: List<PhotoForDelivery>): Map<PhotoCategory, List<PhotoForDelivery>> =
        photos.groupBy { it.category }
}
