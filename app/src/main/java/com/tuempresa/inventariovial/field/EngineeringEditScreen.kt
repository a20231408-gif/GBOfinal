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
import com.tuempresa.inventariovial.*
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.model.form.*
import com.tuempresa.inventariovial.repository.EngineeringEdits
import kotlinx.coroutines.launch

@Composable
fun EngineeringEditScreen(id:String,onBack:()->Unit) {
    val context=LocalContext.current;val db=remember {InventoryDatabase.getInstance(context)};val scope=rememberCoroutineScope()
    var snapshot by remember(id){mutableStateOf<com.tuempresa.inventariovial.data.entity.InventorySnapshot?>(null)}
    var review by com.tuempresa.inventariovial.rememberRecordReview(id)
    var hasEnds by rememberSaveable(id){mutableStateOf(false)}
    var observations by rememberSaveable(id){mutableStateOf("")}
    var coordinate by rememberSaveable(id){mutableStateOf<com.tuempresa.inventariovial.location.GeoLocation?>(null)}
    var coordinateEdited by rememberSaveable(id){mutableStateOf(false)}
    var gpsError by remember{mutableStateOf<String?>(null)}
    var openedRevision by rememberSaveable(id) {mutableStateOf<Long?>(null)}
    var detail by rememberSaveable(id) {mutableStateOf<SicFormDetail?>(null)}
    var paths by rememberSaveable(id) {mutableStateOf<List<String>>(emptyList())}
    var categories by rememberSaveable(id) {mutableStateOf<Map<String,String>>(emptyMap())}
    var captures by rememberSaveable(id) {mutableStateOf<Map<String,com.tuempresa.inventariovial.camera.PhotoCaptureMetadata>>(emptyMap())}
    var error by remember {mutableStateOf<String?>(null)};var saving by remember {mutableStateOf(false)}
    var loaded by rememberSaveable(id) {mutableStateOf(false)}
    var core by rememberSaveable(id){mutableStateOf<SicCoreDraft?>(null)}
    var reference by remember {mutableStateOf(com.tuempresa.inventariovial.road.RoadReferenceData(emptyList(),emptyList(),emptyList()))}
    LaunchedEffect(id) {runCatching {reference=com.tuempresa.inventariovial.road.FieldPrRepository(db.inventoryDao()).merge(com.tuempresa.inventariovial.road.RoadReferenceRepository(context).loadFromAssets());val s=requireNotNull(db.inventoryDao().snapshot(id));snapshot=s;if(loaded)return@runCatching;loaded=true;core=SicCoreDraft.from(s.record);hasEnds=SicFormPolicy.hasEnds(s.record.sicCode,s.record.assetType);openedRevision=s.record.updatedAt;observations=s.record.observations.orEmpty();coordinate=s.record.let{r->if(r.latitude!=null && r.longitude!=null) com.tuempresa.inventariovial.location.GeoLocation(r.latitude,r.longitude,r.gpsAccuracyM?.toFloat() ?: Float.NaN,source=r.locationSource,timestamp=r.gpsTimestamp ?: 0) else null};detail=EngineeringEdits.form(s);paths=s.photos.filter {it.scapInspectionId==null}.sortedBy {it.photoIndex}.map {it.localPath};categories=s.photos.filter {it.scapInspectionId==null}.mapNotNull {p->p.photoCategory?.let {p.localPath to it}}.toMap()}.onFailure {error=it.message}}
    BackHandler(enabled=!saving,onBack=onBack)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        OutlinedButton(enabled=!saving,onClick=onBack) {Text("Volver")}
        Text("Editar ficha técnica",style=MaterialTheme.typography.headlineMedium)
        com.tuempresa.inventariovial.core.AutomaticSyncPanel("SIC",id,snapshot?.record?.status=="ACTIVE")
        core?.let {draft->snapshot?.record?.let {r->SicFormCore(SicFormMode.EDIT,r.sicCode,r.assetType,(detail as? SicFormDetail.Sic20)?.state?.classCode.orEmpty(),reference,draft){core=it}}}
        OptionalDeviceGps(coordinate,onChange={coordinate=it;coordinateEdited=true},onError={gpsError=it})
        core?.let {c->
            val length=com.tuempresa.inventariovial.validation.ContractualLength.calculate(
                com.tuempresa.inventariovial.validation.LengthEndpoint(c.route,c.roadbed,c.startPr,c.startDistance),
                com.tuempresa.inventariovial.validation.LengthEndpoint(c.route,c.roadbed,c.endPr,c.endDistance),reference.prs,c.direction)
            val wall=(detail as? SicFormDetail.Sic20)?.state?.classCode=="14"
            if(snapshot?.record?.assetType in setOf("WALL","DITCH","FORD") ||
                detail?.let {com.tuempresa.inventariovial.validation.TechnicalElementRules.linear(it)}==true)
                TechnicalLengthField(length,if(wall) {{metres:Double->val d=detail as? SicFormDetail.Sic20;if(d!=null)detail=SicFormDetail.Sic20(d.state.copy(wallLengthMeters=metres.toString(),lengthMode="AUTO"))}} else null)
        }
        when(val d=detail) {
            is SicFormDetail.Sic17 -> Sic17Fields(d.state) {detail=SicFormDetail.Sic17(it)}
            is SicFormDetail.Sic23 -> Sic23Fields(d.state) {detail=SicFormDetail.Sic23(it)}
            is SicFormDetail.Sic18 -> {Sic18Fields(d.state) {detail=SicFormDetail.Sic18(it)}}
            is SicFormDetail.Sic19 -> Sic19Fields(d.state) {detail=SicFormDetail.Sic19(it)}
            is SicFormDetail.Sic20 -> Sic20Fields(d.state) {detail=SicFormDetail.Sic20(it.copy(lengthMode=if(it.wallLengthMeters!=d.state.wallLengthMeters) "MEASURED" else d.state.lengthMode))}
            is SicFormDetail.Sic21 -> Sic21Fields(d.state) {detail=SicFormDetail.Sic21(it)}
            is SicFormDetail.Sic22 -> {
                if(d.state.typeCode in setOf("4","5","6")) Text("Tipo histórico ${d.state.typeCode}: se conservará mientras no selecciones un tipo nuevo.")
                Sic22Fields(d.state) {detail=SicFormDetail.Sic22(it)}
            }
            else -> Text("Cargando ficha…")
        }
        detail?.let {d->
            val code=when(d) {is SicFormDetail.Sic17->"SIC-17";is SicFormDetail.Sic18->"SIC-18";is SicFormDetail.Sic19->"SIC-19";is SicFormDetail.Sic20->"SIC-20";is SicFormDetail.Sic21->"SIC-21";is SicFormDetail.Sic22->"SIC-22";is SicFormDetail.Sic23->"SIC-23"}
            val cls=when(d) {is SicFormDetail.Sic19->d.state.classCode;is SicFormDetail.Sic20->d.state.classCode;is SicFormDetail.Sic21->d.state.classCode;is SicFormDetail.Sic23->d.state.classCode;else->""}
            val type=when(d) {is SicFormDetail.Sic21->d.state.typeCode;else->""}
            LegacyInventoryPhotos(code,paths,categories,classCode=cls,typeCode=type,hasEnds=hasEnds,
                onPhotoMetadata={path,metadata->captures=captures+(path to metadata)},captureMetadata=captures) {p,c->paths=p;categories=c}
        }
        OutlinedTextField(observations,{observations=it},label={Text("OBSERVACIONES")},minLines=3,modifier=Modifier.fillMaxWidth())
        error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
        OutlinedButton(enabled=detail!=null && !saving,onClick={val d=detail ?: return@OutlinedButton;saving=true;scope.launch {
            try {EngineeringEdits.save(db,id,d,paths,categories,captures,expectedUpdatedAt=openedRevision,observations=observations,coordinateEdit=coordinateEdited,coordinate=coordinate,finalize=false,core=core);onBack()}catch(e:Exception){error=e.message}finally{saving=false}
        }}) {Text("Guardar borrador")}
        LegacyInventoryReviewButton(enabled=detail!=null && !saving && gpsError==null){review=true}
    }
    if(review) snapshot?.let {original->detail?.let {d->
        val errors=EngineeringEdits.reviewErrors(original,d,paths)+(core?.errors(hasEnds).orEmpty().map {com.tuempresa.inventariovial.validation.ValidationError(it.field,it.reason)})
        val request=com.tuempresa.inventariovial.model.form.InventorySaveRequest(original.record.sicCode,original.record.assetType,original.record.routeCode,original.record.roadbedCode,original.record.startPrCode,original.record.startDistanceM.toString(),original.record.endPrCode,original.record.endDistanceM?.toString(),original.record.sideCode,original.record.latitude ?: Double.NaN,original.record.longitude ?: Double.NaN,original.record.gpsAccuracyM?.toFloat(),original.record.surveyDate,observations,"",d,photoPaths=paths)
        LegacyRecordReviewDialog(captureSummary(request),paths.map {java.io.File(it)},com.tuempresa.inventariovial.core.CompletionResult(errors.map {com.tuempresa.inventariovial.core.ValidationIssue(it.field,it.message,com.tuempresa.inventariovial.core.ValidationIssue.Severity.BLOCKING_EXPORT)}),enabled=!saving,onDismiss={review=false},onConfirm={saving=true;scope.launch {
            try {EngineeringEdits.save(db,id,d,paths,categories,captures,expectedUpdatedAt=openedRevision,observations=observations,coordinateEdit=coordinateEdited,coordinate=coordinate,core=core);review=false;onBack()}catch(e:Exception){error=e.message;review=false}finally{saving=false}
        }})
    }}
}
