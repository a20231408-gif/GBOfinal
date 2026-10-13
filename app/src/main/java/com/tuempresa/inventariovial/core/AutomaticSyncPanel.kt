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
        if(retrying)return "Reintentando · esperando ejecución de Android"
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
    Text("Pendientes: ${intents.count {it.state !in setOf("SYNCED","DRAFT")}} · Subiendo: ${intents.count {SyncProgressPresentation.running(it,work)}} · Confirmadas: ${intents.count {SyncIntentPresentation.confirmed(it)}}")
    Text("El cierre programa el envío. Android puede diferirlo; la cola queda guardada.",style=MaterialTheme.typography.bodySmall)
}

@Composable fun AutomaticSyncPanel(kind:String,id:String,validated:Boolean,compact:Boolean=false) {
    val context=LocalContext.current;val db=remember(context){InventoryDatabase.getInstance(context)}
    val intents by remember(db){db.automaticSyncDao().observe()}.collectAsState(emptyList())
    val intent=intents.find {it.key=="$kind/$id"};val scope=rememberCoroutineScope()
    val work=observedDriveWork()
    val progress by remember(context){SyncProgressStore.get(context).flow}.collectAsState()
    val active=SyncProgressPresentation.running(intent,work)
    val retrying=work.any {it.state==WorkInfo.State.ENQUEUED && it.runAttemptCount>0}
    val currentProgress=progress["$kind/$id"]?.takeIf {it.revision==intent?.revision}
    var message by remember {mutableStateOf<String?>(null)}
    var diagnostic by remember {mutableStateOf<String?>(null)}
    var checkingCapabilities by remember {mutableStateOf(false)}
    var tick by remember {mutableIntStateOf(0)}
    LaunchedEffect(Unit){while(true){kotlinx.coroutines.delay(3000);tick++}}
    val connected=remember(tick){val manager=context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        manager.getNetworkCapabilities(manager.activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)==true}
    Column {
        Text("Drive: ${SyncIntentPresentation.label(intent,validated,connected,DriveUploadPolicy.configurationError()==null,active,retrying)}")
        currentProgress?.let {p->
            if(active)Text(SyncProgressPresentation.phase(p))
            if(SyncIntentPresentation.confirmed(intent))Text("Sincronización completada")
            Text(SyncProgressPresentation.files(p))
            if(!SyncIntentPresentation.confirmed(intent)) {
                Text(SyncProgressPresentation.elapsed(p,System.currentTimeMillis()))
                Text("Tiempo restante no disponible")
            }
        }
        if(!compact) {
            Text("Fotografías V2 en compilación: ${if(com.tuempresa.inventariovial.BuildConfig.GBO_PHOTO_NAMES_V2_ENABLED) "habilitado" else "deshabilitado"}")
            TextButton(enabled=!checkingCapabilities && DriveUploadPolicy.configurationError()==null,onClick={
                checkingCapabilities=true
                scope.launch {
                    try {
                        diagnostic=kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            AutomaticDriveHttp().photoNamingDiagnostic().text()
                        }
                    } catch(e:kotlinx.coroutines.CancellationException) {throw e}
                    catch(_:Exception) {diagnostic="Capacidad del servidor no comprobada. Revisa la conexión y vuelve a consultar."}
                    finally {checkingCapabilities=false}
                }
            }){Text(if(checkingCapabilities) "Consultando capacidad…" else "Consultar capacidad fotográfica")}
            diagnostic?.let {Text(it,style=MaterialTheme.typography.bodySmall)}
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
            if(validated && intent?.state!="CONFLICT")TextButton(onClick={scope.launch {
                runCatching {AutomaticSync.manual(context,kind,id)}.onSuccess {message="Trabajo programado. Android lo ejecutará cuando haya conexión y recursos."}.onFailure {message=it.message}
            }}){Text("Reintentar ahora")}
            message?.let {Text(it)}
        }
    }
}
