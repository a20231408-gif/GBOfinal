package com.tuempresa.inventariovial.scap.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.tuempresa.inventariovial.camera.PhotoCaptureMetadata
import com.tuempresa.inventariovial.camera.createPhotoFile
import com.tuempresa.inventariovial.scap.catalog.ScapCatalog
import com.tuempresa.inventariovial.scap.data.ScapInspectionSnapshot
import com.tuempresa.inventariovial.scap.domain.ScapElementView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** A record retains its UUID when its image is replaced or removed. Originals remain on disk. */
@Composable internal fun ScapPhotoRecords(s:ScapInspectionSnapshot,c:ScapController,catalog:ScapCatalog,editable:Boolean) {
    val context=LocalContext.current;val scope=rememberCoroutineScope()
    val list=rememberLazyListState();val photos=s.photos.sortedBy{it.photoIndex}
    val destination=LocalScapDestination.current
    LaunchedEffect(destination,photos.map{it.id}) {
        destination?.second?.takeIf{it.subsection=="F2"}?.let{route->
            val index=photos.indexOfFirst{it.id==route.repeatableItemId}
            list.scrollToItem(if(index<0)0 else index+1)
        }
    }
    var adding by remember{mutableStateOf(false)};var name by rememberSaveable{mutableStateOf("")}
    var description by rememberSaveable{mutableStateOf("")}
    var target by rememberSaveable{mutableStateOf<String?>(null)}
    var cameraPath by rememberSaveable{mutableStateOf<String?>(null)}
    var busy by remember{mutableStateOf(false)};var message by remember{mutableStateOf<String?>(null)}
    var remove by remember{mutableStateOf<Pair<String,Boolean>?>(null)}
    val camera=rememberLauncherForActivityResult(com.tuempresa.inventariovial.core.FieldTakePicture()){ok->
        val path=cameraPath;val id=target
        if(ok&&path!=null&&id!=null) {
            val capture=PhotoCaptureMetadata(System.currentTimeMillis(),null)
            c.submit{it.replacePhoto(s.inspection.id,id,path,capture)}
            PhotoCaptureMetadata.capture(context,capture.capturedAt!!) {metadata->
                c.submit{it.updatePhotoCapture(s.inspection.id,path,metadata)}
            }
        } else path?.let{File(it).delete()}
        cameraPath=null;target=null
    }
    fun take() {runCatching{val file=createPhotoFile(context,"SCAP");cameraPath=file.file.absolutePath;camera.launch(file.uri)}
        .onFailure{message="No se pudo abrir la cámara."}}
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){if(it)take() else message="Permiso de cámara no concedido. Puedes seleccionar una imagen."}
    val importer=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
        val id=target
        if(uri!=null&&id!=null)scope.launch {
            busy=true
            try {
                val path=withContext(Dispatchers.IO) {
                    val suffix=when(context.contentResolver.getType(uri)){"image/png"->".png";"image/webp"->".webp";else->".jpg"}
                    val file=File.createTempFile("SCAP_IMPORT_",suffix,File(context.filesDir,"images").apply{mkdirs()})
                    try {
                        context.contentResolver.openInputStream(uri).use{input->requireNotNull(input);file.outputStream().use{input.copyTo(it)}}
                        val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true};BitmapFactory.decodeFile(file.absolutePath,bounds)
                        require(bounds.outWidth>0&&bounds.outHeight>0);file.absolutePath
                    }catch(e:Exception){file.delete();throw e}
                }
                c.submit{it.replacePhoto(s.inspection.id,id,path)}
            }catch(e:Exception){message="No se pudo leer la imagen seleccionada."}finally{busy=false;target=null}
        }
    }
    LazyColumn(Modifier.fillMaxSize(),state=list,contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item {
            Text("F.2 · PANEL FOTOGRÁFICO",style=MaterialTheme.typography.headlineSmall)
            Text("NRO. · FECHA · DESCRIPCIÓN · DIGITAL")
            ScapFieldTarget("inspection","addPhoto"){OutlinedButton(enabled=editable&&!busy,onClick={adding=true}){Text("+ Añadir registro fotográfico")}}
            if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
            message?.let{Text(it,color=MaterialTheme.colorScheme.error)}
        }
        items(photos,key={it.id}){photo->
            val values=scapValues(s,c,photo.id)
            ScapElementCard("NRO. ${photo.photoIndex}") {
                ScapFieldTarget(photo.id,"photoName") {
                    ScapChoice("Nombres de la plantilla",values["photoName"] ?: ScapElementView.photoName(s,photo.id,photo.photoCategory),catalog.photoNames,editable){c.edit(s.inspection.id,photo.id,"photoName",it)}
                    OutlinedTextField(values["photoName"] ?: ScapElementView.photoName(s,photo.id,photo.photoCategory),{c.edit(s.inspection.id,photo.id,"photoName",it)},label={Text("Nombre / tipo (editable)")},enabled=editable,modifier=Modifier.fillMaxWidth().scapFocus())
                }
                ScapFieldTarget(photo.id,"photoDate"){OutlinedTextField(values["photoDate"] ?: SimpleDateFormat("dd/MM/yyyy",Locale.ROOT).format(Date(photo.createdAt)),{c.edit(s.inspection.id,photo.id,"photoDate",it)},label={Text("Fecha (dd/MM/aaaa)")},enabled=editable,modifier=Modifier.fillMaxWidth().scapFocus())}
                ScapFieldTarget(photo.id,"photoDescription") {
                    var text by remember(photo.id,photo.description){mutableStateOf(photo.description.orEmpty())}
                    OutlinedTextField(text,{text=it;c.submit{repo->repo.describePhoto(s.inspection.id,photo.id,it)}},label={Text("Descripción")},enabled=editable,modifier=Modifier.fillMaxWidth().scapFocus(),minLines=2)
                }
                ScapFieldTarget(photo.id,"photo") {
                    if(photo.localPath.isNotBlank())ScapImage(com.tuempresa.inventariovial.DriveUploadPolicy.uploadPath(photo)) else Text("Sin imagen · pendiente")
                    com.tuempresa.inventariovial.core.PhotoCaptureCard("Digital · tomar / seleccionar / reemplazar",emptyList(),
                        onTakePhoto={target=photo.id;if(ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)take() else permission.launch(Manifest.permission.CAMERA)},
                        onSelectFile={target=photo.id;importer.launch(arrayOf("image/*"))},onDelete={},editable=editable&&!busy)
                    if(photo.localPath.isNotBlank())TextButton(enabled=editable,onClick={remove=photo.id to false}){Text("Eliminar imagen")}
                    if(editable&&photo.localPath.isNotBlank()) {
                        val original=photo.originalPath.ifBlank{photo.localPath};val v=s.values()
                        val location=photo.photoLatitude?.let{lat->photo.photoLongitude?.let{lon->com.tuempresa.inventariovial.location.GeoLocation(lat,lon,photo.photoGpsAccuracyM?.toFloat() ?: Float.NaN,photo.photoAltitudeM)}}
                        com.tuempresa.inventariovial.field.PhotoStampPanel(listOf(original),com.tuempresa.inventariovial.camera.PhotoStampData(v["route"].orEmpty(),v["sic17.roadbedCode"].orEmpty(),v["progressive"].orEmpty(),location,photo.createdAt,s.inspection.createdBy,s.inspection.bridgeName),
                            photo.stampedPath?.let{mapOf(original to it)} ?: emptyMap(),mapOf(original to PhotoCaptureMetadata(photo.capturedAt,location,photo.photoLocationSource))) {
                            c.submit{repo->val file=File(original);val copy=com.tuempresa.inventariovial.camera.PhotoExif.writableCopy(file,file,File(context.filesDir,"stamped"));com.tuempresa.inventariovial.camera.PhotoExif.write(copy,file,photo);repo.stampPhoto(s.inspection.id,photo.id,copy.absolutePath)}
                        }
                    }
                }
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    TextButton(enabled=editable&&photo!=photos.firstOrNull(),onClick={c.submit{it.movePhoto(s.inspection.id,photo.id,-1)}}){Text("↑ Subir")}
                    TextButton(enabled=editable&&photo!=photos.lastOrNull(),onClick={c.submit{it.movePhoto(s.inspection.id,photo.id,1)}}){Text("↓ Bajar")}
                    TextButton(enabled=editable,onClick={remove=photo.id to true}){Text("Eliminar registro")}
                }
            }
        }
    }
    if(adding)AlertDialog(onDismissRequest={adding=false},title={Text("Añadir registro fotográfico")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        ScapChoice("Nombre de plantilla",name,catalog.photoNames){name=it}
        OutlinedTextField(name,{name=it},label={Text("Nombre / tipo")},modifier=Modifier.fillMaxWidth())
        OutlinedTextField(description,{description=it},label={Text("Descripción")},modifier=Modifier.fillMaxWidth())
    }},confirmButton={TextButton(onClick={val n=name;val d=description;c.submit{it.addPhotoRecord(s.inspection.id,n,d)};adding=false;name="";description=""}){Text("Añadir")}},dismissButton={TextButton(onClick={adding=false}){Text("Cancelar")}})
    remove?.let{(id,record)->AlertDialog(onDismissRequest={remove=null},title={Text(if(record)"Eliminar registro" else "Eliminar imagen")},text={Text("Los archivos originales se conservarán en el dispositivo.")},confirmButton={TextButton(onClick={c.submit{if(record)it.removePhoto(s.inspection.id,id) else it.clearPhotoImage(s.inspection.id,id)};remove=null}){Text("Eliminar")}},dismissButton={TextButton(onClick={remove=null}){Text("Cancelar")}})}
}
