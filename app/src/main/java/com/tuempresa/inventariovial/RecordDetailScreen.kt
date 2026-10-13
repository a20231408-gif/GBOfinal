package com.tuempresa.inventariovial

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tuempresa.inventariovial.core.*
import com.tuempresa.inventariovial.dashboard.DashboardDrafts
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.export.*
import com.tuempresa.inventariovial.field.*
import com.tuempresa.inventariovial.sic.*
import com.tuempresa.inventariovial.supplementary.*
import com.tuempresa.inventariovial.viewmodel.InventoryViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.text.DateFormat
import java.util.Date

/** A successful edit returns to the already-open detail instead of stacking another detail. */
val LocalRecordSaved = staticCompositionLocalOf<((String) -> Unit)?> { null }
val LocalReviewOnOpen = staticCompositionLocalOf { false }

@Composable fun rememberRecordReview(key:Any?=null):MutableState<Boolean> {
    val initial=LocalReviewOnOpen.current
    return rememberSaveable(key) {mutableStateOf(initial)}
}

@Composable
fun RecordDetailScreen(vm: InventoryViewModel, id: String, initialPage:String="DETAIL", onBack: () -> Unit) {
    val context = LocalContext.current
    val db = remember(context) { InventoryDatabase.getInstance(context) }
    val loaded by remember(db, id) { db.inventoryDao().observeSnapshot(id).map { true to it } }.collectAsState(false to null)
    val snapshot=loaded.second
    val intents by remember(db) { db.automaticSyncDao().observe() }.collectAsState(emptyList())
    val ordered by vm.sequencedHistory.collectAsState()
    var page by rememberSaveable(id) { mutableStateOf(initialPage) }
    var supplemental by remember(id) { mutableStateOf<SupplementaryFormat?>(null) }
    var message by remember(id) { mutableStateOf<String?>(null) }
    var deliveryExpanded by rememberSaveable(id) {mutableStateOf(false)}
    val back = { if (page == "DETAIL") onBack() else page = "DETAIL" }
    BackHandler(onBack = back)
    val s = snapshot
    if (s == null) {
        Column(Modifier.padding(24.dp)) { OutlinedButton(onClick=onBack) { Text("Volver") }; Text(if(loaded.first) "El registro ya no está disponible." else "Cargando registro…") }
        return
    }
    val r = s.record
    if(r.sicCode=="SCAP") {
        var inspectionId by remember(id) {mutableStateOf<String?>(null)}
        LaunchedEffect(id) {inspectionId=db.scapDao().forRoadRecord(id)?.id}
        inspectionId?.let {inspection->com.tuempresa.inventariovial.scap.ui.ScapWorkspace(vm,initialInspectionId=inspection,onBack=onBack)}
            ?: Text("Abriendo inspección SCAP…")
        return
    }
    val sync = intents.find { it.key == "SIC/$id" }
    supplemental?.let { format -> SupplementaryScreen(vm,r,format) { supplemental=null }; return }
    if (page == "TECHNICAL" || page == "REVIEW") {
        val format = SicFieldFormat.fromCode(r.sicCode)
        CompositionLocalProvider(LocalRecordSaved provides { _:String -> page="DETAIL" },LocalReviewOnOpen provides (page=="REVIEW")) {
        when {
            r.sicCode == "SIC-18" -> CulvertUnifiedScreen(vm,id,onBack={page="DETAIL"})
            format != null -> InventoryElementWorkspace(vm,format,id) { page="DETAIL" }
            else -> if(r.status=="DRAFT" && page!="REVIEW") {
                if(r.sicCode in setOf("SIC-21","SIC-22")) {
                    val signal=com.tuempresa.inventariovial.model.SignalizationType.entries.find {it.name==r.assetType} ?: when {r.sicCode=="SIC-22"->com.tuempresa.inventariovial.model.SignalizationType.VERTICAL;s.sic21?.classCode=="20"->com.tuempresa.inventariovial.model.SignalizationType.HORIZONTAL_STUDS;s.sic21?.classCode=="19"->com.tuempresa.inventariovial.model.SignalizationType.SAFETY;else->com.tuempresa.inventariovial.model.SignalizationType.HORIZONTAL_MARKS}
                    SignalizationFormScreen(signal,vm,{page="DETAIL"},{page="DETAIL"},r)
                } else {
                    val type=when(r.sicCode){"SIC-17"->com.tuempresa.inventariovial.model.RoadAssetType.BRIDGE;"SIC-19"->com.tuempresa.inventariovial.model.RoadAssetType.DITCH;"SIC-23"->com.tuempresa.inventariovial.model.RoadAssetType.RIGHT_OF_WAY;else->when(s.sic20?.classCode){"13"->com.tuempresa.inventariovial.model.RoadAssetType.TUNNEL;"14"->com.tuempresa.inventariovial.model.RoadAssetType.WALL;else->com.tuempresa.inventariovial.model.RoadAssetType.FORD}}
                    AssetFormScreen(type,vm,{page="DETAIL"},{page="DETAIL"},r)
                }
            } else EngineeringEditScreen(id) { page="DETAIL" }
        }
        }
        return
    }
    if (page == "EXCEL") { RecordExcelScreen(vm,s,sync) {page="DETAIL"}; return }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal=20.dp),contentPadding=PaddingValues(vertical=20.dp),
        verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item { OutlinedButton(onClick=back,modifier=Modifier.heightIn(min=48.dp)) {Text(if(page=="DETAIL") "Volver" else "Volver al detalle")} }
        item {
            Text(s.sic17a?.bridgeName?.takeIf {it.isNotBlank()} ?: RecordDetailPresentation.title(r),style=MaterialTheme.typography.headlineMedium)
            Text("${r.sicCode} · ${r.routeCode} · Calzada ${r.roadbedCode}")
            Text(DashboardDrafts.progressive(r))
        }
        when(page) {
            "DETAIL" -> {
                item {
                    Text("Fecha de registro: ${r.surveyDate}")
                    Text(if(r.status=="ANNULLED") "Anulado" else "Drive: ${RecordDetailPresentation.status(sync,r.status=="ACTIVE")}",style=MaterialTheme.typography.titleMedium)
                    if(sync?.revision != sync?.syncedRevision || sync==null) Text("El Excel remoto puede representar una revisión anterior.")
                    Text("Georreferenciación externa: ${runCatching {JSONObject(sync?.receiptJson ?: "{}").optString("externalGeoreferencingStatus").takeIf {it.isNotBlank()}}.getOrNull() ?: "Pendiente de confirmación externa"}")
                    RecordDetailPresentation.summary(s).filter { (label,_) ->
                        label.startsWith("Condición") || label in setOf("Tipo / material","Sección transversal","Longitud (m)","Código de señal")
                    }.forEach { (label,value) -> Text("$label: $value") }
                    Text("Observaciones: ${r.observations?.takeIf {it.isNotBlank()} ?: "Sin observaciones"}")
                }
                item { DetailActions({page="SHEET"},{page="PHOTOS"},{page="EXCEL"},{page=if(SicFieldFormat.fromCode(r.sicCode)!=null) "TECHNICAL" else "EDIT"}) }
                item {
                    TextButton(onClick={deliveryExpanded=!deliveryExpanded}) {Text(if(deliveryExpanded) "Ocultar entrega Drive" else "Detalles de entrega Drive / reintentar")}
                    if(deliveryExpanded) AutomaticSyncPanel("SIC",id,r.status=="ACTIVE")
                    sync?.error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
                }
                item {
                    if(r.status!="DRAFT" && SicFieldFormat.fromCode(r.sicCode)==null) TextButton(enabled=com.tuempresa.inventariovial.core.InventoryLifecyclePolicy.enabled, onClick={vm.setRecordStatus(r.id,r.status!="ACTIVE") {message=it}}) {
                        Text(if(r.status=="ACTIVE") "Anular registro" else "Restaurar registro")
                    }
                    if(r.status!="DRAFT") Text(com.tuempresa.inventariovial.core.InventoryLifecyclePolicy.unavailableReason)
                    message?.let {Text(it,color=MaterialTheme.colorScheme.error)}
                }
            }
            "SHEET" -> {
                item { Text("Ficha completa · consulta",style=MaterialTheme.typography.titleLarge); RecordSheet(s) }
                item { Button(onClick={page=if(SicFieldFormat.fromCode(r.sicCode)!=null) "TECHNICAL" else "EDIT"},modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)) {Text("EDITAR")} }
            }
            "PHOTOS" -> item {
                Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    RecordPhotoSheet(s,db)
                    Button(onClick={page="TECHNICAL"}) {Text("Añadir / editar fotografías")}
                }
            }
            "EDIT" -> {
                item { Text("Edición del registro original",style=MaterialTheme.typography.titleLarge)
                    Button(onClick={page="TECHNICAL"},modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)) { Text("Editar ficha técnica y fotografías") }
                    if(r.sicCode=="SIC-17") SupplementaryFormat.entries.filter {it!=SupplementaryFormat.SIC18A && it.enabled}.forEach { f ->
                        OutlinedButton(onClick={supplemental=f}) {Text("Editar ${f.title}")}
                    }
                }
                item { ordered.find {it.item.record.id==id}?.let { row ->
                    HistoryRecordCard(row,vm,{page="TECHNICAL"},{supplemental=it},emptyList(),startEditing=true,onFinished={page="DETAIL"}) {}
                } }
            }
        }
    }
}

