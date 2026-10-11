package com.tuempresa.inventariovial.sic

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tuempresa.inventariovial.ChoiceSelector
import com.tuempresa.inventariovial.FinalLocationCapture
import com.tuempresa.inventariovial.core.*
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.field.CulvertUnifiedScreen
import com.tuempresa.inventariovial.camera.PhotoCaptureMetadata
import com.tuempresa.inventariovial.export.*
import com.tuempresa.inventariovial.location.GeoLocation
import com.tuempresa.inventariovial.viewmodel.InventoryViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.util.UUID

@Composable
fun SicCodeDropdown(field:SicField,value:String,onChange:(String)->Unit,enabled:Boolean=true) {
    Column {
        Text(field.label,fontWeight=androidx.compose.ui.text.font.FontWeight.Bold)
        val labels=field.options.map {if(it.code==ACCIDENT_TYPE_SOURCE_CONFLICT) it.name else it.label}
        val selected=field.options.indexOfFirst {it.code==value}
        ChoiceSelector(labels,labels.getOrNull(selected).orEmpty(),enabled) {label->onChange(field.options[labels.indexOf(label)].code)}
    }
}

@Composable
fun InventoryElementWorkspace(vm:InventoryViewModel,format:SicFieldFormat,recordId:String?=null,onBack:()->Unit) {
    when(format) {
        SicFieldFormat.SIC08,SicFieldFormat.SIC09 -> FajaWorkspace(vm,format,recordId,onBack)
        SicFieldFormat.SIC18A -> if(recordId!=null) CulvertUnifiedScreen(vm,recordId,onBack=onBack) else Text("Selecciona una alcantarilla desde el historial")
        else -> SicFieldWorkspace(vm,format,recordId,onBack)
    }
}

@Composable
fun FajaWorkspace(vm:InventoryViewModel,format:SicFieldFormat,recordId:String?=null,onBack:()->Unit) {
    require(format in setOf(SicFieldFormat.SIC08,SicFieldFormat.SIC09))
    val context=LocalContext.current
    val db=remember(context) {InventoryDatabase.getInstance(context)}
    val records by db.inventoryDao().observeAllRecords().collectAsState(initial=emptyList())
    var selected by rememberSaveable(format,recordId) {mutableStateOf(recordId)}
    var creating by rememberSaveable(format,recordId) {mutableStateOf(false)}
    if(creating || selected!=null) {
        SicFieldWorkspace(vm,format,selected) {selected=null;creating=false;if(recordId!=null)onBack()}
        return
    }
    BackHandler(onBack=onBack)
    val visible=records.filter {it.sicCode==format.code && it.status in setOf("ACTIVE","DRAFT")}
    LazyColumn(Modifier.fillMaxSize().padding(horizontal=24.dp),contentPadding=PaddingValues(vertical=24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        item {OutlinedButton(onClick=onBack) {Text("Volver")};Text(format.code,style=MaterialTheme.typography.headlineMedium);Text(format.title)}
        item {Button(onClick={creating=true}) {Text("Nuevo registro")}}
        if(visible.isEmpty()) item {Text("Todavía no hay registros guardados")}
        items(visible,key={it.id}) {record->
            Card(Modifier.fillMaxWidth()) {Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("${record.routeCode} · ${record.roadbedCode}",style=MaterialTheme.typography.titleLarge)
                Text("PR ${record.startPrCode}+${record.startDistanceM} → ${record.endPrCode.orEmpty()}+${record.endDistanceM ?: ""}")
                Text(if(record.status=="DRAFT") "Borrador" else "Guardado")
                OutlinedButton(onClick={selected=record.id}) {Text("Abrir")}
            }}
        }
    }
}

