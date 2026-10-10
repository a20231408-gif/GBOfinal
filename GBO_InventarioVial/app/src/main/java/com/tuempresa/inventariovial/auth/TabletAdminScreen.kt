package com.tuempresa.inventariovial.auth

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Build
import android.os.PersistableBundle
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tuempresa.inventariovial.access.DeviceAccessManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TabletAdminScreen(graph:AuthGraph,onClose:(Boolean)->Unit) {
    val context=LocalContext.current
    SideEffect{com.tuempresa.inventariovial.diagnostics.StabilityDiagnostics.screen("TABLET_ADMIN")}
    val scope=rememberCoroutineScope()
    val current=remember {DeviceAccessManager(context).fingerprint()}
    val user by graph.sessions.current.collectAsState()
    var pin by remember {mutableStateOf("")}
    var active by remember {mutableStateOf(false)}
    var busy by remember {mutableStateOf(false)}
    var message by remember {mutableStateOf<String?>(null)}
    var rows by remember {mutableStateOf<List<JSONObject>>(emptyList())}
    var events by remember {mutableStateOf<List<JSONObject>>(emptyList())}
    var fingerprint by remember {mutableStateOf("")}
    var alias by remember {mutableStateOf("")}
    var confirmation by remember {mutableStateOf<Pair<String,String>?>(null)}
    // Intentionally no rememberSaveable, Room, preferences, logs or analytics for either token.
    var oneTimeToken by remember {mutableStateOf<String?>(null)}
    fun close(expired:Boolean=false){pin="";oneTimeToken=null;graph.tablets.forget();onClose(expired)}
    BackHandler {close()}
    DisposableEffect(Unit){onDispose{graph.tablets.forget();oneTimeToken=null;pin=""}}
    LaunchedEffect(active) {
        if(active) while(true) {
            delay(1000)
            if(!graph.tablets.authorized) {close(true);break}
        }
    }
    fun task(block:suspend ()->Unit) {
        busy=true
        scope.launch {
            try{block()}
            catch(e:CancellationException){throw e}
            catch(e:UserServiceException){
                message=e.message
                if(active && e.code in setOf("SESSION_EXPIRED","TABLET_ADMIN_REQUIRED","DEVICE_REVOKED"))close(true)
            }
            catch(_:Exception){message="No se pudo completar la operación. Revisa la conexión e inténtalo nuevamente."}
            finally{busy=false}
        }
    }
    suspend fun refresh(){val a=graph.tablets.call("listTablets").getJSONArray("tablets");rows=List(a.length()){a.getJSONObject(it)}}
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item {
            TextButton(onClick={close()}){Text("← Inicio")}
            Text("ADMINISTRACIÓN DE TABLETS",style=MaterialTheme.typography.headlineSmall)
            Text(user?.displayName.orEmpty())
            if(!active) {
                Text("Reingresa tu PIN para autorizar la administración durante 15 minutos.")
                PinField(pin,{pin=it},"PIN")
                Button(enabled=!busy,onClick={val entered=pin.toCharArray();pin="";task {
                    try {graph.tablets.authenticate(entered);active=true;refresh()} finally {entered.fill('\u0000')}
                }}){Text("Autorizar administración")}
            } else {
                Text("Huella completa de la nueva tablet")
                OutlinedTextField(fingerprint,{fingerprint=it.trim().lowercase(Locale.ROOT)},label={Text("Huella (64 caracteres)")},modifier=Modifier.fillMaxWidth())
                OutlinedTextField(alias,{alias=it.take(120)},label={Text("Alias")},modifier=Modifier.fillMaxWidth())
                Button(enabled=!busy && fingerprint.matches(Regex("[a-f0-9]{64}")) && alias.isNotBlank(),onClick={task {
                    oneTimeToken=null
                    val r=graph.tablets.call("provisionTabletAdmin",JSONObject().put("targetDeviceId",fingerprint).put("alias",alias))
                    oneTimeToken=r.getString("tabletToken");fingerprint="";alias="";refresh()
                }}){Text("Autorizar tablet")}
                TextButton(enabled=!busy,onClick={task{refresh()}}){Text("Actualizar lista")}
            }
            if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
            message?.let{Text(it)}
        }
        if(active)items(rows,key={it.getString("deviceId")}){t->
            val id=t.getString("deviceId")
            Card(Modifier.fillMaxWidth()) {Column(Modifier.padding(12.dp)) {
                Text(t.getString("alias"),style=MaterialTheme.typography.titleMedium)
                Text("${id.take(8)}…${id.takeLast(8)} · ${if(t.getBoolean("active"))"Activa" else "Revocada"}")
                TextButton(enabled=!busy,onClick={task {
                    val a=graph.tablets.call("getTabletAudit",JSONObject().put("targetDeviceId",id)).getJSONArray("events")
                    events=List(a.length()){a.getJSONObject(it)}
                }}){Text("Historial")}
                if(t.getBoolean("active")) {
                    TextButton(enabled=!busy && id!=current,onClick={confirmation="revokeTabletAdmin" to id}){Text("Revocar")}
                    if(id==current)Text("No puedes revocar esta tablet desde la sesión actual.")
                } else TextButton(enabled=!busy,onClick={confirmation="reactivateTabletAdmin" to id}){Text("Reactivar")}
                TextButton(enabled=!busy,onClick={confirmation="rotateTabletToken" to id}){Text("Rotar token")}
            }}
        }
        items(events){e->Text("${SimpleDateFormat("dd/MM/yyyy HH:mm",Locale.getDefault()).format(Date(e.getLong("timestamp")))} · ${e.optString("actor")} · ${e.optString("action")} · ${e.optString("alias")} · ${e.optString("deviceId").take(8)}…")}
    }
    confirmation?.let{(action,id)->AlertDialog(onDismissRequest={confirmation=null},title={Text(when(action){"rotateTabletToken"->"Rotar token";"revokeTabletAdmin"->"Revocar tablet";else->"Reactivar tablet"})},
        text={Text(if(action=="rotateTabletToken")"El token anterior dejará de funcionar." else "La operación se aplicará en el servidor. Se conservarán los datos y el historial.")},
        confirmButton={TextButton(onClick={confirmation=null;task {
            oneTimeToken=null
            val r=graph.tablets.call(action,JSONObject().put("targetDeviceId",id))
            oneTimeToken=r.optString("tabletToken").ifBlank{null}
            // Rotating the current device invalidates this session too; let its one-time response be copied first.
            if(id!=current)refresh() else active=false
        }}){Text("Confirmar")}},dismissButton={TextButton(onClick={confirmation=null}){Text("Cancelar")}})}
    oneTimeToken?.let{secret->AlertDialog(onDismissRequest={oneTimeToken=null},title={Text("Token de la tablet · única visualización")},
        text={Column {Text("Cópialo ahora y configura la tablet autorizada. No se podrá consultar después.");Text(secret)}},
        confirmButton={TextButton(onClick={
            val clip=ClipData.newPlainText("Token de tablet",secret)
            if(Build.VERSION.SDK_INT>=33)clip.description.extras=PersistableBundle().apply {putBoolean("android.content.extra.IS_SENSITIVE",true)}
            context.getSystemService(ClipboardManager::class.java).setPrimaryClip(clip)
            oneTimeToken=null
        }){Text("Copiar y descartar")}},dismissButton={TextButton(onClick={oneTimeToken=null}){Text("Descartar")}})}
}
