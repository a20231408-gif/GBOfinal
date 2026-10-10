package com.tuempresa.inventariovial.pavement.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tuempresa.inventariovial.pavement.*
import kotlinx.coroutines.*

@Composable internal fun PavementSicPanel(s: PavementSampleEntity, damages: List<PavementDamageEntity>, repo: PavementRepository, canEdit: Boolean) {
    val context=LocalContext.current;val scope=rememberCoroutineScope()
    var editing by remember(s.id) { mutableStateOf<PavementSicFormat?>(null) }
    var exporting by remember { mutableStateOf<PavementSicFormat?>(null) }
    var message by remember { mutableStateOf("") }
    val export=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) { uri ->
        val format=exporting
        if(uri!=null && format!=null)scope.launch {
            runCatching { withContext(Dispatchers.IO) { PavementGeneratedFiles.export(context,uri,s,"SIC_${format.name}","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet","xlsx") { PavementSic.write(it,s,damages,format) } } }
                .onSuccess { message="${format.label} guardado." }.onFailure { message=it.message.orEmpty() }
        }
    }
    Text("Datos para SIC",style=MaterialTheme.typography.titleMedium)
    PavementSicFormat.entries.filter { it.surface==s.surface }.forEach { format ->
        val status=PavementSic.status(s,damages,format)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            Text("${format.label} · ${if(status.complete)"✓ Completo" else "! Faltan ${status.missingCount}"}",Modifier.weight(1f))
            if(canEdit)TextButton(onClick={editing=format}) { Text("Completar") }
            TextButton(enabled=status.complete,onClick={exporting=format;export.launch("${format.label}-M${s.muestra}.xlsx")}) { Text("Excel") }
        }
        status.blockers.forEach { Text(it,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.error) }
    }
    if(message.isNotBlank())Text(message)
    editing?.let { format ->
        val status=PavementSic.status(s,damages,format)
        var chosen by remember(format) { mutableStateOf(damages.firstOrNull()?.id.orEmpty()) }
        var revision by remember { mutableIntStateOf(0) }
        val d=damages.firstOrNull { it.id==chosen }
        val missing=status.rows.firstOrNull { it.damageId==chosen }?.missing.orEmpty()
        val input=remember(s.sicDataJson,format,chosen,revision) { mutableStateMapOf<String,String>().apply { PavementSic.manualFields.forEach { key -> this[key]=PavementSic.value(s,format,chosen,key) } } }
        AlertDialog(onDismissRequest={editing=null},title={Text("Completar ${format.label}")},text={
            Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Text("Ruta, calzada, fecha, dimensiones y gravedad se reutilizan cuando son inequívocas. PR contractual no se deduce de la progresiva.",style=MaterialTheme.typography.bodySmall)
                if(d!=null)Choice("Daño",chosen,damages.map { it.id },{ id -> damages.first { it.id==id }.let { "#${it.correlativo} · ${it.name}" } }) { chosen=it }
                if(missing.isEmpty())Text("Datos de este daño completos.")
                for(key in PavementSic.manualFields.filter { it in missing || !input[it].isNullOrBlank() }) {
                    when(key) {
                        "tipo" -> Choice("Tipo contractual",input[key].orEmpty(),(1..format.types.size).map{it.toString()},{ code -> code.toIntOrNull()?.let { "${code.padStart(2,'0')} · ${format.types[it-1]}" } ?: "Selecciona" }) { input[key]=it }
                        "gravedad","densidad" -> Choice(if(key=="gravedad")"Nivel de gravedad"else"Clase de densidad",input[key].orEmpty(),listOf("1","2","3"),{it.ifBlank{"Selecciona"}}) { input[key]=it }
                        "faja" -> Field("Faja SIC (número)",input[key].orEmpty()) { input[key]=it }
                        else -> Field(when(key) { "prInicio"->"Código PR inicio (4 dígitos)";"prFin"->"Código PR fin (4 dígitos)";"distanciaInicio"->"Distancia desde PR inicio (m)";"distanciaFin"->"Distancia desde PR fin (m)";"longitud"->"Longitud de daño (m)";else->"Ancho de daño (m)" },input[key].orEmpty()) { input[key]=it }
                    }
                }
                Text("Captura temporal: ${PavementSic.manualOrigin}",style=MaterialTheme.typography.bodySmall)
                status.blockers.forEach { Text(it,color=MaterialTheme.colorScheme.error) }
                if(message.isNotBlank())Text(message)
            }
        },confirmButton={TextButton(enabled=d!=null,onClick={scope.launch {
            runCatching { repo.saveSic(s.id,format,chosen,input.toMap()) }.onSuccess { message="Faltantes guardados.";revision++ }.onFailure { message=it.message.orEmpty() }
        }}) { Text("Guardar") }},dismissButton={TextButton(onClick={editing=null}) { Text("Cerrar") }})
    }
}
