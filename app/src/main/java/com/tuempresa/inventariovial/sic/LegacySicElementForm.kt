package com.tuempresa.inventariovial.sic

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.tuempresa.inventariovial.ChoiceSelector
import com.tuempresa.inventariovial.FinalLocationCapture
import com.tuempresa.inventariovial.SectionTitle
import com.tuempresa.inventariovial.camera.PhotoCaptureMetadata
import com.tuempresa.inventariovial.core.*
import com.tuempresa.inventariovial.data.entity.SicExtensionEntity
import com.tuempresa.inventariovial.field.*
import com.tuempresa.inventariovial.location.GeoLocation
import com.tuempresa.inventariovial.road.*
import com.tuempresa.inventariovial.viewmodel.InventoryViewModel

/** Technical-content adapter. The actual root and common UI belong to the historical forms. */
@Composable fun LegacySicElementForm(vm:InventoryViewModel,state:SicFieldState,editable:Boolean,baseEditable:Boolean,busy:Boolean,
    loaded:Boolean,notice:String?,fix:GeoLocation?,endFix:GeoLocation?,paths:List<String>,categories:Map<String,String>,
    widthValues:Map<String,String>,widthId:String,widthComplete:Boolean,widthExpanded:Boolean,
    widthChildren:List<SicExtensionEntity>,trackRecordId:String?,
    onChange:(String,String)->Unit,onFix:(GeoLocation,Boolean)->Unit,onPhotos:(List<String>,Map<String,String>)->Unit,
    onMetadata:(String,PhotoCaptureMetadata)->Unit,onWidthChange:(String,String)->Unit,onWidthSelect:(String)->Unit,
    onExpandWidths:()->Unit,onTrackId:(String)->Unit,onDraft:()->Unit,onBack:()->Unit,onReview:()->Unit,
    exportContent:@Composable ()->Unit) {
    val context=LocalContext.current
    val survey=remember(context) {SurveyPreferences(context)}
    val reference by vm.field.reference.collectAsState()
    val settings by vm.field.settings.collectAsState()
    val format=state.format
    val faja=format==SicFieldFormat.SIC08
    val mutable=baseEditable && !busy
    val route=state.text("routeCode");val bed=state.text("roadbedCode")
    val segment=state.text("_segment").ifBlank {survey.projectRoutes.segment(route)}
    val direction=state.text("_direction").ifBlank {"INCREASING"}
    val source=state.text("_locationSource").ifBlank {"MANUAL"}
    val endSource=state.text("_endLocationSource").ifBlank {"MANUAL"}
    var suggestion by remember {mutableStateOf<LocationSuggestion?>(null)}
    var endSuggestion by remember {mutableStateOf<LocationSuggestion?>(null)}
    fun change(key:String,value:String) {if(mutable)onChange(key,value)}
    fun manual(key:String,value:String,end:Boolean=false) {
        change(key,value);change(if(end)"_endLocationSource" else "_locationSource",LocationSource.edited(if(end)endSource else source,if(end)endSuggestion!=null else suggestion!=null))
    }
    val title=when(format) {SicFieldFormat.SIC08,SicFieldFormat.SIC09->"Registrar Faja";SicFieldFormat.SIC13->"Registrar Punto Crítico";SicFieldFormat.SIC34->"Registrar Accidente";else->error("SIC-18A pertenece a Alcantarillas")}
    val subtitle=when(format) {SicFieldFormat.SIC08->"SIC-08 · SIC-09";SicFieldFormat.SIC09->"SIC-09 · Anchos históricos";SicFieldFormat.SIC13->"SIC-13 · Puntos críticos";SicFieldFormat.SIC34->"SIC-34 · Accidentes de tránsito";else->""}
    LegacyInventoryFormScaffold(title,subtitle,onDraft,onBack,notice.orEmpty(),enabled=loaded && !busy,draftEnabled=editable && loaded && !busy) {
        item {
            SurveyHeader(survey,segment,route,bed,direction,
                onSegment={change("_segment",it)},onRoute={change("routeCode",it);change("_routeSource",LocationSource.MANUAL_OVERRIDE);change("_locationSource",LocationSource.MANUAL_OVERRIDE);change("_endLocationSource",LocationSource.MANUAL_OVERRIDE)},
                onRoadbed={manual("roadbedCode",it)},onDirection={change("_direction",it)})
        }
        item {OptionalDeviceGps(if(state.text("_gpsExplicit")=="true" || state.text("_latitude").isNotBlank()) {
            val a=state.text("_latitude").toDoubleOrNull();val o=state.text("_longitude").toDoubleOrNull()
            if(a!=null && o!=null) GeoLocation(a,o,state.text("_accuracy").toFloatOrNull() ?: Float.NaN,source=state.text("_coordinateSource"),timestamp=state.text("_gpsTimestamp").toLongOrNull() ?: 0) else null
        } else fix,editable=mutable,route=route,roadbed=bed,onChange={gps->
            onChange("_latitude",gps?.latitude?.toString().orEmpty());onChange("_longitude",gps?.longitude?.toString().orEmpty())
            onChange("_coordinateSource",gps?.source ?: "CONTRACTUAL_PR_ONLY")
            onChange("_accuracy",gps?.horizontalAccuracy?.takeIf{it.isFinite()}?.toString().orEmpty())
            onChange("_gpsTimestamp",gps?.timestamp?.toString().orEmpty())
            onChange("_gpsExplicit","true")
            if(gps!=null) onFix(gps,false)
        },onError={onChange("_gpsError",it.orEmpty())})}
        item {HorizontalDivider()}
        item {
            LegacyLocationSection(reference,settings.matchConfig(),route,bed,fix,state.text("startPrCode"),state.text("startDistanceM"),source,"inicio",
                {suggestion=it},{found->
                    suggestion=found;found.suggestedRouteCode?.let {change("routeCode",it);change("_segment",survey.projectRoutes.segment(it))}
                    change("_routeSource",LocationSource.GNSS_CONFIRMED)
                    found.pr?.let {change("startPrCode",it.suggestedPrCode);change("startDistanceM",it.distanceText)}
                    change("_locationSource",if(found.pr==null)LocationSource.GNSS_CONFIRMED else LocationSource.GNSS_PR_CONFIRMED)
                },{change("_locationSource",LocationSource.MANUAL_OVERRIDE);change("_routeSource",LocationSource.MANUAL_OVERRIDE)},
                {manual("startPrCode",it)},{manual("startDistanceM",it)},vm.field,editable=mutable)
        }
        if(format.hasEnd) item {
            LegacyLocationSection(reference,settings.matchConfig(),route,bed,endFix,state.text("endPrCode"),state.text("endDistanceM"),endSource,"fin",
                {endSuggestion=it},{found->if(route==found.suggestedRouteCode) {
                    endSuggestion=found;found.pr?.let {change("endPrCode",it.suggestedPrCode);change("endDistanceM",it.distanceText)}
                    change("_endLocationSource",if(found.pr==null)LocationSource.GNSS_CONFIRMED else LocationSource.GNSS_PR_CONFIRMED)
                }},{change("_endLocationSource",LocationSource.MANUAL_OVERRIDE)},
                {manual("endPrCode",it,true)},{manual("endDistanceM",it,true)},editable=mutable)
        }
        item {HorizontalDivider()}
        item {
            Column {
                SectionTitle(when(format) {SicFieldFormat.SIC08->"Características de faja";SicFieldFormat.SIC09->"Ancho de faja";SicFieldFormat.SIC13->"Puntos críticos";else->"Accidentes de tránsito"})
                Text(format.code,style=MaterialTheme.typography.bodyLarge)
                format.fields.filter {it.key !in SicFieldRepository.sharedKeys && it.key!="description"}.forEach {field->
                    SicFieldInput(if(faja && field.key=="typeCode")field.copy(label="Tipo de faja") else field,state.text(field.key),{change(field.key,it)},mutable)
                }
                if(format==SicFieldFormat.SIC13) TechnicalCriterion("Ver criterio") {Text(SicFieldFormat.criticalClasses.find {it.code==state.text("classCode")}?.description.orEmpty())}
                if(format==SicFieldFormat.SIC34) TechnicalCriterion("Ver criterio") {Text(SicFieldFormat.levels.find {it.code==state.text("levelCode")}?.description.orEmpty())}
            }
        }
        if(faja) item {
            Column {
                SectionTitle("Ancho de faja");Text("SIC-09",style=MaterialTheme.typography.bodyLarge)
                Text("Anchos · ${if(widthComplete) "✓" else "Pendiente"}")
                if(widthChildren.size>1) ChoiceSelector(widthChildren.mapIndexed {i,_->"Medición de anchos ${i+1}"},"Medición de anchos ${widthChildren.indexOfFirst {it.recordId==widthId}+1}",!busy) {label->onWidthSelect(widthChildren[label.substringAfterLast(' ').toInt()-1].recordId)}
                if(!widthComplete) OutlinedButton(onClick=onExpandWidths) {Text("Completar anchos")}
                if(widthExpanded || widthValues.values.any {it.isNotBlank()} || widthComplete) SicFieldFormat.SIC09.fields.filter {it.key in setOf("usefulWidthM","totalWidthM")}.forEach {field->
                    SicFieldInput(field,widthValues[field.key].orEmpty(),{onWidthChange(field.key,it)},!widthComplete && !busy)
                }
                if(!widthComplete) Text("Puedes guardar la faja y completar anchos aquí después",style=MaterialTheme.typography.bodySmall)
            }
        }
        item {
            Column {
                SectionTitle("Registro");Text("Fecha: ${state.text("surveyDate")}")
                if(format.fields.any {it.key=="description"}) SicFieldInput(format.fields.single {it.key=="description"},state.text("description"),{change("description",it)},mutable)
            }
        }
        item {LegacyInventoryPhotos(format.code,paths,categories,editable=mutable,onPhotoMetadata=onMetadata,onChange=onPhotos)}
        item {OutlinedTextField(state.text("observations"),{change("observations",it)},enabled=mutable,label={Text("OBSERVACIONES")},minLines=3,modifier=Modifier.fillMaxWidth())}
        if(editable) item {LegacyInventoryReviewButton(loaded && !busy,onReview)}
        item {if(busy)LinearProgressIndicator(Modifier.fillMaxWidth());exportContent()}
    }
}
