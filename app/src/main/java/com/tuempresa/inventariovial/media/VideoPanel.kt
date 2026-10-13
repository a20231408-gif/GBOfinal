package com.tuempresa.inventariovial.media

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.tuempresa.inventariovial.field.PhotoCategoryPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

@Composable fun BridgeMediaCategory(parentKey:String,mediaCategory:PhotoCategoryPolicy.Category,editable:Boolean=true,
    photoCount:Int,onPhoto:()->Unit,onImportPhotos:()->Unit,photoContent:@Composable ()->Unit) {
    require(VideoPolicy.bridgeOnly(parentKey))
    val categories=listOf(mediaCategory)
    val context=LocalContext.current;val scope=rememberCoroutineScope()
    val dao=remember(context){VideoDatabase.get(context).videos()};val storage=remember(context){VideoStorage(context)}
    val videos by remember(parentKey){dao.observe(parentKey)}.collectAsState(emptyList())
    var category by rememberSaveable(parentKey){mutableStateOf(categories.firstOrNull()?.key ?: "GENERAL")}
    var capturePath by rememberSaveable(parentKey){mutableStateOf<String?>(null)}
    var message by remember(parentKey){mutableStateOf<String?>(null)}
    var replace by remember{mutableStateOf<VideoEntity?>(null)}
    var retiring by remember{mutableStateOf<VideoEntity?>(null)}
    var videoMenu by remember{mutableStateOf(false)}
    var busy by remember{mutableStateOf(false)};var playing by remember{mutableStateOf<VideoEntity?>(null)}
    fun import(uri:android.net.Uri,selected:String) {scope.launch {busy=true;try {withContext(Dispatchers.IO){storage.import(parentKey,selected,categories.find {it.key==selected}?.label ?: selected,uri)};message="Video conservado en tablet"}catch(e:Exception){message=e.message}finally{busy=false}}}
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) {uris->val selected=category;scope.launch {
        busy=true;try {for(uri in if(replace!=null)uris.take(1) else uris) withContext(Dispatchers.IO){storage.import(parentKey,selected,categories.find {it.key==selected}?.label ?: selected,uri)};if(uris.isNotEmpty()){replace?.let {storage.retire(it.id)};message="Videos conservados en tablet"};replace=null}catch(e:Exception){message=e.message}finally{busy=false}
    }}
    val camera=rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()) {ok->val path=capturePath
        if(ok&&path!=null)import(FileProvider.getUriForFile(context,"${context.packageName}.fileprovider",File(path)),category)
        else path?.let {File(it).delete()}
        capturePath=null
    }
    fun capture() {runCatching {val dir=File(context.filesDir,"videos/capture").apply {mkdirs()};val file=File(dir,"${UUID.randomUUID()}.mp4");capturePath=file.path;camera.launch(FileProvider.getUriForFile(context,"${context.packageName}.fileprovider",file))}.onFailure {message="No se pudo abrir la cámara de video; puedes importar un archivo"}}
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {if(it)capture() else message="Cámara no autorizada; puedes importar videos"}
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
        message?.let {Text(it)}
        categories.forEach {c->
            Card(Modifier.fillMaxWidth()) {Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text(c.label,style=MaterialTheme.typography.titleMedium)
                Text("$photoCount fotos · ${videos.count {it.category==c.key&&!it.retired}} videos")
                Text("Evidencia del apartado · disponible offline. Videos MP4/WebM/3GP, hasta 32 MiB; originales conservados.",style=MaterialTheme.typography.bodySmall)
                if(editable) {
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        Button(enabled=!busy,onClick=onPhoto,modifier=Modifier.weight(1f).heightIn(min=52.dp)) {Icon(androidx.compose.ui.res.painterResource(com.tuempresa.inventariovial.R.drawable.ic_media_photo),null);Spacer(Modifier.width(8.dp));Text("FOTO")}
                        Box(Modifier.weight(1f)) {
                            Button(enabled=!busy,onClick={videoMenu=true},modifier=Modifier.fillMaxWidth().heightIn(min=52.dp)) {Icon(androidx.compose.ui.res.painterResource(com.tuempresa.inventariovial.R.drawable.ic_media_video),null);Spacer(Modifier.width(8.dp));Text("VIDEO")}
                            DropdownMenu(expanded=videoMenu,onDismissRequest={videoMenu=false}) {
                                DropdownMenuItem(text={Text("Grabar video")},onClick={videoMenu=false;category=c.key;if(ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)capture() else permission.launch(Manifest.permission.CAMERA)})
                                DropdownMenuItem(text={Text("Importar videos")},onClick={videoMenu=false;category=c.key;replace=null;picker.launch(arrayOf("video/mp4","video/webm","video/3gpp"))})
                            }
                        }
                    }
                    TextButton(enabled=!busy,onClick=onImportPhotos){Text("Importar fotografías")}
                }
                photoContent()
                videos.filter {it.category==c.key}.forEach {v->
                    val thumbnail by produceState<android.graphics.Bitmap?>(null,v.id) {
                        value=withContext(Dispatchers.IO) {runCatching {val r=MediaMetadataRetriever();try {r.setDataSource(v.localPath);r.frameAtTime?.let {original->val scale=minOf(1f,320f/maxOf(original.width,original.height));android.graphics.Bitmap.createScaledBitmap(original,(original.width*scale).toInt().coerceAtLeast(1),(original.height*scale).toInt().coerceAtLeast(1),true)}}finally{r.release()}}.getOrNull()}
                    }
                    thumbnail?.let {Image(it.asImageBitmap(),"Miniatura de video",Modifier.height(120.dp).fillMaxWidth())}
                    val status=when(v.state){"SYNCED"->"Sincronizado · confirmado por servidor";"BACKEND_REQUIRED"->"Pendiente del backend";"WAITING_PARENT"->"Pendiente de finalizar y sincronizar ficha";"UPLOADING"->"Subiendo ${v.uploadedBytes*100/v.sizeBytes}%";"ERROR"->"Error · original conservado";"RETIRED"->"Retirado · original conservado";else->"Entrega pendiente"}
                    Text("${v.id.take(8)} · ${v.sizeBytes/1024} KiB · $status")
                    v.error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
                    Row {TextButton(onClick={playing=v}){Text("Reproducir")}
                        if(!v.retired && v.state!="SYNCED")TextButton(onClick={scope.launch{storage.retry(v.id)}}){Text("Reintentar")}
                        if(editable&&!v.retired){TextButton(onClick={replace=v;category=c.key;picker.launch(arrayOf("video/mp4","video/webm","video/3gpp"))}){Text("Reemplazar")};TextButton(onClick={retiring=v}){Text("Retirar")}}
                    }
                }
            }}
        }
    }
    retiring?.let {v->AlertDialog(onDismissRequest={retiring=null},title={Text("Retirar video")},text={Text("Se conservará el original y su historial. No se borrará el archivo remoto.")},confirmButton={TextButton(onClick={scope.launch{storage.retire(v.id)};retiring=null}){Text("Retirar")}},dismissButton={TextButton(onClick={retiring=null}){Text("Cancelar")}})}
    playing?.let {v->Dialog(onDismissRequest={playing=null},properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.fillMaxSize()){Column {TextButton(onClick={playing=null}){Text("Cerrar video")}
            AndroidView(factory={ctx->VideoView(ctx).apply {setVideoPath(v.localPath);setMediaController(MediaController(ctx).also {it.setAnchorView(this)});setOnPreparedListener {start()};setOnErrorListener {_,_,_->message="No se pudo reproducir este video";true}}},modifier=Modifier.weight(1f).fillMaxWidth(),onRelease={it.stopPlayback()})
        }}
    }}
}
