package com.tuempresa.inventariovial.field

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

@Composable fun CulvertSupplementFields(values:Map<String,String>,editable:Boolean,onChange:(Map<String,String>)->Unit) {
    Column {
        com.tuempresa.inventariovial.SectionTitle("Detalle de condición mala")
        Text("SIC-18A · datos heredados de la ficha principal",style=MaterialTheme.typography.bodyLarge)
        listOf("Función" to listOf("functionCode"),"Falla estructural" to listOf("failureLocationCode","failureTypeCode"),"Falla funcional" to listOf("functionalStateCode","probableCauseCode")).forEach {(title,keys)->
            com.tuempresa.inventariovial.SectionTitle(title)
            SicFieldFormat.SIC18A.fields.filter {it.key in keys}.forEach {field->
                SicCodeDropdown(field,values[field.key].orEmpty(),{onChange(values+(field.key to it))},editable)
            }
        }
    }
}

/** Mature SIC-18 is the base; the supplement has no independent location, photographs or record. */
@Composable fun CulvertUnifiedScreen(vm:InventoryViewModel,id:String,initialExtra:Map<String,String> = emptyMap(),onBack:()->Unit) {
    val context=LocalContext.current
    val db=remember(context) {InventoryDatabase.getInstance(context)}
    val repo=remember(db) {SicFieldRepository(db)}
    val scope=rememberCoroutineScope()
    var snapshot by remember(id) {mutableStateOf<InventorySnapshot?>(null)}
    var base by remember(id) {mutableStateOf(Sic18FormState())}
    var extra by remember(id) {mutableStateOf(initialExtra)}
    var extraComplete by remember(id) {mutableStateOf(false)}
    var paths by remember(id) {mutableStateOf<List<String>>(emptyList())}
    var categories by remember(id) {mutableStateOf<Map<String,String>>(emptyMap())}
    var captures by remember(id) {mutableStateOf<Map<String,com.tuempresa.inventariovial.camera.PhotoCaptureMetadata>>(emptyMap())}
    var expanded by remember(id) {mutableStateOf(initialExtra.isNotEmpty())}
    var review by remember {mutableStateOf(false)}
    var dirty by remember {mutableStateOf(initialExtra.isNotEmpty())}
    var editingDraft by remember {mutableStateOf(false)}
    var busy by remember {mutableStateOf(false)}
    var exit by remember {mutableStateOf(false)}
    var notice by remember {mutableStateOf<String?>(null)}
    var conditionChange by remember {mutableStateOf<Sic18FormState?>(null)}
    var files by remember {mutableStateOf<List<GeneratedFile>>(emptyList())}
    LaunchedEffect(id) {try {
        val s=requireNotNull(db.inventoryDao().snapshot(id));snapshot=s
        base=(EngineeringEdits.form(s) as SicFormDetail.Sic18).state
        val saved=repo.load(id,SicFieldFormat.SIC18A)!!
        extra=saved.values+initialExtra
        extraComplete=initialExtra.isEmpty() && (s.sicExtension?.status=="COMPLETE" || s.sicExtension==null && s.sic18a!=null && saved.completion().canFinalize)
        paths=s.photos.sortedBy {it.photoIndex}.map {it.localPath};categories=s.photos.mapNotNull {p->p.photoCategory?.let {p.localPath to it}}.toMap()
        val owner=vm.field.auth.sessions.requireUser().id
        CulvertEditDraft.read(context,id,owner)?.let {draft->base=draft.base;extra=draft.extra;paths=draft.photos;categories=draft.categories;captures=draft.captures;dirty=true;editingDraft=true;expanded=true;notice="Borrador de edición recuperado"}
    } catch(e:Exception) {notice=e.message}}
    val required=InventoryElements.requiresCulvertDetail(base.structuralConditionCode,base.functionalConditionCode)
    val hasExtra=extra.any {it.key in culvertAdditionalKeys && it.value.isNotBlank()}
    val r=snapshot?.record
    val extraState=SicFieldState(SicFieldFormat.SIC18A,(r?.let {SicFieldRepository.core(it)} ?: emptyMap())+extra+mapOf("classCode" to base.classCode,"typeCode" to base.typeCode,"spans" to base.spans),id)
    fun baseIssues():List<ValidationIssue> {
        val record=r ?: return listOf(ValidationIssue("Alcantarilla","Cargando datos",ValidationIssue.Severity.MISSING_USER_INPUT))
        val request=InventorySaveRequest(record.sicCode,record.assetType,record.routeCode,record.roadbedCode,record.startPrCode,record.startDistanceM.toString(),record.endPrCode,record.endDistanceM?.toString(),record.sideCode,record.latitude,record.longitude,record.gpsAccuracyM?.toFloat(),record.surveyDate,record.observations.orEmpty(),"",SicFormDetail.Sic18(base),photoPaths=paths)
        return CaptureValidation.errors(request).map {ValidationIssue(it.field,it.message,ValidationIssue.Severity.MISSING_USER_INPUT)}
    }
    val completion=CompletionResult(baseIssues()+if(required)extraState.completion().issues else emptyList())
    fun leave() {if(dirty)exit=true else onBack()}
    fun save(finalize:Boolean,after:(()->Unit)?=null) {
        if(busy || r==null)return
        busy=true
        scope.launch {try {
            val owner=vm.field.auth.sessions.requireUser().id
            if(!finalize) {
                CulvertEditDraft.save(context,id,owner,CulvertEditingDraft(base,extra,paths,categories,captures))
                editingDraft=true
                notice="✓ Borrador guardado en tablet"
            } else {
                require(completion.canFinalize) {"Completa los pendientes antes de finalizar"}
                db.withTransaction {
                    EngineeringEdits.save(db,id,SicFormDetail.Sic18(base),paths,categories,captures)
                    if(!extraComplete && hasExtra) repo.save(id,extraState,owner,required)
                }
                CulvertEditDraft.clear(context,id,owner)
                editingDraft=false
                snapshot=db.inventoryDao().snapshot(id);extraComplete=db.inventoryDao().sicExtension(id)?.status=="COMPLETE" || extraComplete
                notice="✓ Alcantarilla finalizada · datos y detalle guardados"
            }
            dirty=false;review=false;after?.invoke()
        } catch(e:Exception) {notice=e.message} finally {busy=false}}
    }
    BackHandler(enabled=!busy) {leave()}
    LegacyInventoryFormScaffold("Registrar Alcantarilla","SIC-18 · Obras de drenaje",
        onSaveDraft={save(false)},onBack=::leave,draftNotice=notice.orEmpty(),enabled=!busy && snapshot!=null) {
        r?.let {record->
            item {
                val survey=remember(context) {SurveyPreferences(context)}
                SurveyHeader(survey,record.segment.orEmpty(),record.routeCode,record.roadbedCode,record.surveyDirection ?: "INCREASING",{},{},{},{})
            }
            item {LocationSummaryCard(record.latitude,record.longitude,record.gpsAccuracyM,"${record.startPrCode} + ${record.startDistanceM} m",record.routeCode,record.locationSource)}
            item {Text("PR ${record.startPrCode} + ${record.startDistanceM} m · ${record.surveyDate}")}
        }
        if(snapshot!=null) {
            item {Sic18Fields(base,onCompleteSic18A={expanded=true}) {next->
                if(!busy) {
                    if(required && !InventoryElements.requiresCulvertDetail(next.structuralConditionCode,next.functionalConditionCode) && hasExtra)conditionChange=next
                    else {base=next;dirty=true;review=false;files=emptyList()}
                }
            }}
            item {
                Text("SIC-18 · ${if(dirty) "Edición en borrador" else "Completo"}")
                Text("SIC-18A · ${if(!required) if(hasExtra) "No requerido · antecedente conservado" else "No requerido" else if(extraComplete) "Completo" else "Pendiente"}")
                if(!required && hasExtra)Text("Esta alcantarilla ya contiene un detalle de condición mala. La condición actual ya no requiere SIC-18A.")
                if(hasExtra && !required)OutlinedButton(onClick={expanded=!expanded}) {Text(if(expanded)"Ocultar detalle de condición mala" else "Ver antecedente")}
            }
            if(expanded && (required || hasExtra)) item {CulvertSupplementFields(extra,!extraComplete && !busy) {extra=it;dirty=true;review=false;files=emptyList()}}
            item {CulvertPhotos(paths,categories,onPhotoMetadata={path,metadata->captures=captures+(path to metadata)}) {p,c->if(!busy){paths=p;categories=c;dirty=true;files=emptyList()}}}
            r?.observations?.takeIf {it.isNotBlank()}?.let {item {com.tuempresa.inventariovial.SectionTitle("Registro");Text(it)}}
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
        text={Text("Esta alcantarilla ya contiene un detalle de condición mala. La condición actual ya no requiere SIC-18A.")},
        confirmButton={TextButton(onClick={base=next;conditionChange=null;dirty=true;review=false;files=emptyList()}) {Text("Conservar como antecedente")}},
        dismissButton={TextButton(onClick={conditionChange=null}) {Text("Cancelar cambio")}})}
    if(exit) AlertDialog(onDismissRequest={exit=false},title={Text("Guardar cambios")},text={Text("Conserva los cambios como borrador antes de volver")},
        confirmButton={TextButton(enabled=!busy,onClick={save(false){exit=false;onBack()}}) {Text("Guardar borrador y volver")}},dismissButton={TextButton(onClick={exit=false}) {Text("Seguir editando")}})
}
