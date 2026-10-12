package com.tuempresa.inventariovial

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.tuempresa.inventariovial.location.GeoLocation
import com.tuempresa.inventariovial.location.TabletLocationProvider
import com.tuempresa.inventariovial.viewmodel.InventoryViewModel
import com.tuempresa.inventariovial.road.SequencedRecord
import com.tuempresa.inventariovial.road.*
import com.tuempresa.inventariovial.field.LocationSuggestionPanel
import com.tuempresa.inventariovial.field.FieldPrPanel
import com.tuempresa.inventariovial.supplementary.SupplementaryFormat
import com.tuempresa.inventariovial.field.SupplementaryScreen
import com.tuempresa.inventariovial.data.entity.InventoryRecordEntity

@Composable
fun FinalLocationCapture(location: GeoLocation?, onLocation: (GeoLocation) -> Unit) = FinalLocationCapture(location,onLocation,"final")

@Composable
fun FinalLocationCapture(location: GeoLocation?, onLocation: (GeoLocation) -> Unit, label: String) {
    val context = LocalContext.current
    val provider = remember(context) { TabletLocationProvider(context) }
    var locating by remember { mutableStateOf(value = false) }
    var error by remember { mutableStateOf<String?>(null) }
    fun capture() {
        locating = true
        error = null
        provider.getCurrentLocation(
            onSuccess = { locating = false; onLocation(it) },
            onError = { locating = false; error = it },
        )
    }
    val permissions = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.any { it }) capture()
        else error = "Debes permitir el acceso a la ubicación."
    }
    OutlinedButton(enabled = !locating, onClick = {
        if (
            (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) ||
            (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED)
        ) {
            capture()
        } else {
            permissions.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }) { Text(if (locating) "Obteniendo GPS $label…" else "Capturar GPS $label") }
    if (location == null) Text("GPS $label: pendiente (opcional)")
    else Text("GPS $label: ${location.latitude}, ${location.longitude} · ± ${location.accuracyHorizontal} m")
    error?.let { ErrorText(it) }
}

@Composable
fun RecordHistoryScreen(
    viewModel: InventoryViewModel,
    onScap: (() -> Unit)? = null,
    onBack: () -> Unit
) {
    val ordered by viewModel.sequencedHistory.collectAsState()
    var showAnnulled by remember { mutableStateOf(false) }
    val visibleRecords = ordered.filter {
        it.item.record.sicCode != "SCAP" &&
        (showAnnulled || it.item.record.status == "ACTIVE" || it.item.record.status=="DRAFT" && com.tuempresa.inventariovial.sic.SicFieldFormat.fromCode(it.item.record.sicCode)!=null)
    }

    val elementRecords=visibleRecords.map {it.item.record}
    val elementExtensions=visibleRecords.mapNotNull {it.item.sicExtension}
    val records=visibleRecords // SIC-08 and SIC-09 reopen by their own UUID.

    var extension by remember {mutableStateOf<Pair<String,com.tuempresa.inventariovial.sic.SicFieldFormat>?>(null)}
    extension?.let {(id,format)->com.tuempresa.inventariovial.sic.InventoryElementWorkspace(viewModel,format,id) {extension=null};return}

    var technicalId by remember { mutableStateOf<String?>(null) }
    technicalId?.let { id ->
        if(ordered.any {it.item.record.id==id && it.item.record.sicCode=="SIC-18"})
            com.tuempresa.inventariovial.field.CulvertUnifiedScreen(viewModel,id,onBack={technicalId=null})
        else com.tuempresa.inventariovial.field.EngineeringEditScreen(id) {technicalId=null}
        return
    }

    var complementary by remember {
        mutableStateOf<Pair<InventoryRecordEntity, SupplementaryFormat>?>(null)
    }

    complementary?.let { (record, format) ->
        SupplementaryScreen(viewModel, record, format) {
            complementary = null
        }
        return
    }

    var error by remember { mutableStateOf<String?>(null) }

    BackHandler(onBack = onBack)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentPadding = PaddingValues(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            OutlinedButton(onClick = onBack) {
                Text("Volver")
            }

            Text(
                "Registros locales",
                style = MaterialTheme.typography.headlineMedium
            )

            onScap?.let { open ->
                OutlinedButton(onClick = open) {
                    Text("Inspecciones SCAP de puentes")
                }
            }

            Text(
                "Ordenados por ruta y posición sobre eje; " +
                "respaldo identificado cuando falta matching"
            )

            Row {
                Checkbox(
                    checked = showAnnulled,
                    onCheckedChange = { showAnnulled = it }
                )
                Text("Mostrar también anulados")
            }

            error?.let { ErrorText(it) }
        }

        if (records.isEmpty()) {
            item {
                Text("Todavía no hay registros guardados.")
            }
        }

        items(
            items = records,
            key = { item: SequencedRecord -> item.item.record.id }
        ) { row ->
            HistoryRecordCard(
                row,
                viewModel,
                { val format=com.tuempresa.inventariovial.sic.SicFieldFormat.fromCode(row.item.record.sicCode)
                    if(format!=null) extension=row.item.record.id to format else technicalId = row.item.record.id },
                { complementary = row.item.record to it },
                visibleRecords.filter {it.item.sicExtension?.sourceRecordId==row.item.record.id && it.item.record.sicCode=="SIC-09"}.map {it.item}
            ) {
                error = it
            }
        }
    }
}

