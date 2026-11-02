package com.tuempresa.inventariovial.scap.ui

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
import androidx.compose.ui.platform.testTag
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
internal fun ScapExportPanel(s:ScapInspectionSnapshot,ready:Boolean,controller:ScapController,onCompleteMissing:(com.tuempresa.inventariovial.scap.domain.ScapPendingField)->Unit={},
    requestedAction:String?=null,onHandled:()->Unit={},onStatus:(Boolean,String?)->Unit={_,_->}) {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    val localChoices by remember(controller){controller.localDatabase.localFilesDao().choices()}.collectAsState(emptyList())
    val onlyDevice=localChoices.any{it.key=="SCAP/${s.inspection.id}"&&it.destination=="DEVICE"}
    val exporter=remember(s.values()["bridgeType"]){ScapDelivery.exporter(context,s)}
    val review=remember(s){exporter.review(s)}
    val catalog=remember {com.tuempresa.inventariovial.scap.catalog.ScapCatalog.load(context)}
    val completion=remember(s){com.tuempresa.inventariovial.scap.domain.ScapValidation.review(s,catalog.forSnapshot(s))}
    val completeness=remember(s){com.tuempresa.inventariovial.scap.domain.ScapCompletion.assess(s,catalog.forSnapshot(s))}
    val contractualErrors=if(com.tuempresa.inventariovial.scap.domain.ScapBridgeType.from(s)!=null) completion.errors else emptyList()
    val exportErrors=(review.errors+contractualErrors+completeness.pending.filter{it.severity==com.tuempresa.inventariovial.scap.domain.ScapIssueSeverity.BLOCKING}.map{it.label}).distinct()
    val locationIssues=remember(s){ScapDriveLocation.issues(context,s)}
    val sic=remember(s){ScapSicExporter.previews(s)}
    var busy by remember(s.inspection.id){mutableStateOf(false)}
    var layout by remember(s.inspection.id,s.inspection.updatedAt){mutableStateOf<ScapHumanLayout?>(null)}
    var packageBusy by remember(s.inspection.id){mutableStateOf(false)}
    var savedDocumentUri by rememberSaveable(s.inspection.id,s.inspection.updatedAt){mutableStateOf<String?>(null)}
    var savedSicRoute by rememberSaveable(s.inspection.id,s.inspection.updatedAt){mutableStateOf<String?>(null)}
    var savedSicFormat by rememberSaveable(s.inspection.id,s.inspection.updatedAt){mutableStateOf<String?>(null)}
    var message by rememberSaveable(s.inspection.id){mutableStateOf<String?>(null)}
    var warningAction by remember(s.inspection.id){mutableStateOf<String?>(null)}
    var headerWarning by remember(s.inspection.id){mutableStateOf(false)}
    var missingSicFormat by rememberSaveable(s.inspection.id){mutableStateOf<String?>(null)}
    DisposableEffect(Unit){onDispose{onStatus(false,null)}}
    fun export(format:String) {
        if(busy || packageBusy)return
        busy=true;message=if(format=="SCAP")"Preparando SCAP…" else "Actualizando $format…"
        scope.launch {
            busy=true;message=if(format=="SCAP")"Preparando SCAP…" else "Actualizando $format de ${s.roadRecord?.routeCode ?: s.values()["route"].orEmpty()}…"
            try {
                val saved=controller.savedSnapshot(s.inspection.id)
                if(format!="SCAP") {
                    val preview=ScapSicExporter.previews(saved).single{it.format==format}
                    val route=saved.roadRecord?.routeCode?.takeIf{it.isNotBlank()} ?: saved.values()["route"].orEmpty()
                    if(!com.tuempresa.inventariovial.export.LocalBridgeDocument.located(saved)) {
                        savedDocumentUri=com.tuempresa.inventariovial.export.LocalBridgeDocument.provisional(context,controller.localDatabase,saved,format).toString()
                        savedSicRoute=null;savedSicFormat=null
                        message="ARCHIVO GUARDADO · $format PROVISIONAL. Ubicación pendiente; separado del consolidado de ruta."
                    } else {
                        val result=com.tuempresa.inventariovial.export.LocalSicConsolidates.update(context,controller.localDatabase,route,format,selectedInspection=saved.inspection.id)
                        savedDocumentUri=result.uri.toString();savedSicRoute=route;savedSicFormat=format;message="ARCHIVO GUARDADO · "+result.message
                    }
                    missingSicFormat=format.takeIf{preview.missingRequired.isNotEmpty()}
                    if(preview.missingRequired.isNotEmpty())message=message.orEmpty()+" · ${preview.missingRequired.size} datos por completar. Consulta Diagnóstico técnico SIC."
                    // Explicit document export is local; the original-record outbox owns Drive.
                    return@launch
                }
                controller.localScapPackage(saved.inspection.id).await()
                message="Preparando SCAP…"
            } catch(e:CancellationException){throw e}
            catch(e:Exception){message="No se pudo preparar $format: "+com.tuempresa.inventariovial.localfiles.LocalCopyDrain.publicError(e)}
            finally{busy=false}
        }
    }
    LaunchedEffect(busy,packageBusy,message){onStatus(busy || packageBusy,message)}
    LaunchedEffect(requestedAction) {
        val action=requestedAction?.substringBefore(':') ?: return@LaunchedEffect
        onHandled()
        if(!ready || busy || packageBusy){message="Espera el guardado o la exportación en curso.";return@LaunchedEffect}
        if(action=="SCAP") {
            // A local provisional copy preserves even incomplete/invalid raw capture.
            // Remote/contractual validation is independent and remains enforced.
            if(exportErrors.isNotEmpty() || completeness.pending.isNotEmpty() || s.photos.size>32)headerWarning=true else export(action)
        } else {
            val p=sic.firstOrNull{it.format==action}
            if(p==null)message="Formato SIC no disponible."

            else export(action)
        }
    }
    Text(if(s.inspection.status=="COMPLETE")"Ficha cerrada y guardada en GBO" else "Ficha guardada en GBO; puedes continuar completándola")
    Text("Generar un Excel local no confirma su entrega a Drive.",style=MaterialTheme.typography.bodySmall)
    missingSicFormat?.let{format->
        val missingDate=format=="SIC-17" && sic.first{it.format==format}.missingRequired.contains("Fecha")
        if(missingDate)Text("Falta fecha de inspección para SIC-17")
        TextButton(modifier=Modifier.testTag("complete-missing-sic"),onClick={controller.navigate(com.tuempresa.inventariovial.scap.domain.ScapFieldRoute("SIC",repeatableItemId=format,fieldKey=if(missingDate)"surveyDate"else""))}){Text(if(missingDate)"COMPLETAR FECHA"else"Completar $format")}
    }
    ScapLocalExportStatus(s.inspection.id,s.inspection.updatedAt,controller){running,text->packageBusy=running;message=text}
    com.tuempresa.inventariovial.core.AutomaticSyncPanel("SCAP",s.inspection.id,s.inspection.status=="COMPLETE",compact=true,database=controller.localDatabase)
    if(headerWarning)AlertDialog(onDismissRequest={headerWarning=false},title={Text("Confirmar exportación local")},text={Column {
        Text("Datos pendientes: ${completeness.pending.size}. Se conserva una copia provisional cuando falten datos; no se anuncia como contractual completa.")
        if(s.photos.size>32)Text("${s.photos.size} fotografías: el paquete ZIP conserva F.2 con 32, un índice de continuación y todas las evidencias. No se modifica la plantilla ni se confirma Drive.")
    }},confirmButton={TextButton(onClick={headerWarning=false;export("SCAP")}){Text("Preparar copia local")}},dismissButton={TextButton(onClick={headerWarning=false}){Text("Volver")}})
    if(!ready)Text("Espera el guardado local antes de exportar.")
    var technical by rememberSaveable(s.inspection.id){mutableStateOf(false)}
    TextButton(onClick={technical=!technical}){Text(if(technical)"Ocultar opciones y diagnóstico"else"Opciones de exportación y diagnóstico")}
    if(technical) {
    if(s.photos.isNotEmpty()) com.tuempresa.inventariovial.core.RecordReviewSection("Fotografías",
        emptyList(),s.photos.sortedBy {it.photoIndex}.map { File(it.originalPath.ifBlank {it.localPath}) },
        com.tuempresa.inventariovial.core.CompletionResult(emptyList()))
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        OutlinedButton(enabled=ready && !busy,onClick={scope.launch {
            controller.submit {it.saveDraft(s.inspection.id);message="Ficha guardada dentro de GBO. Pendientes: ${completeness.pending.size}"}
        }}) {Text("Guardar borrador")}
        TextButton(onClick={completeness.pending.firstOrNull()?.let(onCompleteMissing)}) {Text("Completar faltantes")}
    }
    Text("Exportación Excel",style=MaterialTheme.typography.titleMedium)
    Text("Exportar SCAP prepara y verifica el paquete en la ubicación local autorizada. Guardar como… permite elegir otro destino desde el historial.")
    if(!ready) Text("Espera a que termine el guardado local y resuelve cualquier error antes de exportar.")
    exportErrors.forEach{Text(it,color=MaterialTheme.colorScheme.error)}
    review.warnings.forEach{Text("• $it",style=MaterialTheme.typography.bodySmall)}
    Button(enabled=ready && !busy && !packageBusy && (!packageBusy),onClick={if(completeness.pending.isNotEmpty() || exportErrors.isNotEmpty())headerWarning=true else export("SCAP")}){Text("Exportar SCAP")}
    if(locationIssues.isNotEmpty()){
        Text(ScapDriveLocation.MESSAGE,color=MaterialTheme.colorScheme.error)
        locationIssues.forEach{Text("• $it")}
        TextButton(onClick={controller.navigate(com.tuempresa.inventariovial.scap.domain.ScapFieldRoute("SIC",fieldKey="startPrCode"))}){Text("Ir a ubicación")}
        TextButton(enabled=ready,onClick={scope.launch{
            message=runCatching{com.tuempresa.inventariovial.localfiles.LocalCopies.requestCurrent(context,com.tuempresa.inventariovial.data.database.InventoryDatabase.getInstance(context),"SCAP",s.inspection.id);"Actualizando archivos locales… sin esperar PR ni Drive"}.getOrElse{it.message}
        }}){Text("Guardar copia local")}
    }
    val configurationError=DriveUploadPolicy.configurationError()
    com.tuempresa.inventariovial.core.AutomaticSyncPanel("SCAP",s.inspection.id,s.inspection.status=="COMPLETE",database=controller.localDatabase)
    Text(if(s.values("completion")["excelRevision"]==s.inspection.updatedAt.toString())"✓ Excel generado para esta revisión" else "Excel: pendiente para esta revisión")
    s.values("drive")["message"]?.takeIf{it.isNotBlank()}?.let{Text(it)}
    Text("Se enviarán la ficha Excel, las fotografías originales limpias y los croquis. Cada versión queda identificada por puente e inspección.",style=MaterialTheme.typography.bodySmall)
    Text("Estado de la ficha: ${s.values("completion")["status"] ?: if(completeness.pending.isEmpty())"COMPLETE" else "INCOMPLETE"}. Los pendientes no impiden el respaldo confirmado.")
    OutlinedButton(enabled=ready && !busy && exportErrors.isEmpty() && locationIssues.isEmpty(),onClick={scope.launch {
        busy=true
        try {layout=ScapHumanDelivery.prepare(context,com.tuempresa.inventariovial.data.database.InventoryDatabase.getInstance(context),controller.savedSnapshot(s.inspection.id))}
        catch(e:CancellationException){throw e}
        catch(e:Exception){layout=null;message="No se preparó la entrega: "+com.tuempresa.inventariovial.localfiles.LocalCopyDrain.publicError(e)+". Puedes guardar el paquete provisional sin inventar ubicación."}
        finally{busy=false}
    }}){Text("Preparar entrega SCAP")}
    layout?.let {delivery->
        Text("${delivery.shortCode} · ${delivery.routeCode}/PUENTES/PUENTE N (el servidor confirma el número)")
        Text((listOf("SCAP.xlsx","manifest.json")+delivery.photoPaths.values+delivery.sketchPaths.values).joinToString("\n"))
        Text("Las revisiones anteriores se conservan en REVISIONES.")
    }
    fun uploadConfirmed() {scope.launch {
        busy=true
        try {com.tuempresa.inventariovial.core.AutomaticSync.manual(context,"SCAP",s.inspection.id,warnings=true);message="SCAP en cola. Se enviará cuando haya conexión."}
        catch(e:CancellationException){throw e}
        catch(e:Exception){message="No se pudo programar SCAP: ${e.message}"}
        finally{busy=false}
    }}
    OutlinedButton(enabled=ready && !busy && configurationError==null && exportErrors.isEmpty() &&
        s.inspection.status=="COMPLETE" && !onlyDevice && locationIssues.isEmpty(),onClick={
        warningAction="UPLOAD"
    }){Text(if(s.inspection.syncStatus=="ERROR") "Reintentar subida SCAP" else "Subir SCAP a Drive")}
    warningAction?.let{action->AlertDialog(onDismissRequest={warningAction=null},title={Text(if(action=="UPLOAD")"Confirmar subida SCAP" else "Exportar ficha incompleta")},
        text={Column {
            Text("Pendientes: ${completeness.pending.size}. Puedes completar la ficha posteriormente en gabinete.")
            if(action=="UPLOAD")Text("Destino: RUTA/PUENTES/PUENTE N. El servicio confirma el orden y conserva las revisiones.")
            TextButton(onClick={warningAction=null;completeness.pending.firstOrNull()?.let(onCompleteMissing)}){Text("Revisar pendientes")}
        }},confirmButton={TextButton(onClick={warningAction=null;if(action=="UPLOAD")uploadConfirmed() else export("SCAP")}){Text(if(action=="UPLOAD")"Subir de todos modos" else "Exportar de todos modos")}},
        dismissButton={TextButton(onClick={warningAction=null}){Text("Cancelar")}})}
    var sicDiagnostic by remember {mutableStateOf(false)}
    sic.forEach{p->
        val enabled=com.tuempresa.inventariovial.core.SicContributionPolicy.enabled(p.format)
        Card(Modifier.fillMaxWidth().padding(vertical=4.dp)) {
            Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Text(p.format,style=MaterialTheme.typography.titleMedium)
                if(!enabled)Text("Aporte opcional deshabilitado para la entrega. Los datos existentes se conservan.")
                else if(p.missingRequired.isNotEmpty())Text("${p.format}: datos por completar. La copia local será provisional; consulta Diagnóstico técnico SIC.",color=MaterialTheme.colorScheme.error)
                if(sicDiagnostic&&p.missingRequired.isNotEmpty())Text("${if(enabled)"Campos pendientes"else"Campos históricos sin exigir en esta entrega"} de ${p.format}: ${p.missingRequired.joinToString()}")
                if(sicDiagnostic&&p.missingOptional.isNotEmpty()) Text("Se exportan vacíos: ${p.missingOptional.joinToString()}.")
                if(sicDiagnostic) p.pending.forEach{Text("• $it",style=MaterialTheme.typography.bodySmall)}
                Text("Actualiza un único Excel por ruta con las filas locales válidas, conservando la última versión válida de las fichas en corrección. No contiene datos de otras tablets que no se hayan recibido.",style=MaterialTheme.typography.bodySmall)
                OutlinedButton(enabled=ready && !busy && !packageBusy,onClick={export(p.format)}){Text("Exportar ${p.format}")}
                Text("Se conservan las columnas y estilos. Los datos pendientes no bloquean la copia local; los registros sin ubicación se guardan separados.",style=MaterialTheme.typography.bodySmall)
            }
        }
    }
    TextButton(onClick={onCompleteMissing(com.tuempresa.inventariovial.scap.domain.ScapPendingField("E","Revisar croquis y principal","inspection","ELEVATION"))}){Text("Revisar croquis y anexos")}
    TextButton(onClick={onCompleteMissing(com.tuempresa.inventariovial.scap.domain.ScapPendingField("A","Revisar progresiva","inspection","progressive"))}){Text("Revisar ubicación para Drive")}

    TextButton(onClick={sicDiagnostic=!sicDiagnostic}) {Text("Diagnóstico técnico SIC")}
    }
    message?.takeIf{ready || !it.startsWith("Ficha guardada") }?.let{Text(it,Modifier.testTag("scap-export-result"))}
    savedDocumentUri?.let{savedUri->TextButton(onClick={scope.launch {
        try {
            val uri=if(savedSicRoute!=null&&savedSicFormat!=null)com.tuempresa.inventariovial.export.LocalSicConsolidates.current(context,savedSicRoute!!,savedSicFormat!!).uri else android.net.Uri.parse(savedUri)
            context.startActivity(com.tuempresa.inventariovial.localfiles.SharedFileActions.view(uri,SicExcelWriter.XLSX_MIME).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
        }catch(e:CancellationException){throw e}catch(e:Exception){message="No se pudo abrir el Excel: "+com.tuempresa.inventariovial.localfiles.LocalCopyDrain.publicError(e)+". El respaldo se conserva."}
    }
    }){Text("ABRIR ARCHIVO")}
        TextButton(onClick={
            val uri=android.net.Uri.parse(savedUri)
            runCatching {
                val intent=com.tuempresa.inventariovial.localfiles.SharedFileActions.location(uri)
                    ?: com.tuempresa.inventariovial.localfiles.SharedFileActions.view(uri,SicExcelWriter.XLSX_MIME)
                        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                message="Android abre la ubicación disponible; puede ser Descargas general. ABRIR ARCHIVO accede al Excel exacto. No se garantiza seleccionar su carpeta."
            }.onFailure{message="Usa ABRIR ARCHIVO o Archivos → Descargas para localizar el Excel guardado."}
        }){Text("ABRIR UBICACIÓN DISPONIBLE")}

    }
    var filesVisible by rememberSaveable(s.inspection.id){mutableStateOf(false)}
    TextButton(onClick={filesVisible=!filesVisible}){Text(if(filesVisible)"Ocultar archivos locales" else "Abrir archivos locales e historial")}
    if(filesVisible)com.tuempresa.inventariovial.localfiles.LocalFilesList("SCAP/${s.inspection.id}")
}
