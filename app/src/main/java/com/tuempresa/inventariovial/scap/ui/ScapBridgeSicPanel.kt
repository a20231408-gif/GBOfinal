package com.tuempresa.inventariovial.scap.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tuempresa.inventariovial.catalog.SicCatalogRepository
import com.tuempresa.inventariovial.road.ProjectRouteCatalog
import com.tuempresa.inventariovial.scap.data.ScapInspectionSnapshot
import com.tuempresa.inventariovial.scap.domain.*
import com.tuempresa.inventariovial.supplementary.FieldKind

internal fun sicMapping(s:ScapInspectionSnapshot,format:String)=when(format) {
    "SIC-17" -> ScapToSicMapper.mapToSic17(s)
    "SIC-17A" -> ScapToSicMapper.mapToSic17A(s)
    else -> ScapToSicMapper.mapToSic17B(s)
}

@Composable internal fun ScapBridgeSicPanel(s:ScapInspectionSnapshot,c:ScapController,editable:Boolean) {
    var open by rememberSaveable(s.inspection.id) { mutableStateOf<String?>("SIC-17") }
    val overrides by c.overrides.collectAsState()
    val destination by c.destination.collectAsState()
    LaunchedEffect(destination){destination?.second?.takeIf{it.section=="SIC"}?.let{route->
        open=route.repeatableItemId.takeIf{it in BridgeSicContract.formats} ?: "SIC-17"
    }}
    Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text("DATOS PARA SIC",style=MaterialTheme.typography.titleLarge,color=MaterialTheme.colorScheme.primary)
        BridgeSicTabs(open ?: "SIC-17"){open=it}
        open?.let { format -> key(s.inspection.id,format) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text(when(format) {"SIC-17A"->"SIC-17A · DATOS COMPLEMENTARIOS DEL PUENTE";"SIC-17B"->"SIC-17B · CARACTERÍSTICAS DEL PUENTE";else->"SIC-17 · PUENTES"},style=MaterialTheme.typography.titleLarge)
            if(!editable) Text("Reabre la inspección para editar los datos SIC.")
            if(format=="SIC-17") SicLocation(s,c,editable)
            val mapping=sicMapping(s,format)
            val prefix=BridgeSicContract.prefix(format)
            val overlayPrefix="${s.inspection.id}/${prefix.removeSuffix(".")}/"
            val overlay=overrides.filterKeys{it.startsWith(overlayPrefix)}.mapKeys{it.key.removePrefix(overlayPrefix)}
            val effective=mapping.fields.associate{it.sicField to it.value}+overlay
            BridgeSicContract.fields(format,effective).filter { it.key !in BridgeSicContract.unresolved(format) && (format!="SIC-17" || it.key !in BridgeSicContract.locationKeys) }.forEach { field ->
                ScapFieldTarget(format,field.key) {
                if(format=="SIC-17" && field.key=="surveyDate") {
                    Text("Fecha de la inspección actual. No corresponde a la última inspección anterior.")
                    if(s.values.any{it.ownerId=="inspection" && it.key=="sic17.surveyDate" && it.source=="CAPTURE_DATE_DEFAULT"})
                        Text("Se propone la fecha local de captura; revísala y corrígela si corresponde.",style=MaterialTheme.typography.bodySmall)
                }
                if(format=="SIC-17B") when(field.key) {
                    "wearingSurface" -> Text("TABLERO Y SUPERESTRUCTURA",style=MaterialTheme.typography.titleMedium)
                    "abutmentElevationCode" -> { Text("ESTRIBOS",style=MaterialTheme.typography.titleMedium);Text("Clasificación SIC global revisada; no se elige un estribo SCAP automáticamente.") }
                    "pierElevationCode" -> { Text("PILARES",style=MaterialTheme.typography.titleMedium);Text("Clasificación SIC global revisada; selecciona 0 — No aplica si corresponde a ausencia de pilares.") }
                    "comments" -> Text("COMENTARIOS SIC-17B",style=MaterialTheme.typography.titleMedium)
                }
                val raw=effective[field.key].orEmpty()
                val mapped=mapping.fields.lastOrNull{it.sicField==field.key}
                mapped?.notes?.takeIf{it.startsWith("REVISAR:")}?.let{Text(it,color=MaterialTheme.colorScheme.error)}
                val inherited=mapped!=null && !mapped.scapField.startsWith(prefix) && field.key !in overlay
                if(inherited) {
                    Text("${field.label}: $raw")
                    Text("Tomado de SCAP · ${mapped!!.scapField}",style=MaterialTheme.typography.bodySmall)
                    if(editable) TextButton(onClick={c.editSic(s.inspection.id,format,field.key,raw)}) { Text("Revisar valor SIC · ${field.label}") }
                } else {
                    val error=raw.isNotBlank() && !BridgeSicContract.valid(field,if(field.key=="wearingSurface") raw.substringBefore(" - ") else raw)
                    if(field.kind==FieldKind.CODE || field.kind==FieldKind.DESCRIBED_CODE) {
                        val selected=if(field.key=="wearingSurface") overlay[field.key] ?: s.values()["sic17b.wearingSurfaceDescription"] ?: s.sic17b?.wearingSurface?.takeIf{it.contains(" - ")} ?: field.options.singleOrNull{it.substringBefore(" - ")==raw} ?: raw
                            else field.options.find{it.substringBefore(" - ")==raw} ?: raw
                        if(field.key in setOf("structuralConditionCode","functionalConditionCode")) com.tuempresa.inventariovial.field.CatalogConditionSelector(field.label,raw,field.options,editable) {c.editSic(s.inspection.id,format,field.key,it)}
                        else Box(Modifier.testTag("sic-field-${field.key}")) { ScapChoice(field.label,selected,field.options,editable) {c.editSic(s.inspection.id,format,field.key,if(field.key=="wearingSurface") it else it.substringBefore(" - "))} }
                        if(field.key=="wearingSurface") Text("El documento fuente repite 3 para Concreto pobre y Acero. Se conserva el código y la descripción elegida, sin renumerar.",style=MaterialTheme.typography.bodySmall)
                        if(error) Text("${field.label}: revisa el código.",color=MaterialTheme.colorScheme.error)
                    } else OutlinedTextField(raw,{c.editSic(s.inspection.id,format,field.key,it)},label={Text(field.label)},enabled=editable,isError=error,
                        singleLine=field.key!="comments",minLines=if(field.key=="comments") 3 else 1,modifier=Modifier.fillMaxWidth().scapFocus().testTag("sic-input-$format-${field.key}"),
                        keyboardOptions=KeyboardOptions(keyboardType=when(field.kind) {FieldKind.INTEGER,FieldKind.YEAR->KeyboardType.Number;FieldKind.DECIMAL->KeyboardType.Decimal;else->KeyboardType.Text}),
                        supportingText={if(error) Text("Valor inválido; revisa formato y unidad.") else if(field.key=="surveyDate") Text("dd/mm/aaaa")})
                    s.values()[prefix+"override."+field.key]?.takeIf{it.startsWith("SCAP_OVERRIDE:")}?.let {
                        Text("Origen: SCAP · valor SIC revisado",style=MaterialTheme.typography.bodySmall)
                        if(editable) TextButton(onClick={c.restoreSic(s.inspection.id,format,field.key)}) { Text("Restaurar SCAP · ${field.label}") }
                    }
                }
                }
            }
            Text("Los cambios SIC se guardan localmente. Espera Guardado local antes de exportar.",style=MaterialTheme.typography.bodySmall)
            var diagnostic by remember {mutableStateOf(false)}
            TextButton(onClick={diagnostic=!diagnostic}) {Text("Diagnóstico técnico · $format")}
            if(diagnostic) {
                Text(BridgeSicContract.SOURCE)
                BridgeSicContract.unresolved(format).values.forEach {Text(it)}
                if(format=="SIC-17A") Text("SIC-17A usa latitud/longitud WGS84. UTM SCAP no se copia ni convierte sin transformación validada. Vías de tránsito no se interpreta como carriles.")
            }
            }
        } }
    }
}

