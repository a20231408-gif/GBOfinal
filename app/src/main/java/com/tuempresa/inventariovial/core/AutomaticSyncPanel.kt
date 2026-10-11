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

object SyncIntentPresentation {
    fun label(intent:DriveSyncIntent?,validated:Boolean,connected:Boolean,configured:Boolean):String {
        if(!validated)return if((intent?.syncedRevision ?: 0)>0) "Actualización pendiente · requiere cierre" else "Borrador"
        if(intent?.state=="SYNCED" && intent.syncedRevision==intent.revision)return "Sincronizado"
        if(intent?.state=="BLOCKED")return "Bloqueado"
        if(intent?.state=="PENDING_VALIDATION" || intent==null)return "Pendiente de validación/ordenamiento"
        if(!configured || intent.state=="UNCONFIGURED")return "Sin configurar"
        if(intent.state=="ERROR")return "Error"
        if(intent.state=="UPLOADING")return "Subiendo"
        if(!connected)return "Pendiente de conexión"
        if(intent.state=="UPDATE_PENDING")return "Actualización pendiente"
        return "En cola"
    }
}

@Composable fun AutomaticSyncSummary() {
    val context=LocalContext.current;val db=remember(context){InventoryDatabase.getInstance(context)}
    val intents by remember(db){db.automaticSyncDao().observe()}.collectAsState(emptyList())
    Text("Drive · SIC, SCAP y pavimentos")
    Text("Pendientes: ${intents.count {it.state !in setOf("SYNCED","DRAFT")}} · Subiendo: ${intents.count {it.state=="UPLOADING"}} · Confirmadas: ${intents.count {it.state=="SYNCED" && it.revision==it.syncedRevision}}")
    Text("El cierre programa el envío. Android puede diferirlo; la cola queda guardada.",style=MaterialTheme.typography.bodySmall)
}

@Composable fun AutomaticSyncPanel(kind:String,id:String,validated:Boolean,compact:Boolean=false) {
    val context=LocalContext.current;val db=remember(context){InventoryDatabase.getInstance(context)}
    val intents by remember(db){db.automaticSyncDao().observe()}.collectAsState(emptyList())
    val intent=intents.find {it.key=="$kind/$id"};val scope=rememberCoroutineScope()
    var message by remember {mutableStateOf<String?>(null)}
    var tick by remember {mutableIntStateOf(0)}
    LaunchedEffect(Unit){while(true){kotlinx.coroutines.delay(3000);tick++}}
    val connected=remember(tick){val manager=context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        manager.getNetworkCapabilities(manager.activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)==true}
    Column {
        Text("Drive: ${SyncIntentPresentation.label(intent,validated,connected,DriveUploadPolicy.configurationError()==null)}")
        if(!compact) {
            intent?.let {
                Text("Origen: ${if(it.origin=="AUTOMATIC") "Automática" else "Manual desde la app"}")
                Text("Revisión local: ${it.revision} · confirmada: ${it.syncedRevision}")
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
            if(validated)TextButton(onClick={scope.launch {
                runCatching {AutomaticSync.manual(context,kind,id)}.onSuccess {message="Trabajo programado. Android lo ejecutará cuando haya conexión y recursos."}.onFailure {message=it.message}
            }}){Text("Reintentar ahora")}
            message?.let {Text(it)}
        }
    }
}
