package com.tuempresa.inventariovial.field

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.room.withTransaction
import com.tuempresa.inventariovial.Sic18Fields
import com.tuempresa.inventariovial.core.*
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.model.form.*
import com.tuempresa.inventariovial.repository.EngineeringEdits
import com.tuempresa.inventariovial.sic.*
import com.tuempresa.inventariovial.validation.CaptureValidation
import com.tuempresa.inventariovial.viewmodel.InventoryViewModel
import kotlinx.coroutines.launch
import java.io.File

val culvertAdditionalKeys=setOf("functionCode","failureLocationCode","failureTypeCode","functionalStateCode","probableCauseCode")

@Composable fun CulvertFunctionField(values:Map<String,String>,editable:Boolean=true,onChange:(Map<String,String>)->Unit) {
    SicCodeDropdown(SicFieldFormat.SIC18A.fields.single {it.key=="functionCode"},values["functionCode"].orEmpty(),
        {onChange(values+("functionCode" to it))},editable)
}

@Composable fun CulvertSupplementFields(values:Map<String,String>,editable:Boolean,onChange:(Map<String,String>)->Unit) {
    Column {
        com.tuempresa.inventariovial.SectionTitle("Fallas · condición regular o mala")
        Text("SIC-18A · datos heredados de la ficha principal",style=MaterialTheme.typography.bodyLarge)
        listOf("Falla estructural" to listOf("failureLocationCode","failureTypeCode"),"Falla funcional" to listOf("functionalStateCode","probableCauseCode")).forEach {(title,keys)->
            com.tuempresa.inventariovial.SectionTitle(title)
            SicFieldFormat.SIC18A.fields.filter {it.key in keys}.forEach {field->
                SicCodeDropdown(field,values[field.key].orEmpty(),{onChange(values+(field.key to it))},editable)
            }
        }
    }
}

