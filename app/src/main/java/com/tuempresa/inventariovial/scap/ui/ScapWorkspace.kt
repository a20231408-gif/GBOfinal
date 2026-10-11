package com.tuempresa.inventariovial.scap.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.tuempresa.inventariovial.access.DeviceAccessManager
import com.tuempresa.inventariovial.location.GeoLocation
import com.tuempresa.inventariovial.location.TabletLocationProvider
import com.tuempresa.inventariovial.scap.calculator.*
import com.tuempresa.inventariovial.scap.catalog.ScapCatalog
import com.tuempresa.inventariovial.scap.data.*
import com.tuempresa.inventariovial.scap.domain.*
import com.tuempresa.inventariovial.viewmodel.InventoryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal val scapSections=linkedMapOf("A" to "Identificación", "B" to "Datos generales", "C1" to "Resumen del puente",
    "C2" to "Tramos", "C3" to "Tablero", "C4" to "Subestructura", "C5" to "Detalles", "C6" to "Accesos",
    "C7" to "Seguridad vial", "C8" to "Cargas", "C9" to "Cruces alternativos", "C10" to "Estado de la vía",
    "D1" to "Suelos", "D2" to "Niveles de agua", "D3" to "Hidráulica", "D4" to "Perfil del cauce",
    "E" to "Croquis", "F1" to "Condición global del puente", "F2" to "Panel fotográfico",
    "G1" to "Condición estadística", "G2" to "Observaciones y recomendaciones", "SIC" to "Datos para SIC", "REVIEW" to "Revisión y exportación")

@Composable
fun ScapWorkspace(vm:InventoryViewModel,onBack:()->Unit) {
    val c=vm.scap
    val catalog by c.catalog.collectAsState()
    val selected by c.selected.collectAsState()
    val snapshot by c.snapshot.collectAsState()
    val inspections by c.inspections.collectAsState()
    val session by vm.field.session.collectAsState()
    val pending by c.pending.collectAsState()
    val error by c.error.collectAsState()
    val saved by c.saveState.collectAsState()
    val context=LocalContext.current
    val goBack:()->Unit={if(selected!=null)c.select(null) else onBack()}
    BackHandler(onBack=goBack)
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal=12.dp),horizontalArrangement=Arrangement.SpaceBetween) {
            TextButton(onClick=goBack){Text(if(selected==null) "← Inicio" else "← Inspecciones")}
            Text("SCAP · Puentes",modifier=Modifier.padding(12.dp),style=MaterialTheme.typography.titleMedium)
        }
        if(error!=null) Card(Modifier.fillMaxWidth().padding(horizontal=12.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.errorContainer)) {
            Text(error.orEmpty(),Modifier.padding(12.dp))
            TextButton(onClick=c::retry,enabled=pending==0){Text("Reintentar guardado")}
        }
        val cat=catalog
        if(cat==null) {Text("Cargando catálogo SCAP…",Modifier.padding(16.dp));return@Column}
        if(selected==null) {
            Text("TIPO DE PUENTE",Modifier.padding(horizontal=16.dp),style=MaterialTheme.typography.titleLarge)
            Row(Modifier.fillMaxWidth().padding(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                ScapBridgeType.entries.forEach {type->Button(enabled=pending==0,onClick={c.submit {repo->
                    val captureSession=vm.field.dao.currentSession()
                    val id=repo.create(captureSession?.operator.orEmpty(),captureSession?.device ?: DeviceAccessManager(context).fingerprint(),captureSession?.sessionId,type)
                    c.select(id)
                }},modifier=Modifier.weight(1f)){Text(type.label)}}
            }
            Text("Las inspecciones se guardan en este dispositivo. Abre una ficha para continuarla o revisar su cierre.",Modifier.padding(16.dp))
            LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                if(inspections.isEmpty()) item {Text("Aún no hay inspecciones SCAP.")}
                items(inspections,key={it.id}) {i->Card(onClick={c.select(i.id)},modifier=Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(i.bridgeName.ifBlank {"Puente sin nombre"},style=MaterialTheme.typography.titleMedium)
                        Text("${i.bridgeCode.ifBlank {"Código pendiente"}} · ${statusLabel(i.status)}")
                        com.tuempresa.inventariovial.core.AutomaticSyncPanel("SCAP",i.id,i.status=="COMPLETE",compact=true)
                        Text("Último cambio: ${SimpleDateFormat("dd/MM/yyyy HH:mm",Locale.getDefault()).format(Date(i.updatedAt))}")
                    }
                }}
            }
        } else {
            val s=snapshot?.takeIf {it.inspection.id==selected}
            if(s==null) Text("Abriendo inspección…",Modifier.padding(16.dp))
            else ScapEditor(s,c,cat.forSnapshot(s),vm,saved,pending,error)
        }
    }
}

