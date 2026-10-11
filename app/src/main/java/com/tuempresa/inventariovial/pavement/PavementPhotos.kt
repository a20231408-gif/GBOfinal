package com.tuempresa.inventariovial.pavement

import android.content.Context
import android.net.Uri
import android.graphics.BitmapFactory
import java.io.File

internal object PavementPhotos {
    // Same managed-copy pattern as CulvertPhotos; no temporary URI is persisted.
    fun importImage(context: Context, uri: Uri): File {
        // Imports need a managed file, not a FileProvider URI for an external camera.
        val directory=File(context.filesDir,"images")
        require(directory.isDirectory || directory.mkdirs()) { "No se pudo crear el directorio de fotografías." }
        val file = File.createTempFile("PAVIMENTO_IMPORT_", ".jpg", directory)
        try {
            context.contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "No se pudo abrir la imagen." }
                file.outputStream().use { input.copyTo(it) }
            }
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Imagen no compatible." }
            // Existing JPEG bytes and EXIF remain untouched. Other formats need JPEG conversion for protocol 1.
            val jpeg=file.inputStream().use { it.read()==0xff && it.read()==0xd8 }
            if(!jpeg) {
                val bitmap=requireNotNull(com.tuempresa.inventariovial.camera.loadCorrectlyOrientedBitmap(file.absolutePath)) { "Imagen no compatible." }
                try { file.outputStream().use { require(bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG,95,it)) } }
                finally { bitmap.recycle() }
            }
            return file
        } catch (e: Exception) { file.delete(); throw e }
    }
}
