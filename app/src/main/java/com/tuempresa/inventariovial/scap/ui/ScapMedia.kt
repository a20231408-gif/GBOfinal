package com.tuempresa.inventariovial.scap.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.tuempresa.inventariovial.camera.createPhotoFile
import com.tuempresa.inventariovial.camera.loadCorrectlyOrientedBitmap
import com.tuempresa.inventariovial.scap.catalog.ScapCatalog
import com.tuempresa.inventariovial.scap.data.ScapInspectionSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

internal val photoLabels=com.tuempresa.inventariovial.scap.domain.ScapPhotoCategories.labels
internal val sketchLabels=linkedMapOf("ELEVATION" to "Elevación", "PLAN" to "Planta", "CROSS_SECTION" to "Sección transversal")

@Composable
fun ScapMedia(s:ScapInspectionSnapshot,c:ScapController,catalog:ScapCatalog,editable:Boolean,sketch:Boolean,initialElementCode:String?=null) {
    if(!sketch && catalog.bridgeType!=null) {ScapPhotoRecords(s,c,catalog,editable);return}
    val context=LocalContext.current
    val historicVideos by remember(s.inspection.id) {com.tuempresa.inventariovial.media.VideoDatabase.get(context).videos().observe("SCAP/${s.inspection.id}")}.collectAsState(emptyList())
    val scope=rememberCoroutineScope()
    var category by rememberSaveable(sketch){mutableStateOf(if(sketch) "ELEVATION" else "GENERAL")}
    var code by rememberSaveable(initialElementCode) {mutableStateOf(initialElementCode.orEmpty())}
    var additional by rememberSaveable {mutableStateOf(false)}
    var cameraPath by rememberSaveable {mutableStateOf<String?>(null)}
    var captureCategory by rememberSaveable {mutableStateOf("")}
    var captureCode by rememberSaveable {mutableStateOf("")}
    var importing by remember {mutableStateOf(false)}
    var message by remember {mutableStateOf<String?>(null)}
    var removing by remember {mutableStateOf<String?>(null)}
    var replaceTarget by rememberSaveable {mutableStateOf<String?>(null)}
    val replacementPicker=com.tuempresa.inventariovial.core.rememberPhotoFilePicker({path->
        replaceTarget?.let {id->c.submit {it.replacePhoto(s.inspection.id,id,path)}};replaceTarget=null
    },{message=it;replaceTarget=null})
    val labels=if(sketch) sketchLabels else photoLabels
    fun attach(path:String,selectedCategory:String,selectedCode:String,capture:com.tuempresa.inventariovial.camera.PhotoCaptureMetadata?=null) {
        c.submit {if(sketch)it.addSketch(s.inspection.id,selectedCategory,path) else {
            val marked=File(path)
            it.addPhoto(s.inspection.id,path,selectedCategory,selectedCode.ifBlank{null},marked.absolutePath,capture)
        }}
    }
    val camera=rememberLauncherForActivityResult(com.tuempresa.inventariovial.core.FieldTakePicture()){ok->
        val path=cameraPath
        if(ok && path!=null) {
            val capture=com.tuempresa.inventariovial.camera.PhotoCaptureMetadata(System.currentTimeMillis(),null)
            c.submit {
                if(sketch) it.addSketch(s.inspection.id,captureCategory,path) else {
                    val marked=File(path)
                    it.addPhoto(s.inspection.id,path,captureCategory,captureCode.ifBlank {null},marked.absolutePath,capture)
                    com.tuempresa.inventariovial.camera.PhotoCaptureMetadata.capture(context,capture.capturedAt!!) { metadata ->
                        c.submit { repo -> repo.updatePhotoCapture(s.inspection.id,path,metadata) }
                        if(metadata.fix==null) message="Fotografía guardada sin GNSS propio disponible."
                    }
                }
            }
        }
        else if(path!=null) File(path).delete()
        cameraPath=null
    }
    val takePhoto:()->Unit={
        runCatching {
            val photo=createPhotoFile(context,"SCAP")
            cameraPath=photo.file.absolutePath
            camera.launch(photo.uri)
        }.onFailure{message="No se pudo abrir la cámara: ${it.message}"}
    }
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){if(it)takePhoto() else message="Permiso de cámara no concedido. Puedes importar una imagen."}
    val importer=rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()){uris->
        if(uris.isNotEmpty()) {
            val selectedCategory=captureCategory;val selectedCode=captureCode
            importing=true
            scope.launch {
                try {
                    for(uri in uris) {
                    val path=withContext(Dispatchers.IO) {
                        val directory=File(context.filesDir,"images").apply{mkdirs()}
                        val mime=context.contentResolver.getType(uri)
                        val suffix=when(mime){"image/png"->".png";"image/webp"->".webp";else->".jpg"}
                        val file=File.createTempFile("SCAP_IMPORT_",suffix,directory)
                        try {
                            context.contentResolver.openInputStream(uri).use{input->requireNotNull(input){"No se pudo leer la imagen."};file.outputStream().use{input.copyTo(it)}}
                            val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true};BitmapFactory.decodeFile(file.absolutePath,bounds)
                            require(bounds.outWidth>0 && bounds.outHeight>0){"El archivo no es una imagen compatible."}
                            file.absolutePath
                        } catch(e:Exception){file.delete();throw e}
                    }
                    attach(path,selectedCategory,selectedCode)
                    }
                } catch(e:Exception){message="No se pudo importar: ${e.message}"} finally {importing=false}
            }
        }
    }
    val elementOptions=catalog.elements.map {"${it.code} · ${it.name}"}
    @Composable fun captureButtons(cat:String,element:String="") {
        com.tuempresa.inventariovial.core.PhotoCaptureCard("Añadir fotografía",emptyList(),
            onTakePhoto={captureCategory=cat;captureCode=element
                if(ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED) takePhoto() else permission.launch(Manifest.permission.CAMERA)},
            onSelectFile={captureCategory=cat;captureCode=element;importer.launch(arrayOf("image/*"))},onDelete={},editable=editable && !importing)
    }
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        if(sketch) item {ScapSketchDimensions(s,c,editable)}
        item {
            Text(if(sketch) "Croquis del puente" else "FOTOGRAFÍAS",style=MaterialTheme.typography.titleLarge)
            if(!sketch) {
                Text("Estas tomas son recordatorios; puedes registrar varias fotos por categoría.")
            }
            if(importing) LinearProgressIndicator(Modifier.fillMaxWidth())
            message?.let {Text(it,color=MaterialTheme.colorScheme.error)}
        }
        if(sketch) {
            sketchLabels.forEach {(cat,label)->
                item {ScapFieldTarget("inspection",cat){ScapElementCard(label) {captureButtons(cat)}}}
                items(s.sketches.filter {it.type==cat},key={it.id}) {item->ScapElementCard(label) {
                    ScapImage(item.localUri)
                    val principal=com.tuempresa.inventariovial.scap.domain.ScapSketchSelection.principal(s,cat)?.id==item.id
                    Text(if(principal) "Principal en Excel · también se conserva como anexo" else "Anexo · original conservado")
                    TextButton(enabled=editable,onClick={c.submit{it.selectPrincipalSketch(s.inspection.id,cat,if(principal)null else item.id)}}){Text(if(principal) "Dejar principal pendiente" else "Usar como croquis principal")}

                    TextButton(enabled=editable,onClick={removing=item.id}) {Text("Retirar croquis")}
                }}
            }
        } else {
            val categories=com.tuempresa.inventariovial.scap.domain.ScapPhotoCategories.primary.keys.toList() +
                (s.photos.map {it.photoCategory.orEmpty()}+historicVideos.map {it.category}).distinct().filter {it !in com.tuempresa.inventariovial.scap.domain.ScapPhotoCategories.primary}
            categories.forEach {cat->
                val photos=s.photos.filter {it.photoCategory.orEmpty()==cat}.sortedBy {it.photoIndex}
                item(key="category:$cat") {
                    com.tuempresa.inventariovial.media.BridgeMediaCategory("SCAP/${s.inspection.id}",com.tuempresa.inventariovial.field.PhotoCategoryPolicy.Category(cat.ifBlank {"GENERAL"},com.tuempresa.inventariovial.scap.domain.ScapPhotoCategories.label(cat),cat),editable&&!importing,
                        photoCount=photos.size,
                        onPhoto={captureCategory=cat.ifBlank {"OTHER"};captureCode=if(cat=="DEFECT")code else "";if(ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)takePhoto() else permission.launch(Manifest.permission.CAMERA)},
                        onImportPhotos={captureCategory=cat.ifBlank {"OTHER"};captureCode=if(cat=="DEFECT")code else "";importer.launch(arrayOf("image/*"))}) {
                        if(cat=="DEFECT")ScapChoice("Elemento SCAP (opcional)",catalog.element(code)?.let {"${it.code} · ${it.name}"}.orEmpty(),elementOptions,editable){code=it.substringBefore(" · ")}
                        photos.forEach {photo->ScapPhotoCard(s,c,catalog,photo,editable,onRemove={removing=photo.id},onReplace={replaceTarget=photo.id;replacementPicker()})}
                    }
                }
            }
            item {
                OutlinedButton(enabled=editable,onClick={additional=!additional}) {Text("+ AÑADIR FOTO / CATEGORÍA ADICIONAL")}
                if(additional) {
                    ScapChoice("Categoría adicional",photoLabels[category].orEmpty(),photoLabels.values.toList(),editable) {v->category=(photoLabels.entries.firstOrNull {it.value==v}?.key ?: "OTHER")}
                    captureButtons(category)
                }
            }
        }
        item {Text(if(sketch) "Croquis guardados: ${s.sketches.size}" else "Fotografías guardadas: ${s.photos.size}")}
    }
    removing?.let{id->AlertDialog(onDismissRequest={removing=null},title={Text(if(sketch) "Retirar croquis" else "Eliminar fotografía")},text={Text("Se retirará esta imagen de la ficha SCAP.")},
        confirmButton={TextButton(onClick={c.submit{if(sketch) it.removeSketch(s.inspection.id,id) else it.removePhoto(s.inspection.id,id)};removing=null}){Text("Retirar")}},dismissButton={TextButton(onClick={removing=null}){Text("Cancelar")}})}
}

