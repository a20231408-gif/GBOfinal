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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val history by vm.history.collectAsState()
    val deliveries by remember(context) { InventoryDatabase.getInstance(context).inventoryDao().observeAllDeliveries() }
        .collectAsState(initial = emptyList())
    val db=remember(context) { InventoryDatabase.getInstance(context) }
    val scap by db.scapDao().observeInspections().collectAsState(initial=emptyList())
    val samples by db.pavementDao().observeSamples().collectAsState(initial=emptyList())
    var refresh by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while(true) { kotlinx.coroutines.delay(2000);refresh++ } }
    val pavementStates=remember(samples,refresh) { samples.filter { it.status=="CLOSED" }.map { DriveReceiptStore.state(context,it) } }
    val photos = history.filter { it.record.status == "ACTIVE" && it.record.sicCode != "SCAP" }
        .flatMap { it.photos }.filter { it.scapInspectionId == null }
    val states = (photos.map { it.syncStatus } + deliveries.map { it.status } + scap.filter { i -> i.status=="COMPLETE" && history.any { it.record.id==i.roadRecordId && it.record.status in setOf("ACTIVE","DRAFT") } }.map { it.syncStatus } + pavementStates)
    val counts = SyncOverview.counts(states)
    var notice by remember { mutableStateOf<String?>(null) }
    var showDetails by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    val times = DriveSyncClock.timestamps(context)
    fun displayTime(time: Long): String = if (time <= 0) "Aún no registrado" else DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(time))
    fun enqueue(batches: List<DeliveryBatchEntity>, retry: Boolean) {
        scope.launch {
            refreshing = true
            notice = try {
                val count=UnifiedSyncAdapter.enqueue(context,retry)
                if(count==0) "No hay entregas autorizadas pendientes. Revisa las fichas preparadas." else "$count entregas programadas para cuando haya Internet."
            } catch (e: Exception) { e.message ?: "No se pudo programar el reintento." }
            refreshing = false
        }
    }
    AlertDialog(onDismissRequest=onClose, title={Text("Centro de sincronización")},
        text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            val pending = states.count { SyncOverview.status(it) == UserSyncState.PENDIENTE }
            val errors = states.count { SyncOverview.status(it) == UserSyncState.ERROR }
            Text(if(pending == 0 && errors == 0 && states.none {SyncOverview.status(it)==UserSyncState.SUBIENDO} && states.all {SyncOverview.status(it) == UserSyncState.SINCRONIZADO}) "TODO SINCRONIZADO" else "Pendientes: $pending · Subiendo: ${states.count { SyncOverview.status(it)==UserSyncState.SUBIENDO }} · Errores: $errors")
            Text("Último intento de entrega SIC: ${displayTime(times.first)}")
            Text("Última entrega SIC correcta: ${displayTime(times.second)}")
            Text("✓ Guardado en tablet")
            Text("Inventario/SIC: ${deliveries.size} · SCAP: ${scap.size} · Pavimentos: ${pavementStates.size}")
            if (deliveries.any { it.status == "PREPARED" }) Text("Hay entregas preparadas que requieren confirmación antes de enviar.")
            if (deliveries.any { it.status == "BLOCKED" }) Text("Hay entregas que requieren revisión. No se reintentarán sin corregir la causa.", color=MaterialTheme.colorScheme.error)
            DriveUploadPolicy.configurationError()?.let { Text(it, color=MaterialTheme.colorScheme.error) }
            Button(enabled=!refreshing, onClick={enqueue(deliveries, false)}) { Text("Sincronizar ahora") }
            OutlinedButton(enabled=!refreshing && states.any {it == "ERROR"},onClick={enqueue(deliveries, true)}) { Text("Reintentar errores") }
            TextButton(onClick={showDetails=!showDetails}) { Text(if(showDetails) "Ocultar detalles" else "Ver detalles") }
            if(showDetails) {
                UserSyncState.entries.forEach { state -> Text("${state.label}: ${counts[state] ?: 0}") }
                deliveries.take(6).forEach { batch -> Text("${batch.routeCode}: ${SyncOverview.status(batch.status).label}" + (batch.error?.let { " · $it" } ?: "")) }
                Text("SCAP requiere revisión vigente; Pavimentos requiere cierre. Los borradores permanecen locales.", style=MaterialTheme.typography.bodySmall)
            }
            notice?.let {Text(it)}
        }}, confirmButton={TextButton(onClick=onClose) {Text("Cerrar")}})
}
