package com.tuempresa.inventariovial.road

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.tuempresa.inventariovial.data.entity.InventoryRecordEntity
import com.tuempresa.inventariovial.field.FieldSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class VisualPosition(val meters:Double?,val warning:String,val manual:Boolean=false)
/** Local geometry and overrides never enter Room revisions or the delivery reference. */
class LocalVisualOrder(private val context:Context) {
    private val prefs=context.getSharedPreferences("visual_order_9",Context.MODE_PRIVATE)
    private val directory=File(context.filesDir,"visual_geometry_9")
    private val cache=mutableMapOf<String,KmzRoutePreview>()
    private val bundled by lazy {JsonRoadReferenceImporter().parse(context.assets.open("road_reference.json").bufferedReader().use{it.readText()})}
    fun save(preview:KmzRoutePreview) {
        val checked=KmzRouteParser.parseKml(preview.kml,preview.routeCode)
        val dest=File(directory,"${checked.sha256}.kml");check(directory.isDirectory || directory.mkdirs())
        if(!dest.isFile) {val temp=File.createTempFile("pending", ".kml",directory);try {temp.outputStream().use {it.write(checked.kml);it.fd.sync()};check(temp.renameTo(dest))} finally {temp.delete()}}
        check(prefs.edit().putString("route/${checked.routeCode}",checked.sha256).commit())
    }
    fun active(route:String):KmzRoutePreview?=runCatching {
        val hash=prefs.getString("route/${normalizedRoadCode(route)}",null) ?: return null
        require(hash.matches(Regex("[a-f0-9]{64}")))
        cache.getOrPut(hash){KmzRouteParser.parseKml(File(directory,"$hash.kml").readBytes(),route)}
    }.getOrNull()
    fun segments(route:String,roadbed:String):List<RoadSegment> {
        val imported=active(route)
        if(imported!=null)return cumulative(imported.segments,reversed(route))
        val all=bundled.segments.filter {normalizedRoadCode(it.routeCode)==normalizedRoadCode(route)}
        val matching=all.filter {normalizedRoadCode(it.roadbedCode)==normalizedRoadCode(roadbed)}
        return cumulative(if(matching.isNotEmpty())matching else all.filter {it.roadbedCode=="EJE"},reversed(route))
    }
    fun reverse(route:String,value:Boolean) {check(prefs.edit().putBoolean("reverse/$route",value).commit())}
    fun reversed(route:String)=prefs.getBoolean("reverse/$route",false)
    fun manual(id:String,meters:Double?) {require(meters==null || meters.isFinite()&&meters>=0);val e=prefs.edit();if(meters==null)e.remove("manual/$id") else e.putString("manual/$id",meters.toString());check(e.commit())}
    fun position(r:InventoryRecordEntity):VisualPosition {
        prefs.getString("manual/${r.id}",null)?.toDoubleOrNull()?.let {return VisualPosition(it,"Posición manual local · revisar",true)}
        val p=segments(r.routeCode,r.roadbedCode)
        if(p.isEmpty())return VisualPosition(null,"Sin geometría local · respaldo por PR/fecha")
        val lat=r.latitude ?: return VisualPosition(null,"Sin GPS · requiere revisión manual")
        val lon=r.longitude ?: return VisualPosition(null,"Sin GPS · requiere revisión manual")
        val config=FieldSettings.load(context).matchConfig()
        return locate(p,lat,lon,r.gpsAccuracyM ?: Double.NaN,config)
    }
    companion object {
        fun locate(segments:List<RoadSegment>,lat:Double,lon:Double,accuracy:Double,config:MatchConfig):VisualPosition {
            if(!GeoMath.validCoordinate(lat,lon) || !accuracy.isFinite() || accuracy<0 || accuracy>config.maxAccuracyM)return VisualPosition(null,"GPS impreciso · requiere revisión manual")
            val x=111195.08*kotlin.math.cos(Math.toRadians(lat));val y=111195.08
            val matches=segments.flatMap {segment->segment.points.zipWithNext().mapNotNull {(a,b)->
                val ax=(a.longitude-lon)*x;val ay=(a.latitude-lat)*y
                val dx=(b.longitude-a.longitude)*x;val dy=(b.latitude-a.latitude)*y;val length=dx*dx+dy*dy
                if(length<0.000001)null else {
                    val t=(-(ax*dx+ay*dy)/length).coerceIn(0.0,1.0)
                    (a.chainageM+t*(b.chainageM-a.chainageM)) to kotlin.math.hypot(ax+t*dx,ay+t*dy)
                }
            }}.filter {it.second<=config.maxDistanceToAxisM}.sortedBy {it.second}
            val match=matches.firstOrNull() ?: return VisualPosition(null,"GPS alejado del eje · requiere revisión manual")
            if(matches.drop(1).any {it.second-match.second<=maxOf(config.ambiguityDistanceM,accuracy) && kotlin.math.abs(it.first-match.first)>config.maxDistanceToAxisM*2})return VisualPosition(null,"Cruce o eje paralelo ambiguo · requiere revisión manual")
            return VisualPosition(match.first,"Orden estimado por GPS/KML · revisar sentido y segmentos")
        }
        /** Sum the line lengths in file order; disconnected gaps are never straight-line distances. */
        fun cumulative(segments:List<RoadSegment>,reverse:Boolean=false):List<RoadSegment> {
            var meters=0.0
            return (if(reverse)segments.reversed() else segments).map {segment->
                val points=if(reverse)segment.points.reversed() else segment.points
                segment.copy(points=points.mapIndexed {i,p->
                    if(i>0)meters+=GeoMath.distanceM(points[i-1].latitude,points[i-1].longitude,p.latitude,p.longitude)
                    p.copy(sequence=i,chainageM=meters)
                })
            }
        }
    }
}

