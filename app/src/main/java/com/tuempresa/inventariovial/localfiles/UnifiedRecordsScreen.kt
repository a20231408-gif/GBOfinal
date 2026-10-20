package com.tuempresa.inventariovial.localfiles

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.tuempresa.inventariovial.RecordDetailScreen
import com.tuempresa.inventariovial.core.*
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.InventoryRecordWithPhotos
import com.tuempresa.inventariovial.pavement.*
import com.tuempresa.inventariovial.road.*
import com.tuempresa.inventariovial.scap.data.ScapInspectionEntity
import com.tuempresa.inventariovial.viewmodel.InventoryViewModel
import kotlinx.coroutines.*
import org.json.JSONObject
import java.text.DateFormat
import java.util.Date

data class LocalInventoryItem(val kind:String,val id:String,val family:String,val name:String,val route:String,val progressive:String,
    val position:Double?,val roadbed:String,val direction:String,val capture:String,val revision:Long) {
    val key get()="$kind/$id"
}
object LocalInventoryIndex {
    fun project(records:List<InventoryRecordWithPhotos>,bridges:List<ScapInspectionEntity>,samples:List<PavementSampleEntity>):List<LocalInventoryItem> {
        val linked=bridges.map{it.roadRecordId}.toSet();val byId=records.associateBy{it.record.id}
        val items=records.filter{it.record.id !in linked && it.record.sicCode!="SCAP" && it.sicExtension?.sourceRecordId !in linked}.map {row->
            val r=row.record
            LocalInventoryItem("SIC",r.id,"SIC","${r.sicCode} · ${AssetPresentationCatalog.resolve(row).label}",r.routeCode,
                if(r.startPrCode.isBlank())"Progresiva pendiente"else "PR ${r.startPrCode} + ${r.startDistanceM} m",
                r.axisMeasureM?.takeIf{it.isFinite()&&it>=0} ?: ChainageCalculator().calculate(r).chainageM,r.roadbedCode,r.surveyDirection.orEmpty(),r.status,r.updatedAt)
        }+bridges.map {b->val r=byId[b.roadRecordId]?.record
            LocalInventoryItem("SCAP",b.id,"SCAP / Puentes",b.bridgeName.ifBlank{"Puente SCAP"},r?.routeCode.orEmpty(),
                if(r==null||r.startPrCode.isBlank())"Progresiva pendiente"else "PR ${r.startPrCode} + ${r.startDistanceM} m",
                r?.axisMeasureM?.takeIf{it.isFinite()&&it>=0} ?: r?.let{ChainageCalculator().calculate(it).chainageM},r?.roadbedCode.orEmpty(),r?.surveyDirection.orEmpty(),b.status,b.updatedAt)
        }+samples.map {s->LocalInventoryItem("PAVEMENT",s.id,"Pavimentos · ${PavementSurface.valueOf(s.surface).label}","Muestra ${s.muestra} · ${s.tramo}",s.ruta,s.prInicial.ifBlank{"Progresiva pendiente"},
            runCatching{PavementChainage.parse(s.prInicial)}.getOrNull(),s.calzada,s.carril,s.status,s.updatedAt)}
        return items.distinctBy{it.key}.sortedWith(compareBy({it.route},{it.roadbed},{it.direction},{it.position ?: Double.MAX_VALUE},{it.key}))
    }
    fun localState(item:LocalInventoryItem,job:LocalExportJob?):String=when {
        job==null->"Sin copia documental · incorporación histórica disponible"
        job.revision!=item.revision->"Actualizando archivos locales… · copia anterior disponible"
        job.state=="READY"->"Archivos locales actualizados"
        job.state in setOf("QUEUED","RUNNING")->"Actualizando archivos locales…"
        else->"No se pudo actualizar la copia local. Los datos originales siguen guardados en GBO"
    }
}