@Composable
private fun HistoryRecordCard(
    row: SequencedRecord,
    viewModel: InventoryViewModel,
    onTechnical: ()->Unit,
    onSupplementary: (SupplementaryFormat)->Unit,
    fajaWidths: List<com.tuempresa.inventariovial.data.entity.InventoryRecordWithPhotos>,
    onError: (String) -> Unit
) {
    val item = row.item
    val record = item.record
    var editing by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    // Keep the opened revision while editing; background ACKs must not discard typed fields.
    val openedRecord=remember(record.id,editing) {record}
    var route by remember(openedRecord, editing) { mutableStateOf(openedRecord.routeCode) }
    var roadbed by remember(openedRecord, editing) { mutableStateOf(openedRecord.roadbedCode) }
    var startPr by remember(openedRecord, editing) { mutableStateOf(openedRecord.startPrCode) }
    var startDistance by remember(openedRecord, editing) { mutableStateOf(openedRecord.startDistanceM.toString()) }
    var endPr by remember(openedRecord, editing) { mutableStateOf(openedRecord.endPrCode.orEmpty()) }
    var endDistance by remember(openedRecord, editing) { mutableStateOf(openedRecord.endDistanceM?.toString().orEmpty()) }
    var startSource by remember(openedRecord, editing) { mutableStateOf(openedRecord.locationSource) }
    var endSource by remember(openedRecord, editing) { mutableStateOf(openedRecord.endLocationSource) }
    var routeSource by remember(openedRecord, editing) { mutableStateOf(openedRecord.routeSource) }
    var startSuggestion by remember(openedRecord, editing) { mutableStateOf<LocationSuggestion?>(null) }
    var endSuggestion by remember(openedRecord, editing) { mutableStateOf<LocationSuggestion?>(null) }
    val reference by viewModel.field.reference.collectAsState()
    val settings by viewModel.field.settings.collectAsState()
    var side by remember(openedRecord, editing) { mutableStateOf(openedRecord.sideCode.orEmpty()) }
    var observations by remember(openedRecord, editing) { mutableStateOf(openedRecord.observations.orEmpty()) }
    var editError by remember { mutableStateOf<String?>(null) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if(record.sicCode in setOf("SIC-08","SIC-09","SIC-13","SIC-34","SIC-18")) com.tuempresa.inventariovial.sic.InventoryElements.title(record.sicCode) else record.assetType, style = MaterialTheme.typography.titleLarge)
            Text(record.sicCode,style=MaterialTheme.typography.bodySmall)
            Text("${row.asset?.shortCode ?: "Sin número operativo"} · ${row.position.chainageM?.let { String.format(java.util.Locale.US,"%.2f m",it) } ?: "Posición no determinada"}")
            Text(when(row.position.source) {
                com.tuempresa.inventariovial.road.PositionSource.AXIS_GEOMETRY -> "Posición geométrica sobre eje (GNSS)"
                com.tuempresa.inventariovial.road.PositionSource.OFFICIAL_PR -> "Progresiva: catálogo oficial de PR"
                com.tuempresa.inventariovial.road.PositionSource.ESTIMATED_FROM_PR_CODE -> "Progresiva ESTIMADA desde código PR"
                com.tuempresa.inventariovial.road.PositionSource.KILOMETRIC -> "Progresiva kilométrica registrada"
                else -> "Revisar PR en catálogo"
            })
            if (row.asset?.axisMeasureM == null) Text("Posición sobre eje no determinada · orden de respaldo")
            record.distanceToRoadAxisM?.let { Text(String.format(java.util.Locale.US, "Distancia al eje: %.1f m", it)) }
            Text("${record.routeCode} / ${record.roadbedCode} · PR ${record.startPrCode} + ${record.startDistanceM}")
            Text(if(record.sicCode=="SIC-22") "Fuente ruta: ${record.routeSource} · Ubicación del elemento: ${record.locationSource}" else "Fuente ruta: ${record.routeSource} · Inicio: ${record.locationSource} · Fin: ${record.endLocationSource}")
            if (record.suggestedRouteCode != null || record.suggestedPrCode != null)
                Text("Sugerencia guardada: ${record.suggestedRouteCode ?: "--"} · PR ${record.suggestedPrCode ?: "--"} + ${record.suggestedDistanceFromPrM ?: "--"} m")
            if(record.sicCode!="SIC-22") record.endSuggestedPrCode?.let { Text("Sugerencia fin: PR $it + ${record.endSuggestedDistanceFromPrM} m") }
            com.tuempresa.inventariovial.field.ProjectRouteFields(record.routeCode)
            viewModel.field.projectRoutes.segment(record.routeCode, record.segment)?.let { Text("$it · Sentido: ${if(record.surveyDirection=="DECREASING") "Decreciente" else "Creciente"}") }
            item.sic18?.let { detail ->
                if(detail.crossSectionCode=="2") Text("Forma: ${when(detail.sectionShape) { "CIRCULAR" -> "Circular"; "OVAL" -> "Ovalada"; else -> "Circular / ovalada (registro anterior)" }}")
                Text("Dimensión 1: ${detail.dimension1M ?: "Sin dato"} m · Dimensión 2: ${detail.dimension2M?.let { "$it m" } ?: "No aplica / sin dato"}")
            }
            Text("Fotos: ${item.photos.size} · Pendientes: ${item.photos.count { it.syncStatus != "SYNCED" }}")
            com.tuempresa.inventariovial.core.AutomaticSyncPanel("SIC",record.id,record.status=="ACTIVE")
            DriveUploadPolicy.configurationError()?.let { Text("Drive · SIN CONFIGURAR: $it") }
            item.photos.sortedBy { it.photoIndex }.forEach { photo ->
                Text("Foto ${photo.photoIndex} · ${DriveUploadPolicy.statusLabel(photo.syncStatus)}")
            }
            Text(when(record.status) {"ACTIVE"->"Estado: activo";"DRAFT"->"Estado: borrador";else->"Estado: anulado"})
            if(com.tuempresa.inventariovial.server.ServerConfiguration.enabled) {
                Text(if(com.tuempresa.inventariovial.server.ServerConfiguration.enabled) "Servidor: ${record.serverSyncStatus}" else "Servidor: sin configurar · datos guardados localmente")
                record.serverSyncError?.let { ErrorText(it) }
            }
            Text("${if(record.sicCode=="SIC-22") "GPS del elemento" else "GPS inicial"}: ${record.latitude}, ${record.longitude}")
            if(com.tuempresa.inventariovial.validation.requiresEndLocation(record.sicCode,record.assetType,item.sic20?.classCode,item.sic23?.classCode)) {
                record.endLatitude?.let { Text("GPS final: $it, ${record.endLongitude}") }
            }
            item.sic23?.let { detail ->
                Text("Clase: ${detail.classCode} · Tipo: ${detail.typeCode ?: "Sin objeto"}")
                Text("Ancho: ${detail.widthM?.let { String.format(java.util.Locale.US, "%.2f m", it) } ?: "Sin objeto"}")
                if (detail.description.isNotBlank()) Text("Descripción: ${detail.description}")
                Text("Fin: PR ${record.endPrCode} + ${record.endDistanceM} · Lado: ${record.sideCode}")
            }
            item.sic18?.let {d->
                val state = com.tuempresa.inventariovial.sic.InventoryElements.culvertDetailStatus(d,item.sic18a,item.sicExtension)
                Text("SIC-18A: $state",style=MaterialTheme.typography.titleMedium,
                    color=if(state=="Pendiente") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)

                Text("Estructural: ${d.structuralConditionCode} · Funcional: ${d.functionalConditionCode}")
                if(com.tuempresa.inventariovial.catalog.EngineeringConditions.badCulvert(d.structuralConditionCode,d.functionalConditionCode)) Text("! Condición mala detectada",color=MaterialTheme.colorScheme.error)
                com.tuempresa.inventariovial.catalog.EngineeringConditions.culvertPhotos.forEach {(key,label)->Text("$label: ${item.photos.count {it.photoCategory==key}}")}
                if(item.photos.any {it.photoCategory==null}) Text("Fotos históricas sin categoría: ${item.photos.count {it.photoCategory==null}}")
            }
            item.sic19?.let {d->
                Text("Estructural: ${d.structuralConditionCode} · Funcional: ${d.functionalConditionCode}")
                Text(com.tuempresa.inventariovial.catalog.EngineeringConditions.structural(d.typeCode,d.structuralCriterion)[d.structuralConditionCode] ?: "! Criterio estructural pendiente")
            }
            item.sic20?.let {d->
                Text("Estructural: ${d.structuralConditionCode} · Funcional: ${d.functionalConditionCode ?: "Pendiente"}")
                if(d.classCode=="14") {Text("Altura promedio del cuerpo: ${d.dimension1M ?: "Pendiente"} m");Text("Longitud del muro (interno): ${d.wallLengthMeters ?: "Pendiente"} m")}
            }
            item.sic18a?.let {d->
                Text("SIC-18A guardado · ${if(item.sicExtension?.status=="DRAFT") "Borrador" else "Detalle vinculado a alcantarilla"}",style=MaterialTheme.typography.titleMedium)
                com.tuempresa.inventariovial.supplementary.SupplementaryForms.fields(SupplementaryFormat.SIC18A,d.values()).forEach {f->
                    val v=d.values()[f.key].orEmpty();Text("${f.label}: ${f.options.find {it.substringBefore(" - ")==v} ?: v}")
                }
            }
            if(record.status=="ACTIVE" && record.sicCode in listOf("SIC-17","SIC-18","SIC-19","SIC-20","SIC-21","SIC-22","SIC-23")) OutlinedButton(onClick=onTechnical) {Text(if(record.sicCode=="SIC-18") "Abrir alcantarilla / exportar formatos" else "Editar ficha técnica y fotografías")}
            if(com.tuempresa.inventariovial.sic.SicFieldFormat.fromCode(record.sicCode)!=null) {
                OutlinedButton(onClick=onTechnical) {Text(if(record.status=="DRAFT") "Editar borrador" else "Ver ficha / exportar")}
            } else if (!editing) {
                if(record.status=="ACTIVE") SupplementaryFormat.entries.filter { it.enabled &&
                    record.sicCode=="SIC-17" && it!=SupplementaryFormat.SIC18A
                }.forEach { format -> OutlinedButton(onClick={onSupplementary(format)}) {Text(if(format==SupplementaryFormat.SIC18A && item.sic18a!=null) if(item.sicExtension?.status=="DRAFT") "Editar borrador SIC-18A" else "Ver SIC-18A / exportar" else "Completar ${format.title}")} }
                record.observations?.let { Text(it) }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { editing = true; editError = null }) { Text("Editar") }
                    OutlinedButton(onClick = {
                        viewModel.setRecordStatus(record.id, record.status != "ACTIVE", onError)
                    }) { Text(if (record.status == "ACTIVE") "Anular" else "Restaurar") }
                }
            } else {
                OutlinedTextField(value = route, onValueChange = { route = it; routeSource = LocationSource.MANUAL_OVERRIDE; startSource = LocationSource.MANUAL_OVERRIDE; endSource = LocationSource.MANUAL_OVERRIDE }, label = { Text("Ruta") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = roadbed, onValueChange = { roadbed = it; startSource = LocationSource.MANUAL_OVERRIDE; endSource = LocationSource.MANUAL_OVERRIDE }, label = { Text("Calzada") }, modifier = Modifier.fillMaxWidth())
                LocationSuggestionPanel(reference, settings.matchConfig(), route, roadbed, record.latitude, record.longitude, record.gpsAccuracyM, if(record.sicCode=="SIC-22") "elemento" else "inicio",
                    { if (it != null && (startSource != LocationSource.MANUAL_OVERRIDE || (startSuggestion == null && record.suggestedRouteCode == null && record.suggestedPrCode == null))) startSuggestion = it }, { suggestion ->
                        startSuggestion = suggestion
                        route = suggestion.suggestedRouteCode ?: route; routeSource = LocationSource.GNSS_CONFIRMED
                        suggestion.pr?.let { startPr = it.suggestedPrCode; startDistance = it.distanceText }
                        startSource = if (suggestion.pr == null) LocationSource.GNSS_CONFIRMED else LocationSource.GNSS_PR_CONFIRMED
                    }, { startSource = LocationSource.MANUAL_OVERRIDE })
                FieldPrPanel(viewModel.field, route, roadbed)
                com.tuempresa.inventariovial.field.ContractualPrField(value = startPr, onValueChange = { startPr = it; startSource = LocationSource.edited(startSource, startSuggestion != null) }, label = { Text(if(record.sicCode=="SIC-22") "PR del elemento" else "PR inicio") }, modifier = Modifier.fillMaxWidth())
                com.tuempresa.inventariovial.field.PrDistanceField(value = startDistance, onValueChange = { startDistance = it; startSource = LocationSource.edited(startSource, startSuggestion != null) }, label = { Text(if(record.sicCode=="SIC-22") "Distancia desde PR (m)" else "Distancia inicio (m)") }, modifier = Modifier.fillMaxWidth())
                if (com.tuempresa.inventariovial.validation.requiresEndLocation(record.sicCode, record.assetType, item.sic20?.classCode, item.sic23?.classCode)) {
                    LocationSuggestionPanel(reference, settings.matchConfig(), route, roadbed, record.endLatitude, record.endLongitude, record.endGpsAccuracyM, "fin",
                        { if (it != null && (endSource != LocationSource.MANUAL_OVERRIDE || (endSuggestion == null && record.endSuggestedPrCode == null))) endSuggestion = it }, { suggestion ->
                            if (normalizedRoadCode(route) == suggestion.suggestedRouteCode) {
                                endSuggestion = suggestion
                                suggestion.pr?.let { endPr = it.suggestedPrCode; endDistance = it.distanceText }
                                endSource = if (suggestion.pr == null) LocationSource.GNSS_CONFIRMED else LocationSource.GNSS_PR_CONFIRMED
                            } else editError = "El fin corresponde a otra ruta. Revisa la ubicación contractual."
                        }, { endSource = LocationSource.MANUAL_OVERRIDE })
                }
                if(record.sicCode!="SIC-22") {
                com.tuempresa.inventariovial.field.ContractualPrField(value = endPr, onValueChange = { endPr = it; endSource = LocationSource.edited(endSource, endSuggestion != null) }, label = { Text("PR fin") }, modifier = Modifier.fillMaxWidth())
                com.tuempresa.inventariovial.field.PrDistanceField(value = endDistance, onValueChange = { endDistance = it; endSource = LocationSource.edited(endSource, endSuggestion != null) }, label = { Text("Distancia fin (m)") }, modifier = Modifier.fillMaxWidth())
                }
                if(com.tuempresa.inventariovial.export.TechnicalLengthExport.applies(item)) {
                    com.tuempresa.inventariovial.field.TechnicalLengthField(com.tuempresa.inventariovial.validation.ContractualLength.calculate(
                        com.tuempresa.inventariovial.validation.LengthEndpoint(route,roadbed,startPr,startDistance),
                        com.tuempresa.inventariovial.validation.LengthEndpoint(route,roadbed,endPr,endDistance),reference.prs,record.surveyDirection ?: "INCREASING"))
                }
                if(item.sic20?.classCode in setOf("13","14")) {
                    Text("Lado")
                    val allowed=if(item.sic20?.classCode=="13")listOf("S") else listOf("I","D")
                    if(side !in allowed) Text("Valor histórico $side conservado; selecciona un lado válido para corregirlo.")
                    ChoiceSelector(allowed,side) {side=it}
                } else OutlinedTextField(value = side, onValueChange = { side = it }, label = { Text("Lado") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = observations, onValueChange = { observations = it }, label = { Text("OBSERVACIONES") }, minLines=3, modifier = Modifier.fillMaxWidth())
                editError?.let { ErrorText(it) }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(enabled = !saving, onClick = {
                        val start = startDistance.trim().replace(',', '.').toDoubleOrNull()
                        val end = endDistance.trim().replace(',', '.').toDoubleOrNull()
                        when {
                            start == null || !start.isFinite() || start < 0 ->
                                editError = "La distancia inicial debe ser un número positivo o cero."
                            record.sicCode!="SIC-22" && endDistance.isNotBlank() && (end == null || !end.isFinite() || end < 0) ->
                                editError = "La distancia final debe ser un número positivo o cero."
                            record.sicCode!="SIC-22" && (endPr.isBlank() != endDistance.isBlank()) ->
                                editError = "Completa el PR y la distancia final."
                            else -> {
                                saving = true
                                viewModel.updateCoreFields(openedRecord.copy(
                                    routeCode = route, roadbedCode = roadbed, startPrCode = startPr,
                                    startDistanceM = start, endPrCode = if(record.sicCode=="SIC-22") record.endPrCode else endPr.ifBlank { null },
                                    endDistanceM = if(record.sicCode=="SIC-22") record.endDistanceM else end, sideCode = side.trim().uppercase().ifBlank { null },
                                    locationSource = startSource, endLocationSource = endSource, routeSource = routeSource,
                                    observations = observations.trim().ifBlank { null }).withSuggestions(startSuggestion, endSuggestion),
                                    onSuccess = { saving = false; editing = false },
                                    onError = { saving = false; editError = it })
                            }
                        }
                    }) { Text("Guardar cambios") }
                    OutlinedButton(enabled = !saving, onClick = { editing = false }) { Text("Cancelar") }
                }
            }
        }
    }
}
