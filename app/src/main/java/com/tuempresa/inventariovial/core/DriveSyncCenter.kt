package com.tuempresa.inventariovial.core

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    var message by remember {mutableStateOf<String?>(null)}
    AlertDialog(onDismissRequest=onClose,title={Text("Centro de sincronización")},text={
        Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            AutomaticSyncSummary()
            Text("Guardado en tablet y confirmación en Drive se muestran por separado.")
            Button(onClick={scope.launch {message=runCatching {"${UnifiedSyncAdapter.enqueue(context,false)} registros programados."}.getOrElse {it.message}}}) {Text("Sincronizar ahora")}
            OutlinedButton(onClick={scope.launch {message=runCatching {"${UnifiedSyncAdapter.enqueue(context,true)} reintentos programados."}.getOrElse {it.message}}}) {Text("Reintentar errores")}
            intents.forEach {i->AutomaticSyncPanel(i.kind,i.recordId,validated=i.state!="DRAFT",compact=false)}
            message?.let {Text(it)}
        }
    },confirmButton={TextButton(onClick=onClose){Text("Cerrar")}})
}