@Composable internal fun BridgeSicTabs(selected:String,onSelect:(String)->Unit) {
    ScrollableTabRow(selectedTabIndex=BridgeSicContract.formats.indexOf(selected).coerceAtLeast(0),edgePadding=0.dp) {
        BridgeSicContract.formats.forEach {format->Tab(selected==format,{onSelect(format)},text={Text(format)})}
    }
}

@Composable private fun SicLocation(s:ScapInspectionSnapshot,c:ScapController,editable:Boolean) {
    val r=s.roadRecord
    // Automatically inherited draft projections must not mask a newer linked SCAP location.
    val draft=s.values.filter{it.ownerId=="inspection" && it.key.startsWith("sicLocation.") && it.source!=ScapSicLocation.INHERITED}
        .associate{it.key.removePrefix("sicLocation.") to it.value}
    val complete=r!=null && r.routeCode.isNotBlank() && r.roadbedCode.isNotBlank() && r.startPrCode.isNotBlank()
    var editing by rememberSaveable(s.inspection.id) {mutableStateOf(!complete)}
    var route by rememberSaveable(s.inspection.id) {mutableStateOf(draft["routeCode"] ?: r?.routeCode.orEmpty())}
    var roadbed by rememberSaveable(s.inspection.id) {mutableStateOf(draft["roadbedCode"] ?: r?.roadbedCode.orEmpty().ifBlank{s.values()["sic17.roadbedCode"].orEmpty()})}
    var pr by rememberSaveable(s.inspection.id) {mutableStateOf(draft["startPrCode"] ?: r?.startPrCode.orEmpty())}
    var distance by rememberSaveable(s.inspection.id) {mutableStateOf(draft["startDistanceM"] ?: if(r?.startPrCode.isNullOrBlank()) "" else r!!.startDistanceM.toString())}
    var endPr by rememberSaveable(s.inspection.id) {mutableStateOf(draft["endPrCode"] ?: r?.endPrCode.orEmpty())}
    var endDistance by rememberSaveable(s.inspection.id) {mutableStateOf(draft["endDistanceM"] ?: r?.endDistanceM?.toString().orEmpty())}
    var dirty by rememberSaveable(s.inspection.id){mutableStateOf(false)}
    fun refreshEditor(){
        route=draft["routeCode"] ?: r?.routeCode.orEmpty()
        roadbed=draft["roadbedCode"] ?: r?.roadbedCode.orEmpty().ifBlank{s.values()["sic17.roadbedCode"].orEmpty()}
        pr=draft["startPrCode"] ?: r?.startPrCode.orEmpty()
        distance=draft["startDistanceM"] ?: if(r?.startPrCode.isNullOrBlank()) "" else r!!.startDistanceM.toString()
        endPr=draft["endPrCode"] ?: r?.endPrCode.orEmpty()
        endDistance=draft["endDistanceM"] ?: r?.endDistanceM?.toString().orEmpty()
    }
    LaunchedEffect(r,draft){if(!dirty)refreshEditor()}
    fun save(key:String,value:String){dirty=true;c.submit{it.setSicLocationDraft(s.inspection.id,key,value)}}
    val context=LocalContext.current
    val routes=remember(context) {ProjectRouteCatalog.load(context).routeCodes}
    Text("UBICACIÓN SIC",style=MaterialTheme.typography.titleMedium)
    if(complete) {
        Text("${r!!.routeCode} · ${r.roadbedCode} · PR ${r.startPrCode} + ${r.startDistanceM} m")
        Text(when(r.locationSource){
            ScapSicLocation.INHERITED->"Heredado de kilometraje SCAP · pendiente de revisión SIC"
            ScapSicLocation.CONFIRMED->"Ubicación confirmada manualmente en SIC"
            else->"Registro vial vinculado · pendiente de revisión SIC"
        },style=MaterialTheme.typography.bodySmall)
        if(editable) TextButton(onClick={if(!dirty)refreshEditor();editing=!editing}) {Text("Modificar ubicación SIC")}
    }
    ScapSicLocation.discrepancy(s)?.let {message->
        Text(message,color=MaterialTheme.colorScheme.error,modifier=Modifier.testTag("sic-location-discrepancy"))
        if(editable) {
            if(ScapSicLocation.parse(s.values()["progressive"])!=null && s.values()["route"].orEmpty().let{it.isNotBlank() && it==r?.routeCode})
                TextButton(onClick={c.submit{it.useScapProgressive(s.inspection.id)};dirty=false;editing=false}){Text("Usar kilometraje SCAP")}
            TextButton(onClick={c.navigate(ScapFieldRoute("A",fieldKey="progressive"))}){Text("Revisar kilometraje en sección A")}
        }
    }
    if(editing) {
        Text("Si el PR aún no está disponible, ingrésalo manualmente. Puedes guardar una ubicación incompleta.",style=MaterialTheme.typography.bodySmall)
        ScapChoice("Ruta",route,routes,editable) {route=it;save("routeCode",it)}
        ScapChoice("Calzada",roadbed,SicCatalogRepository.roadbedCodes,editable) {roadbed=it;save("roadbedCode",it)}
        SicLocationNumber("Código PR",pr,false,editable){pr=it;save("startPrCode",it)}
        SicLocationNumber("Distancia desde PR (m)",distance,true,editable){distance=it;save("startDistanceM",it)}
        Text("Ubicación final del puente",style=MaterialTheme.typography.titleMedium)
        SicLocationNumber("Código PR final",endPr,false,editable){endPr=it;save("endPrCode",it)}
        SicLocationNumber("Distancia final desde PR (m)",endDistance,true,editable){endDistance=it;save("endDistanceM",it)}
        OutlinedButton(enabled=editable,onClick={c.submit{it.saveSicLocation(s.inspection.id,route,roadbed,pr,distance,endPr,endDistance)}}) {Text("Guardar ubicación SIC")}
    } else if(r?.endPrCode.isNullOrBlank() && editable) OutlinedButton(onClick={editing=true}) {Text("Completar ubicación final SIC")}
}

@Composable private fun SicLocationNumber(label:String,value:String,decimal:Boolean,editable:Boolean,change:(String)->Unit) {
    if(decimal) com.tuempresa.inventariovial.field.PrDistanceField(value,change,label={Text(label)},enabled=editable)
    else com.tuempresa.inventariovial.field.ContractualPrField(value,change,label={Text(label)},enabled=editable)
}
