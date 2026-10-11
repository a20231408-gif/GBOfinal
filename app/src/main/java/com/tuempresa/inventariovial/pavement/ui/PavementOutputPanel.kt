package com.tuempresa.inventariovial.pavement.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import com.tuempresa.inventariovial.pavement.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

@Composable internal fun PavementOutputPanel(s:PavementSampleEntity, damages:List<PavementDamageEntity>, repo:PavementRepository) {
    val context=LocalContext.current;val scope=rememberCoroutineScope()
    val calculator=remember{PavementCalculator(context)}
    val result=remember(s,damages){calculator.calculate(s,damages)}
    var message by remember{mutableStateOf("")}
    var busy by remember{mutableStateOf(false)}
    var technicalDetails by remember(s.id){mutableStateOf(false)}
    val samples by remember { repo.samples() }.collectAsState(emptyList())
    val sections by remember { repo.sections() }.collectAsState(emptyList())
    var sectionDialog by remember{mutableStateOf(false)}
    val section=sections.firstOrNull{it.ruta==s.ruta && it.tramo==s.tramo && it.surface==s.surface}
    val pdf=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")){uri->
        if(uri!=null)scope.launch{busy=true;runCatching{
            val photos=damages.associate{it.id to repo.photos(it.id)}
            withContext(Dispatchers.IO){PavementGeneratedFiles.export(context,uri,s,"FIELD","application/pdf","pdf") { PavementPdf.write(it,PavementExportSample(s,damages,result),photos) }}
        }.onSuccess{message="PDF guardado."}.onFailure{message=it.message.orEmpty()};busy=false}
    }
    val excel=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")){uri->
        if(uri!=null)scope.launch{busy=true;runCatching{
            val samples=repo.samples().first().filter{it.surface==s.surface}
            val data=samples.map{sample->val rows=repo.damages(sample.id).first();PavementExportSample(sample,rows,calculator.calculate(sample,rows))}
            val configs=repo.sections().first()
            withContext(Dispatchers.IO){PavementGeneratedFiles.export(context,uri,s,"GROUP_DATA","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet","xlsx") { PavementExcel.write(it,data,configs) }}
        }.onSuccess{message="Excel guardado: ${s.surface}."}.onFailure{message=it.message.orEmpty()};busy=false}
    }
    val reviewPhotos by produceState<List<java.io.File>>(emptyList(),s.id,damages) { value=damages.flatMap { repo.photos(it.id) }.map { java.io.File(it.localPath) } }
    com.tuempresa.inventariovial.core.RecordReviewSection("Revisión de muestra",
        listOf("Muestra" to s.muestra.toString(),"Progresiva" to "${s.prInicial} – ${s.prFinal}"),reviewPhotos,
        com.tuempresa.inventariovial.core.CompletionResult(result.pending.map {com.tuempresa.inventariovial.core.ValidationIssue("PCI/URCI",it,com.tuempresa.inventariovial.core.ValidationIssue.Severity.BLOCKING_EXPORT)}))
    Text("✓ Guardado en tablet · ☁ ${com.tuempresa.inventariovial.core.SyncOverview.status(com.tuempresa.inventariovial.core.DriveReceiptStore.state(context,s)).label}")
    HorizontalDivider()
    Text("${if(s.surface=="UNSURFACED")"URCI" else "PCI"}: ${result.index?.toString()?:"no calculable"} ${result.rating.orEmpty()}")
    if(result.pending.isNotEmpty()) {
        Text("Cálculo pendiente: revisa los datos o las curvas validadas.",style=MaterialTheme.typography.bodySmall)
        TextButton(onClick={technicalDetails=!technicalDetails}) { Text(if(technicalDetails)"Ocultar detalle técnico"else"Ver detalle técnico") }
        if(technicalDetails)result.pending.forEach{Text(it,style=MaterialTheme.typography.bodySmall)}
    }
    Row{TextButton(enabled=!busy,onClick={pdf.launch("Pavimentos-${s.muestra}.pdf")}){Text("Reporte PDF")};TextButton(enabled=!busy,onClick={excel.launch("Pavimentos-${s.surface}.xlsx")}){Text("Excel de superficie")}}
    Button(enabled=!busy && s.status=="CLOSED", onClick={scope.launch {
        busy=true; message="Sincronizando pavimentos con Drive…"
        runCatching { PavementDriveUploader.uploadSample(context,repo,s,damages) }
            .onSuccess { message=it.message }
            .onFailure { message="Drive: ${it.message ?: "no se pudo completar la entrega"}" }
        busy=false
    }}, modifier=Modifier.fillMaxWidth()) { Text("Subir muestra a Drive") }
    if(s.status!="CLOSED") Text("Cierra la muestra para habilitar su entrega a Drive.",style=MaterialTheme.typography.bodySmall)
    OutlinedButton(onClick={message="";sectionDialog=true}) { Text("Sección y muestras adicionales") }
    if(message.isNotBlank())Text(message)
    if(sectionDialog) {
        var area by remember { mutableStateOf(section?.areaM2?.toString().orEmpty()) }
        var additional by remember { mutableStateOf(section?.additionalIds()?:emptySet()) }
        val group=samples.filter{it.ruta==s.ruta && it.tramo==s.tramo && it.surface==s.surface}
        AlertDialog(onDismissRequest={sectionDialog=false},title={Text("Sección: ${s.ruta} / ${s.tramo}")},text={
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("Manual pp.55–56, Ec.4–6. Ingresa el área completa de la sección, incluyendo la parte no inspeccionada. Marca únicamente las muestras adicionales; las restantes son representativas. En un censo, no marques adicionales.")
                Field("Área total de sección (m²)",area){area=it}
                group.forEach{sample -> Row {
                    Checkbox(sample.id in additional,{checked->additional=if(checked)additional+sample.id else additional-sample.id})
                    Text("Muestra ${sample.muestra} · ${sample.inspectorNombre}")
                } }
                if(message.isNotBlank())Text(message)
            }
        },confirmButton={TextButton(onClick={scope.launch {
            runCatching{repo.saveSection(PavementSectionEntity(s.ruta,s.tramo,s.surface,area.replace(',','.').toDouble(),additional.sorted().joinToString(","),s.inspectorUserId))}
                .onSuccess{sectionDialog=false;message="Sección guardada. El Excel aplicará las ecuaciones 4–6 cuando las muestras sean calculables."}
                .onFailure{message=it.message?:"Revisa el área."}
        }}){Text("Guardar")}},dismissButton={TextButton(onClick={sectionDialog=false}){Text("Cancelar")}})
    }
}