@Composable fun UnifiedRecordsScreen(vm:InventoryViewModel,onBack:()->Unit) {
    val c=LocalContext.current;val db=remember(c){InventoryDatabase.getInstance(c)}
    val records by db.inventoryDao().observeHistory().collectAsState(emptyList())
    val bridges by db.scapDao().observeInspections().collectAsState(emptyList())
    val samples by db.pavementDao().observeSamples().collectAsState(emptyList())
    val jobs by db.localFilesDao().observeJobs().collectAsState(emptyList())
    val drive by db.automaticSyncDao().observe().collectAsState(emptyList())
    var detail by rememberSaveable{mutableStateOf<String?>(null)}
    var search by rememberSaveable{mutableStateOf("")};var route by rememberSaveable{mutableStateOf("")}
    var pr by rememberSaveable{mutableStateOf("")};var state by rememberSaveable{mutableStateOf("")};var family by rememberSaveable{mutableStateOf("Todas")}
    var historical by rememberSaveable{mutableStateOf(false)}
    val all=remember(records,bridges,samples){LocalInventoryIndex.project(records,bridges,samples)}
    detail?.let {key->val id=key.substringAfter('/');when(key.substringBefore('/')) {
        "SIC"->RecordDetailScreen(vm,id){detail=null}
        "SCAP"->com.tuempresa.inventariovial.scap.ui.ScapWorkspace(vm,id){detail=null}
        "PAVEMENT"->com.tuempresa.inventariovial.pavement.ui.PavementWorkspace(id){detail=null}
    };return}
    BackHandler(onBack=onBack)
    val connected=runCatching{val manager=c.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager;manager.getNetworkCapabilities(manager.activeNetwork)?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)==true}.getOrDefault(false)
    val configured=com.tuempresa.inventariovial.DriveUploadPolicy.configurationError()==null
    val latest=remember(jobs){jobs.filter{it.scope.startsWith("FULL")&&it.state!="SUPERSEDED"}.groupBy{it.recordKey}.mapValues{(_,v)->v.maxWith(compareBy({it.revision},{it.createdAt}))}}
    val shown=all.filter {r->val d=drive.find{it.key==r.key};val j=latest[r.key]
        (historical||r.capture !in setOf("DISCARDED_LOCAL","ANNULLED")) && (family=="Todas"||r.family.startsWith(family)) &&
        "${r.name} ${r.id} ${r.route}".contains(search,true)&&r.route.contains(route,true)&&r.progressive.contains(pr,true)&&
        "${r.capture} ${d?.state.orEmpty()} ${SyncIntentPresentation.label(d,r.capture in setOf("ACTIVE","COMPLETE","CLOSED"),connected,configured)} ${LocalInventoryIndex.localState(r,j)} ${j?.state.orEmpty()}".contains(state,true)
    }
    LazyColumn(Modifier.testTag("mis-registros").fillMaxSize().padding(horizontal=16.dp),contentPadding=PaddingValues(vertical=16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item {
            TextButton(onClick=onBack){Text("Volver")};Text("Mis registros",style=MaterialTheme.typography.headlineMedium)
            Text("SIC, puentes SCAP y Pavimentos · orden por carretera, calzada, sentido y posición disponible. No renumera carpetas.")
            OutlinedTextField(search,{search=it},label={Text("Buscar elemento o UUID")},modifier=Modifier.fillMaxWidth())
            Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                listOf("Todas","SIC","SCAP","Pavimentos").forEach{f->FilterChip(family==f,{family=f},label={Text(f)})}
            }
            OutlinedTextField(route,{route=it},label={Text("Carretera")},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(pr,{pr=it},label={Text("Progresiva")},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(state,{state=it},label={Text("Estado de captura / Drive / copia local")},modifier=Modifier.fillMaxWidth())
            Row{Checkbox(historical,{historical=it});Text("Mostrar descartados y anulados")}
            Text("Copias automáticas ${if(LocalCopySettings.enabled(c))"activadas"else"desactivadas"}. Descargas ocupa espacio y puede ser visible para otras aplicaciones. Configura en Herramientas y ajustes → Copias locales.",style=MaterialTheme.typography.bodySmall)
            Text("${shown.size} registros")
        }
        items(shown,key={it.key}){r->
            val d=drive.find{it.key==r.key};val job=latest[r.key]
            Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
                Text(r.name,style=MaterialTheme.typography.titleMedium);Text("${r.family} · ${r.route.ifBlank{"Ruta pendiente"}} · ${r.progressive}")
                Text("UUID: ${r.id}",style=MaterialTheme.typography.bodySmall)
                Text("Captura: ${r.capture} · revisión ${r.revision} · ${DateFormat.getDateTimeInstance().format(Date(r.revision))}",style=MaterialTheme.typography.bodySmall)
                Text("Drive de la ficha: ${SyncIntentPresentation.label(d,r.capture in setOf("ACTIVE","COMPLETE","CLOSED"),connected,configured)}${if(d?.syncedRevision!=null&&d.syncedRevision>0)" · ACK R${d.syncedRevision}"else" · sin ACK"}")
                Text(LocalInventoryIndex.localState(r,job))
                d?.receiptJson?.let{runCatching{JSONObject(it)}.getOrNull()}?.takeIf{it.optBoolean("ok")&&it.optString("key")==r.key&&it.optLong("revision")==d.syncedRevision}?.optString("logicalPath")?.takeIf{it.isNotBlank()}?.let{Text("Drive confirmado: $it",style=MaterialTheme.typography.bodySmall)}
                OutlinedButton(onClick={detail=r.key}){Text("Ver detalle")}
                DirectLocalDocuments(r,job)
                if(r.capture in setOf("DRAFT","DISCARDED_LOCAL"))LocalDraftDiscardButton(r.kind,r.id,r.capture=="DISCARDED_LOCAL")
            }}
        }
        if(shown.isEmpty())item{Text("No hay registros con estos filtros.")}
    }
}

