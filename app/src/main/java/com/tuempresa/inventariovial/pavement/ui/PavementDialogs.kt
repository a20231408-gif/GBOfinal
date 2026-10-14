package com.tuempresa.inventariovial.pavement.ui

import android.content.Context
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.tuempresa.inventariovial.pavement.*
import com.tuempresa.inventariovial.road.ProjectRouteCatalog
import org.json.JSONObject
import kotlin.math.*

internal object PavementTechnicalText {
    private var catalog: JSONObject? = null
    fun measurement(context:Context,code:Int):String {
        val root=catalog?:context.assets.open("pavement/catalogo_danos.json").bufferedReader().use{JSONObject(it.readText())}.also{catalog=it}
        val items=root.getJSONArray("danos")
        val item=(0 until items.length()).map{items.getJSONObject(it)}.first{it.getInt("codigo")==code}
        return "Manual, pág. ${item.getJSONObject("fuente").getInt("pagina_inicio")}:\n"+item.getString("medicion_texto_fuente")
    }
    fun severity(context: Context, code: Int, severity: String?): String {
        val root = catalog ?: context.assets.open("pavement/catalogo_danos.json").bufferedReader().use { JSONObject(it.readText()) }.also { catalog = it }
        val damages = root.getJSONArray("danos")
        for (i in 0 until damages.length()) {
            val item = damages.getJSONObject(i)
            if (item.getInt("codigo") != code) continue
            val levels = item.getJSONObject("severidad").getJSONArray("niveles")
            for (j in 0 until levels.length()) if (levels.getJSONObject(j).getString("codigo_manual") == PavementSeverity.canonical(severity)) return levels.getJSONObject(j).getString("descripcion")
        }
        return "Consulta las reglas específicas de medición en el manual."
    }
}

@Composable internal fun SampleDialog(s: PavementSampleEntity, feedback: String, dismiss: () -> Unit, save: (PavementSampleEntity) -> Unit) {
    val context=LocalContext.current
    val projectRoutes=remember(context){ProjectRouteCatalog.load(context)}
    var ruta by rememberSaveable { mutableStateOf(s.ruta) }
    var tramo by rememberSaveable { mutableStateOf(s.tramo.ifBlank { projectRoutes.find(ruta)?.segmentCode.orEmpty() }) }
    var carril by rememberSaveable { mutableStateOf(s.carril) }; var calzada by rememberSaveable { mutableStateOf(s.calzada) }
    var start by rememberSaveable { mutableStateOf(s.prInicial) }
    var width by rememberSaveable { mutableStateOf(s.anchoM.toString()) }; var length by rememberSaveable { mutableStateOf(s.largoM.toString()) }
    var rows by rememberSaveable { mutableStateOf(s.slabRows.toString()) }; var cols by rememberSaveable { mutableStateOf(s.slabColumns.toString()) }
    var slabWidth by rememberSaveable { mutableStateOf(PavementHeader.slabWidth(s).toString()) }
    var slabLength by rememberSaveable { mutableStateOf(PavementHeader.slabLength(s).toString()) }
    val rigid = s.surface == "RIGID"
    val geometricLength = if (rigid) (rows.toIntOrNull() ?: 0) * (slabLength.replace(',', '.').toDoubleOrNull() ?: 0.0) else length.replace(',', '.').toDoubleOrNull() ?: 0.0
    val end = runCatching { PavementChainage.format(PavementChainage.parse(start) + geometricLength) }.getOrDefault("Revisa progresiva y dimensiones")
    var laneWidth by rememberSaveable { mutableStateOf(s.laneWidthM?.toString().orEmpty()) }
    var sampleNumber by rememberSaveable { mutableStateOf(s.muestra.toString()) }; var error by rememberSaveable { mutableStateOf("") }
    EditorDialog("Cabecera de muestra", dismiss, {
        runCatching {
            val w = if (rigid) cols.toInt() * number(slabWidth) else number(width)
            val l = geometricLength
            require(w > 0 && l > 0) { "Ancho y largo positivos." }
            val r = rows.toInt(); val c = cols.toInt()
            if (s.surface == "RIGID") require(r in 1..100 && c in 1..100 && r*c <= 1000) { "Grilla inválida (máximo 1000 losas)." }
            save(PavementHeader.normalize(s.copy(tramo = tramo.trim(), ruta = ruta.trim(), carril = carril.trim(), calzada = calzada.trim(),
                prInicial = start, prFinal = end, anchoM = w, largoM = l, slabRows = r, slabColumns = c,
                slabWidthM = if (rigid) number(slabWidth) else null, slabLengthM = if (rigid) number(slabLength) else null,
                fecha = s.fecha, laneWidthM = laneWidth.takeIf { it.isNotBlank() }?.let { number(it).also { n -> require(n > 0) } }, muestra = sampleNumber.toInt())))
        }.onFailure { error = it.message ?: "Revisa los campos." }
    }) {
        Text("Inspeccionado: ${s.inspectorNombre}")
        if(feedback.isNotBlank()) Text(feedback,color=MaterialTheme.colorScheme.error)
        Choice("Ruta",ruta,projectRoutes.routeCodes,{it}) { selected -> ruta=selected; tramo=projectRoutes.requireRoute(selected).segmentCode }
        OutlinedTextField(tramo,{},label={Text("Tramo del proyecto")},readOnly=true,enabled=false,modifier=Modifier.fillMaxWidth())
        Field("Unidad de inspección · Muestra", sampleNumber) { sampleNumber = it }
        Choice("Carril",PavementHeader.lane(carril),PavementHeader.lanes,{it}) { carril = it }
        Choice("Calzada",calzada,PavementHeader.roadbeds,{it}) { calzada = it }
        Field("Progresiva inicial (00+000 o metros)", start) { start = it }
        Text(runCatching { "Inicio: ${PavementChainage.format(PavementChainage.parse(start))}" }.getOrDefault("Ingresa una progresiva válida"))
        if (rigid) {
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) { Field("Filas longitudinales",rows){rows=it}; Field("Largo de losa (m)",slabLength){slabLength=it} }
                Column(Modifier.weight(1f)) { Field("Columnas transversales",cols){cols=it}; Field("Ancho de losa (m)",slabWidth){slabWidth=it} }
            }
            Text("Total: ${(rows.toIntOrNull()?:0)*(cols.toIntOrNull()?:0)} losas")
            Text("Longitud = $rows filas × $slabLength m = ${PavementDisplay.number(geometricLength)} m")
        } else {
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) { Field("Ancho de muestra (m)",width){width=it} }
                Column(Modifier.weight(1f)) { Field("Longitud de unidad (m)",length){length=it} }
            }
        }
        OutlinedTextField(end,{},label={Text("Progresiva final · automática")},readOnly=true,modifier=Modifier.fillMaxWidth())
        Field("Ancho carril (m)", laneWidth) { laneWidth = it }; Text("Fecha de creación: ${PavementHistory.dateLabel(s.fecha)}")
        if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
    }
}

