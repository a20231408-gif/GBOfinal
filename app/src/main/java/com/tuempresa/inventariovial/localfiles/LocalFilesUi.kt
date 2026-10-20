package com.tuempresa.inventariovial.localfiles

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tuempresa.inventariovial.auth.AuthGraph
import com.tuempresa.inventariovial.auth.UserRole
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import kotlinx.coroutines.*
import java.io.File

@Composable fun RecordDeliveryChoice(kind:String,id:String,validated:Boolean){
    val c=LocalContext.current;val db=remember(c){InventoryDatabase.getInstance(c)};val scope=rememberCoroutineScope();val key="$kind/$id"
    val choices by db.localFilesDao().choices().collectAsState(emptyList());val only=choices.find{it.key==key}?.destination=="DEVICE"
    val intents by db.automaticSyncDao().observe().collectAsState(emptyList());val drive=intents.find{it.key==key}
    var changing by remember{mutableStateOf(false)}
    var confirm by remember{mutableStateOf<String?>(null)};var message by rememberSaveable(key){mutableStateOf<String?>(null)}
    val user by remember(c){AuthGraph.get(c).sessions.current}.collectAsState()
    Text("Destino de esta ficha",style=MaterialTheme.typography.titleSmall)
    Text(if(LocalCopySettings.enabled(c))"Tras guardar se actualizarán automáticamente los archivos locales, también de borradores. Descargas ocupa espacio y puede ser visible para otras aplicaciones."else"Copias documentales automáticas desactivadas en Herramientas y ajustes; los datos originales permanecen en GBO.",style=MaterialTheme.typography.bodySmall)
    Text(if(only)"Solo dispositivo · no se programa Drive" else "Drive y copia local (si está activada)")
    if(!validated&&drive==null){
        TextButton(onClick={confirm=if(only)"DRIVE"else"DEVICE"}){Text(if(only)"Elegir Drive y copia local"else"Elegir Solo dispositivo")}
    }
    if(only&&validated&&user?.role==UserRole.ADMIN)TextButton(onClick={confirm="AUTHORIZE"}){Text("Autorizar envío posterior a Drive")}
    if(only&&validated&&user?.role!=UserRole.ADMIN)Text("Un administrador puede autorizar después el envío a Drive.",style=MaterialTheme.typography.bodySmall)
    if(confirm!=null)AlertDialog(onDismissRequest={if(!changing)confirm=null},title={Text(if(confirm=="AUTHORIZE")"Autorizar entrega a Drive"else"Confirmar destino")},text={Text(if(confirm=="AUTHORIZE")"Se programará la revisión guardada con el mismo UUID. La copia local se conserva."else if(confirm=="DEVICE")"Esta ficha se exportará al dispositivo al finalizar y no se enviará a Drive. Las copias compartidas pueden ser visibles para otras aplicaciones o usuarios de la tablet. No se cancelarán entregas existentes."else"El cierre volverá a usar Drive y la opción general de copia local.")},confirmButton={TextButton(enabled=!changing,onClick={val action=confirm;changing=true;scope.launch{
        message=runCatching{val current=AuthGraph.get(c).sessions.requireUser();if(action=="AUTHORIZE")DeliveryDestination.authorizeDrive(db,kind,id,current)else DeliveryDestination.choose(db,kind,id,action=="DEVICE",current);"Destino guardado para esta ficha."}.getOrElse{it.message};changing=false;confirm=null
    }}){Text("Confirmar")}},dismissButton={TextButton(enabled=!changing,onClick={confirm=null}){Text("Cancelar")}})
    message?.let{Text(it,style=MaterialTheme.typography.bodySmall)}
}