@Composable private fun DirectLocalDocuments(item:LocalInventoryItem,job:LocalExportJob?) {
    val c=LocalContext.current;val db=remember(c){InventoryDatabase.getInstance(c)};val scope=rememberCoroutineScope()
    val files by db.localFilesDao().observeFilesFor(item.key).collectAsState(emptyList())
    val jobs by db.localFilesDao().observeJobsFor(item.key).collectAsState(emptyList())
    var selector by remember{mutableStateOf(false)};var message by remember{mutableStateOf<String?>(null)}
    val revisions=jobs.associateBy{it.id}
    val documents=files.filter{it.state=="READY"&&it.mimeType in setOf(com.tuempresa.inventariovial.export.SicExcelWriter.XLSX_MIME,"application/pdf","application/zip")}
        .sortedWith(compareByDescending<LocalSharedFile>{revisions[it.jobId]?.revision ?: 0}.thenBy{if(it.name.startsWith("SCAP")||it.mimeType=="application/pdf")0 else 1}.thenBy{it.name}).distinctBy{it.uri}
    fun open(share:Boolean){if(documents.size!=1){selector=true;return};val file=documents.single();scope.launch{
        message=runCatching{withContext(Dispatchers.IO){require(AndroidSharedDocumentStore(c).digest(android.net.Uri.parse(file.uri))==(file.size to file.sha256)){"El archivo ya no está disponible. Reintenta la copia local."}}
            c.startActivity(if(share)SharedFileActions.share(android.net.Uri.parse(file.uri),file.mimeType)else SharedFileActions.view(android.net.Uri.parse(file.uri),file.mimeType).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK));null
        }.getOrElse{"No se pudo abrir el documento: comprueba acceso o una aplicación compatible. Los originales se conservan."}
    }}
    Row(Modifier.horizontalScroll(rememberScrollState())){
        TextButton(enabled=documents.isNotEmpty(),onClick={open(false)}){Text("Abrir archivo")}
        TextButton(onClick={message=runCatching{c.startActivity(SharedFileActions.downloads());"${documents.firstOrNull()?.relativePath ?: "Download/GBO_Ruta_Clara/"} · Android puede abrir Descargas sin seleccionar la carpeta exacta."}.getOrElse{"Abre Archivos → Descargas → GBO_Ruta_Clara"}}){Text("Ver ubicación")}
        TextButton(enabled=documents.isNotEmpty(),onClick={open(true)}){Text("Compartir")}
    }
    documents.firstOrNull()?.let{Text("Local: ${it.relativePath}",style=MaterialTheme.typography.bodySmall)}
    if(job==null||job.state in setOf("FAILED","PARTIAL"))TextButton(onClick={scope.launch{message=runCatching{withContext(Dispatchers.IO){LocalCopies.requestCurrent(c,db,item.kind,item.id)};"Actualizando archivos locales…"}.getOrElse{"No se pudo solicitar la copia; los originales siguen guardados."}}}){Text(if(job==null)"Incorporar histórico a copias locales"else"Reintentar copia local")}
    if(selector)AlertDialog(onDismissRequest={selector=false},title={Text("Documentos disponibles")},text={
        LazyColumn{items(documents,key={it.id}){file->Text("R${revisions[file.jobId]?.revision ?: "?"}${if(file.name.contains("PROVISIONAL")||file.name.contains("BORRADOR"))" · provisional"else""}");SavedFileActions(file)}}
    },confirmButton={TextButton(onClick={selector=false}){Text("Cerrar")}})
    message?.let{Text(it,style=MaterialTheme.typography.bodySmall)}
}
