package com.tuempresa.inventariovial.core

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.work.*
import com.tuempresa.inventariovial.DriveUploadPolicy
import com.tuempresa.inventariovial.DriveUploadWorker
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.DeliveryBatchEntity
import com.tuempresa.inventariovial.viewmodel.InventoryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

/** Record-only UI: counters include existing reviewed batches; never uploads unreviewed drafts. */
object DriveSyncClock {
    private const val STORE = "drive_core_sync_times"
    fun attempt(context: Context) { context.getSharedPreferences(STORE, Context.MODE_PRIVATE).edit().putLong("attempt", System.currentTimeMillis()).apply() }
    fun success(context: Context) { context.getSharedPreferences(STORE, Context.MODE_PRIVATE).edit().putLong("success", System.currentTimeMillis()).apply() }
    fun timestamps(context: Context): Pair<Long,Long> = context.getSharedPreferences(STORE, Context.MODE_PRIVATE).run {
        getLong("attempt", 0L) to getLong("success", 0L)
    }
}

object DriveDeliveryRetry {
    fun enqueue(context: Context, batch: DeliveryBatchEntity, replaceFailed: Boolean): Operation {
        require(batch.status in if(replaceFailed) setOf("ERROR") else setOf("ERROR", "QUEUED")) {
            "El lote debe estar revisado y en error o pendiente"
        }
        require(java.io.File(batch.manifestPath).isFile) { "No existe la copia local de la entrega" }
        DriveUploadPolicy.requireConfiguration()
        val job = OneTimeWorkRequestBuilder<DriveUploadWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setInputData(workDataOf("deliveryId" to batch.id))
            .build()
        DriveSyncClock.attempt(context)
        return WorkManager.getInstance(context).enqueueUniqueWork("route-delivery-${batch.routeCode}",
            if(replaceFailed) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP, job)
    }
}

@Composable
fun DriveSyncCenter(vm: InventoryViewModel, onClose: () -> Unit) {
    val context=LocalContext.current;val scope=rememberCoroutineScope()
    val db=remember(context){InventoryDatabase.getInstance(context)}
    val intents by db.automaticSyncDao().observe().collectAsState(emptyList())
    val identities by produceState<Map<String,DriveDeliveryIdentity>>(emptyMap(),intents) {
        value=withContext(Dispatchers.IO){intents.associate{i->i.key to runCatching{DriveDeliveryPresentation.identity(db,i)}
            .getOrElse{DriveDeliveryIdentity("Elemento ${i.kind}","No se pudo consultar la ficha",null,false)}}}
    }
    val work=observedDriveWork()
    val progress by remember(context){SyncProgressStore.get(context).flow}.collectAsState()
    var connected by remember{mutableStateOf(false)}
    LaunchedEffect(context){while(true){
        val manager=context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        connected=manager.getNetworkCapabilities(manager.activeNetwork)?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)==true
        kotlinx.coroutines.delay(3000)
    }}
    val views=intents.associate{i->i.key to DriveDeliveryPresentation.view(i,identities[i.key] ?: DriveDeliveryIdentity("Consultando ficha…","",null,false),
        SyncProgressPresentation.running(i,work),connected,DriveUploadPolicy.configurationError()==null,
        work.any{it.state==WorkInfo.State.ENQUEUED&&it.runAttemptCount>0}&&i.state=="ERROR",progress[i.key])}
    var message by remember {mutableStateOf<String?>(null)}
    AlertDialog(onDismissRequest=onClose,title={Text("Entregas Drive")},text={
        Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("${views.values.count{it.confirmed}} confirmadas · ${views.values.count{!it.confirmed}} pendientes")
            Text("Guardar en la tablet no confirma una entrega en Drive.",style=MaterialTheme.typography.bodySmall)
            Button(onClick={scope.launch {message=runCatching {"${UnifiedSyncAdapter.enqueue(context,false)} registros programados."}.getOrElse {it.message}}}) {Text("Sincronizar ahora")}
            OutlinedButton(onClick={scope.launch {message=runCatching {"${UnifiedSyncAdapter.enqueue(context,true)} reintentos programados."}.getOrElse {it.message}}}) {Text("Reintentar errores")}
            val rows=intents.map{i->DriveDeliveryEntry(i,identities[i.key] ?: DriveDeliveryIdentity("Consultando ficha…","",null,false),views.getValue(i.key),progress[i.key])}
            DriveDeliveryLists(rows){i->
                scope.launch{message=runCatching{AutomaticSync.manual(context,i.kind,i.recordId);"Reintento programado."}.getOrElse{it.message}}
            }
            DeliveryCenterTechnicalSummary()
            message?.let {Text(it)}
        }
    },confirmButton={TextButton(onClick=onClose){Text("Cerrar")}})
}

@Composable private fun DeliveryCenterTechnicalSummary() {
    var expanded by rememberSaveable{mutableStateOf(false)}
    TextButton(onClick={expanded=!expanded}){Text("${if(expanded)"−"else"+"} Diagnóstico de la cola")}
    if(expanded)SyncQueueOverview(technical=true)
}
