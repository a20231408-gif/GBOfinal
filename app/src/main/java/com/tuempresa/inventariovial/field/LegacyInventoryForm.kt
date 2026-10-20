package com.tuempresa.inventariovial.field

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.tuempresa.inventariovial.SectionTitle
import com.tuempresa.inventariovial.core.*
import com.tuempresa.inventariovial.location.*
import com.tuempresa.inventariovial.road.*
import java.io.File

/** Extracted from AssetFormScreen: same list, rows, order, sizes and controls. */
@Composable
fun LegacyInventoryFormScaffold(title:String,subtitle:String,onSaveDraft:()->Unit,onBack:()->Unit,
    draftNotice:String="",enabled:Boolean=true,draftEnabled:Boolean=enabled,draftErrors:@Composable ()->Unit={},content:LazyListScope.()->Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).testTag("legacy-inventory-form"),
        contentPadding = PaddingValues(top = 24.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            OutlinedButton(enabled=draftEnabled,onClick=onSaveDraft) {Text("Guardar borrador")}
            if(draftNotice.isNotBlank()) Text(draftNotice)
            draftErrors()
        }
        item {OutlinedButton(enabled=enabled,onClick=onBack) {Text("← Volver")}}
        item {
            Text(title,fontSize=30.sp,fontWeight=FontWeight.Bold)
            Text(subtitle,style=MaterialTheme.typography.bodyLarge)
        }
        content()
    }
}

/** Original AssetFormScreen GPS button, with its handlers supplied by the host. */
@Composable fun LegacyInitialGpsButton(locating:Boolean,onCapture:()->Unit,label:String="Actualizar GPS inicial") {
    OutlinedButton(enabled=!locating,onClick=onCapture) {Text(label)}
}

/** Permission/provider adapter only; presentation uses the original initial GPS button. */
@Composable fun LegacyInitialGpsCapture(onLocation:(GeoLocation)->Unit) {
    val context=LocalContext.current
    val provider=remember(context) {TabletLocationProvider(context)}
    var locating by remember {mutableStateOf(false)}
    var error by remember {mutableStateOf<String?>(null)}
    val notify by rememberUpdatedState(onLocation)
    fun capture() {locating=true;error=null;provider.getCurrentLocation({locating=false;notify(it)},{locating=false;error=it})}
    val permissions=rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {result->
        if(result.values.any {it})capture() else error="Debes permitir el acceso a la ubicación."
    }
    LegacyInitialGpsButton(locating,{
        if(ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED) capture()
        else permissions.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION))
    })
    error?.let {com.tuempresa.inventariovial.ErrorText(it)}
}

/** PR, metres and GNSS blocks extracted from AssetFormScreen, without another section card. */
@Composable fun LegacyLocationSection(reference:RoadReferenceData,config:MatchConfig,route:String,roadbed:String,
    location:GeoLocation?,pr:String,distance:String,source:String,endpoint:String,
    onSuggestion:(LocationSuggestion?)->Unit,onConfirm:(LocationSuggestion)->Unit,onEdit:()->Unit,
    onPr:(String)->Unit,onDistance:(String)->Unit,field:FieldController?=null,editable:Boolean=true) {
    Column {
        SectionTitle(if(endpoint=="inicio") "Ubicación inicio" else "Ubicación fin")
        val initial=endpoint=="inicio"
        ContractualPrField(value=pr,onValueChange=onPr,readOnly=!editable,
            label={Text(if(ContractualPrInput.catalog(reference.prs,route,roadbed).isEmpty())
                if(initial) "Kilómetro de progresiva inicial (PR)" else "Kilómetro de progresiva final (PR)"
                else if(initial) "Código PR inicial" else "Código PR final")},
            placeholder={Text("4 dígitos, ej. 0010")},modifier=Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        PrDistanceField(value=distance,onValueChange=onDistance,readOnly=!editable,
            label={Text(if(ContractualPrInput.catalog(reference.prs,route,roadbed).isEmpty())
                if(initial) "Metros desde el kilómetro inicial" else "Metros desde el kilómetro final"
                else if(initial) "Metros desde el PR inicial" else "Metros desde el PR final")},
            placeholder={Text(if(initial) "Ej. 125.50" else "Ej. 180.25")},modifier=Modifier.fillMaxWidth())
    }
}

/** Original AssetFormScreen footer, not a second workspace action design. */
@Composable fun LegacyInventoryReviewButton(enabled:Boolean=true,onReview:()->Unit) {
    Button(enabled=enabled,onClick=onReview,modifier=Modifier.fillMaxWidth().height(72.dp)) {
        Text("Revisar y finalizar",fontSize=20.sp,fontWeight=FontWeight.Bold)
    }
}

/** Extracted from SaveWarningsDialog; the host retains its own validation and persistence. */
@Composable fun LegacyRecordReviewDialog(fields:List<Pair<String,String>>,photos:List<File>,completion:CompletionResult,
    warnings:List<String> = emptyList(),enabled:Boolean=true,onConfirm:()->Unit,onDismiss:()->Unit,recordId:String?=null) {
    AlertDialog(onDismissRequest=onDismiss,title={Text("Verificar registro antes de guardar")},
        text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            recordId?.let {com.tuempresa.inventariovial.localfiles.RecordDeliveryChoice("SIC",it,false)}
            RecordReviewSection("Revisión SIC",fields,photos,completion,initiallyExpanded=true)
            warnings.forEach {Text("• $it")}
            Text("Puedes corregir los datos o guardar después de revisar estos avisos.")
        }},
        confirmButton={TextButton(enabled=enabled && completion.canFinalize,onClick=onConfirm) {Text("Confirmar y guardar")}},
        dismissButton={TextButton(onClick=onDismiss) {Text("Revisar")}})
}
