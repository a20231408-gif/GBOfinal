package com.tuempresa.inventariovial.field

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.tuempresa.inventariovial.camera.loadCorrectlyOrientedBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable fun InventoryPhotoGallery(paths: List<String>, onAdd: ((String)->Unit)?=null, onTake: (()->Unit)?=null, onRemove: (String)->Unit) {
    val picker=com.tuempresa.inventariovial.core.rememberPhotoFilePicker({ onAdd?.invoke(it) },{})
    com.tuempresa.inventariovial.core.PhotoCaptureCard("Fotografías del elemento",paths.map { java.io.File(it) },
        onTakePhoto={ onTake?.invoke() },onSelectFile=picker,onDelete={ onRemove(it.path) },captureEnabled=onAdd!=null && onTake!=null)
}
@Composable internal fun LocalPhotoImage(path: String, maxEdge: Int, modifier: Modifier) {
    val bitmap by produceState<Bitmap?>(null,path,maxEdge) {
        value=withContext(Dispatchers.IO) { loadCorrectlyOrientedBitmap(path,maxEdge) }
    }
    // The renderer retains ImageBitmap references across composition/frame boundaries.
    // Reading the delegated state from the old effect's onDispose recycled the NEW
    // bitmap as soon as decoding completed (null -> bitmap), crashing both review
    // screens. Let GC release these bounded thumbnails when rendering releases them.
    val image=bitmap
    if(image!=null) Image(image.asImageBitmap(),"Fotografía del elemento",modifier,contentScale=ContentScale.Fit)
    else Box(modifier) { Text("Cargando / fotografía no disponible") }
}

/** Reuses the current local decoder for review; no capture or storage infrastructure. */
@Composable fun InventoryPhotoReview(paths: List<String>) {
    var preview by rememberSaveable { mutableStateOf<String?>(null) }
    LazyRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        itemsIndexed(paths) {index,path->
            LocalPhotoImage(path,480,Modifier.size(148.dp,112.dp).testTag("review-photo-$index").clickable {preview=path})
        }
    }
    preview?.let { path -> com.tuempresa.inventariovial.core.PhotoPreview(java.io.File(path)) { preview=null } }
}
