package com.tuempresa.inventariovial.delivery

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tuempresa.inventariovial.ChoiceSelector
import com.tuempresa.inventariovial.viewmodel.InventoryViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.catch

@Composable
fun DeliveryReviewPanel(vm: InventoryViewModel, route: String) {
    var policy by remember(route) { mutableStateOf(DeliveryNamingPolicy.FOLDER_COMPACT) }
    var prepared by remember(route, policy) { mutableStateOf<PreparedRouteDelivery?>(null) }
    var busy by remember { mutableStateOf(false) }
    var message by remember(route) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val batches by remember(route) {
        vm.field.dao.observeDeliveries(route).catch {
            message = "No se pudo leer el historial de entregas: ${it.message ?: "error local"}"
            emit(emptyList())
        }
    }.collectAsState(initial = emptyList())
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Entrega de $route", style = MaterialTheme.typography.titleMedium)
            Text("Reordena y prepara las copias antes de crear carpetas definitivas en Drive.")
            ChoiceSelector(DeliveryNamingPolicy.entries.map { it.name }, policy.name) { if (!busy) policy = DeliveryNamingPolicy.valueOf(it) }
            Text(if (policy == DeliveryNamingPolicy.FOLDER_COMPACT) "Recomendado: 001_F01.jpg · correlativo general + índice del elemento"
                else "Convención histórica opcional del cliente. Se comprobará el límite de 20 caracteres; no se recortan nombres.")
            Button(enabled = !busy, onClick = { scope.launch {
                busy = true; prepared = null
                try { prepared = vm.routeDelivery.prepare(route, vm.field.reference.value, vm.field.settings.value.matchConfig(), policy); message = "Entrega lista para revisar." }
                catch (e: Exception) { message = e.message }
                finally { busy = false }
            } }) { Text(if (busy) "Preparando…" else "PREPARAR ENTREGA") }
            prepared?.let { delivery ->
                delivery.plan.folders.groupBy { it.asset.sibCode }.forEach { (sib, folders) ->
                    Text(sib.orEmpty(), style = MaterialTheme.typography.titleMedium)
                    folders.forEach { folder ->
                        Text("${folder.asset.folderName} · ${folder.photos.size} fotos + metadata.json")
                        folder.photos.forEach { Text(it.plannedPath, style = MaterialTheme.typography.bodySmall) }
                    }
                }
                Text("${delivery.fileCount} archivos, incluidos Excel SIC sin cambios de formato.")
                delivery.plan.warnings.distinct().forEach { Text(it, color = MaterialTheme.colorScheme.error) }
                val nonCompliant = delivery.plan.folders.flatMap { it.photos }.any { FileNameComplianceValidator.warning(it.photoName) != null }
                Button(enabled = !busy && !nonCompliant, onClick = { scope.launch {
                    busy = true
                    try { vm.routeDelivery.enqueue(delivery); message = "Entrega programada. Puedes cerrar esta pantalla; WorkManager conservará el lote." }
                    catch (e: Exception) { message = e.message }
                    finally { busy = false }
                } }) { Text("SINCRONIZAR") }
            }
            batches.firstOrNull()?.let { Text("Estado: ${it.status}${it.error?.let { error -> " · $error" } ?: ""}") }
            message?.let { Text(it) }
        }
    }
}