private fun number(value: String): Double = value.replace(',', '.').toDouble().also { require(it.isFinite()) { "Número no válido." } }

@Composable internal fun DamageDialog(d: PavementDamageEntity, feedback: String, dismiss: () -> Unit, save: (PavementDamageEntity) -> Unit) {
    var x1 by rememberSaveable { mutableStateOf(d.x1?.toString().orEmpty()) }; var y1 by rememberSaveable { mutableStateOf(d.y1?.toString().orEmpty()) }
    var x2 by rememberSaveable { mutableStateOf(d.x2?.toString().orEmpty()) }; var y2 by rememberSaveable { mutableStateOf(d.y2?.toString().orEmpty()) }
    var diameter by rememberSaveable { mutableStateOf(d.diameterM?.toString().orEmpty()) }
    var depth by rememberSaveable { mutableStateOf(d.depthMm?.toString().orEmpty()) }
    var holeArea by rememberSaveable { mutableStateOf(d.areaM2?.toString().orEmpty()) }
    var measure by rememberSaveable { mutableStateOf((if(d.geometry=="AREA")d.areaM2 else d.lengthM)?.toString().orEmpty()) }; var crack by rememberSaveable { mutableStateOf(d.crackWidthMm?.toString().orEmpty()) }
    var observation by rememberSaveable { mutableStateOf(d.observation.orEmpty()) }; var slabs by rememberSaveable { mutableStateOf(d.slabs) }
    var severity by rememberSaveable { mutableStateOf(PavementSeverity.canonical(d.severity)) }; var error by rememberSaveable { mutableStateOf("") }
    val definition = PavementCatalog.forSurface(PavementSurface.valueOf(d.surface)).first { it.code == d.code }
    var localized by rememberSaveable {mutableStateOf(PavementSlabGeometry.localized(d))}
    EditorDialog("${d.code} · ${d.name}", dismiss, {
        runCatching {
            val geometric = d.geometry in listOf("AREA", "LINEAR", "POTHOLE") || localized
            val two = d.geometry in listOf("AREA", "LINEAR") || localized && PavementSlabGeometry.mark(d.code)!=SlabMark.POINT
            val a = if (geometric) number(x1) else null; val b = if (geometric) number(y1) else null
            val c = if (two) number(x2) else null; val e = if (two) number(y2) else null
            val w = if (two) abs(c!!-a!!) else null; val l = if (two) abs(e!!-b!!) else null
            val override = measure.takeIf { it.isNotBlank() }?.let { number(it) }
            save(d.copy(severity = severity, x1=a, y1=b, x2=c, y2=e,
                diameterM = if (d.geometry == "POTHOLE") number(diameter) else null,
                widthM=w, lengthM=if (d.geometry=="LINEAR") override ?: hypot(w!!,l!!) else l,
                areaM2=if (d.geometry=="AREA") override ?: w!!*l!! else if(d.geometry=="POTHOLE")holeArea.takeIf { it.isNotBlank() }?.let(::number) else null,
                quantity=if(d.geometry=="POTHOLE") 1.0 else null,
                crackWidthMm=if(PavementDamageFields.crackWidth(d.code))crack.takeIf { it.isNotBlank() }?.let { number(it) } else d.crackWidthMm, observation=observation, slabs=slabs,
                depthMm=if(PavementDamageFields.depth(d.code))depth.takeIf { it.isNotBlank() }?.let { number(it) } else d.depthMm))
        }.onFailure { error = it.message ?: "Revisa los campos." }
    }) {
        if(feedback.isNotBlank()) Text(feedback,color=MaterialTheme.colorScheme.error)
        if (definition.severities.isNotEmpty()) Choice("Severidad", severity ?: definition.severities.first(), definition.severities, { PavementSeverity.label(it) }) { severity = it }
        if(d.geometry=="SLABS") {
            Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){Switch(localized,{localized=it});Text("Localizar dentro de las losas indicadas")}
            Text("Croquis ${PavementSlabGeometry.mark(d.code).name}: coordenadas en metros de la muestra. La cantidad contractual sigue siendo losas, nunca el área del dibujo.")
        }
        if(d.geometry in listOf("AREA","LINEAR","POTHOLE") || localized) { Field("X1 / centro X (m)",x1){x1=it}; Field("Y1 / centro Y (m)",y1){y1=it} }
        if(d.geometry in listOf("AREA","LINEAR") || localized && PavementSlabGeometry.mark(d.code)!=SlabMark.POINT) { Field("X2 (m)",x2){x2=it}; Field("Y2 (m)",y2){y2=it} }
        if(d.geometry in listOf("AREA","LINEAR")) Field("Medida manual opcional (${definition.unit}); vacío = geométrica",measure){measure=it}
        if(PavementDamageFields.depth(d.code))Field(PavementDamageFields.depthLabel(d.code),depth){depth=it}
        if(d.geometry=="POTHOLE") {
            Field("Diámetro medido (m)",diameter){diameter=it}
            val measuredDiameter=diameter.replace(',','.').toDoubleOrNull()?:0.0
            if((d.code==13 && measuredDiameter>.75) || (d.code==85 && measuredDiameter>.9)) {
                Field("Área medida del hueco (m²)",holeArea){holeArea=it}
                Text("La cantidad equivalente se calcula automáticamente con el área medida · manual pág. ${if(d.code==13)108 else 233}",style=MaterialTheme.typography.bodySmall)
            }
            val suggestion=runCatching{PavementRules.potholeSeverity(d.code,number(diameter),number(depth))}.getOrNull()
            if(suggestion!=null) TextButton(onClick={severity=PavementSeverity.canonical(suggestion)}){Text("Aplicar severidad sugerida: ${PavementSeverity.label(suggestion)}")}
            else Text("Sin sugerencia: faltan medidas o el límite no está definido de forma inequívoca en el cuadro.",style=MaterialTheme.typography.bodySmall)
        }
        if(d.geometry=="SLABS") Field("Losas afectadas, separadas por coma",slabs){slabs=it}
        if(PavementDamageFields.crackWidth(d.code)) Field("Ancho de fisura (mm), si aplica",crack){crack=it}
        OutlinedTextField(observation,{observation=it},label={Text("OBSERVACIONES")},minLines=3,modifier=Modifier.fillMaxWidth())
        Text("Un daño de frontera se asigna a la muestra de la izquierda o de adelante. En una junta, 21 y 33: selecciona ambas losas afectadas.", style = MaterialTheme.typography.bodySmall)
        if(error.isNotEmpty()) Text(error,color=MaterialTheme.colorScheme.error)
    }
}

@Composable private fun EditorDialog(title: String, dismiss: () -> Unit, save: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest=dismiss) {
        Surface(shape=MaterialTheme.shapes.large) {
            Column(Modifier.padding(16.dp).heightIn(max=650.dp)) {
                Text(title,style=MaterialTheme.typography.titleLarge)
                Column(Modifier.weight(1f,false).verticalScroll(rememberScrollState()),content=content)
                Row { TextButton(onClick=dismiss){Text("Cancelar")}; Button(onClick=save){Text("Aceptar") } }
            }
        }
    }
}
