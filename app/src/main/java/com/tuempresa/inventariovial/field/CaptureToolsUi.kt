package com.tuempresa.inventariovial.field

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.tuempresa.inventariovial.*
import com.tuempresa.inventariovial.camera.*
import com.tuempresa.inventariovial.location.GeoLocation
import com.tuempresa.inventariovial.road.*
import com.tuempresa.inventariovial.tracking.*
import com.tuempresa.inventariovial.viewmodel.InventoryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.Locale

@Composable
fun TrackCapturePanel(viewModel: InventoryViewModel,recordId: String?,sicCode: String,asset: String,route: String,
    roadbed: String,pr: String,distance: String,side: String?,location: GeoLocation?,
    onRecordId: (String)->Unit,onEndLocation: (GeoLocation)->Unit,segment: String="",direction: String="INCREASING",prSource: String="MANUAL",routeSource: String="MANUAL",suggestion: LocationSuggestion?=null) {
    val context=LocalContext.current;val scope=rememberCoroutineScope()
    val active by TrackCaptureService.activeRecordId.collectAsState()
    val message by TrackCaptureService.message.collectAsState()
    val reference by viewModel.field.reference.collectAsState()
    val settings by viewModel.field.settings.collectAsState()
    val points by remember(recordId) {
        if(recordId==null) kotlinx.coroutines.flow.flowOf(emptyList()) else viewModel.field.dao.observeTrack(recordId)
    }.collectAsState(initial=emptyList())
    val lengths by produceState(TrackLengths(0.0,null,0.0),points,reference,settings) {
        value=withContext(Dispatchers.Default) { TrackCalculator.lengths(points,reference,settings.matchConfig()) }
    }
    var error by remember { mutableStateOf<String?>(null) };var starting by remember { mutableStateOf(false) }
    fun start() {
        if(location==null) { error="Obtén primero una posición GNSS.";return }
        if(ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED) {
            error="El recorrido requiere permiso de ubicación precisa.";return
        }
        starting=true
        viewModel.field.startTrack(recordId,sicCode,asset,route,roadbed,pr,distance,side,location,
            {starting=false;onRecordId(it)},{starting=false;error=it},segment,direction,prSource,routeSource,suggestion)
    }
    val permissions=rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { start() }
    SectionTitle("Recorrido del elemento")
    if(active==recordId && recordId!=null) {
        Button(onClick={
            viewModel.field.stopTrack()
            scope.launch {
                withTimeoutOrNull(5_000) {TrackCaptureService.activeRecordId.first {it==null}}
                viewModel.field.dao.track(recordId).lastOrNull()?.let {
                    onEndLocation(GeoLocation(it.latitude,it.longitude,it.accuracy.toFloat(),it.altitude,timestamp=it.timestamp))
                }
            }
        }) {Text("FINALIZAR RECORRIDO")}
    } else Button(enabled=active==null && !starting,onClick={
        val needed=mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION)
        if(Build.VERSION.SDK_INT>=33) needed+=Manifest.permission.POST_NOTIFICATIONS
        permissions.launch(needed.toTypedArray())
    }) {Text(if(starting) "Iniciando…" else "INICIAR RECORRIDO")}
    if(active!=null && active!=recordId) Text("Otro registro tiene un recorrido activo.")
    Text("Puntos guardados: ${points.size}")
    fun meters(value:Double?)=value?.let {String.format(Locale.US,"%.2f m",it)} ?: "Sin coincidencia continua con el eje"
    Text("ESTIMACIÓN · Longitud sobre eje vial: ${meters(lengths.matchedRoadLength)}")
    Text("ESTIMACIÓN · Longitud trayectoria GNSS: ${meters(lengths.rawTrackLength)}")
    Text("ESTIMACIÓN · Distancia directa: ${meters(lengths.startEndStraightDistance)}")
    if(lengths.hasGaps) Text("Recorrido con interrupciones: la longitud GNSS solo suma los tramos observados. No se unen las pausas.")
    Text("Estas estimaciones no reemplazan la medición validada por el ingeniero.")
    message?.let {Text(it)};error?.let {ErrorText(it)}
}

@Composable
fun PhotoStampPanel(paths: List<String>,data: PhotoStampData,stampedPaths: Map<String,String>,photoCaptures: Map<String,PhotoCaptureMetadata> = emptyMap(),onStamped: (Map<String,String>)->Unit) {
    if(paths.isEmpty()) return
    Text("Las fotografías se entregan limpias; ubicación y fecha se conservan como metadatos.")

}
