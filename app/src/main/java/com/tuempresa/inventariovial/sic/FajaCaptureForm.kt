package com.tuempresa.inventariovial.sic

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.tuempresa.inventariovial.ChoiceSelector
import com.tuempresa.inventariovial.field.*

/** Independent official fields. This capture path never instantiates a location provider. */
@Composable
fun FajaCaptureForm(state:SicFieldState,editable:Boolean,notice:String?,onChange:(String,String)->Unit,
    onDraft:()->Unit,onBack:()->Unit,onSave:()->Unit,photoContent:@Composable ()->Unit={},exportContent:@Composable ()->Unit={}) {
    require(state.format in setOf(SicFieldFormat.SIC08,SicFieldFormat.SIC09))
    val context=LocalContext.current
    val preferences=remember(context) {SurveyPreferences(context)}
    var photosExpanded by remember {mutableStateOf(false)}
    LegacyInventoryFormScaffold(state.format.code,state.format.title,onDraft,onBack,notice.orEmpty(),enabled=editable) {
        item {
            Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Text("Ruta",style=MaterialTheme.typography.titleMedium)
                ChoiceSelector(preferences.projectRoutes.routeCodes,state.text("routeCode"),editable) {
                    onChange("routeCode",it)
                    onChange("_segment",preferences.projectRoutes.requireRoute(it).segmentCode)
                }
                ProjectRouteFields(state.text("routeCode"))
                if(state.text("routeCode") !in preferences.projectRoutes.routeCodes)
                    SicFieldInput(state.format.fields.single {it.key=="routeCode"},state.text("routeCode"),{onChange("routeCode",it)},editable)
                Text("Calzada",style=MaterialTheme.typography.titleMedium)
                val beds=com.tuempresa.inventariovial.catalog.SicCatalogRepository.roadbedCodes
                ChoiceSelector(beds+"Otro",state.text("roadbedCode"),editable) {onChange("roadbedCode",if(it=="Otro") "" else it)}
                if(state.text("roadbedCode") !in beds)
                    SicFieldInput(state.format.fields.single {it.key=="roadbedCode"},state.text("roadbedCode"),{onChange("roadbedCode",it)},editable)
            }
        }
        state.format.fields.filter {it.key !in setOf("routeCode","roadbedCode")}.forEach {field ->
            item {SicFieldInput(field,state.text(field.key),{onChange(field.key,it)},editable)}
        }
        item {
            TextButton(onClick={photosExpanded=!photosExpanded}) {Text(if(photosExpanded) "Ocultar fotografías" else "Fotografías")}
            if(photosExpanded) photoContent()
        }
        item {
            OutlinedTextField(state.text("observations"),{onChange("observations",it)},enabled=editable,
                label={Text("OBSERVACIONES")},minLines=3,modifier=Modifier.fillMaxWidth().testTag("sic-observations"))
        }
        item {Button(onClick=onSave,enabled=editable,modifier=Modifier.fillMaxWidth().height(64.dp)) {Text("Guardar registro")}}
        item {exportContent()}
    }
}
