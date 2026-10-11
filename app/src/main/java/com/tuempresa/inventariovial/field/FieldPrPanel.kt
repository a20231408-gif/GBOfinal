package com.tuempresa.inventariovial.field

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tuempresa.inventariovial.ChoiceSelector
import com.tuempresa.inventariovial.FinalLocationCapture
import com.tuempresa.inventariovial.location.GeoLocation
import com.tuempresa.inventariovial.road.*
import kotlinx.coroutines.launch

@Composable
fun FieldPrPanel(field: FieldController, selectedRoute: String, selectedBed: String) {
    var expanded by remember { mutableStateOf(false) }
    var route by remember(selectedRoute) { mutableStateOf(selectedRoute) }
    var bed by remember(selectedBed) { mutableStateOf(selectedBed) }
    var code by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(FieldPrRepository.types.first()) }
    var description by remember { mutableStateOf("") }
    var fix by remember { mutableStateOf<GeoLocation?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val reference by field.reference.collectAsState()
    val settings by field.settings.collectAsState()
    val scope = rememberCoroutineScope()
    OutlinedButton(onClick = { expanded = !expanded }) { Text("REGISTRAR PR") }
    if (expanded) Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("PR de campo · confirma la ruta y calzada del hito")
        ChoiceSelector(reference.routes.map { it.routeCode }, route) { route = it }
        Text("Calzada contractual")
        ChoiceSelector(com.tuempresa.inventariovial.catalog.SicCatalogRepository.roadbedCodes + "Otro", bed) { bed = if (it == "Otro") "" else it }
        if (bed !in com.tuempresa.inventariovial.catalog.SicCatalogRepository.roadbedCodes)
            OutlinedTextField(bed, { bed = it.uppercase() }, label = { Text("Código de calzada") })
        OutlinedTextField(code, { code = it }, label = { Text("Código PR (4 dígitos)") })
        ChoiceSelector(FieldPrRepository.types, type) { type = it }
        OutlinedTextField(description, { description = it }, label = { Text("Descripción opcional") })
        FinalLocationCapture(fix) { fix = it }
        LocationSuggestionPanel(reference, settings.matchConfig(), "", bed, fix?.latitude, fix?.longitude,
            fix?.horizontalAccuracy?.toDouble(), "PR de campo", {}, { it.suggestedRouteCode?.let { found -> route = found } }, {})
        Button(enabled = !saving && fix != null, onClick = { scope.launch {
            saving = true
            try {
                FieldPrRepository(field.dao).register(reference, settings.matchConfig(), route, bed, code, type, description, fix!!)
                field.reloadReference()
                message = "PR guardado. Los registros anteriores no se han modificado."
            } catch (e: Exception) { message = e.message ?: "No se pudo guardar el PR." }
            finally { saving = false }
        } }) { Text("GUARDAR PR") }
        OutlinedButton(onClick = { field.recalculateSuggestions { message = it } }) { Text("RECALCULAR UBICACIONES") }
        message?.let { Text(it) }
    }
}