@Composable internal fun DetailActions(onSheet:()->Unit,onPhotos:()->Unit,onExcel:()->Unit,onEdit:()->Unit) {
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        listOf("VER FICHA" to onSheet,"VER FOTOGRAFÍAS" to onPhotos,"VER EXCEL SIC" to onExcel,"EDITAR" to onEdit).forEach { (label, action) ->
            Button(onClick=action,modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)) {Text(label)}
        }
    }
}

@Composable private fun RecordSheet(s:InventorySnapshot) {
    val r=s.record
    val row=RecordDetailPresentation.exportRow(s)
    val format=SicExportFormat.entries.find {it.code==r.sicCode}
    val fieldFormat=SicFieldFormat.fromCode(r.sicCode)
    if(fieldFormat!=null) {
        val values=s.sicExtension?.values().orEmpty()+SicFieldRepository.core(r)
        fieldFormat.fields.forEach { f -> val v=values[f.key].orEmpty(); Text("${f.label}: ${f.options.find {it.code==v}?.label ?: v.ifBlank {"Sin dato"}}") }
    } else if(format!=null) {
        RecordDetailPresentation.summary(s).forEach { (label,value) -> Text("$label: $value") }
        Text("Campos contractuales",style=MaterialTheme.typography.titleMedium)
        val values=runCatching {format.values(row)}
        values.getOrNull()?.let { cells -> format.columns.zip(cells).forEach { (c,v) -> Text("${c.label}: ${v ?: "Sin dato / no aplica"}") } }
        values.exceptionOrNull()?.let {Text("Ficha pendiente de completar: ${it.message}")}
    }
    listOf(SupplementaryFormat.SIC17A to s.sic17a?.values(),SupplementaryFormat.SIC17B to s.sic17b?.values(),
        SupplementaryFormat.SIC18A to s.sic18a?.values()).forEach { (f,values) -> if(values!=null) {
        HorizontalDivider();Text(f.title,style=MaterialTheme.typography.titleMedium)
        SupplementaryForms.fields(f,values).forEach { field -> val v=values[field.key].orEmpty();Text("${field.label}: ${field.options.find {it.substringBefore(" - ")==v} ?: v.ifBlank {"Sin dato"}}") }
    } }
    s.sic18?.let { Text("Forma: ${it.sectionShape ?: "Sin dato"}");Text("Daño estructural: ${it.structuralDamagePercent ?: "Sin dato"} %");Text("Obstrucción funcional: ${it.functionalObstructionPercent ?: "Sin dato"} %") }
    s.sic19?.let {Text("Criterio estructural: ${it.structuralCriterion ?: "Sin dato"}")}
    s.sic20?.wallLengthMeters?.let {Text("Longitud medida / histórica del muro: $it m")}
    s.sic22?.let {Text("Ancho: ${it.signWidthM ?: "Sin dato"} m · Alto: ${it.signHeightM ?: "Sin dato"} m");Text("Altura del borde inferior: ${it.lowerEdgeHeightM ?: "Sin dato"} m");Text("Identificación: ${it.signalCodeSource}");Text("Descripción: ${it.signalDescription}");Text("Observación de señal: ${it.signalObservation}")}
    if(TechnicalLengthExport.applies(row)) {
        val context=LocalContext.current
        val reference=remember(context) {com.tuempresa.inventariovial.road.RoadReferenceRepository(context)}
        var length by remember(s) {mutableStateOf<JSONObject?>(null)}
        LaunchedEffect(s) {val db=InventoryDatabase.getInstance(context);val refs=com.tuempresa.inventariovial.road.FieldPrRepository(db.inventoryDao()).merge(reference.loadFromAssets());length=TechnicalLengthExport.metadata(row,refs.prs)}
        length?.let {Text("Longitud aplicable: ${it.opt("applicableM")} m · ${it.optString("applicableSource")}");if(!it.isNull("warning"))Text(it.optString("warning"))}
    }
    Text("Observaciones: ${r.observations.orEmpty().ifBlank {"Sin observaciones"}}")
    Text("GPS inicial: ${r.latitude ?: "Pendiente"}, ${r.longitude ?: "Pendiente"} · Fuente: ${r.locationSource}")
    Text("GPS final: ${r.endLatitude ?: "Pendiente"}, ${r.endLongitude ?: "Pendiente"}")
    Text("Registrado: ${DateFormat.getDateTimeInstance().format(Date(r.createdAt))}")
}

