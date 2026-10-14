package com.tuempresa.inventariovial.field

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tuempresa.inventariovial.location.*
import com.tuempresa.inventariovial.validation.ContractualLocationInput

/** No permission request: a denied permission never blocks contractual capture. */
@Composable fun OptionalDeviceGps(fix:GeoLocation?,editable:Boolean=true,onChange:(GeoLocation?)->Unit,onError:(String?)->Unit={},route:String="",roadbed:String="") {
    val context=LocalContext.current
    val provider=remember(context){TabletLocationProvider(context)}
    var expanded by rememberSaveable {mutableStateOf(false)}
    var lat by remember(fix){mutableStateOf(fix?.latitude?.toString().orEmpty())}
    var lon by remember(fix){mutableStateOf(fix?.longitude?.toString().orEmpty())}
    var providerError by remember {mutableStateOf<String?>(null)}
    var manuallyEdited by remember {mutableStateOf(false)}
    var locating by remember {mutableStateOf(false)}
    val change by rememberUpdatedState(onChange)
    val report by rememberUpdatedState(onError)
    fun permitted()=ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED
    fun capture(){manuallyEdited=false;if(!permitted()){providerError="Ubicación no autorizada; puedes guardar sin GPS.";return}
        locating=true;provider.getCurrentLocation({locating=false;providerError=null;if(!manuallyEdited){report(null);change(it)}}, {locating=false;providerError="GPS no disponible; puedes guardar sin coordenadas."})}
    LaunchedEffect(Unit){if(editable && fix==null && permitted())capture()}
    fun manual(a:String,o:String){manuallyEdited=true;lat=a;lon=o;val error=ContractualLocationInput.optionalCoordinateError(a,o);report(error)
        if(error==null) change(if(a.isBlank() && o.isBlank()) null else GeoLocation(a.replace(',', '.').toDouble(),o.replace(',', '.').toDouble(),Float.NaN,source="MANUAL_COORDINATES",isRtkFixed=false))}
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        TextButton(onClick={expanded=!expanded}){Text("GPS del dispositivo (opcional)")}
        if(fix!=null)com.tuempresa.inventariovial.road.GnssRouteIndicatorText(fix,route,roadbed)
        if(expanded){
            OutlinedTextField(lat,{manual(it,lon)},enabled=editable,label={Text("Latitud")},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(lon,{manual(lat,it)},enabled=editable,label={Text("Longitud")},modifier=Modifier.fillMaxWidth())
            Text("Fuente: ${fix?.source ?: "Sin coordenadas"}")
            fix?.horizontalAccuracy?.takeIf{it.isFinite()}?.let{Text("Precisión disponible: $it m")}
            fix?.timestamp?.takeIf{it>0}?.let{Text("Hora: ${java.text.DateFormat.getDateTimeInstance().format(java.util.Date(it))}")}
            Text(if(fix?.source?.startsWith("EXTERNAL")==true) "Georreferenciación externa: evidencia histórica" else "Georreferenciación submétrica externa: pendiente",style=MaterialTheme.typography.bodySmall)
            if(editable) Row {TextButton(enabled=!locating,onClick={capture()}){Text("Actualizar posición")};TextButton(onClick={manuallyEdited=true;lat="";lon="";report(null);change(null)}){Text("Dejar sin coordenadas")}}
            ContractualLocationInput.optionalCoordinateError(lat,lon)?.let{Text(it,color=MaterialTheme.colorScheme.error)}
            providerError?.let{Text(it,style=MaterialTheme.typography.bodySmall)}
        }
    }
}