internal fun statusLabel(status:String)=when(status){"COMPLETE"->"Completa";"IN_PROGRESS"->"En progreso";else->"Borrador"}

@Composable
internal fun ScapEditor(s:ScapInspectionSnapshot,c:ScapController,catalog:ScapCatalog,vm:InventoryViewModel,saved:String,pending:Int,error:String?) {
    var section by rememberSaveable(s.inspection.id){mutableStateOf("A")}
    var photoElement by rememberSaveable(s.inspection.id){mutableStateOf<String?>(null)}
    val destination by c.destination.collectAsState()
    LaunchedEffect(destination){destination?.second?.let{section=if(it.subsection=="FOTO")"F2" else it.subsection}}
    LaunchedEffect(section) {
        if(section=="F3") section="F2"
        if(section=="G") section="REVIEW"
    }
    var confirm by remember {mutableStateOf(false)}
    var savedDraft by remember(s.inspection.id){mutableStateOf(false)}
    var showPending by remember(s.inspection.id){mutableStateOf(false)}
    val values=scapValues(s,c,"inspection")
    val editable=s.inspection.status!="COMPLETE"
    val review=remember(s,catalog){ScapValidation.review(s,catalog)}
    val completeness=remember(s,catalog){ScapCompletion.assess(s,catalog)}
    val sectionOrder=ScapBridgeType.from(s)?.let{ScapCaptureModels.forType(it).sections} ?: scapSections.keys.toList()
    Column(Modifier.fillMaxSize()) {
        Surface(color=MaterialTheme.colorScheme.primary,contentColor=MaterialTheme.colorScheme.onPrimary) {
            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                Text("SCAP · " + values["bridgeName"].orEmpty().ifBlank {"Puente sin nombre"},style=MaterialTheme.typography.titleLarge)
                if(catalog.generalHeader.isNotBlank()) Text(catalog.generalHeader,style=MaterialTheme.typography.labelSmall)
                ScapBridgeType.from(s)?.let{Text("Tipo de puente: ${it.label}")}
                Text("Ruta: ${values["route"].orEmpty().ifBlank {"Pendiente"}} · Progresiva: ${values["progressive"].orEmpty().ifBlank {"Pendiente"}}")
                Text("$section · ${scapSections[section]} · ${statusLabel(s.inspection.status)}")
                Text(saved,style=MaterialTheme.typography.labelMedium)
            }
        }
        if(savedDraft) Row(Modifier.fillMaxWidth().padding(horizontal=12.dp),horizontalArrangement=Arrangement.SpaceBetween) {
            Text("Guardado localmente. Pendientes: ${completeness.pending.size}",Modifier.weight(1f).padding(top=12.dp))
            TextButton(onClick={showPending=true}){Text("Ver pendientes")}
        }
        LazyRow(contentPadding=PaddingValues(horizontal=12.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            items(listOf("A","B","C","D","E","F","G","SIC","REVIEW")) {group->FilterChip(
                selected=if(group=="REVIEW") section=="REVIEW" else section.startsWith(group),
                onClick={section=if(group=="REVIEW") "REVIEW" else sectionOrder.first{it.startsWith(group)}},
                label={Text(if(group=="REVIEW") "REVISIÓN" else if(group=="SIC") "DATOS PARA SIC" else group)})}
        }
        val subsections=if(section=="REVIEW") listOf("REVIEW") else sectionOrder.filter{it.startsWith(section.take(1)) && it!="REVIEW"}
        if(subsections.size>1) LazyRow(contentPadding=PaddingValues(horizontal=12.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            items(subsections) {key->FilterChip(section==key,{section=key},label={Text(when(key) {
                "FOTO"->"Panel fotográfico"
                else->"${key.take(1)}.${key.drop(1)} · ${scapSections[key]}"
            })})}
        }
        if(!editable) Row(Modifier.padding(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("Ficha cerrada",Modifier.weight(1f).padding(top=12.dp))
            OutlinedButton(enabled=pending==0,onClick={c.submit {it.reopen(s.inspection.id)}}){Text("Reabrir para editar")}
        }
        if(!editable) Text(com.tuempresa.inventariovial.DriveUploadPolicy.SCAP_LOCAL_MESSAGE,Modifier.padding(horizontal=16.dp),style=MaterialTheme.typography.bodySmall)
        Box(Modifier.weight(1f)) {
            CompositionLocalProvider(LocalScapDestination provides destination) {
            when(section) {
                "A"->ScapIdentification(s,c,catalog,vm,editable)
                "E"->ScapMedia(s,c,catalog,editable,true)
                "F1"->ScapElementPresencePanel(s,c,catalog,editable)
                "F2"->ScapMedia(s,c,catalog,editable,false,photoElement)
                "FOTO"->ScapMedia(s,c,catalog,editable,false,photoElement)
                "G1"->ScapStatisticsPanel(s,c,catalog,editable)
                "G2"->ScapRecommendationsPanel(s,c,editable)
                "SIC"->Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {ScapSicSupplementPanel(s,c,editable)}
                "REVIEW"->ScapReviewPanel(s,catalog,review,pending==0 && error==null,c) {c.navigate(it.route)}
                else->ScapTechnicalSection(s,c,catalog,section,editable)
            }
            }
        }
        if(editable) Row(Modifier.fillMaxWidth().padding(12.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick={c.submit{it.saveDraft(s.inspection.id);savedDraft=true}},modifier=Modifier.weight(1f)){Text("Guardar borrador")}
            Button(enabled=pending==0 && error==null,onClick={section="REVIEW";confirm=true},modifier=Modifier.weight(1f)){Text("Finalizar ficha")}
        }
    }
    if(confirm) AlertDialog(onDismissRequest={confirm=false},title={Text("Confirmar cierre SCAP")},
        text={Column(Modifier.heightIn(max=420.dp).verticalScroll(rememberScrollState())) {
            Text("${s.inspection.bridgeName} · ${s.inspection.bridgeCode}")
            Text("${s.spans.size} tramos · ${review.present} elementos · ${review.unevaluated} sin evaluar")
            Text("${s.defects.size} defectos · ${s.photos.size} fotografías · ${s.sketches.size} croquis")
            if(review.errors.isNotEmpty()) {Text("Corrige antes de cerrar:",color=MaterialTheme.colorScheme.error);review.errors.forEach{Text("• $it")}}
            Text("Datos pendientes: ${review.pending.size}")
            review.pending.forEach{Text("• $it")}
            completeness.pending.forEach {field->TextButton(onClick={c.navigate(field.route);confirm=false}) {Text("${field.section} · ${field.field}")}}
        }},confirmButton={TextButton(enabled=review.errors.isEmpty() && pending==0 && error==null,onClick={c.submit {it.complete(s.inspection.id,warningsConfirmed=true)};confirm=false}){Text(if(completeness.pending.isEmpty()) "Finalizar y programar envío" else "Confirmar cierre y envío con pendientes")}},
        dismissButton={TextButton(onClick={confirm=false}){Text("Volver a la ficha")}})
    if(showPending) AlertDialog(onDismissRequest={showPending=false},title={Text("SCAP · Pendientes ${completeness.pending.size}")},
        text={Column(Modifier.heightIn(max=420.dp).verticalScroll(rememberScrollState())) {
            completeness.pending.forEach {field->TextButton(onClick={c.navigate(field.route);showPending=false}) {Text("${field.section} · ${field.field}")}}
        }},confirmButton={TextButton(onClick={showPending=false}){Text("Volver")}})
}

@Composable
private fun ScapIdentification(s:ScapInspectionSnapshot,c:ScapController,catalog:ScapCatalog,vm:InventoryViewModel,editable:Boolean) {
    val context=LocalContext.current
    val provider=remember {TabletLocationProvider(context)}
    var fix by remember {mutableStateOf<GeoLocation?>(null)}
    var locationMessage by remember {mutableStateOf<String?>(null)}
    var locating by remember {mutableStateOf(false)}
    val requestFix:()->Unit={locating=true;locationMessage=null;provider.getCurrentLocation({fix=it;locating=false},{locationMessage=it;locating=false})}
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){if(it)requestFix() else locationMessage="Permiso de ubicación no concedido. Puedes seguir completando la ficha."}
    val values=scapValues(s,c,"inspection")
    val session by vm.field.session.collectAsState()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(values["createdBy"] ?: s.inspection.createdBy,{c.edit(s.inspection.id,"inspection","createdBy",it)},label={Text("Inspector")},enabled=editable,modifier=Modifier.fillMaxWidth())
        if(session?.road!=null) OutlinedButton(enabled=editable,onClick={
            session?.road?.let{c.edit(s.inspection.id,"inspection","route",it,"SESSION_ACCEPTED")}
        }) {Text("Usar ruta de la sesión: ${session?.road}")}
        Text("GPS inicial",style=MaterialTheme.typography.titleMedium)
        OutlinedButton(enabled=editable && !locating,onClick={
            if(ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED) requestFix()
            else permission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }){Text(if(locating) "Obteniendo ubicación…" else "Obtener ubicación GNSS")}
        locationMessage?.let{Text(it)}
        fix?.let {f->
            Text("Propuesta GNSS: ${f.latitude}, ${f.longitude} · precisión ${f.horizontalAccuracy} m")
            Button(enabled=editable,onClick={c.submit{it.setLocation(s.inspection.id,f)};fix=null}){Text("Aceptar ubicación GNSS")}
        }
        s.inspection.latitude?.let{Text("GNSS guardado: $it, ${s.inspection.longitude} · precisión ${s.inspection.gpsAccuracyM} m")}
        Text(if(catalog.bridgeType==null) "Las coordenadas UTM de la ficha se ingresan manualmente; la captura GNSS conserva latitud y longitud." else "Latitud y longitud de la ficha se ingresan en grados decimales. Aceptar GNSS conserva la ubicación observada.")
        ScapFields(s,c,catalog,"A",editable=editable)
        ScapSicSupplementPanel(s,c,editable)
    }
}