@Composable
fun SicFieldWorkspace(vm:InventoryViewModel,format:SicFieldFormat,recordId:String?=null,onBack:()->Unit) {
    if(format==SicFieldFormat.SIC18A) {InventoryElementWorkspace(vm,format,recordId,onBack);return}
    val context=LocalContext.current
    val db=remember(context) {InventoryDatabase.getInstance(context)}
    val repo=remember(db) {SicFieldRepository(db)}
    val scope=rememberCoroutineScope()
    val session by vm.field.session.collectAsState()
    val survey=remember(context) {com.tuempresa.inventariovial.field.SurveyPreferences(context)}
    val independent=format in setOf(SicFieldFormat.SIC08,SicFieldFormat.SIC09)
    var id by rememberSaveable(format,recordId) {mutableStateOf(recordId ?: UUID.randomUUID().toString())}
    var trackRecordId by rememberSaveable(format,recordId) {mutableStateOf(recordId)}
    var source by rememberSaveable(format,recordId) {mutableStateOf<String?>(null)}
    var raw by rememberSaveable(format,recordId) {mutableStateOf("{}")}
    var loaded by rememberSaveable(format,recordId) {mutableStateOf(false)}
    var formSessionId by rememberSaveable(format,recordId) {mutableStateOf<String?>(null)}
    var complete by rememberSaveable(format,recordId) {mutableStateOf(false)}
    var dirty by rememberSaveable(format,recordId) {mutableStateOf(false)}
    var review by rememberSaveable {mutableStateOf(false)}
    var exit by remember {mutableStateOf(false)}
    var busy by remember {mutableStateOf(false)}
    var notice by remember {mutableStateOf<String?>(null)}
    var files by remember {mutableStateOf<List<GeneratedFile>>(emptyList())}
    var paths by rememberSaveable {mutableStateOf<List<String>>(emptyList())}
    var photosChanged by rememberSaveable(format,recordId) {mutableStateOf(false)}
    var categories by remember {mutableStateOf<Map<String,String>>(emptyMap())}
    var captures by remember {mutableStateOf<Map<String,PhotoCaptureMetadata>>(emptyMap())}
    var fix by remember {mutableStateOf<GeoLocation?>(null)}
    var endFix by remember {mutableStateOf<GeoLocation?>(null)}
    fun values(json:String)=JSONObject(json).let {j->j.keys().asSequence().associateWith {j.getString(it)}}
    val state=SicFieldState(format,values(raw),source)
    val completion=state.completion()
    val editable=true // Completed records keep their UUID; changes must be validated again.
    fun change(key:String,value:String) {raw=JSONObject(values(raw)+(key to value)).toString();dirty=true;review=false;files=emptyList()}
    LaunchedEffect(id) {
        if(!loaded) try {
            val saved=repo.load(id,format)
            if(saved!=null) {
                raw=JSONObject(saved.values).toString();source=saved.sourceRecordId
                complete=db.inventoryDao().sicExtension(id)?.status=="COMPLETE"
                val snapshot=db.inventoryDao().snapshot(id)!!
                formSessionId=snapshot.record.sessionId
                paths=snapshot.photos.sortedBy {it.photoIndex}.map {it.localPath}
                categories=if(independent) snapshot.photos.mapNotNull {p->p.photoCategory?.let {p.localPath to it}}.toMap()
                    else snapshot.photos.associate {it.localPath to (it.photoCategory ?: "GENERAL")}
                if(snapshot.record.locationSource!="CONTRACTUAL_PR_ONLY" && snapshot.record.latitude!=null && snapshot.record.longitude!=null) fix=GeoLocation(snapshot.record.latitude,snapshot.record.longitude,snapshot.record.gpsAccuracyM?.toFloat() ?: Float.POSITIVE_INFINITY,snapshot.record.altitudeM)
                snapshot.record.endLatitude?.let {endFix=GeoLocation(it,snapshot.record.endLongitude!!,snapshot.record.endGpsAccuracyM?.toFloat() ?: Float.POSITIVE_INFINITY)}

            } else {
                val initial=mutableMapOf("surveyDate" to LocalDate.now().format(SicFieldFormat.dateFormatter))
                val captureSession=db.inventoryDao().currentSession()
                val existingRecord=db.inventoryDao().recordById(id)
                formSessionId=if(existingRecord!=null) existingRecord.sessionId else captureSession?.sessionId
                val location=com.tuempresa.inventariovial.field.FieldSessionDefaults.location(captureSession,existingRecord,vm.field.projectRoutes)
                existingRecord?.let {initial.putAll(SicFieldRepository.core(it));initial["observations"]=it.observations.orEmpty()}
                initial["routeCode"]=location.route;initial["roadbedCode"]=location.roadbed
                val prior=if(existingRecord==null) com.tuempresa.inventariovial.field.FieldSessionDefaults.matchingPosition(captureSession,survey.last()) else null
                prior?.let {initial["startPrCode"]=it.pr;initial["startDistanceM"]=it.distance.toString()}
                initial["_direction"]=existingRecord?.surveyDirection ?: location.direction
                initial["_segment"]=existingRecord?.segment ?: location.segment
                raw=JSONObject(initial).toString()
            }
            loaded=true
        } catch(e:Exception) {notice=e.message}
    }
    fun leave() {if(dirty)exit=true else onBack()}
    fun save(finalize:Boolean,after:(()->Unit)?=null) {
        if(busy)return
        busy=true
        scope.launch {try {
            val owner=vm.field.auth.sessions.requireUser().id
            if(finalize) require(completion.canFinalize) {"Revisa los pendientes antes de finalizar"}
            repo.save(id,state,owner,finalize,if(independent && !photosChanged)null else paths,categories=categories,captures=captures,sessionId=formSessionId)
            complete=db.inventoryDao().sicExtension(id)?.status=="COMPLETE"
            trackRecordId=id
            dirty=false;photosChanged=false;review=false;notice=if(finalize) "✓ Finalizado · guardado en tablet" else "✓ Borrador guardado en tablet"
            if(finalize) {
                if(com.tuempresa.inventariovial.tracking.TrackCaptureService.activeRecordId.value==id)vm.field.stopTrack()
                survey.remember(com.tuempresa.inventariovial.field.SurveyPosition(
                    survey.projectRoutes.segment(state.text("routeCode"),state.text("_segment")),state.text("routeCode"),state.text("roadbedCode"),state.text("startPrCode"),
                    SicFieldState.number(state.text("startDistanceM")) ?: 0.0,state.text("_direction").ifBlank {"INCREASING"},formSessionId))
            }
            after?.invoke()
        } catch(e:Exception) {notice=e.message} finally {busy=false}}
    }
    BackHandler(enabled=!busy) {leave()}
    if(!loaded) {Text(notice ?: "Cargando ficha…");return}
    val exportContent:@Composable ()->Unit = {
        if(independent) {
            val intents by db.automaticSyncDao().observe().collectAsState(initial=emptyList())
            val sync=intents.find {it.key=="SIC/$id"}
            Text(when {
                dirty -> "Cambios sin guardar"
                !complete -> "Borrador local"
                sync?.state=="SYNCED" -> "Sincronizado"
                sync?.state=="PENDING_VALIDATION" -> "Entrega pendiente de completar"
                else -> "Guardado local · sincronización pendiente"
            },style=MaterialTheme.typography.bodySmall)
        } else com.tuempresa.inventariovial.core.AutomaticSyncPanel("SIC",id,complete && !dirty)
        if(complete) Button(enabled=!busy && !dirty,onClick={busy=true;scope.launch {try {
            files=ElementExports.prepare(context,db,id,vm.localExport.savedHeading)
            notice="Exportación preparada"
        } catch(e:Exception) {notice=e.message} finally {busy=false}}}) {Text("Exportar")}
        GeneratedElementFiles(files) {notice=it}
    }
    if(independent) FajaCaptureForm(state,!busy,notice,onChange=::change,onDraft={save(false)},onBack=::leave,
        onSave={if(completion.canFinalize)save(true) else notice=completion.issues.joinToString("\n") {"${it.field}: ${it.reason}"}},
        photoContent={com.tuempresa.inventariovial.field.LegacyInventoryPhotos(format.code,paths,categories,editable=!busy,captureLocation=false,
            onPhotoMetadata={path,metadata->captures=captures+(path to metadata)},
            onChange={p,c->paths=p;categories=c;photosChanged=true;dirty=true;files=emptyList()})},exportContent=exportContent)
    else LegacySicElementForm(vm,state,editable,true,busy,loaded,notice,fix,endFix,paths,categories,
        emptyMap(),"",false,false,emptyList(),trackRecordId,
        onChange=::change,onFix={location,end->
            if(end)endFix=location else fix=location
            val metadata=if(end)mapOf("_endLatitude" to location.latitude.toString(),"_endLongitude" to location.longitude.toString(),"_endAccuracy" to location.horizontalAccuracy.toString(),"_endGpsTimestamp" to location.timestamp.toString())
                else mapOf("_latitude" to location.latitude.toString(),"_longitude" to location.longitude.toString(),"_accuracy" to location.horizontalAccuracy.toString(),"_altitude" to location.altitude.toString(),"_gpsTimestamp" to location.timestamp.toString())
            raw=JSONObject(values(raw)+metadata).toString();dirty=true;review=false;files=emptyList()
        },onPhotos={p,c->paths=p;categories=c;dirty=true;files=emptyList()},onMetadata={path,metadata->captures=captures+(path to metadata)},
        onWidthChange={_,_->},onWidthSelect={},onExpandWidths={},onTrackId={id=it;trackRecordId=it;save(false)},
        onDraft={if(editable)save(false)},onBack=::leave,onReview={review=true},exportContent=exportContent)
    if(review) com.tuempresa.inventariovial.field.LegacyRecordReviewDialog(
        format.fields.map {f->f.label to (f.options.find {it.code==state.text(f.key)}?.name ?: state.text(f.key))},
        paths.map(::File),completion,enabled=!busy,onConfirm={save(true)},onDismiss={review=false})
    if(exit) AlertDialog(onDismissRequest={exit=false},title={Text("Guardar cambios")},text={Text("Conserva los cambios como borrador antes de volver")},confirmButton={TextButton(enabled=!busy,onClick={save(false){exit=false;onBack()}}) {Text("Guardar borrador y volver")}},dismissButton={TextButton(onClick={exit=false}) {Text("Seguir editando")}})
}

