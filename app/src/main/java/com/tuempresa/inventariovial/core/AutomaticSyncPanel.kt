package com.tuempresa.inventariovial.core

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.tuempresa.inventariovial.DriveUploadPolicy
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.DateFormat
import java.util.Date
import androidx.work.WorkManager
import androidx.work.WorkInfo
import kotlinx.coroutines.flow.combine

object SyncIntentPresentation {
    fun confirmed(intent:DriveSyncIntent?):Boolean = intent!=null && intent.state=="SYNCED" &&
        intent.revision==intent.syncedRevision && intent.lastSyncedAt!=null && runCatching {
            val ack=JSONObject(intent.receiptJson ?: return@runCatching false)
            ack.optBoolean("ok") && ack.optString("key")==intent.key && ack.optLong("revision")==intent.revision
        }.getOrDefault(false)
    fun label(intent:DriveSyncIntent?,validated:Boolean,connected:Boolean,configured:Boolean,running:Boolean=false,retrying:Boolean=false):String {
        if(intent?.state=="CONFLICT")return "Conflicto · edición local conservada"
        if(!validated)return if((intent?.syncedRevision ?: 0)>0) "Actualización pendiente · requiere cierre" else "Borrador"
        if(confirmed(intent))return "Sincronizado"
        if(intent?.state=="BLOCKED")return "Bloqueado"
        if(intent?.state=="PENDING_VALIDATION" || intent==null)return "Pendiente de validación/ordenamiento"
        if(!configured || intent.state=="UNCONFIGURED")return "Sin configurar"
        if(!connected)return if(intent.syncedRevision>0) "Actualización pendiente · sin conexión" else "Pendiente de conexión"
        if(running)return "Subiendo"
        if(retrying || intent.state=="ERROR" && intent.error?.contains("(BUSY)")==true)return "Esperando para reintentar"
        if(intent.state=="ERROR")return "Error"
        if(intent.state=="UPLOADING")return "Pendiente de confirmación · esperando ejecución de Android"
        if(intent.state=="UPDATE_PENDING")return "Actualización pendiente"
        return "En cola"
    }
}

@Composable private fun observedDriveWork():List<WorkInfo> {
    val context=LocalContext.current
    val flow=remember(context) {val manager=WorkManager.getInstance(context)
        combine(manager.getWorkInfosForUniqueWorkFlow("automatic-drive-drain"),manager.getWorkInfosForUniqueWorkFlow("automatic-drive-recovery")){a,b->(a+b).distinctBy {it.id}}
    }
    return flow.collectAsState(emptyList()).value
}

@Composable fun AutomaticSyncSummary() {
    val context=LocalContext.current;val db=remember(context){InventoryDatabase.getInstance(context)}
    val intents by remember(db){db.automaticSyncDao().observe()}.collectAsState(emptyList())
    val work=observedDriveWork()
    Text("Drive · SIC, SCAP y pavimentos")
    Text("Fichas pendientes: ${intents.count {it.state!="DRAFT" && !SyncIntentPresentation.confirmed(it)}} · Subiendo: ${intents.count {SyncProgressPresentation.running(it,work)}} · Confirmadas: ${intents.count {SyncIntentPresentation.confirmed(it)}}")
    Text("Con destino Drive, el cierre programa el envío. Solo dispositivo conserva la copia local y no crea una entrega Drive.",style=MaterialTheme.typography.bodySmall)
    SyncQueueOverview()
}