@Composable
private fun ScapPhotoCard(s:ScapInspectionSnapshot,c:ScapController,catalog:ScapCatalog,photo:com.tuempresa.inventariovial.data.entity.PhotoEntity,editable:Boolean,onRemove:()->Unit,onReplace:()->Unit) {
    val context=LocalContext.current
    val historicVideos by remember(s.inspection.id) {com.tuempresa.inventariovial.media.VideoDatabase.get(context).videos().observe("SCAP/${s.inspection.id}")}.collectAsState(emptyList())
    ScapElementCard("Foto ${photo.photoIndex} · ${com.tuempresa.inventariovial.scap.domain.ScapPhotoCategories.label(photo.photoCategory)}") {
        Text("Fecha: ${java.text.SimpleDateFormat("dd/MM/yyyy",java.util.Locale.getDefault()).format(java.util.Date(photo.createdAt))}")
        var description by remember(photo.id) {mutableStateOf(photo.description.orEmpty())}
        OutlinedTextField(description,{description=it;c.submit {repo->repo.describePhoto(s.inspection.id,photo.id,it)}},label={Text("Descripción")},enabled=editable,modifier=Modifier.fillMaxWidth())
        ScapImage(com.tuempresa.inventariovial.DriveUploadPolicy.uploadPath(photo))
        ScapChoice("Categoría",com.tuempresa.inventariovial.scap.domain.ScapPhotoCategories.label(photo.photoCategory),photoLabels.values.toList(),editable) {v->
            c.submit {it.setPhotoMetadata(s.inspection.id,photo.id,(photoLabels.entries.firstOrNull {it.value==v}?.key ?: "OTHER"),photo.scapElementCode)}
        }
        ScapChoice("Elemento SCAP",catalog.element(photo.scapElementCode.orEmpty())?.let {"${it.code} · ${it.name}"}.orEmpty(),catalog.elements.map {"${it.code} · ${it.name}"},editable) {v->
            c.submit {it.setPhotoMetadata(s.inspection.id,photo.id,photo.photoCategory ?: "OTHER",v.substringBefore(" · ").ifBlank {null})}
        }
        if(photo.photoCategory=="DEFECT") {
            s.defects.filter {it.photoId==photo.id}.forEach {d->key(d.id) {ScapDefectFields(s,c,catalog,d,editable)}}
            OutlinedButton(enabled=editable,onClick={c.submit {it.saveDefect(s.inspection.id,com.tuempresa.inventariovial.scap.data.ScapDefectEntity(
                java.util.UUID.randomUUID().toString(),s.inspection.id,photo.scapElementCode,description,"",photo.id,false,true,System.currentTimeMillis()))}}) {Text("+ Asociar observación / ubicación")}
        }
        if(editable) {
            val original=photo.originalPath ?: photo.localPath
            val v=s.values()
            val location=photo.photoLatitude?.let {lat->photo.photoLongitude?.let {lon->com.tuempresa.inventariovial.location.GeoLocation(lat,lon,photo.photoGpsAccuracyM?.toFloat() ?: Float.NaN,photo.photoAltitudeM)}}
            com.tuempresa.inventariovial.field.PhotoStampPanel(listOf(original),com.tuempresa.inventariovial.camera.PhotoStampData(
                v["route"].orEmpty(),v["sic17.roadbedCode"].orEmpty(),v["progressive"].orEmpty(),location,photo.createdAt,s.inspection.createdBy,s.inspection.bridgeName),
                photo.stampedPath?.let {mapOf(original to it)} ?: emptyMap(),
                mapOf(original to com.tuempresa.inventariovial.camera.PhotoCaptureMetadata(photo.capturedAt,location,photo.photoLocationSource))) {paths->c.submit {
                    val marked=File(original)
                    val copy=com.tuempresa.inventariovial.camera.PhotoExif.writableCopy(File(original),marked,File(context.filesDir,"stamped"))
                    com.tuempresa.inventariovial.camera.PhotoExif.write(copy,File(original),photo)
                    it.stampPhoto(s.inspection.id,photo.id,copy.absolutePath)
                }}
        }
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            TextButton(enabled=editable && photo!=s.photos.minByOrNull {it.photoIndex},onClick={c.submit {it.movePhoto(s.inspection.id,photo.id,-1)}}) {Text("↑ Subir")}
            TextButton(enabled=editable && photo!=s.photos.maxByOrNull {it.photoIndex},onClick={c.submit {it.movePhoto(s.inspection.id,photo.id,1)}}) {Text("↓ Bajar")}
            TextButton(enabled=editable,onClick=onReplace) {Text("Reemplazar")}
            TextButton(enabled=editable,onClick=onRemove) {Text("Eliminar")}
        }
    }
}

@Composable
internal fun ScapImage(path:String) {
    var preview by remember(path) { mutableStateOf(false) }
    val bitmap by produceState<android.graphics.Bitmap?>(null,path) {value=withContext(Dispatchers.IO){runCatching{com.tuempresa.inventariovial.camera.FieldThumbnailCache.get(path,480)}.getOrNull()}}
    bitmap?.let{Image(it.asImageBitmap(),"Imagen de inspección",Modifier.fillMaxWidth().height(140.dp),contentScale=ContentScale.Fit)} ?: Text("Vista previa no disponible")
    TextButton(onClick={preview=true}) { Text("Ver fotografía") }
    if(preview) com.tuempresa.inventariovial.core.PhotoPreview(File(path)) { preview=false }
}