@Composable fun LocalGeometryControls(route:String,onChanged:()->Unit) {
    val context=LocalContext.current;val scope=rememberCoroutineScope();val store=remember(context){LocalVisualOrder(context)}
    var preview by remember(route){mutableStateOf<KmzRoutePreview?>(null)}
    var message by remember(route){mutableStateOf<String?>(null)}
    var reversed by remember(route){mutableStateOf(store.reversed(route))}
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {uri->if(uri!=null)scope.launch {
        try {preview=withContext(Dispatchers.IO) {
            val bytes=context.contentResolver.openInputStream(uri)!!.use {input->val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192);while(true){val n=input.read(buffer);if(n<0)break;require(out.size()+n<=10*1024*1024);out.write(buffer,0,n)};out.toByteArray()}
            if(bytes.take(2)==listOf(80.toByte(),75.toByte()))KmzRouteParser.parseKmz(bytes,route) else KmzRouteParser.parseKml(bytes,route)
        }} catch(_:Exception){message="No se pudo importar un KML/KMZ válido (máximo 10 MiB)."}
    }}
    OutlinedButton(enabled=route.isNotBlank(),onClick={picker.launch(arrayOf("*/*"))}){Text("Importar KML/KMZ para orden local")}
    Row {Checkbox(reversed,{reversed=it;store.reverse(route,it);onChanged()});Text("Invertir sentido del orden local")}
    Text("Los segmentos siguen el orden del archivo y acumulan su longitud sin sumar los huecos. Revisa el sentido y los cruces. Las carpetas Drive conservan su identidad y número.")
    message?.let {Text(it)}
    preview?.let {p->AlertDialog(onDismissRequest={preview=null},title={Text("Geometría local · $route")},text={Text("${p.segments.size} segmentos · ${p.vertices} vértices. Se usará sin conexión para estimar el orden visual. No calibra PR ni cambia la georreferenciación contractual.")},confirmButton={TextButton(onClick={scope.launch {try {withContext(Dispatchers.IO){store.save(p)};message="Geometría local guardada";onChanged()}catch(_:Exception){message="No se pudo guardar"};preview=null}}){Text("Usar para orden local")}},dismissButton={TextButton(onClick={preview=null}){Text("Cancelar")}})}
}

@Composable fun ManualVisualPosition(r:InventoryRecordEntity,onChanged:()->Unit={}) {
    val context=LocalContext.current;val store=remember(context){LocalVisualOrder(context)}
    var input by remember(r.id){mutableStateOf("")};var expanded by remember(r.id){mutableStateOf(false)};var error by remember{mutableStateOf<String?>(null)}
    TextButton(onClick={expanded=!expanded}){Text("Revisar posición local manual")}
    if(expanded)Column {
        OutlinedTextField(input,{input=it},label={Text("Distancia acumulada sobre KML (m)")})
        Row {TextButton(onClick={val n=input.replace(',','.').toDoubleOrNull();if(n!=null&&n.isFinite()&&n>=0){store.manual(r.id,n);expanded=false;onChanged()}else error="Ingresa metros mayores o iguales a cero"}){Text("Aplicar")};TextButton(onClick={store.manual(r.id,null);expanded=false;onChanged()}){Text("Usar GPS")}}
        error?.let {Text(it)}
    }
}

@Composable fun LocalOperatorPosition(route:String) {
    val context=LocalContext.current;val store=remember(context){LocalVisualOrder(context)}
    var fix by remember(route){mutableStateOf<com.tuempresa.inventariovial.location.GeoLocation?>(null)}
    var roadbed by remember(route){mutableStateOf("CD")}
    com.tuempresa.inventariovial.ChoiceSelector(listOf("UC","UD","CD","A1","A2","EJE"),roadbed){roadbed=it}
    com.tuempresa.inventariovial.FinalLocationCapture(fix,{fix=it},"para orden local")
    fix?.let {gps->
        val segments=store.segments(route,roadbed)
        if(segments.isEmpty())Text("Sin eje para esta ruta/calzada: importa KML/KMZ o revisa la calzada")
        else {
            val result=LocalVisualOrder.locate(segments,gps.latitude,gps.longitude,gps.horizontalAccuracy.toDouble(),FieldSettings.load(context).matchConfig())
            Text("Operador sobre eje: ${result.meters?.let {"%.1f m".format(it)} ?: "sin posición fiable"} · ${result.warning}")
        }
    }
    GnssRouteIndicatorText(fix,route,roadbed)
}