@Composable fun LocalRecordFiles(kind:String,id:String,validated:Boolean){
    val c=LocalContext.current;val db=remember(c){InventoryDatabase.getInstance(c)};val scope=rememberCoroutineScope()
    var message by rememberSaveable(kind,id){mutableStateOf<String?>(null)}
    val key="$kind/$id"
    // Request acknowledgements are transient; Room below owns the durable operation status.
    LaunchedEffect(message){if(message?.endsWith("pendiente.")==true || message=="Copia local pendiente. Puedes continuar trabajando."){delay(3000);message=null}}
    RecordDeliveryChoice(kind,id,validated)
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
        TextButton(onClick={scope.launch{message=runCatching{LocalCopies.requestCurrent(c,db,kind,id);"Copia local pendiente. Puedes continuar trabajando."}.getOrElse{LocalCopyDrain.publicError(it as? Exception ?: Exception())}}}){Text("Guardar en Descargas")}
        TextButton(onClick={scope.launch{message=runCatching{LocalCopies.requestCurrent(c,db,kind,id,"PHOTOS");"Fotografías: copia local pendiente."}.getOrElse{it.message}}}){Text("Exportar fotografías")}
    }
    TextButton(onClick={scope.launch{message=runCatching{LocalCopies.requestCurrent(c,db,kind,id,"ZIP");"Paquete ZIP: copia local pendiente."}.getOrElse{it.message}}}){Text("Guardar paquete ZIP")}
    LocalFilesList(key)
    message?.let{Text(it,style=MaterialTheme.typography.bodySmall)}
}

@Composable fun LocalFilesList(key:String?=null,kind:String?=null){
    val c=LocalContext.current;val db=remember(c){InventoryDatabase.getInstance(c)};val scope=rememberCoroutineScope()
    val jobs by db.localFilesDao().observeJobsFor(key).collectAsState(emptyList());val files by db.localFilesDao().observeFilesFor(key).collectAsState(emptyList())
    var currentRevision by remember(key){mutableStateOf<Long?>(null)}
    DisposableEffect(db,key) {
        fun refresh(){if(key!=null)scope.launch{currentRevision=DeliveryDestination.revision(db,key.substringBefore('/'),key.substringAfter('/'))}}
        val observer=object:androidx.room.InvalidationTracker.Observer("inventory_records","scap_inspections","pavement_samples") {
            override fun onInvalidated(tables:Set<String>){refresh()}
        }
        db.invalidationTracker.addObserver(observer);refresh()
        onDispose{db.invalidationTracker.removeObserver(observer)}
    }
    var visible by remember(key){mutableIntStateOf(8)}
    val filtered=jobs.filter{kind==null||it.kind==kind}.sortedWith(compareByDescending<LocalExportJob>{it.revision}.thenByDescending{it.createdAt}.thenBy{it.id})
    var history by remember(key){mutableStateOf(false)}
    filtered.take(if(history)filtered.size else 6).forEach{j->
        Text(LocalCopyPresentation.context(j,if(key!=null)currentRevision else null,filtered.firstOrNull{it.recordKey==j.recordKey&&it.scope==j.scope}?.id==j.id),style=MaterialTheme.typography.bodySmall)
        Text(LocalCopyPresentation.result(j),style=MaterialTheme.typography.titleSmall)
        if(j.state in setOf("FAILED","PARTIAL")&&j.scope!="SAF")TextButton(onClick={scope.launch{LocalCopies.retry(c,db,j.id)}}){Text("Reintentar copia local")}
    }
    if(filtered.size>6)TextButton(onClick={history=!history}){Text(if(history)"Resumir intentos"else"Ver todos los intentos locales (${filtered.size}; fallidos/parciales: ${filtered.count{it.state in setOf("FAILED","PARTIAL")}})")}
    val items=files.filter{it.state=="READY"&&(kind==null||it.recordKey.startsWith("$kind/"))}.distinctBy{it.uri}
    items.take(visible).forEach{SavedFileActions(it)}
    if(items.size>visible)TextButton(onClick={visible+=12}){Text("Ver más archivos (${items.size})")}
}