@Composable private fun RecordPhotoSheet(s:InventorySnapshot,db:InventoryDatabase) {
    val groups=RecordDetailPresentation.photos(s.photos,s.record.id)
    var history by remember(s) {mutableStateOf<List<PhotoHistoryEntity>>(emptyList())}
    LaunchedEffect(s) {history=db.inventoryDao().photoHistory(s.record.id)}
    Text("Fotografías (${groups.values.sumOf {it.size}})",style=MaterialTheme.typography.titleLarge)
    if(groups.isEmpty()) Text("Este elemento no tiene fotografías locales.")
    val labels=(PhotoCategoryPolicy.linear+PhotoCategoryPolicy.vertical+PhotoCategoryPolicy.culvert).associate {it.key to it.label}
    val defaults=PhotoCategoryPolicy.categories(s.record.sicCode,s.record.assetType,s.sic20?.classCode ?: s.sic19?.classCode ?: s.sic21?.classCode ?: s.sic23?.classCode ?: "",s.sic21?.typeCode ?: s.sic23?.typeCode ?: "",hasEnds=s.record.endPrCode!=null)
    (defaults.map {it.key}+groups.keys).distinct().forEach { category ->
        val photos=groups[category].orEmpty()
        Text("${labels[category] ?: category} (${photos.size})",style=MaterialTheme.typography.titleMedium)
        InventoryPhotoReview(photos.map {it.localPath})
        val originals=photos.filter {it.originalPath.isNotBlank() && it.originalPath!=it.localPath}
        if(originals.isNotEmpty()) { Text("Originales · ${labels[category] ?: category}");InventoryPhotoReview(originals.map {it.originalPath}) }
    }
    if(history.isNotEmpty()) {
        HorizontalDivider();Text("Historial · originales conservados",style=MaterialTheme.typography.titleMedium)
        history.forEach { h ->
            val j=runCatching {JSONObject(h.snapshotJson)}.getOrNull()
            if(j!=null && j.optString("recordId")==s.record.id && j.isNull("scapInspectionId")) {
                Text("${h.action} · ${j.optString("photoCategory").takeUnless {it.isBlank() || it=="null"} ?: "GENERAL"} · revisión ${h.revision}")
                j.optString("originalPath").takeIf {it.isNotBlank() && it!="null"}?.let {InventoryPhotoReview(listOf(it))}
                j.optString("localPath").takeIf {it.isNotBlank()}?.let {InventoryPhotoReview(listOf(it))}
            }
        }
    }
}

