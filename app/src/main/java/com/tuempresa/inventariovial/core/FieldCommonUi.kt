package com.tuempresa.inventariovial.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class FieldCamera(val cameraId: String, val label: String, val external: Boolean)
object FieldCameraCatalog {
    fun cameras(context: Context): List<FieldCamera> = runCatching {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        manager.cameraIdList.map { id ->
            val lens = manager.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING)
            val label = when(lens) {
                CameraCharacteristics.LENS_FACING_BACK -> "Cámara principal"
                CameraCharacteristics.LENS_FACING_FRONT -> "Cámara frontal"
                CameraCharacteristics.LENS_FACING_EXTERNAL -> "Cámara externa"
                else -> "Cámara $id"
            }
            FieldCamera(id, label, lens == CameraCharacteristics.LENS_FACING_EXTERNAL)
        }
    }.getOrDefault(emptyList())
}

@Composable
fun PhotoPreview(file: File, onClose: () -> Unit) {
    val bitmap by produceState<Bitmap?>(null, file.path) {
        value = withContext(Dispatchers.IO) {
            if (file.isFile) com.tuempresa.inventariovial.camera.loadCorrectlyOrientedBitmap(file.path,1600) else null
        }
    }
    Dialog(onDismissRequest = onClose, properties=androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(shape=MaterialTheme.shapes.medium) {
            Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (bitmap != null) Image(bitmap!!.asImageBitmap(), "Fotografía ampliada", Modifier.fillMaxWidth().weight(1f), contentScale = ContentScale.Fit)
                else Text("No se pudo abrir la fotografía")
                TextButton(onClick=onClose) { Text("Cerrar") }
            }
        }
    }
}

/** A UI adapter; the host owns camera activity contracts and persistence. */
@Composable
fun PhotoCaptureCard(title: String, photos: List<File>, onTakePhoto: (cameraId: String?) -> Unit,
    onSelectFile: () -> Unit, onDelete: (File) -> Unit, captureEnabled: Boolean = true, editable: Boolean = true, onReplace: ((File)->Unit)? = null) {
    val context = LocalContext.current
    val available = remember(context) { FieldCameraCatalog.cameras(context) }
    var chosen by remember { mutableStateOf(FieldCameraSelection.chosen(context)) }
    var expanded by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<File?>(null) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style=MaterialTheme.typography.titleMedium)
            if(captureEnabled && editable) Box {
                OutlinedButton(onClick={expanded = true}) {
                    Text(available.firstOrNull { it.cameraId == chosen }?.label ?: "Cámara de Android")
                }
                DropdownMenu(expanded=expanded, onDismissRequest={expanded=false}) {
                    DropdownMenuItem(text={Text("Cámara de Android")},onClick={chosen=null;FieldCameraSelection.select(context,null);expanded=false})
                    available.forEach { camera -> DropdownMenuItem(text={ Text(camera.label) }, onClick={chosen=camera.cameraId; FieldCameraSelection.select(context,chosen); expanded=false}) }
                    if (available.none { it.external }) DropdownMenuItem(text={Text("Cámara externa no detectada por Android")}, enabled=false, onClick={})
                }
            }
            if(captureEnabled && editable) Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Button(onClick={onTakePhoto(chosen)},modifier=Modifier.heightIn(min=56.dp)) { Text("Tomar foto") }
                OutlinedButton(onClick=onSelectFile,modifier=Modifier.heightIn(min=56.dp)) { Text("Incorporar fotos") }
            }
            if(photos.isNotEmpty()) LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                items(photos, key={it.path}) { file ->
                    Column(horizontalAlignment=Alignment.CenterHorizontally) {
                        PhotoThumbnail(file, onClick = { selected=file },tag="inventory-photo-${photos.indexOf(file)}")
                        Text("Foto ${photos.indexOf(file)+1}", style=MaterialTheme.typography.labelSmall)
                        Row {
                            TextButton(onClick={selected=file}) {Text("Ver")}
                            if(editable && onReplace!=null) TextButton(onClick={onReplace(file)}) {Text("Reemplazar")}
                            if(editable) TextButton(onClick={onDelete(file)}) {Text("Eliminar")}
                        }
                    }
                }
            }
        }
    }
    selected?.let { PhotoPreview(it) {selected=null} }
}

@Composable
fun LocationSummaryCard(latitude: Double?, longitude: Double?, accuracyM: Double?, pr: String?,
    route: String?, source: String?, technicalActions: (@Composable () -> Unit)? = null) {
    var expanded by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Ubicación", style=MaterialTheme.typography.titleSmall)
            Text(if (latitude != null && longitude != null) "GPS obtenido" + (accuracyM?.let { " · ±%.1f m".format(it) } ?: "") else "GPS pendiente")
            pr?.let { Text("PR $it") }
            TextButton(onClick={expanded=!expanded}) {Text(if (expanded) "Ocultar detalles" else "Mostrar detalles")}
            if (expanded) {
                Text("Latitud: ${latitude ?: "—"} · Longitud: ${longitude ?: "—"}")
                Text("Ruta: ${route ?: "—"} · Fuente: ${source ?: "—"}")
                technicalActions?.invoke()
            }
        }
    }
}

@Composable
fun ValidationSummaryCard(completion: CompletionResult) {
    Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
        if (completion.issues.isEmpty()) Text("Datos completos")
        completion.issues.forEach { Text("${it.field}: ${it.reason}") }
        Text(if (completion.canFinalize) "Listo para finalizar" else "Puedes guardar borrador; revisa los campos antes de finalizar")
    }
}

@Composable
fun RecordReviewSection(title: String, fields: List<Pair<String,String>>, photos: List<File>, completion: CompletionResult, initiallyExpanded: Boolean = false) {
    var preview by remember { mutableStateOf<File?>(null) }
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            TextButton(onClick={expanded=!expanded}) { Text("$title · ${if (expanded) "Ocultar" else "Revisar"}") }
            if(expanded) {
                fields.forEach { (label, value) -> Text("$label: $value") }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(photos, key={it.path}) { f -> PhotoThumbnail(f, onClick={preview=f}) }
                }
                ValidationSummaryCard(completion)
            }
        }
    }
    preview?.let { PhotoPreview(it) { preview=null } }
}

@Composable
private fun PhotoThumbnail(file: File, onClick: () -> Unit,tag: String = "") {
    val bitmap by produceState<Bitmap?>(null, file.path) {
        value = withContext(Dispatchers.IO) {
            if (file.isFile) {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.path, options)
                var sample = 1
                while (maxOf(options.outWidth, options.outHeight) / sample > 160) sample *= 2
                BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
            } else null
        }
    }
    if (bitmap == null) OutlinedButton(onClick=onClick,modifier=Modifier.testTag(tag)) {Text("Ver foto")}
    else Image(bitmap!!.asImageBitmap(), contentDescription="Fotografía del elemento", modifier=Modifier.testTag(tag).size(88.dp).clickable(onClick=onClick), contentScale=ContentScale.Crop)
}
