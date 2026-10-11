package com.tuempresa.inventariovial.field

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tuempresa.inventariovial.road.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun LocationSuggestionPanel(reference: RoadReferenceData, config: MatchConfig, route: String, roadbed: String,
    latitude: Double?, longitude: Double?, accuracy: Double?, endpoint: String,
    onSuggestion: (LocationSuggestion?) -> Unit, onConfirm: (LocationSuggestion) -> Unit, onEdit: () -> Unit) {
    var choice by remember(reference, route, roadbed, latitude, longitude, accuracy) { mutableStateOf("") }
    var keptRoute by remember(route, latitude, longitude) { mutableStateOf(false) }
    val state = remember(reference, config, route, roadbed, latitude, longitude, accuracy, choice) { mutableStateOf<LocationSuggestion?>(null) }
    val notify by rememberUpdatedState(onSuggestion)
    LaunchedEffect(state) {
        notify(null)
        state.value = withContext(Dispatchers.Default) {
            val engine = LocationSuggestionEngine(reference, config)
            val original = engine.suggest(route, roadbed, latitude, longitude, accuracy)
            if (choice.isBlank()) original else engine.suggest(choice, roadbed, latitude, longitude, accuracy)
                .copy(routeMatchAmbiguous = original.routeMatchAmbiguous, candidates = original.candidates)
        }
        notify(state.value)
    }
    val suggestion = state.value
    val low = suggestion?.match?.let { it.confidence < .65 } == true || suggestion?.routeMatchAmbiguous == true
    val statusColor = if (suggestion?.match == null) Color(0xFFB3261E) else if (low) Color(0xFF8A5D00) else Color(0xFF176B35)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("UBICACIÓN SUGERIDA POR GNSS · $endpoint", style = MaterialTheme.typography.titleMedium)
            Text("Ruta: ${suggestion?.suggestedRouteCode ?: "--"}")
            Text("PR: ${suggestion?.suggestedPrCode ?: "--"} · Distancia: ${suggestion?.pr?.distanceText ?: "--"} m")
            Text("GPS: ${accuracy?.let { String.format(Locale.US, "± %.1f m", it) } ?: "sin precisión"}")
            Text("Distancia al eje: ${suggestion?.routeMatchDistanceM?.let { String.format(Locale.US, "%.1f m", it) } ?: "--"}")
            Text(when {
                suggestion == null -> "Validando ubicación…"
                suggestion.match == null -> "⚠ ${suggestion.message}"
                low -> "⚠ Coincidencia GNSS de baja confianza. Revisar ubicación."
                suggestion.pr != null -> "✓ Ruta detectada · PR identificado · Distancia calculada"
                else -> "✓ Ruta detectada"
            }, color = statusColor)
            if (suggestion?.match != null) suggestion.message?.let { Text(it) }
            if (suggestion?.routeMatchAmbiguous == true) {
                Text("Se detectaron varias rutas posibles. Selecciona una candidata y confirma.")
                suggestion.candidates.forEach { candidate ->
                    OutlinedButton(onClick = { choice = candidate.routeCode }) { Text(candidate.routeCode) }
                }
            }
            suggestion?.discrepancyRoute?.let { other ->
                Text("Ruta registrada: $route · Ruta GNSS sugerida: $other")
                if (keptRoute) Text("Se conserva la ruta $route.")
                else TextButton(onClick = { keptRoute = true }) { Text("Conservar $route") }
                TextButton(onClick = onEdit) { Text("Revisar") }
            }
            Row {
                Button(enabled = suggestion?.match != null && (!suggestion.routeMatchAmbiguous || choice.isNotBlank()),
                    onClick = { suggestion?.let(onConfirm) }) { Text("CONFIRMAR UBICACIÓN") }
                TextButton(onClick = onEdit) { Text("EDITAR") }
            }
        }
    }
}