@Composable private fun RecordExcelScreen(vm:InventoryViewModel,s:InventorySnapshot,sync:DriveSyncIntent?,onBack:()->Unit) {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    val formats=SicExportFormat.entries.filter {it.recordCode==s.record.sicCode && (it!=SicExportFormat.SIC18A || s.sic18a!=null)}
    var code by rememberSaveable(s.record.id) {mutableStateOf(s.record.sicCode)}
    val format=formats.find {it.code==code} ?: formats.firstOrNull()
    var file by remember(format,s.record.routeCode) {mutableStateOf(format?.let {vm.localExport.cachedRoute(it,s.record.routeCode)})}
    var busy by remember {mutableStateOf(false)}
    var message by remember {mutableStateOf<String?>(null)}
    BackHandler(onBack=onBack)
    LazyColumn(Modifier.fillMaxSize().padding(horizontal=20.dp),contentPadding=PaddingValues(vertical=20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item {OutlinedButton(onClick=onBack) {Text("Volver al detalle")};Text("Excel SIC · ${s.record.routeCode}",style=MaterialTheme.typography.headlineMedium)}
        item {
            formats.forEach {f-> FilterChip(selected=f==format,onClick={if(!busy)code=f.code},label={Text(f.code)})}
            Text("Copia local: incluye las fichas validadas de este SIC y ruta disponibles en esta tablet. No representa todo el inventario de Drive.")
            Text("La entrega automática funciona al validar; no depende de generar este archivo.")
            if(format!=null) Button(enabled=!busy,onClick={busy=true;scope.launch {
                try {file=vm.localExport.prepareRoute(format,s.record.routeCode);message="XLSX local generado con la plantilla contractual."}
                catch(e:Exception){message=e.message} finally {busy=false}
            }},modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)) {Text(if(busy) "Generando…" else "Generar / actualizar copia local")}
            file?.let {f ->
                Text("LOCAL · ${java.io.File(f.localPath).name} · ${DateFormat.getDateTimeInstance().format(Date(f.createdAt))}")
                Text("Es una copia de esa fecha; vuelve a generar para incluir cambios posteriores.")
                Button(onClick={runCatching {context.startActivity(GeneratedFileIndex.open(context,f))}.onFailure {message="No se pudo abrir. Instala una aplicación compatible con XLSX o comparte el archivo."}},modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)) {Text("Abrir XLSX local")}
                OutlinedButton(onClick={runCatching {context.startActivity(GeneratedFileIndex.share(context,f))}.onFailure {message=it.message}},modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)) {Text("Compartir XLSX local")}
            }
        }
        item {
            HorizontalDivider();Text("Consolidado de Drive",style=MaterialTheme.typography.titleLarge)
            Text("${s.record.routeCode}/EXCEL_SIC/${format?.code ?: s.record.sicCode}.xlsx")
            val remote=RecordDetailPresentation.remoteFile(sync,format?.code.orEmpty())
            if(remote==null) Text("No hay confirmación disponible del archivo remoto. No se ha descargado ningún Excel.")
            else {
                Text("Archivo confirmado en la revisión ${sync?.syncedRevision}. Su contenido actual requiere conexión y acceso a Drive.")
                if(RecordDetailPresentation.status(sync,s.record.status=="ACTIVE")!="Sincronizado") Text("Actualización pendiente: el Excel remoto puede contener la revisión anterior.")
                OutlinedButton(onClick={runCatching {context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://drive.google.com/file/d/$remote/view")))}.onFailure {message=it.message}},modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)) {Text("Consultar archivo confirmado en Drive")}
            }
            message?.let {Text(it)}
        }
    }
}
