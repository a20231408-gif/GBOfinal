package com.tuempresa.inventariovial.road

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.tuempresa.inventariovial.location.GeoLocation
import com.tuempresa.inventariovial.field.FieldSettings
import java.util.Locale

data class GnssRouteReading(val accuracyM:Double?,val distanceM:Double?,val reason:String)

/** Display-only estimate. Never persists projection, PR, numbering or ordering. */
object GnssRouteIndicators {
    fun evaluate(fix:GeoLocation?,segments:List<RoadSegment>,config:MatchConfig,now:Long,maxAgeMs:Long=30_000):GnssRouteReading {
        if(fix==null)return GnssRouteReading(null,null,"Sin posición GNSS")
        if(!Regex("GNSS|GPS|RTK").containsMatchIn(fix.source.uppercase(Locale.ROOT)) || !GeoMath.validCoordinate(fix.latitude,fix.longitude))
            return GnssRouteReading(null,null,"Las coordenadas manuales no acreditan precisión GNSS")
        val accuracy=fix.horizontalAccuracy.toDouble().takeIf {it.isFinite() && it>0}
            ?: return GnssRouteReading(null,null,"Precisión GNSS no disponible")
        if(fix.timestamp<=0 || now<fix.timestamp || now-fix.timestamp>maxAgeMs)return GnssRouteReading(accuracy,null,"Posición antigua o sin hora fiable: actualiza GPS")
        if(accuracy>config.maxAccuracyM)return GnssRouteReading(accuracy,null,"Precisión insuficiente para estimar distancia al eje")
        if(segments.isEmpty())return GnssRouteReading(accuracy,null,"Sin geometría local de la ruta y calzada seleccionadas")
        val position=LocalVisualOrder.locate(segments,fix.latitude,fix.longitude,accuracy,config)
        if(position.meters==null)return GnssRouteReading(accuracy,null,position.warning)
        val match=RoadMatcher(RoadReferenceData(segments=segments),config).match(fix.latitude,fix.longitude,accuracy)
            ?: return GnssRouteReading(accuracy,null,"Coincidencia ambigua o fuera del umbral del eje")
        return GnssRouteReading(accuracy,match.distanceToRoadAxisM,"Estimación sobre eje local; no certifica precisión submétrica")
    }
}

@Composable fun GnssRouteIndicatorText(fix:GeoLocation?,route:String,roadbed:String) {
    val context=LocalContext.current
    val store=remember(context){LocalVisualOrder(context)}
    var now by remember{mutableLongStateOf(System.currentTimeMillis())}
    LaunchedEffect(fix){while(true){now=System.currentTimeMillis();kotlinx.coroutines.delay(1000)}}
    val settings=FieldSettings.load(context)
    val reading=GnssRouteIndicators.evaluate(fix,if(route.isBlank() || roadbed.isBlank())emptyList() else store.segments(route,roadbed),settings.matchConfig(),now,settings.qualityConfig().maxAgeMs)
    reading.accuracyM?.let{Text(String.format(Locale.US,"Precisión GNSS registrada: ±%.1f m",it))}
    reading.distanceM?.let{Text(String.format(Locale.US,"Distancia estimada a la ruta: %.1f m",it))}
    Text(reading.reason)
}
