package com.tuempresa.inventariovial.field

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.tuempresa.inventariovial.camera.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.io.File

@Composable
fun CulvertPhotos(paths:List<String>,categories:Map<String,String>,onPhotoMetadata:(String,PhotoCaptureMetadata)->Unit={_,_->},
    captureMetadata:Map<String,PhotoCaptureMetadata> = emptyMap(),onChange:(List<String>,Map<String,String>)->Unit) {
    LegacyInventoryPhotos("SIC-18",paths,categories,onPhotoMetadata=onPhotoMetadata,captureMetadata=captureMetadata,onChange=onChange)
}

/** Existing managed originals, camera contract and previews, shared by capture and reopening. */
@Composable
fun LegacyInventoryPhotos(sicCode:String,paths:List<String>,categories:Map<String,String>,editable:Boolean=true,captureLocation:Boolean=true,
    assetType:String="",classCode:String="",typeCode:String="",hasEnds:Boolean=true,
    onPhotoMetadata:(String,PhotoCaptureMetadata)->Unit={_,_->},captureMetadata:Map<String,PhotoCaptureMetadata> = emptyMap(),
    onChange:(List<String>,Map<String,String>)->Unit) {
    val context=LocalContext.current;val scope=rememberCoroutineScope()
    val options=PhotoCategoryPolicy.categories(sicCode,assetType,classCode,typeCode,hasEnds)
    val currentPaths by rememberUpdatedState(paths)
    val currentCategories by rememberUpdatedState(categories)
    val currentCaptures by rememberUpdatedState(captureMetadata)
    val change by rememberUpdatedState(onChange)
    val metadata by rememberUpdatedState(onPhotoMetadata)
    var category by rememberSaveable {mutableStateOf(options.first().key)}
    var target by rememberSaveable {mutableStateOf<String?>(null)}
    var cameraPath by rememberSaveable {mutableStateOf<String?>(null)}
    var remove by remember {mutableStateOf<String?>(null)}
    var replace by remember {mutableStateOf<String?>(null)}
    var error by remember {mutableStateOf<String?>(null)}
    var importing by remember {mutableStateOf(false)}
    fun attach(batch:List<String>,old:String?,key:String) {
        val originals=batch.distinct()
        originals.forEach {path->metadata(path,PhotoCaptureMetadata.fromOriginal(path).copy(replacesPath=old?.let {currentCaptures[it]?.replacesPath ?: it}))}
        val updated=if(old==null)(currentPaths+originals).distinct() else currentPaths.map {if(it==old)originals.single() else it}
        change(updated,currentCategories.filterKeys {it!=old}+originals.associateWith {key})
    }
    val camera=rememberLauncherForActivityResult(com.tuempresa.inventariovial.core.FieldTakePicture()) {ok->
        val old=target;val key=category
        cameraPath?.let {path->if(ok) {
            val previous=old?.let {currentCaptures[it]?.replacesPath ?: it}
            attach(listOf(path),old,key)
            val time=System.currentTimeMillis()
            metadata(path,PhotoCaptureMetadata(time,null,replacesPath=previous))
            if(captureLocation) PhotoCaptureMetadata.capture(context,time) {metadata(path,it.copy(replacesPath=previous))}
        }} // Cancelled captures never remove an existing original.
        cameraPath=null;target=null
    }
    fun take() {runCatching {val photo=createPhotoFile(context,"INVENTARIO");cameraPath=photo.file.absolutePath;camera.launch(photo.uri)}.onFailure {error=it.message;target=null}}
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {if(it)take() else {error="Permiso de cámara denegado. Puedes incorporar fotografías.";target=null}}
    fun takeFor(key:String,old:String?=null) {category=key;target=old
        if(ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)take() else permission.launch(Manifest.permission.CAMERA)}
    suspend fun copy(uri:android.net.Uri):String=withContext(Dispatchers.IO) {
        val f=createPhotoFile(context,"INVENTARIO_IMPORT").file
        context.contentResolver.openInputStream(uri).use {input->requireNotNull(input);f.outputStream().use {input.copyTo(it)}}
        val bounds=android.graphics.BitmapFactory.Options().apply {inJustDecodeBounds=true}
        android.graphics.BitmapFactory.decodeFile(f.path,bounds)
        require(bounds.outWidth>0 && bounds.outHeight>0) {"Imagen no compatible"};f.absolutePath
    }
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) {uris->
        val key=category
        if(uris.isNotEmpty()) {importing=true;scope.launch {try {
            val copied=uris.map {copy(it)};attach(copied,null,key)
        }catch(e:Exception){error=e.message}finally{importing=false}}}
    }
    val replacement=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {uri->
        val old=target;val key=category
        if(uri!=null && old!=null) {importing=true;scope.launch {try {attach(listOf(copy(uri)),old,key)}catch(e:Exception){error=e.message}finally{importing=false;target=null}}} else target=null
    }
    Column(verticalArrangement=Arrangement.spacedBy(12.dp),modifier=Modifier.testTag("photo-section")) {
        Text("FOTOGRAFÍAS",style=MaterialTheme.typography.titleLarge)
        // Unknown / GENERAL photos remain visible and retain their category until explicitly changed.
        val historical=currentPaths.map {currentCategories[it] ?: "GENERAL"}.distinct().filter {key->options.none {it.key==key}}
        val shown=options+historical.map {PhotoCategoryPolicy.Category(it,"Históricas · $it",it)}
        shown.forEach {option->
            val photos=paths.filter {(categories[it] ?: "GENERAL")==option.key}
            com.tuempresa.inventariovial.core.PhotoCaptureCard("${option.label.uppercase(java.util.Locale.ROOT)} — ${photos.size} fotos",photos.map(::File),
                onTakePhoto={takeFor(option.key)},onSelectFile={category=option.key;picker.launch("image/*")},
                onDelete={remove=it.path},editable=editable && !importing && cameraPath==null,
                captureEnabled=option in options,onReplace={replace=it.path})
            if(editable) photos.forEach {path->
                com.tuempresa.inventariovial.scap.ui.ScapChoice("Categoría · Foto ${photos.indexOf(path)+1}",option.label,
                    (options+if(option !in options)listOf(option) else emptyList()).map {it.label}) {label->
                    options.find {it.label==label}?.let {change(currentPaths,currentCategories+(path to it.key))}
                }
            }
        }
        if(importing) LinearProgressIndicator(Modifier.fillMaxWidth())
        error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
    }
    remove?.let {path->AlertDialog(onDismissRequest={remove=null},title={Text("Retirar fotografía")},
        text={Text("Se retirará de esta ficha al guardar. El original y el historial se conservan; la retirada remota requiere confirmar esta revisión.")},
        confirmButton={TextButton(onClick={change(currentPaths-path,currentCategories-path);remove=null}){Text("Retirar")}},dismissButton={TextButton(onClick={remove=null}){Text("Cancelar")}})}
    replace?.let {path->AlertDialog(onDismissRequest={replace=null},title={Text("Reemplazar fotografía")},text={Text("Se conserva su identidad y el original anterior. Elige la nueva imagen.")},
        confirmButton={TextButton(onClick={val key=currentCategories[path] ?: "GENERAL";replace=null;takeFor(key,path)}){Text("Tomar foto")}},
        dismissButton={Row {TextButton(onClick={category=currentCategories[path] ?: "GENERAL";target=path;replace=null;replacement.launch("image/*")}){Text("Incorporar")};TextButton(onClick={replace=null}){Text("Cancelar")}}})}
}
