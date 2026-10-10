package com.tuempresa.inventariovial.scap.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tuempresa.inventariovial.DriveUploadPolicy
import com.tuempresa.inventariovial.export.SicExcelWriter
import com.tuempresa.inventariovial.scap.data.ScapInspectionSnapshot
import com.tuempresa.inventariovial.scap.export.*
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
internal fun ScapExportPanel(s:ScapInspectionSnapshot,ready:Boolean,controller:ScapController,onCompleteMissing:(com.tuempresa.inventariovial.scap.domain.ScapPendingField)->Unit={}) {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    val exporter=remember(s.values()["bridgeType"]){ScapDelivery.exporter(context,s)}
    val review=remember(s){exporter.review(s)}
    val catalog=remember {com.tuempresa.inventariovial.scap.catalog.ScapCatalog.load(context)}
    val completion=remember(s){com.tuempresa.inventariovial.scap.domain.ScapValidation.review(s,catalog.forSnapshot(s))}
    val completeness=remember(s){com.tuempresa.inventariovial.scap.domain.ScapCompletion.assess(s,catalog.forSnapshot(s))}
    val contractualErrors=if(com.tuempresa.inventariovial.scap.domain.ScapBridgeType.from(s)!=null) completion.errors else emptyList()
    val exportErrors=(review.errors+contractualErrors+completeness.pending.filter{it.severity==com.tuempresa.inventariovial.scap.domain.ScapIssueSeverity.BLOCKING}.map{it.label}).distinct()
    val sic=remember(s){ScapSicExporter.previews(s)}
    var busy by remember{mutableStateOf(false)}
    var layout by remember(s.inspection.id,s.inspection.updatedAt){mutableStateOf<ScapHumanLayout?>(null)}
    var prepared by rememberSaveable{mutableStateOf<String?>(null)}
    var preparedRevision by rememberSaveable{mutableStateOf<Long?>(null)}
    var message by rememberSaveable{mutableStateOf<String?>(null)}
    var warningAction by remember{mutableStateOf<String?>(null)}
    val launcher=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(SicExcelWriter.XLSX_MIME)){uri->
        val path=prepared;prepared=null
        if(uri==null) {path?.let{File(it).delete()};message="Exportación cancelada; la ficha sigue guardada localmente."}
        else if(path!=null) scope.launch {
            busy=true
            try {
                withContext(Dispatchers.IO){
                    context.contentResolver.openOutputStream(uri,"wt").use{out->
                        requireNotNull(out){"No se pudo abrir el destino."}
                        File(path).inputStream().use{it.copyTo(out)}
                    }
                }
                message="Excel guardado. Ábrelo en Excel para recalcular sus fórmulas y revisar los campos pendientes."
                preparedRevision?.let{revision->controller.submit{it.markExcelGenerated(s.inspection.id,revision)}}
                preparedRevision=null
            } catch(e:CancellationException){throw e}
            catch(e:Exception){message="No se pudo guardar el Excel: ${e.message}"}
            finally{busy=false}
        }
    }
    fun export(format:String) {
        scope.launch {
            busy=true;message="Preparando $format…"
            var file:File?=null
            try {
                val timestamp=SimpleDateFormat("yyyyMMdd_HHmmss",Locale.US).format(Date())
                // Local document suggestion only. Does not touch final photo names or Drive routing.
                val target=File(File(context.filesDir,"scap_exports").apply { mkdirs() },"${format}_${timestamp}_${UUID.randomUUID().toString().take(8)}.xlsx")
                file=target
                val saved=controller.savedSnapshot(s.inspection.id)
                if(format=="SCAP") ScapDelivery.writeExcel(context,saved,target)
                else withContext(Dispatchers.IO){target.outputStream().use{ScapSicExporter.write(it,saved,format)}}
                preparedRevision=if(format=="SCAP")saved.inspection.updatedAt else null
                prepared=target.absolutePath;message="Elige dónde guardar $format.";launcher.launch(target.name)
            } catch(e:CancellationException){file?.delete();throw e}
            catch(e:Exception){file?.delete();message="No se pudo preparar $format: ${e.message}"}
            finally{busy=false}
        }
    }
    HorizontalDivider()
    Text("REVISIÓN",style=MaterialTheme.typography.titleLarge)
    Text("Datos completos: ${completeness.complete.size}")
    Text("Pendientes: ${completeness.pending.size}")
    completeness.pending.take(8).forEach {pending->
        TextButton(onClick={onCompleteMissing(pending)}) {Text("${pending.section} · ${pending.field}")}
    }
    if(s.photos.isNotEmpty()) com.tuempresa.inventariovial.core.RecordReviewSection("Fotografías",
        emptyList(),s.photos.sortedBy {it.photoIndex}.map { File(it.originalPath.ifBlank {it.localPath}) },
        com.tuempresa.inventariovial.core.CompletionResult(emptyList()))
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        OutlinedButton(enabled=ready && !busy,onClick={scope.launch {
            controller.submit {it.saveDraft(s.inspection.id);message="Guardado localmente. Pendientes: ${completeness.pending.size}"}
        }}) {Text("Guardar borrador")}
        TextButton(onClick={completeness.pending.firstOrNull()?.let(onCompleteMissing)}) {Text("Completar faltantes")}
    }
    Text("Exportación Excel",style=MaterialTheme.typography.titleMedium)
    Text("La ficha y su Excel se guardan localmente aunque Drive no esté disponible.")
    if(!ready) Text("Espera a que termine el guardado local y resuelve cualquier error antes de exportar.")
    exportErrors.forEach{Text(it,color=MaterialTheme.colorScheme.error)}
    review.warnings.forEach{Text("• $it",style=MaterialTheme.typography.bodySmall)}
    Button(enabled=ready && !busy && prepared==null && exportErrors.isEmpty(),onClick={if(completeness.pending.isNotEmpty())warningAction="EXPORT" else export("SCAP")}){Text("Exportar SCAP")}
    val configurationError=DriveUploadPolicy.configurationError()
    Text("Drive SCAP: " + if(configurationError!=null) "NO CONFIGURADO" else DriveUploadPolicy.statusLabel(s.inspection.syncStatus))
    Text(if(s.values("completion")["excelRevision"]==s.inspection.updatedAt.toString())"✓ Excel generado para esta revisión" else "Excel: pendiente para esta revisión")
    s.values("drive")["message"]?.takeIf{it.isNotBlank()}?.let{Text(it)}
    Text("Se enviarán la ficha Excel, las fotografías originales limpias y los croquis. Cada versión queda identificada por puente e inspección.",style=MaterialTheme.typography.bodySmall)
    Text("Estado de la ficha: ${s.values("completion")["status"] ?: if(completeness.pending.isEmpty())"COMPLETE" else "INCOMPLETE"}. Los pendientes no impiden el respaldo confirmado.")
    OutlinedButton(enabled=ready && !busy && exportErrors.isEmpty(),onClick={scope.launch {
        busy=true
        try {layout=ScapHumanDelivery.prepare(context,com.tuempresa.inventariovial.data.database.InventoryDatabase.getInstance(context),controller.savedSnapshot(s.inspection.id))}
        catch(e:CancellationException){throw e}
        catch(e:Exception){layout=null;message="Entrega sin ubicación completa: se usará el respaldo SCAP v1 existente, bajo SCAP y la identidad del puente o inspección. Los campos faltantes permanecen vacíos."}
        finally{busy=false}
    }}){Text("Preparar entrega SCAP")}
    layout?.let {delivery->
        Text("${delivery.shortCode} · ${com.tuempresa.inventariovial.core.DrivePaths.bridge(context,s)}")
        Text((listOf("SCAP.xlsx","manifest.json")+delivery.photoPaths.values+delivery.sketchPaths.values).joinToString("\n"))
        Text("Las revisiones anteriores se conservan en REVISIONES.")
    }
    fun uploadConfirmed() {scope.launch {
        busy=true
        try {ScapDriveSync.enqueue(context,controller.savedSnapshot(s.inspection.id),layout,warningsConfirmed=true);message="SCAP en cola. Se enviará cuando haya conexión."}
        catch(e:CancellationException){throw e}
        catch(e:Exception){message="No se pudo programar SCAP: ${e.message}"}
        finally{busy=false}
    }}
    OutlinedButton(enabled=ready && !busy && configurationError==null && exportErrors.isEmpty() &&
        s.inspection.syncStatus !in setOf("QUEUED","UPLOADING","SYNCED"),onClick={
        warningAction="UPLOAD"
    }){Text(if(s.inspection.syncStatus=="ERROR") "Reintentar subida SCAP" else "Subir SCAP a Drive")}
    warningAction?.let{action->AlertDialog(onDismissRequest={warningAction=null},title={Text(if(action=="UPLOAD")"Confirmar subida SCAP" else "Exportar ficha incompleta")},
        text={Column {
            Text("Pendientes: ${completeness.pending.size}. Puedes completar la ficha posteriormente en gabinete.")
            if(action=="UPLOAD")Text(if(layout==null)"Destino: respaldo SCAP v1 por identidad del puente/inspección." else "Destino: ${layout!!.path}. Se conserva el contrato SCAP v2 y sus revisiones.")
            TextButton(onClick={warningAction=null;completeness.pending.firstOrNull()?.let(onCompleteMissing)}){Text("Revisar pendientes")}
        }},confirmButton={TextButton(onClick={warningAction=null;if(action=="UPLOAD")uploadConfirmed() else export("SCAP")}){Text(if(action=="UPLOAD")"Subir de todos modos" else "Exportar de todos modos")}},
        dismissButton={TextButton(onClick={warningAction=null}){Text("Cancelar")}})}
    var sicDiagnostic by remember {mutableStateOf(false)}
    sic.forEach{p->
        Card(Modifier.fillMaxWidth().padding(vertical=4.dp)) {
            Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Text(p.format,style=MaterialTheme.typography.titleMedium)
                if(p.missingRequired.isNotEmpty()) Text("Exportación bloqueada. Faltan: ${p.missingRequired.joinToString()}.",color=MaterialTheme.colorScheme.error)
                if(p.missingOptional.isNotEmpty()) Text("Se exportan vacíos: ${p.missingOptional.joinToString()}.")
                if(sicDiagnostic) p.pending.forEach{Text("• $it",style=MaterialTheme.typography.bodySmall)}
                Text("Completa los datos de captura en DATOS PARA SIC. Los pendientes contractuales se identifican en el diagnóstico técnico.",style=MaterialTheme.typography.bodySmall)
                OutlinedButton(enabled=ready && !busy && prepared==null && p.missingRequired.isEmpty(),onClick={export(p.format)}){Text("Guardar ${p.format}")}
            }
        }
    }
    TextButton(onClick={sicDiagnostic=!sicDiagnostic}) {Text("Diagnóstico técnico SIC")}
    message?.let{Text(it)}
}

