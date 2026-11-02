package com.tuempresa.inventariovial.localfiles

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.tuempresa.inventariovial.field.*
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
    val position:Double?,val roadbed:String,val direction:String,val capture:String,val revision:Long,val surveyDate:String="") {
    val key get()="$kind/$id"
}
object LocalInventoryIndex {
    fun discarded(item:LocalInventoryItem)=item.capture in setOf("DISCARDED_LOCAL","ANNULLED")
    /** Capture visibility never turns a discarded draft into an active delivery. */
    fun matchesCapture(item:LocalInventoryItem,filter:String,includeHistorical:Boolean,confirmed:Boolean,archived:Boolean=false):Boolean {
        if(filter=="Descartados")return item.capture=="DISCARDED_LOCAL"
        if(discarded(item))return includeHistorical && filter=="Todos"
        if(archived && !includeHistorical && filter!="Archivados")return false
        return when(filter) {
            "Borradores"->item.capture=="DRAFT"
            "Enviados"->confirmed
            "Archivados"->archived
            "Pendientes","Sin confirmar"->item.capture!="DRAFT" && !confirmed
            else->true
        }
    }
    fun chronological(items:List<LocalInventoryItem>)=items.sortedWith(compareByDescending<LocalInventoryItem>{parseRecordDate(it.surveyDate)}.thenBy{it.key})
    fun cleanupCandidate(item:LocalInventoryItem,intent:DriveSyncIntent?,job:LocalExportJob?)=
        intent!=null && DriveDeliveryPresentation.confirmed(intent,item.revision) && job?.state=="READY" && job.revision==item.revision
    fun project(records:List<InventoryRecordWithPhotos>,bridges:List<ScapInspectionEntity>,samples:List<PavementSampleEntity>):List<LocalInventoryItem> {
        val linked=bridges.map{it.roadRecordId}.toSet();val byId=records.associateBy{it.record.id}
        val items=records.filter{it.record.id !in linked && it.record.sicCode!="SCAP" && it.sicExtension?.sourceRecordId !in linked}.map {row->
            val r=row.record
            LocalInventoryItem("SIC",r.id,"SIC","${r.sicCode} · ${AssetPresentationCatalog.resolve(row).label}",r.routeCode,
                if(r.startPrCode.isBlank())"Progresiva pendiente"else "PR ${r.startPrCode} + ${r.startDistanceM} m",
                r.axisMeasureM?.takeIf{it.isFinite()&&it>=0} ?: ChainageCalculator().calculate(r).chainageM,r.roadbedCode,r.surveyDirection.orEmpty(),r.status,r.updatedAt,r.surveyDate)
        }+bridges.map {b->val r=byId[b.roadRecordId]?.record
            LocalInventoryItem("SCAP",b.id,"SCAP / Puentes",b.bridgeName.ifBlank{"Puente SCAP"},r?.routeCode.orEmpty(),
                if(r==null||r.startPrCode.isBlank())"Progresiva pendiente"else "PR ${r.startPrCode} + ${r.startDistanceM} m",
                r?.axisMeasureM?.takeIf{it.isFinite()&&it>=0} ?: r?.let{ChainageCalculator().calculate(it).chainageM},r?.roadbedCode.orEmpty(),r?.surveyDirection.orEmpty(),b.status,b.updatedAt,r?.surveyDate.orEmpty())
        }+samples.map {s->LocalInventoryItem("PAVEMENT",s.id,"Pavimentos · ${PavementSurface.valueOf(s.surface).label}","Muestra ${s.muestra} · ${s.tramo}",s.ruta,s.prInicial.ifBlank{"Progresiva pendiente"},
            runCatching{PavementChainage.parse(s.prInicial)}.getOrNull(),s.calzada,s.carril,s.status,s.updatedAt,s.fecha)}
        return items.distinctBy{it.key}.sortedWith(compareBy({it.route},{it.roadbed},{it.direction},{it.position ?: Double.MAX_VALUE},{it.key}))
    }
    fun localState(item:LocalInventoryItem,job:LocalExportJob?):String=when {
        job==null->"Sin copia documental · incorporación histórica disponible"
        job.revision!=item.revision->"Actualizando archivos locales… · copia anterior disponible"
        job.state=="READY"->"Archivos locales actualizados"
        job.state=="CLEANED"->"Copias públicas retiradas con respaldo; originales conservados"
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
    val choices by db.localFilesDao().choices().collectAsState(emptyList())
    var detail by rememberSaveable{mutableStateOf<String?>(null)}
    var removalPreview by remember{mutableStateOf<List<RemovalPreview.Row>?>(null)}
    var search by rememberSaveable{mutableStateOf("")};var route by rememberSaveable{mutableStateOf("")}
    var pr by rememberSaveable{mutableStateOf("")};var state by rememberSaveable{mutableStateOf("")};var family by rememberSaveable{mutableStateOf("Todas")}
    var historical by rememberSaveable{mutableStateOf(false)}
    var year by rememberSaveable{mutableStateOf<Int?>(null)}
    var month by rememberSaveable{mutableStateOf<Int?>(null)}
    var day by rememberSaveable{mutableStateOf<Int?>(null)}
    var captureFilter by rememberSaveable{mutableStateOf("Todos")}
    var newestFirst by rememberSaveable{mutableStateOf(true)}
    var cleanupReview by remember{mutableStateOf<String?>(null)}
    val dateFilter=RecordDateSelection(year,month,day)
    var selected by remember {mutableStateOf(emptySet<String>())}
    var discardReview by remember {mutableStateOf<List<LocalDraftDiscard.Candidate>?>(null)}
    var discardBusy by remember {mutableStateOf(false)}
    var discardMessage by remember {mutableStateOf<String?>(null)}
    var prepareSegment by rememberSaveable {mutableStateOf(false)}
    val scope=rememberCoroutineScope()
    var cleanupBackup by remember{mutableStateOf<ConfirmedLocalCleanup.Backup?>(null)}
    var cleanupBusy by remember{mutableStateOf(false)}
    val backupPicker=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")){uri->
        val backup=cleanupBackup
        if(uri!=null&&backup!=null){cleanupBusy=true;scope.launch{
            discardMessage=runCatching{
                withContext(Dispatchers.IO){requireNotNull(c.contentResolver.openOutputStream(uri,"wt")).use{out->backup.zip.inputStream().use{it.copyTo(out)}}}
                runCatching{c.contentResolver.takePersistableUriPermission(uri,android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION)}
                val user=com.tuempresa.inventariovial.auth.AuthGraph.get(c).sessions.requireUser()
                val removed=ConfirmedLocalCleanup.clean(c,db,backup,uri,user.id)
                selected=emptySet();"${backup.candidates.size} registros archivados; $removed copias públicas retiradas. Respaldo verificado, originales e historial conservados."
            }.getOrElse{it.message ?: "Limpieza detenida; respaldo y originales conservados."}
            cleanupBusy=false;cleanupBackup=null
        }}
    }
    val all=remember(records,bridges,samples){LocalInventoryIndex.project(records,bridges,samples)}
    removalPreview?.let { rows->RemovalPreviewDialog(rows){removalPreview=null} }
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
        LocalInventoryIndex.matchesCapture(r,captureFilter,historical,d!=null && DriveDeliveryPresentation.confirmed(d,r.revision),ConfirmedLocalCleanup.archived(choices,r.key,r.revision)) &&
        (family=="Todas"||r.family.startsWith(family)) && dateFilter.matches(parseRecordDate(r.surveyDate)) &&
        "${r.name} ${r.id} ${r.route}".contains(search,true)&&r.route.contains(route,true)&&r.progressive.contains(pr,true)&&
        "${r.capture} ${d?.state.orEmpty()} ${SyncIntentPresentation.label(d,r.capture in setOf("ACTIVE","COMPLETE","CLOSED"),connected,configured)} ${LocalInventoryIndex.localState(r,j)} ${j?.state.orEmpty()}".contains(state,true)
    }.let {if(newestFirst)LocalInventoryIndex.chronological(it)else it}
    val selectable=shown.filter{it.capture=="DRAFT" && it.kind in setOf("SIC","PAVEMENT")}
    val cleanable=shown.filter{!ConfirmedLocalCleanup.archived(choices,it.key,it.revision)&&LocalInventoryIndex.cleanupCandidate(it,drive.find{d->d.key==it.key},latest[it.key])}
    cleanupBackup?.let{backup->AlertDialog(onDismissRequest={if(!cleanupBusy)cleanupBackup=null},title={Text("Respaldar y limpiar ${backup.candidates.size} registros")},
        text={Text("El ZIP contiene documentos y medios verificados. Elige dónde guardar el respaldo; se volverá a leer antes de retirar ${backup.files.count{it.state=="READY"}} copias públicas y archivar las fichas del listado activo. Se conservan Room, originales privados, UUID, ACK y vínculos SCAP/SIC. Las revisiones pendientes no se limpian. Puedes restaurar el listado y regenerar copias.")},
        confirmButton={TextButton(enabled=!cleanupBusy,onClick={backupPicker.launch("GBO_RESPALDO_LIMPIEZA_${System.currentTimeMillis()}.zip")}){Text("Guardar respaldo y limpiar")}},
        dismissButton={TextButton(enabled=!cleanupBusy,onClick={cleanupBackup=null}){Text("Cancelar")}})}
    cleanupReview?.let{review->AlertDialog(onDismissRequest={cleanupReview=null},title={Text("Revisión de limpieza")},
        text={Text(review)},confirmButton={TextButton(onClick={cleanupReview=null}){Text("Conservar y volver")}})}
    fun reviewDiscard(rows:List<LocalInventoryItem>) {
        discardReview=rows.map{LocalDraftDiscard.Candidate(it.kind,it.id,it.revision)}
        discardMessage=null
    }
    discardReview?.let {review->AlertDialog(onDismissRequest={if(!discardBusy)discardReview=null},
        title={Text("Descartar ${review.size} borradores locales")},
        text={Text("Se ocultarán del listado activo y se conservarán fichas, fotos y archivos. Puedes recuperarlos desde Mostrar descartados. Si alguno cambió o tiene una entrega pendiente o histórica, se cancelará todo el grupo. Puentes SCAP no admite este descarte.")},
        confirmButton={TextButton(enabled=!discardBusy,onClick={discardBusy=true;scope.launch {
            try {
                val user=com.tuempresa.inventariovial.auth.AuthGraph.get(c).sessions.current.value?.id
                LocalDraftDiscard(db,c).changeMany(review,user)
                selected=emptySet();discardMessage="${review.size} borradores descartados; originales conservados."
            }catch(e:Exception){discardMessage=e.message ?: "No se pudo descartar el grupo."}
            finally{discardBusy=false;discardReview=null}
        }}){Text("Confirmar descarte reversible")}},
        dismissButton={TextButton(enabled=!discardBusy,onClick={discardReview=null}){Text("Cancelar")}})}
    if(prepareSegment) SessionDialog(vm.field,onClose={prepareSegment=false},requireRoute=true,onStarted={
        prepareSegment=false;selected=emptySet();search="";route="";pr="";state="";family="Todas"
        year=null;month=null;day=null;captureFilter="Todos";historical=false;newestFirst=true
        discardMessage="Nuevo tramo iniciado. Las fichas, el historial y las entregas pendientes se conservaron."
    })
    LazyColumn(Modifier.testTag("mis-registros").fillMaxSize().padding(horizontal=16.dp),contentPadding=PaddingValues(vertical=16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item {
            TextButton(onClick=onBack){Text("Volver")};Text("Mis registros",style=MaterialTheme.typography.headlineMedium)
            Text("SIC, puentes SCAP y Pavimentos · orden por carretera, calzada, sentido y posición disponible. No renumera carpetas.")
            OutlinedTextField(search,{search=it},label={Text("Buscar elemento o UUID")},modifier=Modifier.fillMaxWidth())
            Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                listOf("Todas","SIC","SCAP","Pavimentos").forEach{f->FilterChip(family==f,{family=f},label={Text(f)})}
            }
            OutlinedTextField(route,{route=it},label={Text("Carretera")},modifier=Modifier.fillMaxWidth())
            var routeMenu by remember{mutableStateOf(false)}
            Box {
                TextButton(onClick={routeMenu=true}){Text("Elegir carretera registrada")}
                DropdownMenu(routeMenu,onDismissRequest={routeMenu=false}) {
                    DropdownMenuItem(text={Text("Todas")},onClick={route="";routeMenu=false})
                    all.map{it.route}.filter{it.isNotBlank()}.distinct().sorted().forEach{road->
                        DropdownMenuItem(text={Text(road)},onClick={route=road;routeMenu=false})
                    }
                }
            }
            OutlinedTextField(pr,{pr=it},label={Text("Progresiva")},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(state,{state=it},label={Text("Buscar en estados (captura, Drive o copia local)")},modifier=Modifier.fillMaxWidth())
            RecordDateFilter(dateFilter,all.mapNotNull{parseRecordDate(it.surveyDate)}){year=it.year;month=it.month;day=it.day}
            Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                listOf("Todos","Borradores","Pendientes","Enviados","Archivados","Descartados").forEach {f->FilterChip(captureFilter==f,{captureFilter=f;selected=emptySet()},label={Text(f)})}
            }
            FilterChip(newestFirst,{newestFirst=!newestFirst},label={Text(if(newestFirst)"Fecha de registro: más recientes"else"Orden vial")})
            Row{Checkbox(historical,{historical=it});Text("Mostrar descartados, archivados y anulados")}
            if(captureFilter=="Descartados")Text("Borradores conservados fuera del listado activo. Restaurar no programa envíos; las fichas con entrega histórica permanecen protegidas.")
            Text("Copias automáticas ${if(LocalCopySettings.enabled(c))"activadas"else"desactivadas"}. Descargas ocupa espacio y puede ser visible para otras aplicaciones. Configura en Herramientas y ajustes → Copias locales.",style=MaterialTheme.typography.bodySmall)
            Text("${shown.size} registros")
            TextButton(enabled=shown.isNotEmpty(),onClick={
                val candidates=shown.count{LocalInventoryIndex.cleanupCandidate(it,drive.find{d->d.key==it.key},latest[it.key])}
                cleanupReview="${shown.size} registros visibles: $candidates candidatos con ACK y copia local de la revisión actual; ${shown.size-candidates} protegidos o pendientes. No se eliminaron fichas, fotos ni archivos. Antes de liberar espacio se debe verificar físicamente el respaldo y revisar los vínculos SCAP/SIC. Los borradores se descartan por separado de forma recuperable."
            }){Text("Preparar limpieza de registros visibles")}
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                TextButton(enabled=selectable.isNotEmpty(),onClick={selected=selectable.map{it.key}.toSet()}){Text("Seleccionar borradores visibles")}
                TextButton(enabled=cleanable.isNotEmpty(),onClick={selected=cleanable.map{it.key}.toSet()}){Text("Seleccionar entregados visibles")}
                TextButton(enabled=selected.isNotEmpty(),onClick={selected=emptySet()}){Text("Limpiar selección")}
            }
            TextButton(enabled=!cleanupBusy&&cleanable.any{it.key in selected},onClick={cleanupBusy=true;scope.launch{
                runCatching{ConfirmedLocalCleanup.prepare(c,db,cleanable.filter{it.key in selected}.map{ConfirmedLocalCleanup.Candidate(it.key,it.revision)})}
                    .onSuccess{cleanupBackup=it}.onFailure{discardMessage=it.message ?: "No se pudo verificar el respaldo."};cleanupBusy=false
            }}){Text(if(cleanupBusy)"Verificando respaldo…"else"Respaldar y limpiar entregados seleccionados")}
            TextButton(onClick={prepareSegment=true}){Text("Preparar nuevo tramo")}
            TextButton(enabled=shown.any{it.key in selected},onClick={scope.launch {
                runCatching{RemovalPreview.inspect(db,shown.filter{it.key in selected})}
                    .onSuccess{removalPreview=it}.onFailure{discardMessage="No se pudo inventariar la selección; no se modificó ningún registro."}
            }}){Text("Revisar eliminación de seleccionados (no habilitada)")}
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                TextButton(enabled=selectable.any{it.key in selected}&&!discardBusy,onClick={reviewDiscard(selectable.filter{it.key in selected})}){Text("Descartar seleccionados")}
                TextButton(enabled=selectable.isNotEmpty()&&!discardBusy,onClick={reviewDiscard(selectable)}){Text("Descartar todos los borradores visibles")}
            }
            discardMessage?.let{Text(it)}
        }
        items(shown,key={it.key}){r->
            val d=drive.find{it.key==r.key};val job=latest[r.key]
            Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
                if(r in selectable||r in cleanable)Row {
                    Checkbox(r.key in selected,{checked->selected=if(checked)selected+r.key else selected-r.key})
                    Text(if(r in selectable)"Seleccionar borrador"else"Seleccionar entregado para respaldo")
                }
                Text(r.name,style=MaterialTheme.typography.titleMedium);Text("${r.family} · ${r.route.ifBlank{"Ruta pendiente"}} · ${r.progressive}")
                TextButton(onClick={scope.launch {
                    runCatching{RemovalPreview.inspect(db,listOf(r))}.onSuccess{removalPreview=it}
                        .onFailure{discardMessage="No se pudo inventariar la ficha; se conserva sin cambios."}
                }}){Text("Revisar eliminación completa (no habilitada)")}
                Text("Captura: ${when(r.capture){"DISCARDED_LOCAL"->"Descartado · datos conservados";"DRAFT"->"Borrador";"ANNULLED"->"Anulado";else->"Ficha activa"}}")
                var technical by remember(r.key){mutableStateOf(false)}
                Text("Fecha de registro: ${recordDateLabel(r.surveyDate)}",style=MaterialTheme.typography.bodySmall)
                TextButton(onClick={technical=!technical}){Text(if(technical)"Ocultar detalles técnicos"else"Detalles técnicos")}
                if(technical){Text("UUID: ${r.id}",style=MaterialTheme.typography.bodySmall)
                    Text("Captura: ${r.capture} · revisión ${r.revision} · ${DateFormat.getDateTimeInstance().format(Date(r.revision))}",style=MaterialTheme.typography.bodySmall)}
                Text(if(LocalInventoryIndex.discarded(r))"Drive histórico: ${if(d?.syncedRevision!=null&&d.syncedRevision>0)"ACK R${d.syncedRevision}"else"sin confirmación"}. Fuera de pendientes activos; el registro se conserva."else "Drive de la ficha: ${SyncIntentPresentation.label(d,r.capture in setOf("ACTIVE","COMPLETE","CLOSED"),connected,configured)}${if(d?.syncedRevision!=null&&d.syncedRevision>0)" · ACK R${d.syncedRevision}"else" · sin ACK"}")
                Text(LocalInventoryIndex.localState(r,job))
                d?.receiptJson?.let{runCatching{JSONObject(it)}.getOrNull()}?.takeIf{it.optBoolean("ok")&&it.optString("key")==r.key&&it.optLong("revision")==d.syncedRevision}?.optString("logicalPath")?.takeIf{it.isNotBlank()}?.let{Text("Drive confirmado: $it",style=MaterialTheme.typography.bodySmall)}
                OutlinedButton(onClick={detail=r.key}){Text("Ver detalle")}
                val cleaning=choices.firstOrNull{it.key==ConfirmedLocalCleanup.PREFIX+r.key}?.destination?.let{runCatching{JSONObject(it).optString("state")=="CLEANING"}.getOrDefault(false)}==true
                if(cleaning)Text("Limpieza interrumpida: conserva el ZIP y vuelve a seleccionar esta ficha para reanudar, o restaura sus copias desde los originales.")
                if(cleaning||ConfirmedLocalCleanup.archived(choices,r.key,r.revision))TextButton(onClick={scope.launch{discardMessage=runCatching{withContext(Dispatchers.IO){ConfirmedLocalCleanup.restore(db,r.key)};"Registro restaurado al listado; puedes regenerar sus copias desde los originales verificados."}.getOrElse{it.message ?: "No se pudo restaurar; conserva el respaldo."}}}){Text("Restaurar registro archivado")}
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
    var documentHistory by remember(item.key){mutableStateOf(false)}
    val revisions=jobs.associateBy{it.id}
    val allDocuments=files.filter{it.state=="READY"&&it.mimeType in setOf(com.tuempresa.inventariovial.export.SicExcelWriter.XLSX_MIME,"application/pdf","application/zip")}
        .sortedWith(compareByDescending<LocalSharedFile>{revisions[it.jobId]?.revision ?: 0}.thenBy{if(it.name.startsWith("SCAP")||it.mimeType=="application/pdf")0 else 1}.thenBy{it.name}).distinctBy{it.uri}
    val documents=LocalCopyPresentation.currentFiles(jobs,allDocuments,documentHistory)
    val hasHistorical=LocalCopyPresentation.currentFiles(jobs,allDocuments).size<allDocuments.size
    fun open(share:Boolean){if(documents.size!=1){selector=true;return};val file=documents.single();scope.launch{
        message=runCatching{withContext(Dispatchers.IO){require(AndroidSharedDocumentStore(c).digest(android.net.Uri.parse(file.uri))==(file.size to file.sha256)){"El archivo ya no está disponible. Reintenta la copia local."}}
            c.startActivity(if(share)SharedFileActions.share(android.net.Uri.parse(file.uri),file.mimeType)else SharedFileActions.view(android.net.Uri.parse(file.uri),file.mimeType).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK));null
        }.getOrElse{"No se pudo abrir el documento: comprueba acceso o una aplicación compatible. Los originales se conservan."}
    }}
    Row(Modifier.horizontalScroll(rememberScrollState())){
        TextButton(enabled=documents.isNotEmpty(),onClick={open(false)}){Text("Abrir archivo")}
        TextButton(onClick={message="Ubicación local: ${documents.firstOrNull()?.relativePath ?: "Sin copia pública disponible"}. Abrir archivo accede al documento exacto. Android no garantiza abrir su carpeta."}){Text("Mostrar ubicación")}
        TextButton(enabled=documents.isNotEmpty(),onClick={val path=documents.first().relativePath;(c.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(android.content.ClipData.newPlainText("Ubicación local GBO",path));message="Ubicación copiada. Es una ruta descriptiva, no un permiso de acceso."}){Text("Copiar ubicación")}
        TextButton(enabled=documents.isNotEmpty(),onClick={open(true)}){Text("Compartir")}
    }
    documents.firstOrNull()?.let{Text("Local: ${it.relativePath}",style=MaterialTheme.typography.bodySmall)}
    if(hasHistorical)TextButton(onClick={documentHistory=!documentHistory}){Text(if(documentHistory)"Ocultar archivos anteriores"else"Ver archivos anteriores")}
    if(job==null||job.state in setOf("FAILED","PARTIAL","CLEANED"))TextButton(onClick={scope.launch{message=runCatching{withContext(Dispatchers.IO){LocalCopies.requestCurrent(c,db,item.kind,item.id)};"Actualizando archivos locales…"}.getOrElse{"No se pudo solicitar la copia; los originales siguen guardados."}}}){Text(if(job==null)"Incorporar histórico a copias locales"else if(job.state=="CLEANED")"Regenerar copias conservadas"else"Reintentar copia local")}
    if(selector)AlertDialog(onDismissRequest={selector=false},title={Text("Documentos disponibles")},text={
        LazyColumn{items(documents,key={it.id}){file->Text("R${revisions[file.jobId]?.revision ?: "?"}${if(file.name.contains("PROVISIONAL")||file.name.contains("BORRADOR"))" · provisional"else""}");SavedFileActions(file)}}
    },confirmButton={TextButton(onClick={selector=false}){Text("Cerrar")}})
    message?.let{Text(it,style=MaterialTheme.typography.bodySmall)}
}
