package com.tuempresa.inventariovial.core

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Managed original copy, independent of temporary SAF permission; repositories retain ownership. */
@Composable fun rememberPhotoFilePicker(onImported:(String)->Unit,onError:(String)->Unit):()->Unit {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    val attach by rememberUpdatedState(onImported)
    val error by rememberUpdatedState(onError)
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if(uri!=null) scope.launch {
            try {
                val path=withContext(Dispatchers.IO) {
                    val dir=File(context.filesDir,"images").apply { mkdirs() }
                    val f=File.createTempFile("FIELD_IMPORT_",".jpg",dir)
                    try {
                        requireNotNull(context.contentResolver.openInputStream(uri)).use { input -> f.outputStream().use { input.copyTo(it) } }
                        val b=android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds=true }
                        android.graphics.BitmapFactory.decodeFile(f.path,b)
                        require(b.outWidth>0 && b.outHeight>0) { "Imagen no compatible" }
                        f.absolutePath
                    } catch(e:Exception) { f.delete();throw e }
                }
                attach(path)
            } catch(e:Exception) { error(e.message ?: "No se pudo importar la fotografía") }
        }
    }
    return { picker.launch("image/*") }
}
