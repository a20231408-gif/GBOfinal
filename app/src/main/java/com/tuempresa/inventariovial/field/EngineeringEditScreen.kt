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
import com.tuempresa.inventariovial.*
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.model.form.*
import com.tuempresa.inventariovial.repository.EngineeringEdits
import kotlinx.coroutines.launch

@Composable
fun EngineeringEditScreen(id:String,onBack:()->Unit) {
    val context=LocalContext.current;val db=remember {InventoryDatabase.getInstance(context)};val scope=rememberCoroutineScope()
    var observations by remember(id){mutableStateOf("")}
    var coordinate by remember(id){mutableStateOf<com.tuempresa.inventariovial.location.GeoLocation?>(null)}
    var coordinateEdited by remember(id){mutableStateOf(false)}
    var gpsError by remember{mutableStateOf<String?>(null)}
    var openedRevision by remember(id) {mutableStateOf<Long?>(null)}
    var detail by remember(id) {mutableStateOf<SicFormDetail?>(null)}
    var paths by remember(id) {mutableStateOf<List<String>>(emptyList())}
    var categories by remember(id) {mutableStateOf<Map<String,String>>(emptyMap())}
    var captures by remember(id) {mutableStateOf<Map<String,com.tuempresa.inventariovial.camera.PhotoCaptureMetadata>>(emptyMap())}
    var error by remember {mutableStateOf<String?>(null)};var saving by remember {mutableStateOf(false)}
    LaunchedEffect(id) {runCatching {val s=requireNotNull(db.inventoryDao().snapshot(id));openedRevision=s.record.updatedAt;observations=s.record.observations.orEmpty();coordinate=s.record.let{r->if(r.latitude!=null && r.longitude!=null) com.tuempresa.inventariovial.location.GeoLocation(r.latitude,r.longitude,r.gpsAccuracyM?.toFloat() ?: Float.NaN,source=r.locationSource,timestamp=r.gpsTimestamp ?: 0) else null};detail=EngineeringEdits.form(s);paths=s.photos.sortedBy {it.photoIndex}.map {it.localPath};categories=s.photos.mapNotNull {p->p.photoCategory?.let {p.localPath to it}}.toMap()}.onFailure {error=it.message}}
    BackHandler(enabled=!saving,onBack=onBack)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        OutlinedButton(enabled=!saving,onClick=onBack) {Text("Volver")}
        Text("Editar ficha técnica",style=MaterialTheme.typography.headlineMedium)
        com.tuempresa.inventariovial.core.AutomaticSyncPanel("SIC",id,true)
        OptionalDeviceGps(coordinate,onChange={coordinate=it;coordinateEdited=true},onError={gpsError=it})
        when(val d=detail) {
            is SicFormDetail.Sic17 -> Sic17Fields(d.state) {detail=SicFormDetail.Sic17(it)}
            is SicFormDetail.Sic23 -> Sic23Fields(d.state) {detail=SicFormDetail.Sic23(it)}
            is SicFormDetail.Sic18 -> {Sic18Fields(d.state) {detail=SicFormDetail.Sic18(it)};CulvertPhotos(paths,categories,onPhotoMetadata={path,metadata->captures=captures+(path to metadata)}) {p,c->paths=p;categories=c}}
            is SicFormDetail.Sic19 -> Sic19Fields(d.state) {detail=SicFormDetail.Sic19(it)}
            is SicFormDetail.Sic20 -> Sic20Fields(d.state) {detail=SicFormDetail.Sic20(it.copy(lengthMode=if(it.wallLengthMeters!=d.state.wallLengthMeters) "MEASURED" else d.state.lengthMode))}
            is SicFormDetail.Sic21 -> Sic21Fields(d.state) {detail=SicFormDetail.Sic21(it)}
            is SicFormDetail.Sic22 -> {
                if(d.state.typeCode in setOf("4","5","6")) Text("Tipo histórico ${d.state.typeCode}: se conservará mientras no selecciones un tipo nuevo.")
                Sic22Fields(d.state) {detail=SicFormDetail.Sic22(it)}
            }
            else -> Text("Cargando ficha…")
        }
        if(detail!=null && detail !is SicFormDetail.Sic18) com.tuempresa.inventariovial.core.InventoryElementPhotos(paths,true,{paths=it},{path,metadata->captures=captures+(path to metadata)})
        OutlinedTextField(observations,{observations=it},label={Text("OBSERVACIONES")},minLines=3,modifier=Modifier.fillMaxWidth())
        error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
        Button(enabled=detail!=null && !saving && gpsError==null,onClick={val d=detail ?: return@Button;saving=true;scope.launch {
            try {EngineeringEdits.save(db,id,d,paths,categories,captures,expectedUpdatedAt=openedRevision,observations=observations,coordinateEdit=coordinateEdited,coordinate=coordinate);onBack()} catch(e:Exception){error=e.message} finally {saving=false}
        }},modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)) {Text("Guardar cambios")}
    }
}