@Composable
private fun ScapReviewPanel(s:ScapInspectionSnapshot,catalog:ScapCatalog,review:ScapReview,ready:Boolean,controller:ScapController,onNavigate:(ScapPendingField)->Unit) {
    val v=s.values()
    val result=remember(s){ScapConditionCalculator.calculate(ScapElementView.applicable(s).map {e->
        ScapCalculationInput(e.element.elementCode,ScapElementView.factor(s,e.element),e.condition?.let{ScapPercentages.values(it)} ?: List(6){null})
    })}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text("Revisión de inspección",style=MaterialTheme.typography.titleLarge)
        Text("${s.inspection.bridgeName} · ${s.inspection.bridgeCode} · ${v["route"].orEmpty()} · ${v["progressive"].orEmpty()}")
        Text("${s.spans.size} tramos · ${s.substructures.count{it.kind=="PIER"}} pilares · ${s.supports.size} apoyos")
        Text("${review.present} elementos presentes · ${review.unevaluated} sin evaluar · ${s.defects.size} defectos · ${s.photos.size} fotos · ${s.sketches.size} croquis")
        Text("Datos generales",style=MaterialTheme.typography.titleMedium)
        catalog.fields("B","inspection").forEach {Text("${it.label}: ${v[it.key].orEmpty().ifBlank{"Pendiente"}}")}
        s.spans.sortedBy{it.spanIndex}.forEach {Text("Tramo ${it.spanIndex}: ${it.category} / ${it.type} · ${it.lengthM ?: "—"} m")}
        Text("Elementos seleccionados",style=MaterialTheme.typography.titleMedium)
        s.elements.filter{it.element.isPresent}.forEach{e->Text("${e.element.elementCode} · ${e.element.description} · ${e.element.quantity ?: "Metrado pendiente"} ${e.element.unit}")}
        Text("Defectos registrados",style=MaterialTheme.typography.titleMedium)
        s.defects.forEach{Text("${it.elementCode ?: "General"} · ${it.description} · ${it.locationDescription}")}
        HorizontalDivider()
        Text("Resumen de G.1 · Condición estadística",style=MaterialTheme.typography.titleMedium)
        if(result.value!=null) Text("${String.format(Locale.US,"%.3f",result.value)} · ${result.classification}",style=MaterialTheme.typography.headlineMedium)
        else Text("Sin resultado: ${result.reason}")
        Text("Resultado calculado en G.1 con la fórmula de la plantilla SCAP. Esta pantalla solo resume la revisión y exportación.")
        result.elements.forEach {Text("${it.code} · condición ${String.format(Locale.US,"%.3f",it.condition)} · contribución ${String.format(Locale.US,"%.3f",it.contribution)}")}
        Text("Errores que impiden cerrar (${review.errors.size})",style=MaterialTheme.typography.titleMedium)
        review.errors.forEach{Text("• $it",color=MaterialTheme.colorScheme.error)}
        Text("Pendientes para revisión (${review.pending.size})",style=MaterialTheme.typography.titleMedium)
        ScapCompletion.assess(s,catalog).pending.forEach{field->TextButton(onClick={onNavigate(field)}) {Text("${field.section} · ${field.field}")}}
        com.tuempresa.inventariovial.core.RecordReviewSection("Revisión de fotos",
            listOf("Puente" to s.inspection.bridgeName),s.photos.sortedBy {it.photoIndex}.map {java.io.File(it.originalPath.ifBlank { it.localPath })},
            com.tuempresa.inventariovial.core.CompletionResult(review.errors.map {com.tuempresa.inventariovial.core.ValidationIssue("SCAP",it,com.tuempresa.inventariovial.core.ValidationIssue.Severity.BLOCKING_EXPORT)}))
        ScapSicSupplementPanel(s,controller,s.inspection.status!="COMPLETE")
        ScapExportPanel(s,ready,controller,onCompleteMissing=onNavigate)
    }
}