@Composable internal fun SicFieldInput(field:SicField,value:String,onChange:(String)->Unit,enabled:Boolean) {
    if(field.options.isNotEmpty()) SicCodeDropdown(field,value,onChange,enabled)
    else OutlinedTextField(value,onChange,readOnly=!enabled,label={Text(field.label)},singleLine=field.key!="description",minLines=if(field.key=="description")3 else 1,
        keyboardOptions=KeyboardOptions(keyboardType=if(field.kind in setOf(CellKind.DECIMAL,CellKind.INTEGER))KeyboardType.Decimal else KeyboardType.Text),modifier=Modifier.fillMaxWidth())
}

@Composable internal fun TechnicalCriterion(label:String,content:@Composable ()->Unit) {
    var expanded by remember {mutableStateOf(false)}
    TextButton(onClick={expanded=!expanded}) {Text(if(expanded) "Ocultar criterio" else label)}
    if(expanded) content()
}

@Composable fun GeneratedElementFiles(files:List<GeneratedFile>,onError:(String)->Unit) {
    val context=LocalContext.current
    files.forEach {file->
        Text(File(file.localPath).name)
        Row {
            TextButton(onClick={runCatching {context.startActivity(GeneratedFileIndex.open(context,file))}.onFailure {onError("No hay visor compatible")}}) {Text("Abrir")}
            TextButton(onClick={runCatching {context.startActivity(GeneratedFileIndex.share(context,file))}.onFailure {onError("No se pudo compartir")}}) {Text("Compartir")}
        }
    }
}

@Composable fun NewSicDashboard(vm:InventoryViewModel,onOpen:(SicFieldFormat)->Unit) {
    InventoryElementDashboard(onOpen)
}

@Composable fun InventoryElementDashboard(onOpen:(SicFieldFormat)->Unit) {
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        InventoryElements.dashboard.groupBy {it.family}.forEach {(family,entries)->
            Text(family,style=MaterialTheme.typography.titleSmall)
            InventoryFieldRows(entries) {entry->com.tuempresa.inventariovial.InventoryElementButton(entry.title,entry.subtitle) {onOpen(entry.format)}}
        }
    }
}