/** Mature SIC-18 is the base; the supplement has no independent location, photographs or record. */
@Composable fun CulvertUnifiedScreen(vm:InventoryViewModel,id:String,initialExtra:Map<String,String> = emptyMap(),onBack:()->Unit) {
    var showDetail by androidx.compose.runtime.saveable.rememberSaveable(id) {mutableStateOf(false)}
    if(showDetail) {com.tuempresa.inventariovial.RecordDetailScreen(vm,id,onBack=onBack);return}
    val returnToDetail=com.tuempresa.inventariovial.LocalRecordSaved.current
    val context=LocalContext.current
    val db=remember(context) {InventoryDatabase.getInstance(context)}
    val repo=remember(db) {SicFieldRepository(db)}
    val scope=rememberCoroutineScope()
    var snapshot by remember(id) {mutableStateOf<InventorySnapshot?>(null)}
    var observations by rememberSaveable(id){mutableStateOf("")}
    var openedRevision by rememberSaveable(id) {mutableStateOf<Long?>(null)}
    var base by rememberSaveable(id) {mutableStateOf(Sic18FormState())}
    var extra by rememberSaveable(id) {mutableStateOf(initialExtra)}
    var extraComplete by rememberSaveable(id) {mutableStateOf(false)}
    var paths by rememberSaveable(id) {mutableStateOf<List<String>>(emptyList())}
    var categories by rememberSaveable(id) {mutableStateOf<Map<String,String>>(emptyMap())}
    var captures by rememberSaveable(id) {mutableStateOf<Map<String,com.tuempresa.inventariovial.camera.PhotoCaptureMetadata>>(emptyMap())}
    var expanded by rememberSaveable(id) {mutableStateOf(initialExtra.isNotEmpty())}
    var review by com.tuempresa.inventariovial.rememberRecordReview(id)
    var dirty by rememberSaveable {mutableStateOf(initialExtra.isNotEmpty())}
    var editingDraft by rememberSaveable {mutableStateOf(false)}
    var busy by remember {mutableStateOf(false)}
    var exit by rememberSaveable {mutableStateOf(false)}
    var notice by remember {mutableStateOf<String?>(null)}
    var conditionChange by rememberSaveable {mutableStateOf<Sic18FormState?>(null)}
    var files by remember {mutableStateOf<List<GeneratedFile>>(emptyList())}
    var loaded by rememberSaveable(id) {mutableStateOf(false)}
    var core by rememberSaveable(id){mutableStateOf<SicCoreDraft?>(null)}
    val reference by vm.field.reference.collectAsState()
    LaunchedEffect(id) {try {
        val s=requireNotNull(db.inventoryDao().snapshot(id));snapshot=s;if(loaded)return@LaunchedEffect;loaded=true;core=SicCoreDraft.from(s.record);observations=s.record.observations.orEmpty();openedRevision=s.record.updatedAt
        base=(EngineeringEdits.form(s) as SicFormDetail.Sic18).state
        val saved=repo.load(id,SicFieldFormat.SIC18A)!!
        extra=saved.values+initialExtra
        extraComplete=initialExtra.isEmpty() && (s.sicExtension?.status=="COMPLETE" || s.sicExtension==null && s.sic18a!=null && saved.completion().canFinalize)
        paths=s.photos.sortedBy {it.photoIndex}.map {it.localPath};categories=s.photos.mapNotNull {p->p.photoCategory?.let {p.localPath to it}}.toMap()
        val owner=vm.field.auth.sessions.requireUser().id
        CulvertEditDraft.read(context,id,owner)?.let {draft->openedRevision=draft.baseRevision ?: openedRevision;core=draft.core ?: core;base=draft.base;observations=draft.observations ?: observations;extra=draft.extra;paths=draft.photos;categories=draft.categories;captures=draft.captures;dirty=true;editingDraft=true;expanded=true;notice="Borrador de edición recuperado"}
    } catch(e:Exception) {notice=e.message}}
    val required=InventoryElements.requiresCulvertDetail(base.structuralConditionCode,base.functionalConditionCode)
    val hasExtra=extra.any {it.key in culvertAdditionalKeys && it.value.isNotBlank()}
    val r=snapshot?.record
    val coordinateEdited=extra["_gpsExplicit"]=="true"
    val coordinate=if(coordinateEdited){
        val a=extra["_latitude"]?.toDoubleOrNull();val o=extra["_longitude"]?.toDoubleOrNull()
        if(a!=null && o!=null) com.tuempresa.inventariovial.location.GeoLocation(a,o,extra["_accuracy"]?.toFloatOrNull() ?: Float.NaN,
            source=extra["_coordinateSource"] ?: "MANUAL_COORDINATES",timestamp=extra["_gpsTimestamp"]?.toLongOrNull() ?: 0) else null
    } else r?.let{record->if(record.latitude!=null && record.longitude!=null) com.tuempresa.inventariovial.location.GeoLocation(record.latitude,record.longitude,record.gpsAccuracyM?.toFloat() ?: Float.NaN,source=record.locationSource,timestamp=record.gpsTimestamp ?: 0) else null}

    val editedRecord=r?.let {record->core?.takeIf {it.errors(false).isEmpty()}?.applyTo(record,false,false) ?: record}
    val extraState=SicFieldState(SicFieldFormat.SIC18A,extra+(editedRecord?.let {SicFieldRepository.core(it)} ?: emptyMap())+mapOf("classCode" to base.classCode,"typeCode" to base.typeCode,"spans" to base.spans,
        "structuralConditionCode" to base.structuralConditionCode,"functionalConditionCode" to base.functionalConditionCode,"_segment" to core?.segment.orEmpty(),"_direction" to (core?.direction ?: "INCREASING")),id)
    fun baseIssues():List<ValidationIssue> {
        val record=r ?: return listOf(ValidationIssue("Alcantarilla","Cargando datos",ValidationIssue.Severity.MISSING_USER_INPUT))
        val request=InventorySaveRequest(record.sicCode,record.assetType,record.routeCode,record.roadbedCode,record.startPrCode,record.startDistanceM.toString(),record.endPrCode,record.endDistanceM?.toString(),record.sideCode,record.latitude ?: Double.NaN,record.longitude ?: Double.NaN,record.gpsAccuracyM?.toFloat(),record.surveyDate,record.observations.orEmpty(),"",SicFormDetail.Sic18(base),photoPaths=paths)
        return CaptureValidation.errors(request,validateLocation=record.latitude!=null || record.longitude!=null).filterNot{it.field in setOf("startPrCode","startDistanceM","endPrCode","endDistanceM")}.map {ValidationIssue(it.field,it.message,ValidationIssue.Severity.MISSING_USER_INPUT)}
    }
    val completion=CompletionResult(baseIssues()+core?.errors(false).orEmpty()+(if(extra["_gpsError"].isNullOrBlank())emptyList() else listOf(ValidationIssue("GPS",extra["_gpsError"].orEmpty(),ValidationIssue.Severity.MISSING_USER_INPUT)))+extraState.completion().issues)
    fun leave() {if(dirty)exit=true else onBack()}
    fun save(finalize:Boolean,after:(()->Unit)?=null) {
        if(busy || r==null)return
        busy=true
        scope.launch {try {
            val owner=vm.field.auth.sessions.requireUser().id
            if(!finalize) {
                CulvertEditDraft.save(context,id,owner,CulvertEditingDraft(base,extra,paths,categories,captures,baseRevision=openedRevision,observations=observations,core=core))
                editingDraft=true
                notice="✓ Borrador guardado en tablet"
            } else {
                require(completion.canFinalize) {"Completa los pendientes antes de finalizar"}
                db.withTransaction {
                    EngineeringEdits.save(db,id,SicFormDetail.Sic18(base),paths,categories,captures,expectedUpdatedAt=openedRevision,observations=observations,coordinateEdit=coordinateEdited,coordinate=coordinate,core=core)
                    repo.save(id,extraState,owner,true)
                }
                CulvertEditDraft.clear(context,id,owner)
                editingDraft=false
                snapshot=db.inventoryDao().snapshot(id);openedRevision=snapshot?.record?.updatedAt;extraComplete=db.inventoryDao().sicExtension(id)?.status=="COMPLETE" || extraComplete
                notice="✓ Alcantarilla finalizada · datos y detalle guardados"
            }
            dirty=false;review=false;if(finalize) {if(returnToDetail!=null)returnToDetail(id) else showDetail=true};after?.invoke()
        } catch(e:Exception) {notice=e.message} finally {busy=false}}
    }
    BackHandler(enabled=!busy) {leave()}
    LegacyInventoryFormScaffold("Registrar Alcantarilla","SIC-18 · Obras de drenaje",
        onSaveDraft={save(false)},onBack=::leave,draftNotice=notice.orEmpty(),enabled=!busy && snapshot!=null) {
        r?.let {record->
            item {
                core?.let {draft->SicFormCore(SicFormMode.EDIT,"SIC-18",record.assetType,base.classCode,reference,draft){core=it;dirty=true;review=false}}
            }
            item {OptionalDeviceGps(coordinate,editable=!busy,onChange={gps->
                extra=extra+mapOf("_gpsExplicit" to "true","_latitude" to gps?.latitude?.toString().orEmpty(),"_longitude" to gps?.longitude?.toString().orEmpty(),
                    "_accuracy" to gps?.horizontalAccuracy?.takeIf{it.isFinite()}?.toString().orEmpty(),"_coordinateSource" to (gps?.source ?: "CONTRACTUAL_PR_ONLY"),"_gpsTimestamp" to gps?.timestamp?.toString().orEmpty())
                dirty=true;review=false
            },onError={extra=extra+("_gpsError" to it.orEmpty())})}
            item {Text("PR ${record.startPrCode} + ${record.startDistanceM} m · ${record.surveyDate}")}
        }
        if(snapshot!=null) {
            item {Sic18Fields(base,onCompleteSic18A={expanded=true},beforeConditions={
                CulvertFunctionField(extra,!busy) {extra=it;extraComplete=false;dirty=true;review=false;files=emptyList()}
            }) {next->
                if(!busy) {
                    if(required && !InventoryElements.requiresCulvertDetail(next.structuralConditionCode,next.functionalConditionCode) && hasExtra)conditionChange=next
                    else {base=next;dirty=true;review=false;files=emptyList()}
                }
            }}
            item {
                Text("SIC-18 · ${if(dirty) "Edición en borrador" else "Completo"}")
                Text("SIC-18A · ${if(!required) if(hasExtra) "No requerido · antecedente conservado" else "No requerido" else if(extraComplete) "Completo" else "Pendiente"}")
                if(!required && hasExtra)Text("Condición buena: función conservada y fallas anteriores preservadas como antecedente.")
                if(hasExtra && !required)OutlinedButton(onClick={expanded=!expanded}) {Text(if(expanded)"Ocultar detalle de condición mala" else "Ver antecedente")}
            }
            if(required || expanded && hasExtra) item {CulvertSupplementFields(extra,!busy) {extra=it;extraComplete=false;dirty=true;review=false;files=emptyList()}}
            item {CulvertPhotos(paths,categories,captureMetadata=captures,onPhotoMetadata={path,metadata->captures=captures+(path to metadata)}) {p,c->if(!busy){paths=p;categories=c;dirty=true;files=emptyList()}}}
            item {OutlinedTextField(observations,{observations=it;dirty=true;review=false},enabled=!busy,label={Text("OBSERVACIONES")},minLines=3,modifier=Modifier.fillMaxWidth())}
            item {LegacyInventoryReviewButton(!busy) {review=true}}
            item {
                if(editingDraft)Text("Finaliza el borrador de edición para exportar los cambios")
                Button(enabled=!busy && !dirty && !editingDraft,onClick={busy=true;scope.launch {try {
                    files=ElementExports.prepare(context,db,id,vm.localExport.savedHeading);notice="Se generaron ${files.size} formatos para esta alcantarilla"
                } catch(e:Exception) {notice=e.message} finally {busy=false}}}) {Text("Exportar formatos")}
                GeneratedElementFiles(files) {notice=it}
            }
        }
        if(busy)item {LinearProgressIndicator(Modifier.fillMaxWidth())}
    }
    if(review) LegacyRecordReviewDialog(
        listOf("Ubicación" to "${r?.routeCode} · PR ${r?.startPrCode}+${r?.startDistanceM}","Clase / tipo / vanos" to "${base.classCode} / ${base.typeCode} / ${base.spans}","Condición estructural / funcional" to "${base.structuralConditionCode} / ${base.functionalConditionCode}","SIC-18A" to if(required)"Requerido" else "No requerido")+
            SicFieldFormat.SIC18A.fields.filter {it.key in culvertAdditionalKeys}.map {it.label to extraState.text(it.key)},paths.map(::File),completion,
        enabled=!busy,onConfirm={save(true)},onDismiss={review=false})
    conditionChange?.let {next->AlertDialog(onDismissRequest={conditionChange=null},title={Text("Conservar detalle existente")},
        text={Text("La condición buena conserva la función y las fallas anteriores como antecedente; no se eliminarán datos.")},
        confirmButton={TextButton(onClick={base=next;conditionChange=null;dirty=true;review=false;files=emptyList()}) {Text("Conservar como antecedente")}},
        dismissButton={TextButton(onClick={conditionChange=null}) {Text("Cancelar cambio")}})}
    if(exit) AlertDialog(onDismissRequest={exit=false},title={Text("Guardar cambios")},text={Text("Conserva los cambios como borrador antes de volver")},
        confirmButton={TextButton(enabled=!busy,onClick={save(false){exit=false;onBack()}}) {Text("Guardar borrador y volver")}},dismissButton={TextButton(onClick={exit=false}) {Text("Seguir editando")}})
}