@Composable fun SavedFileActions(file:LocalSharedFile){
    val c=LocalContext.current;val scope=rememberCoroutineScope();var message by remember{mutableStateOf<String?>(null)}
    var missing by remember(file.id,file.uri){mutableStateOf(false)}
    val saveAs=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(file.mimeType)){uri->if(uri!=null)scope.launch{
        message=runCatching{withContext(Dispatchers.IO){val source=File(file.source);require(fileDigest(source)==(file.size to file.sha256)){"El original preparado cambió."};requireNotNull(c.contentResolver.openOutputStream(uri,"wt")).use{out->source.inputStream().use{it.copyTo(out)}};LocalCopies.rememberSaf(c,InventoryDatabase.getInstance(c),source,file.recordKey.substringBefore('/'),file.mimeType,uri)};"Archivo exportado y verificado en la ubicación elegida."}.getOrElse{LocalCopyDrain.publicError(it as? Exception ?: Exception())}
    }}
    Column(Modifier.fillMaxWidth().padding(vertical=4.dp)){
        Text("Archivo guardado correctamente · ${file.name}")
        Text(if(Build.VERSION.SDK_INT<29&&file.relativePath.startsWith("Download/"))"Carpeta autorizada / "+file.relativePath.removePrefix("Download/")else file.relativePath.replace("Download/","Descargas/"),style=MaterialTheme.typography.bodySmall)
        fun open(share:Boolean){scope.launch{
            val valid=withContext(Dispatchers.IO){runCatching{AndroidSharedDocumentStore(c).digest(Uri.parse(file.uri))==(file.size to file.sha256)}.getOrDefault(false)}
            if(!valid){missing=true;message="El archivo fue movido, eliminado o perdió acceso. Puedes guardar otra copia desde el original conservado.";return@launch}
            missing=false
            message=runCatching{c.startActivity(if(share)SharedFileActions.share(Uri.parse(file.uri),file.mimeType)else SharedFileActions.view(Uri.parse(file.uri),file.mimeType).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));null}.getOrElse{if(share)"No se pudo abrir el menú de compartir."else"No hay una aplicación compatible para abrir este archivo."}
        }}
        Row {TextButton(onClick={open(false)}){Text("Abrir archivo")};TextButton(onClick={open(true)}){Text("Compartir")}}
        TextButton(onClick={message=if(Build.VERSION.SDK_INT<29||file.relativePath.startsWith("Ubicación"))"Abre Archivos y la carpeta elegida al guardar; Android no garantiza abrir esa carpeta desde GBO."else runCatching{c.startActivity(SharedFileActions.downloads());"Busca GBO_Ruta_Clara en Descargas; Android puede no abrir la carpeta exacta."}.getOrElse{"Abre Archivos → Descargas → GBO_Ruta_Clara. Esta tablet no ofrece acceso directo a Descargas."}}){Text("Ver ubicación")}
        TextButton(onClick={runCatching{saveAs.launch(file.name)}.onFailure{message="No se pudo abrir el selector de Android."}}){Text("Guardar como…")}
        if(missing)TextButton(onClick={scope.launch{message=runCatching{val d=InventoryDatabase.getInstance(c);val job=d.localFilesDao().job(file.jobId);if(job?.scope=="SAF")LocalCopies.document(c,d,File(file.source),file.recordKey.substringBefore('/'),file.mimeType)else LocalCopies.retry(c,d,file.jobId);"Recuperación programada desde el original conservado."}.getOrElse{it.message}}}){Text("Recuperar copia en Descargas")}
        message?.let{Text(it,style=MaterialTheme.typography.bodySmall)}
    }
}

