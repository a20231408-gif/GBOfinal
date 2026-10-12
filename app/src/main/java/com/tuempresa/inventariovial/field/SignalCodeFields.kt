package com.tuempresa.inventariovial.field

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tuempresa.inventariovial.ChoiceSelector
import com.tuempresa.inventariovial.SectionTitle
import com.tuempresa.inventariovial.TrafficSignCatalog
import com.tuempresa.inventariovial.catalog.SicCatalogRepository
import com.tuempresa.inventariovial.model.form.Sic22FormState
import com.tuempresa.inventariovial.model.form.SignalCodeSource

@Composable
fun SignalCodeFields(state: Sic22FormState, onChange: (Sic22FormState) -> Unit) {
    SectionTitle("SIC-22 · Señalización vertical")
    Text("Clase: 20 - Señalización Vertical")
    val types = SicCatalogRepository.options("sic22.type.0")
    val materials = SicCatalogRepository.options("sic22.material.0")
    val conditions = SicCatalogRepository.options("sic22.condition.0")
    fun option(options: List<String>, code: String) = options.find { it.substringBefore(" - ") == code } ?: code
    Text("Tipo")
    ChoiceSelector(types, option(types, state.typeCode)) { selected ->
        val type = selected.substringBefore(" - ")
        onChange(state.copy(typeCode = type, signalCode = if (state.signalCodeSource != SignalCodeSource.CATALOG || TrafficSignCatalog.contains(type, state.signalCode)) state.signalCode else ""))
    }
    Text("Material")
    ChoiceSelector(materials, option(materials, state.materialCode)) { onChange(state.copy(materialCode = it.substringBefore(" - "))) }
    if (state.typeCode == "3") {
        Text("Elemento informativo")
        val informativeElements = listOf("Señal informativa", "Hito / poste kilométrico")
        ChoiceSelector(informativeElements, if (state.isKilometerMarker) informativeElements[1] else informativeElements[0]) {
            onChange(state.selectInformativeElement(it == informativeElements[1]))
        }
    }
    if (state.isKilometerMarker) {
        Text("Hito / poste kilométrico: una sola ubicación. El hito representa la progresiva.")
        if (state.typeCode == "3") {
            Text("I-2A · POSTES DE KILOMETRAJE")
            TrafficSignImage("I-2A","POSTES DE KILOMETRAJE")
        }
        OutlinedTextField(state.kilometerPostNumber, { onChange(state.copy(kilometerPostNumber = it)) },
            label = { Text("Número del poste kilométrico") }, modifier = Modifier.fillMaxWidth())
    }
    if (state.usesSignalCode && !state.isKilometerMarker) {
        val modes = linkedMapOf(SignalCodeSource.CATALOG to "Buscar en catálogo", SignalCodeSource.MANUAL to "Código no listado",
            SignalCodeSource.UNIDENTIFIABLE to "SIN CÓDIGO IDENTIFICABLE")
        Text("Código de señal")
        ChoiceSelector(modes.values.toList(), modes[state.signalCodeSource].orEmpty()) { label ->
            onChange(state.selectSignalSource(modes.entries.first { it.value == label }.key))
        }
        when (state.signalCodeSource) {
            SignalCodeSource.CATALOG -> {
                TrafficSignVisualSelector(state.typeCode,state.signalCode) {onChange(state.selectCatalogSignal(it))}
            }
            SignalCodeSource.MANUAL -> {
                TrafficSignImage(state.signalCode,state.signalDescription,"MANUAL")
                OutlinedTextField(state.signalCode, { onChange(state.copy(signalCode = it)) }, label = { Text("Código manual") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(state.signalDescription, { onChange(state.copy(signalDescription = it)) }, label = { Text("Descripción") }, modifier = Modifier.fillMaxWidth())
            }
            SignalCodeSource.UNIDENTIFIABLE -> {
                TrafficSignImage("","Señal sin identificar","UNIDENTIFIABLE")
                Text("Señal rota, placa desaparecida o ilegible.")
                OutlinedTextField(state.signalObservation, { onChange(state.copy(signalObservation = it)) }, label = { Text("Observación") }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
    CatalogConditionSelector("Condición",state.conditionCode,conditions) { onChange(state.copy(conditionCode=it)) }
}