@Composable fun SyncQueueOverview(technical:Boolean=false) {
    val context=LocalContext.current;val db=remember(context){InventoryDatabase.getInstance(context)}
    val intents by db.automaticSyncDao().observe().collectAsState(emptyList())
    var snapshot by remember{mutableStateOf<JSONObject?>(null)}
    LaunchedEffect(intents) {snapshot=kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO){
        runCatching{com.tuempresa.inventariovial.diagnostics.SyncQueueDiagnostics.snapshot(db)}.getOrNull()
    }}
    snapshot?.let {s->
        val rows=s.getJSONArray("localRecords")
        val local=(0 until rows.length()).map{rows.getJSONObject(it)}.groupBy{it.getString("family")}
        for((family,counts) in local)Text("$family · guardados: ${counts.sumOf{it.getInt("records")}} · sin finalizar: ${counts.filter{it.getString("localStatus") in setOf("DRAFT","OPEN")}.sumOf{it.getInt("records")}}",style=MaterialTheme.typography.bodySmall)
        Text("Archivos preparados conocidos: ${s.getInt("preparedFilesCurrentRevision")} · archivos confirmados: ${s.getInt("confirmedFilesCurrentRevision")}",style=MaterialTheme.typography.bodySmall)
        Text("Archivos conocidos esperando ACK de su ficha: ${s.getInt("knownFilesAwaitingRecordAck")}. Pueden incluir archivos ya transferidos.",style=MaterialTheme.typography.bodySmall)
        if(s.getInt("pendingRecordsWithUnknownFileCount")>0)Text("${s.getInt("pendingRecordsWithUnknownFileCount")} fichas aún sin preparación: su cantidad de archivos todavía no está calculada.",style=MaterialTheme.typography.bodySmall)
        if(s.getInt("validatedWithoutOutbox")>0)Text("${s.getInt("validatedWithoutOutbox")} fichas cerradas sin entrega programada. Revisa y pulsa Sincronizar ahora.",color=MaterialTheme.colorScheme.error)
        if(technical) {
            val families=s.getJSONArray("outboxByFamily")
            for(n in 0 until families.length()) {val row=families.getJSONObject(n);val counts=row.getJSONObject("states")
                Text(row.getString("family")+" · "+com.tuempresa.inventariovial.diagnostics.SyncQueueDiagnostics.states.joinToString(" · "){state->"$state: ${counts.optInt(state)}"},style=MaterialTheme.typography.bodySmall)
            }
            Text("UPLOADING guardado puede ser una ejecución interrumpida; solo el trabajador activo acredita que sigue subiendo.",style=MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable fun AutomaticSyncPanel(kind:String,id:String,validated:Boolean,compact:Boolean=false) {
    val context=LocalContext.current;val db=remember(context){InventoryDatabase.getInstance(context)}
    val intents by remember(db){db.automaticSyncDao().observe()}.collectAsState(emptyList())
    val intent=intents.find {it.key=="$kind/$id"};val scope=rememberCoroutineScope()
    val choices by db.localFilesDao().choices().collectAsState(emptyList())
    val onlyDevice=choices.any {it.key=="$kind/$id"&&it.destination=="DEVICE"}
    val work=observedDriveWork()
    val progress by remember(context){SyncProgressStore.get(context).flow}.collectAsState()
    val active=SyncProgressPresentation.running(intent,work)
    val retrying=work.any {it.state==WorkInfo.State.ENQUEUED && it.runAttemptCount>0}
    val currentProgress=progress["$kind/$id"]?.takeIf {it.revision==intent?.revision}
    var message by remember {mutableStateOf<String?>(null)}
    var tick by remember {mutableIntStateOf(0)}
    LaunchedEffect(Unit){while(true){kotlinx.coroutines.delay(3000);tick++}}
    val connected=remember(tick){val manager=context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        manager.getNetworkCapabilities(manager.activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)==true}
    Column {
        Text(if(onlyDevice) "Drive: no programado · Solo dispositivo" else "Drive: ${SyncIntentPresentation.label(intent,validated,connected,DriveUploadPolicy.configurationError()==null,active,retrying)}")
        currentProgress?.let {p->
            if(active)Text(SyncProgressPresentation.phase(p))
            if(SyncIntentPresentation.confirmed(intent))Text("Entrega confirmada")
            Text(SyncProgressPresentation.files(p))
            if(!SyncIntentPresentation.confirmed(intent)) {
                Text(SyncProgressPresentation.elapsed(p,System.currentTimeMillis()))
                Text("Tiempo restante no disponible")
            }
        }
        if(!compact) com.tuempresa.inventariovial.localfiles.LocalRecordFiles(kind,id,validated)
        if(!compact) {
            intent?.let {
                Text("Origen: ${if(it.origin=="AUTOMATIC") "Automática" else "Manual desde la app"}")
                Text("Revisión local: ${it.revision} · confirmada: ${it.syncedRevision}")
                androidx.compose.foundation.text.selection.SelectionContainer {Text("Identidad: ${it.key}")}
                Text("Última confirmación: ${it.lastSyncedAt?.let {time->DateFormat.getDateTimeInstance().format(Date(time))} ?: "Aún no confirmada"}")
                val prepared=it.preparedJson?.let(::JSONObject);val receipt=it.receiptJson?.let(::JSONObject)
                if(prepared?.optJSONObject("georeferencing")?.optString("externalSubmetricStatus")=="PENDING")
                    Text("Georreferenciación submétrica: pendiente",style=MaterialTheme.typography.bodySmall)
                Text("Archivos preparados: ${prepared?.optJSONArray("files")?.length() ?: 0} · confirmados para esta revisión: ${if(it.syncedRevision==it.revision)receipt?.optJSONArray("files")?.length() ?: 0 else 0}")
                receipt?.optString("logicalPath")?.takeIf {it.isNotBlank()}?.let {path->Text("Destino confirmado: $path")}
                receipt?.optString("folderId")?.takeIf {it.isNotBlank()}?.let {folder->Text("https://drive.google.com/drive/folders/$folder")}
                prepared?.optJSONArray("pendingSic")?.let {pending->for(n in 0 until pending.length())Text("SIC pendiente: ${pending.getString(n)}")}
                receipt?.optJSONArray("retainedSicFormats")?.let {prior->for(n in 0 until prior.length())Text("SIC conserva revisión anterior: ${prior.getString(n)}")}
                it.error?.let {error->Text(error,color=MaterialTheme.colorScheme.error)}
            }
            if(!onlyDevice && validated && intent?.state!="CONFLICT")TextButton(onClick={scope.launch {
                runCatching {AutomaticSync.manual(context,kind,id)}.onSuccess {message="Trabajo programado. Android lo ejecutará cuando haya conexión y recursos."}.onFailure {message=it.message}
            }}){Text("Reintentar ahora")}
            message?.let {Text(it)}
        }
    }
}
