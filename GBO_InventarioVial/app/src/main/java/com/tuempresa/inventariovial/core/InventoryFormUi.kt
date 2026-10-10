package com.tuempresa.inventariovial.core

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.tuempresa.inventariovial.SectionTitle
import com.tuempresa.inventariovial.camera.*
import java.io.File

/** Shared containers; the existing theme, selectors, photographs and review own their UI. */
@Composable fun InventoryFormSection(title:String,subtitle:String?=null,content:@Composable ColumnScope.()->Unit) {
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        SectionTitle(title)
        subtitle?.let {Text(it,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
        content()
    } }
}

@Composable fun <T> InventoryFieldRows(fields:List<T>,content:@Composable (T)->Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns=if(maxWidth>=640.dp) 2 else 1
        Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
            fields.chunked(columns).forEach {pair->
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    pair.forEach {field->Column(Modifier.weight(1f)) {content(field)}}
                    if(columns==2 && pair.size==1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable fun InventoryElementPhotos(paths:List<String>,editable:Boolean,onChange:(List<String>)->Unit,
    onMetadata:(String,PhotoCaptureMetadata)->Unit) {
    val context=LocalContext.current
    var pending by rememberSaveable {mutableStateOf<String?>(null)}
    var remove by remember {mutableStateOf<File?>(null)}
    var error by remember {mutableStateOf<String?>(null)}
    val picker=rememberPhotoFilePicker({path->onChange((paths+path).distinct());onMetadata(path,PhotoCaptureMetadata.fromOriginal(path))},{error=it})
    val camera=rememberLauncherForActivityResult(FieldTakePicture()) {ok->
        pending?.let {path->if(ok) {
            onChange((paths+path).distinct());onMetadata(path,PhotoCaptureMetadata(System.currentTimeMillis(),null))
            PhotoCaptureMetadata.capture(context) {onMetadata(path,it)}
        } else File(path).delete()};pending=null
    }
    fun take() {runCatching {val photo=createPhotoFile(context,"INVENTARIO");pending=photo.file.absolutePath;camera.launch(photo.uri)}.onFailure {error=it.message}}
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {if(it) take() else error="Permiso de cámara denegado"}
    PhotoCaptureCard("Fotografías del elemento · General",paths.map(::File),
        onTakePhoto={if(ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)take() else permission.launch(Manifest.permission.CAMERA)},
        onSelectFile=picker,onDelete={remove=it},editable=editable && pending==null)
    error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
    remove?.let {file->AlertDialog(onDismissRequest={remove=null},title={Text("Eliminar fotografía de la ficha")},
        text={Text("La fotografía dejará de estar vinculada a este elemento.")},
        confirmButton={TextButton(onClick={onChange(paths-file.path);remove=null}) {Text("Eliminar")}},
        dismissButton={TextButton(onClick={remove=null}) {Text("Cancelar")}})}
}