@Composable fun LocalCopiesSettings(){
    val c=LocalContext.current;val db=remember(c){InventoryDatabase.getInstance(c)};val scope=rememberCoroutineScope()
    var enabled by remember{mutableStateOf(LocalCopySettings.enabled(c))};var confirmAuto by remember{mutableStateOf(false)};var message by remember{mutableStateOf<String?>(null)}
    var history by remember{mutableStateOf<List<Triple<String,String,Long>>?>(null)}
    val chooser=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()){uri->if(uri!=null)scope.launch{
        message=runCatching{require(uri.authority=="com.android.externalstorage.documents"){"Selecciona una carpeta del almacenamiento de la tablet."};c.contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION);check(c.getSharedPreferences("gbo_local_copies",0).edit().putString("legacyTree",uri.toString()).commit());"Carpeta local autorizada. Las copias pendientes pueden reintentarse."}.getOrElse{it.message}
    }}
    Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
        Text("Copias locales",style=MaterialTheme.typography.titleMedium)
        Row {Checkbox(enabled,{value->if(value)confirmAuto=true else{scope.launch{LocalCopySettings.enabled(c,false);enabled=false}}});Text("Guardar copias locales automáticamente")}
        Text("Se aplica a nuevos registros y próximas modificaciones guardadas, también borradores. Incluye documentos y originales asociados; no exporta históricos por sí sola ni borra copias anteriores. Solo dispositivo siempre prepara su copia local.",style=MaterialTheme.typography.bodySmall)
        if(Build.VERSION.SDK_INT<29){Text("Android 8/9: elige una carpeta interna para permitir la copia sin permisos amplios.");TextButton(onClick={chooser.launch(null)}){Text("Elegir carpeta local")}}
        TextButton(onClick={scope.launch{history=withContext(Dispatchers.IO){LocalCopies.historyCandidates(db)}}}){Text("Exportar registros existentes al dispositivo")}
        Text("Límite por copia: 512 MiB y 1000 archivos. El trabajo se agrupa durante cinco segundos; Android puede diferirlo por ahorro de batería.",style=MaterialTheme.typography.bodySmall)
        Text("¿Dónde están mis archivos?",style=MaterialTheme.typography.titleSmall)
        Text("Ficha guardada dentro de GBO y archivo exportado son pasos distintos. En Android 10 o posterior: Archivos → Almacenamiento interno → Download → GBO_Ruta_Clara. Las fotos se copian allí, no se publican adicionalmente en Galería. PDF, Excel y ZIP no equivalen a un respaldo restaurable de Room.",style=MaterialTheme.typography.bodySmall)
        TextButton(onClick={message=runCatching{c.startActivity(SharedFileActions.downloads());null}.getOrElse{"Abre el administrador Archivos y entra en Descargas."}}){Text("Abrir Descargas")}
        LocalFilesList();message?.let{Text(it)}
    }}
    if(confirmAuto)AlertDialog(onDismissRequest={confirmAuto=false},title={Text("Activar copias automáticas")},text={Text("Los documentos, fotografías originales y videos SCAP asociados se copiarán automáticamente tras guardar y agrupar cambios próximos. Ocuparán espacio adicional y podrán ser accesibles para otras aplicaciones o usuarios de la tablet. Se conserva el original privado. Las revisiones no se borran automáticamente.")},confirmButton={TextButton(onClick={scope.launch{LocalCopySettings.enabled(c,true);enabled=true;confirmAuto=false}}){Text("Activar")}},dismissButton={TextButton(onClick={confirmAuto=false}){Text("Cancelar")}})
    history?.let{list->AlertDialog(onDismissRequest={history=null},title={Text("Exportar existentes")},text={Text("${list.size} revisiones cerradas aún sin solicitud de copia local. Se programarán como máximo ${minOf(list.size,20)} en este lote y se procesarán de tres en tres. Puede ocupar espacio adicional; no modifica fichas ni estados Drive. Las faltas de archivos se informan por registro.")},confirmButton={TextButton(enabled=list.isNotEmpty(),onClick={history=null;scope.launch{withContext(Dispatchers.IO){for((kind,id,rev)in list.take(20))LocalCopies.request(c,db,kind,id,rev)};message="Lote local programado. Consulta el progreso aquí."}}){Text("Confirmar exportación")}},dismissButton={TextButton(onClick={history=null}){Text("Cancelar")}})}
}
