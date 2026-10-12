package com.tuempresa.inventariovial

import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

import androidx.compose.material3.*
import com.tuempresa.inventariovial.auth.*
import androidx.compose.foundation.layout.Box
import com.tuempresa.inventariovial.field.*
import com.tuempresa.inventariovial.road.*
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import android.Manifest
import com.tuempresa.inventariovial.catalog.SicCatalogRepository
import com.tuempresa.inventariovial.camera.PhotoStampData
import com.tuempresa.inventariovial.data.entity.InventoryRecordEntity
import com.tuempresa.inventariovial.location.TabletLocationProvider

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.produceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.tuempresa.inventariovial.camera.createPhotoFile
import com.tuempresa.inventariovial.pavement.ui.PavementWorkspace
import com.tuempresa.inventariovial.camera.loadCorrectlyOrientedBitmap
import com.tuempresa.inventariovial.location.getCurrentGpsLocation
import com.tuempresa.inventariovial.location.GeoLocation
import com.tuempresa.inventariovial.model.RoadAssetType
import com.tuempresa.inventariovial.model.SignalizationType
import com.tuempresa.inventariovial.ui.theme.InventarioVialTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


import com.tuempresa.inventariovial.model.form.Sic17FormState
import com.tuempresa.inventariovial.model.form.Sic18FormState
import com.tuempresa.inventariovial.model.form.Sic19FormState
import com.tuempresa.inventariovial.model.form.Sic20FormState
import com.tuempresa.inventariovial.model.form.Sic21FormState
import com.tuempresa.inventariovial.model.form.Sic22FormState
import com.tuempresa.inventariovial.model.form.Sic23FormState

import androidx.compose.runtime.collectAsState

import androidx.lifecycle.ViewModelProvider

import com.tuempresa.inventariovial.model.form.InventorySaveRequest
import com.tuempresa.inventariovial.model.form.SicFormDetail

import com.tuempresa.inventariovial.viewmodel.InventoryViewModel
import com.tuempresa.inventariovial.validation.requiresEndLocation


// =========================================================
// UBICACIÓN GPS AUXILIAR
// Se usa para guardar la ubicación final de un elemento.
// =========================================================

//data class GeoLocation(
//    val latitude: Double,
//    val longitude: Double,
//    val accuracyHorizontal: Float
//)


class MainActivity : ComponentActivity() {

    override fun onStart() {
        super.onStart()
        AuthGraph.get(this).foreground()
        com.tuempresa.inventariovial.diagnostics.StabilityDiagnostics.record(this,"FOREGROUND")
    }

    override fun onStop() {
        if (!isChangingConfigurations) AuthGraph.get(this).background()
        if (!isChangingConfigurations) com.tuempresa.inventariovial.diagnostics.StabilityDiagnostics.record(this,"BACKGROUND")
        super.onStop()
    }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        enableEdgeToEdge()


        val inventoryViewModel =

            ViewModelProvider(
                this
            )[
                InventoryViewModel::class.java
            ]


        setContent {

            InventarioVialTheme {

                InventarioVialApp(
                    inventoryViewModel =
                        inventoryViewModel
                )
            }
        }
    }
}




// =========================================================
// APP PRINCIPAL
// =========================================================

@Composable
fun InventarioVialApp(
    inventoryViewModel:
    InventoryViewModel
) {

    if (!deviceAccessGate(inventoryViewModel)) return
    val graph = rememberAuthGraph()
    AuthShell(graph) { InventoryWorkspace(inventoryViewModel, graph) }
}

@Composable
private fun rememberAuthGraph(): AuthGraph = AuthGraph.get(LocalContext.current)

@Composable
private fun InventoryWorkspace(inventoryViewModel: InventoryViewModel, graph: AuthGraph) {
    SaveWarningsDialog(inventoryViewModel)
    var selectedDraft by remember { mutableStateOf<InventoryRecordEntity?>(null) }
    var newSicCode by rememberSaveable {mutableStateOf<String?>(null)}
    var newSicId by rememberSaveable {mutableStateOf<String?>(null)}
    var showExport by rememberSaveable { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var showDelivery by remember { mutableStateOf(false) }
    var showPavement by rememberSaveable { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf<String?>(null) }
    var selectedAsset by remember {
        mutableStateOf<RoadAssetType?>(null)
    }

    var selectedSignalization by remember {
        mutableStateOf<SignalizationType?>(null)
    }

    val authSession by graph.sessions.current.collectAsState()
    val activeTrack by com.tuempresa.inventariovial.tracking.TrackCaptureService.activeRecordId.collectAsState()
    val pendingSave by inventoryViewModel.pendingSave.collectAsState()
    val safeAtHome = newSicCode==null && selectedAsset == null && !showPavement && !showHistory && !showDelivery && !showExport && pendingSave == null
    androidx.compose.runtime.SideEffect {com.tuempresa.inventariovial.diagnostics.StabilityDiagnostics.screen(when {
        showPavement->"PAVEMENT";selectedAsset==RoadAssetType.BRIDGE->"SCAP";selectedAsset==RoadAssetType.SIGNALIZATION->"SIGNALS";safeAtHome->"HOME";else->"INVENTORY"
    })}
    androidx.compose.runtime.SideEffect { graph.safeAtHome = safeAtHome }
    LaunchedEffect(authSession?.closing, safeAtHome, activeTrack) {
        if (authSession?.closing == true && safeAtHome && activeTrack == null) graph.logoutAtHome()
    }

    val recordsToday by
    inventoryViewModel
        .recordsToday
        .collectAsState()


    val pendingSync by
    inventoryViewModel
        .pendingSync
        .collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            when {
                newSicCode!=null -> com.tuempresa.inventariovial.sic.InventoryElementWorkspace(inventoryViewModel,
                    com.tuempresa.inventariovial.sic.SicFieldFormat.fromCode(newSicCode!!)!!,newSicId) {newSicCode=null;newSicId=null}
                showDelivery -> AssetOrderScreen(inventoryViewModel) { showDelivery = false }
                showExport -> SicExportScreen(inventoryViewModel) { showExport = false }
                showHistory -> RecordHistoryScreen(inventoryViewModel, onScap = { showHistory = false; selectedAsset = RoadAssetType.BRIDGE }) { showHistory = false }
                showPavement -> PavementWorkspace { showPavement = false }

                selectedAsset == null -> {

                    HomeScreen(
                        inventoryViewModel = inventoryViewModel,
                        onNewSic = { format ->
                            if(authSession?.closing!=true) {newSicId=null;newSicCode=format.code}
                        },
                        onResumeDraft = { draft ->
                            if(com.tuempresa.inventariovial.sic.SicFieldFormat.fromCode(draft.sicCode)!=null) {
                                newSicId=draft.id;newSicCode=draft.sicCode
                            } else {
                            selectedDraft = draft
                            selectedAsset = when(draft.sicCode) {
                                "SIC-18" -> RoadAssetType.CULVERT
                                "SIC-19" -> RoadAssetType.DITCH
                                "SIC-23" -> RoadAssetType.RIGHT_OF_WAY
                                "SIC-21" -> RoadAssetType.SIGNALIZATION
                                else -> when(draft.assetType) { "WALL", "MURO" -> RoadAssetType.WALL; "TUNNEL", "TUNEL" -> RoadAssetType.TUNNEL; else -> RoadAssetType.FORD }
                            }
                            if(draft.sicCode=="SIC-21") selectedSignalization = runCatching { SignalizationType.valueOf(draft.assetType) }.getOrDefault(SignalizationType.HORIZONTAL_MARKS)
                            }
                        },
                        onExport = { showExport = true },
                        onHistory = { showHistory = true },
                        onSync = { showDelivery = true },
                        syncMessage = syncMessage,
                        recordsToday = recordsToday,
                        pendingSync = pendingSync,
                        onPavement = { showPavement = true },
                        onAssetSelected = {
                            if (authSession?.closing == true) syncMessage = "Finaliza el recorrido abierto; tu acceso fue desactivado."
                            else {
                                selectedDraft = null
                                selectedAsset = it
                            }
                        }
                    )
                }


                selectedAsset == RoadAssetType.BRIDGE -> com.tuempresa.inventariovial.scap.ui.ScapWorkspace(inventoryViewModel) { selectedAsset = null }

                (selectedAsset == RoadAssetType.SIGNALIZATION &&
                        selectedSignalization == null) -> {

                    SignalizationMenuScreen(
                        onBack = {
                            selectedAsset = null
                        },
                        onSelected = {
                            selectedSignalization = it
                        }
                    )
                }


                selectedAsset == RoadAssetType.SIGNALIZATION &&
                        selectedSignalization != null -> {

                    SignalizationFormScreen(
                        draftRecord = selectedDraft,
                        signalizationType = selectedSignalization!!,
                        inventoryViewModel = inventoryViewModel,
                        onBack = {
                            selectedSignalization = null
                        },
                        onSave = {
                            selectedSignalization = null
                            selectedAsset = null
                        }
                    )
                }


                else -> {

                    AssetFormScreen(
                        draftRecord = selectedDraft,
                        assetType = selectedAsset!!,
                        inventoryViewModel = inventoryViewModel,
                        onBack = {
                            selectedAsset = null
                        },
                        onSave = {
                            selectedAsset = null
                        }
                    )
                }
            }
        }
    }
}


// =========================================================
// HOME
// =========================================================

@Composable
fun HomeScreen(
    recordsToday: Int,
    pendingSync: Int,
    onAssetSelected: (RoadAssetType) -> Unit,
    onExport: () -> Unit = {},
    onHistory: () -> Unit = {},
    onSync: () -> Unit = {},
    syncMessage: String? = null,
    inventoryViewModel: InventoryViewModel? = null,
    onResumeDraft: (InventoryRecordEntity) -> Unit = {},
    onPavement: () -> Unit = {},
    onNewSic: (com.tuempresa.inventariovial.sic.SicFieldFormat)->Unit = {}
) {

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),

        contentPadding = PaddingValues(
            top = 24.dp,
            bottom = 32.dp
        ),

        verticalArrangement =
            Arrangement.spacedBy(16.dp)
    ) {

        item {
            Surface(color=MaterialTheme.colorScheme.primary,contentColor=MaterialTheme.colorScheme.onPrimary,shape=androidx.compose.foundation.shape.RoundedCornerShape(20.dp)) {
                Row(Modifier.fillMaxWidth().padding(20.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        Text("GBO · INGENIERÍA EN CAMPO",style=MaterialTheme.typography.labelLarge)
                        Text("Inventario vial",style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Bold)
                        Text("Inspecciona, registra y revisa tus levantamientos.",style=MaterialTheme.typography.bodyLarge)
                    }
                    Image(androidx.compose.ui.res.painterResource(R.mipmap.ic_launcher_foreground),"GBO Ingenieros Consultores",modifier=Modifier.size(104.dp))
                }
            }
        }

        item {
            ElevatedCard(onClick = onPavement, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Fallas en pavimentos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("PCI para asfalto y rígido · URCI para vía no pavimentada", style = MaterialTheme.typography.bodyMedium)
                        Text("Esquema X/Y · daños · fotos · Excel/PDF · Drive", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
                    Text("ABRIR", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            inventoryViewModel?.let { AuthHomePanel(it.field.auth) }
            if(inventoryViewModel!=null) FieldHomePanel(inventoryViewModel,onResumeDraft) else ProjectCard()
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onHistory) { Text("Ver registros") }
                OutlinedButton(onClick = onSync) { Text("Mapa y orden de elementos") }
            }
            OutlinedButton(onClick = onExport, modifier = Modifier.fillMaxWidth()) { Text("Exportar SIC a Excel") }
            syncMessage?.let { Text(it) }
            inventoryViewModel?.let { DriveStatusPanel(it) }
            inventoryViewModel?.let { com.tuempresa.inventariovial.core.CoreFieldTools(it) }
        }

        item {

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {

                StatusCard(
                    title = "Registros",
                    value =
                        recordsToday.toString(),
                    modifier =
                        Modifier.weight(1f)
                )

                Spacer(
                    modifier =
                        Modifier.width(16.dp)
                )

                StatusCard(
                    title = "Pendientes",
                    value =
                        pendingSync.toString(),
                    modifier =
                        Modifier.weight(1f)
                )
            }
        }

        item {

            Text(
                text = "Nuevo registro",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier =
                    Modifier.padding(top = 12.dp)
            )

            Text(
                text =
                    "Seleccione el elemento encontrado en carretera.",
                style =
                    MaterialTheme.typography.bodyMedium
            )
        }

        com.tuempresa.inventariovial.sic.InventoryElements.home.groupBy {it.family}.forEach {(family,destinations)->
            item {Text(family,style=MaterialTheme.typography.titleMedium)}
            items(destinations.chunked(2)) {pair->
                com.tuempresa.inventariovial.core.InventoryFieldRows(pair) {entry->
                    InventoryElementButton(entry.title,entry.subtitle) {
                        entry.asset?.let(onAssetSelected)
                        entry.format?.let(onNewSic)
                    }
                }
            }
        }

        item {

            HorizontalDivider(
                modifier =
                    Modifier.padding(vertical = 8.dp)
            )

            Text(
                text = "Estado del sistema",
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            Text("● Captura offline habilitada")
            Text("● Fotografías guardadas localmente")
            Text("● GPS disponible durante cada registro")
            Text(
                "● Pendientes de sincronización: $pendingSync"
            )
        }
    }
}


// =========================================================
// MENÚ DE SEÑALIZACIÓN
// =========================================================

@Composable
fun SignalizationMenuScreen(
    onBack: () -> Unit,
    onSelected: (SignalizationType) -> Unit
) {

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),

        contentPadding = PaddingValues(
            top = 24.dp,
            bottom = 40.dp
        ),

        verticalArrangement =
            Arrangement.spacedBy(16.dp)
    ) {

        item {

            OutlinedButton(
                onClick = onBack
            ) {
                Text("← Volver")
            }
        }

        item {

            Text(
                text = "Señalización y seguridad",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text =
                    "Seleccione el tipo de elemento que encontró en la carretera.",
                style =
                    MaterialTheme.typography.bodyLarge
            )
        }

        items(
            items =
                SignalizationType.entries
        ) { item ->

            Button(
                onClick = {
                    onSelected(item)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            ) {

                Column(
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Text(
                        text = item.title,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text =
                            "${item.sicCode} · ${item.subtitle}",
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}


// =========================================================
// TARJETAS
// =========================================================

@Composable
fun ProjectCard() {

    Card(
        modifier =
            Modifier.fillMaxWidth(),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 4.dp
            )
    ) {

        Column(
            modifier =
                Modifier.padding(20.dp)
        ) {

            Text(
                text = "Jornada activa",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            Text("Proyecto: Sin seleccionar")
            Text("Ruta: Se registra por elemento")
            Text("Tramo: Sin seleccionar")
            Text("Ingeniero: Sin registrar")
        }
    }
}


@Composable
fun StatusCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {

    Card(
        modifier = modifier,

        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 3.dp
            )
    ) {

        Column(
            modifier =
                Modifier.padding(20.dp)
        ) {

            Text(
                text = value,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )

            Text(text = title)
        }
    }
}


@Composable
fun AssetButton(asset:RoadAssetType,onClick:()->Unit) = InventoryElementButton(
    if(asset==RoadAssetType.CULVERT) "Alcantarillas" else asset.title,
    if(asset==RoadAssetType.CULVERT) "SIC-18 · SIC-18A cuando corresponde" else "${asset.sicCode} · ${asset.subtitle}",onClick)

@Composable
fun InventoryElementButton(title:String,subtitle:String,onClick:()->Unit) {
    OutlinedCard(onClick=onClick,modifier=Modifier.fillMaxWidth().heightIn(min=168.dp),
        colors=CardDefaults.outlinedCardColors(containerColor=MaterialTheme.colorScheme.surface),
        border=androidx.compose.foundation.BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Surface(color=MaterialTheme.colorScheme.primaryContainer,shape=androidx.compose.foundation.shape.RoundedCornerShape(12.dp)) {
                Text("+",Modifier.padding(horizontal=16.dp,vertical=6.dp),style=MaterialTheme.typography.headlineSmall,color=MaterialTheme.colorScheme.primary)
            }
            Text(title,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.primary)
            Text(subtitle,style=MaterialTheme.typography.bodyMedium)
        }
    }
}


// =========================================================
// FORMULARIO DE SEÑALIZACIÓN
// =========================================================

@Composable
fun SignalizationFormScreen(
    signalizationType: SignalizationType,
    inventoryViewModel: InventoryViewModel,
    onBack: () -> Unit,
    onSave: () -> Unit,
    draftRecord: InventoryRecordEntity? = null
) {

    val context = LocalContext.current
    val captureContext = com.tuempresa.inventariovial.field.rememberFieldCaptureContext(
        inventoryViewModel.field.dao, inventoryViewModel.field.projectRoutes, draftRecord)
    if (captureContext == null) { androidx.activity.compose.BackHandler(onBack=onBack); Text("Cargando sesión de campo…"); return }
    val fieldSession = captureContext.session
    val initialLocation = captureContext.location
    val survey = remember { SurveyPreferences(context) }
    val prior = remember { com.tuempresa.inventariovial.field.FieldSessionDefaults.matchingPosition(fieldSession, survey.last()) }
    var segment by rememberSaveable { mutableStateOf(initialLocation.segment) }
    var direction by rememberSaveable { mutableStateOf(initialLocation.direction) }

    var draftId by rememberSaveable { mutableStateOf(draftRecord?.id) }
    var location by remember { mutableStateOf(draftRecord?.takeIf {it.latitude!=null && it.longitude!=null}?.let {
        GeoLocation(requireNotNull(it.latitude),requireNotNull(it.longitude),it.gpsAccuracyM?.toFloat() ?: Float.POSITIVE_INFINITY,it.altitudeM,source=it.locationSource,timestamp=it.gpsTimestamp ?: 0)
    }) }
    val prReference by inventoryViewModel.field.reference.collectAsState()
    val prSettings by inventoryViewModel.field.settings.collectAsState()
    var locationSource by rememberSaveable { mutableStateOf(draftRecord?.locationSource ?: "MANUAL") }
    var endLocationSource by rememberSaveable { mutableStateOf(draftRecord?.endLocationSource ?: "MANUAL") }
    var sideSource by rememberSaveable { mutableStateOf("MANUAL") }
    var stampedPaths by remember { mutableStateOf<Map<String,String>>(emptyMap()) }
    var photoCaptures by remember { mutableStateOf<Map<String, com.tuempresa.inventariovial.camera.PhotoCaptureMetadata>>(emptyMap()) }
    var photoCategories by remember {mutableStateOf<Map<String,String>>(emptyMap())}
    var photoGpsWarning by remember { mutableStateOf<String?>(null) }
    var routeSource by rememberSaveable { mutableStateOf(draftRecord?.routeSource ?: LocationSource.MANUAL) }
    var startSuggestion by remember { mutableStateOf<LocationSuggestion?>(null) }
    var endSuggestion by remember { mutableStateOf<LocationSuggestion?>(null) }


    // Comunes SIC-21 / SIC-22
    var route by rememberSaveable {
        mutableStateOf(initialLocation.route)
    }

    var roadbed by rememberSaveable {
        mutableStateOf(initialLocation.roadbed)
    }

    var startPr by rememberSaveable {
        mutableStateOf(draftRecord?.startPrCode ?: prior?.pr.orEmpty())
    }

    var startDistance by rememberSaveable {
        mutableStateOf(draftRecord?.startDistanceM?.toString().orEmpty())
    }

    LaunchedEffect(route, roadbed, startPr, startDistance, location, fieldSession?.operator) {
        // A confirmed stamp must reflect the current form's location and operator.
        stampedPaths = emptyMap()
    }

    var endPr by remember {
        mutableStateOf("")
    }

    var endDistance by remember {
        mutableStateOf("")
    }

    var side by remember {
        mutableStateOf("D - Derecho")
    }

    var observations by remember {
        mutableStateOf("")
    }

    val registrationDate = remember {
        draftRecord?.surveyDate ?: SimpleDateFormat(
            "dd/MM/yyyy",
            Locale.getDefault()
        ).format(Date())
    }

    // Foto
    var capturedPhotos by remember { mutableStateOf<List<String>>(emptyList()) }
    var endLocation by remember { mutableStateOf<GeoLocation?>(null) }
    var photoPath by remember {
        mutableStateOf<String?>(null)
    }

    var photoCaptured by remember {
        mutableStateOf(false)
    }

    var photoError by remember {
        mutableStateOf<String?>(null)
    }

    // GPS
    var latitude by remember {
        mutableStateOf<Double?>(location?.latitude)
    }

    var longitude by remember {
        mutableStateOf<Double?>(location?.longitude)
    }

    var gpsAccuracy by remember {
        mutableStateOf<Float?>(location?.horizontalAccuracy)
    }

    var locating by remember {
        mutableStateOf(false)
    }

    var gpsInputError by remember {mutableStateOf<String?>(null)}
    var locationError by remember {
        mutableStateOf<String?>(null)
    }

    var formError by remember {
        mutableStateOf<String?>(null)
    }

    // =========================================================
    // ESTADOS SIC CALIFICADOS
    // =========================================================

    var sic21State by remember(signalizationType) {

        mutableStateOf(

            when (signalizationType) {

                SignalizationType.HORIZONTAL_MARKS ->
                    Sic21FormState(
                        classCode = "18",
                        typeCode = "1",
                        materialCode = "",
                        conditionCode = "1"
                    )

                SignalizationType.SAFETY ->
                    Sic21FormState(
                        classCode = "19",
                        typeCode = "1",
                        materialCode = "1",
                        conditionCode = "1"
                    )

                SignalizationType.HORIZONTAL_STUDS ->
                    Sic21FormState(
                        classCode = "20",
                        typeCode = "1",
                        materialCode = "",
                        conditionCode = "1"
                    )

                SignalizationType.VERTICAL ->
                    Sic21FormState()
            }
        )
    }


    var sic22State by remember(signalizationType) {

        mutableStateOf(
            Sic22FormState()
        )
    }


    fun hasLocationPermission(): Boolean {

        val fine =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) ==
                    PackageManager.PERMISSION_GRANTED

        val coarse =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) ==
                    PackageManager.PERMISSION_GRANTED

        return fine || coarse
    }


    fun captureLocation() {

        if (!hasLocationPermission()) return

        locating = true
        locationError = null

        TabletLocationProvider(context).getCurrentLocation(
            onSuccess = { fix ->
                latitude = fix.latitude
                longitude = fix.longitude
                gpsAccuracy = fix.horizontalAccuracy
                location = fix
                locating = false
            },

            onError = { error ->

                locationError = error
                locating = false
            }
        )
    }






    val takePictureLauncher =
        rememberLauncherForActivityResult(
            contract =
                com.tuempresa.inventariovial.core.FieldTakePicture()
        ) { success ->

            if (success) photoPath?.let { path ->
                capturedPhotos = capturedPhotos + path
                val capturedAt = System.currentTimeMillis()
                photoCaptures = photoCaptures + (path to com.tuempresa.inventariovial.camera.PhotoCaptureMetadata(capturedAt, null))
                com.tuempresa.inventariovial.camera.PhotoCaptureMetadata.capture(context, capturedAt) { metadata ->
                    photoCaptures = photoCaptures + (path to metadata)
                    photoGpsWarning = if (metadata.fix == null) "Fotografía guardada sin GNSS propio disponible." else null
                }
            }
            photoCaptured = capturedPhotos.isNotEmpty()
            photoPath = capturedPhotos.lastOrNull()

            if (success) {

                photoError = null



            } else {

                photoError =
                    "La fotografía fue cancelada o no pudo guardarse."
            }
        }


    fun openCamera() {

        try {

            val prefix = when (signalizationType) {

                SignalizationType.VERTICAL ->
                    "SENAL_VERTICAL"

                SignalizationType.HORIZONTAL_MARKS ->
                    "MARCA_HORIZONTAL"

                SignalizationType.HORIZONTAL_STUDS ->
                    "TACHA"

                SignalizationType.SAFETY ->
                    "SEGURIDAD_VIAL"
            }

            val photo =
                createPhotoFile(
                    context = context,
                    prefix = prefix
                )

            photoPath =
                photo.file.absolutePath

            photoError = null

            takePictureLauncher.launch(
                photo.uri
            )

        } catch (error: Exception) {

            photoError =
                error.message
                    ?: "No se pudo abrir la cámara."
        }
    }


    val cameraPermissionLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts
                    .RequestPermission()
        ) { granted ->

            if (granted) {

                openCamera()

            } else {

                photoError =
                    "Debes permitir el acceso a la cámara."
            }
        }


    fun currentDraftRequest():InventorySaveRequest { return InventorySaveRequest(
        sicCode=signalizationType.sicCode,assetType=signalizationType.name,routeCode=route,roadbedCode=roadbed,
        startPrCode=startPr,startDistanceM=startDistance,endPrCode=endPr,endDistanceM=endDistance,
        sideCode=codeFromOption(side),latitude=latitude ?: Double.NaN,longitude=longitude ?: Double.NaN,
        gpsAccuracyM=gpsAccuracy,surveyDate=registrationDate,observations=observations,
        photoPath=capturedPhotos.firstOrNull().orEmpty(),photoPaths=capturedPhotos,recordId=draftId ?: java.util.UUID.randomUUID().toString().also { draftId=it },
        detail=if(signalizationType==SignalizationType.VERTICAL) SicFormDetail.Sic22(sic22State) else SicFormDetail.Sic21(sic21State),location=location,endLocation=endLocation,locationSource=locationSource,
        endLocationSource=endLocationSource,routeSource=routeSource,sideSource=sideSource,
        sessionId=draftRecord?.sessionId ?: fieldSession?.sessionId,segment=segment,direction=direction,
        photoCaptures=photoCaptures,photoCategories=photoCategories) }
    var draftNotice by remember { mutableStateOf("") }
    LaunchedEffect(draftRecord?.id) {
        val raw=com.tuempresa.inventariovial.core.InventoryDraftStore.read(context,draftRecord?.id)
        if(raw!=null) {
            startDistance=raw.getString("startDistanceM")
            endDistance=if(raw.isNull("endDistanceM")) "" else raw.getString("endDistanceM")
            observations=draftRecord?.observations.orEmpty()
            side=draftRecord?.sideCode ?: side
            endPr=draftRecord?.endPrCode.orEmpty()
            if(draftRecord?.locationSource=="DRAFT_LOCATION_PENDING") { latitude=null;longitude=null;location=null }
            location=com.tuempresa.inventariovial.core.InventoryDraftStore.restoreFix(raw.optJSONObject("location")) ?: location
            endLocation=com.tuempresa.inventariovial.core.InventoryDraftStore.restoreFix(raw.optJSONObject("endLocation"))
            sideSource=raw.optString("sideSource",sideSource)
            val paths=raw.getJSONArray("photoPaths");capturedPhotos=(0 until paths.length()).map { paths.getString(it) }
            val cats=raw.getJSONObject("photoCategories");photoCategories=cats.keys().asSequence().associateWith {cats.getString(it)}
            photoCaptured=capturedPhotos.isNotEmpty();photoPath=capturedPhotos.lastOrNull()
            sic21State=com.tuempresa.inventariovial.core.InventoryDraftStore.restore(raw,sic21State)
            sic22State=com.tuempresa.inventariovial.core.InventoryDraftStore.restore(raw,sic22State)
            val captures=raw.getJSONObject("photoCaptures")
            photoCaptures=captures.keys().asSequence().associateWith { key ->
                val m=captures.getJSONObject(key)
                val fix=com.tuempresa.inventariovial.core.InventoryDraftStore.restoreFix(m.optJSONObject("fix")) ?: if(m.isNull("latitude")||m.isNull("longitude")) null else GeoLocation(m.getDouble("latitude"),m.getDouble("longitude"),m.optDouble("accuracy",Double.NaN).toFloat(),
                    if(m.isNull("altitude"))null else m.getDouble("altitude"),timestamp=m.optLong("timestamp"))
                com.tuempresa.inventariovial.camera.PhotoCaptureMetadata(if(m.isNull("capturedAt"))null else m.getLong("capturedAt"),fix,m.getString("source"),if(m.isNull("replacesPath"))null else m.getString("replacesPath"))
            }
        }
    }
    LaunchedEffect(capturedPhotos,photoCaptures,photoCategories) {
        if(capturedPhotos.isNotEmpty() || draftId!=null) {
            kotlinx.coroutines.delay(400)
            if(gpsInputError==null) inventoryViewModel.saveDraft(currentDraftRequest(),{draftId=it;draftNotice="✓ Guardado en tablet · Borrador"},{draftNotice=it})
        }
    }

    val signalLength = com.tuempresa.inventariovial.validation.ContractualLength.calculate(
        com.tuempresa.inventariovial.validation.LengthEndpoint(route,roadbed,startPr,startDistance),
        com.tuempresa.inventariovial.validation.LengthEndpoint(route,roadbed,endPr,endDistance),prReference.prs,direction)
    com.tuempresa.inventariovial.field.LegacyInventoryFormScaffold(
        title="Registrar ${signalizationType.title}",subtitle="${signalizationType.sicCode} · ${signalizationType.subtitle}",
        onSaveDraft={if(gpsInputError==null) inventoryViewModel.saveDraft(currentDraftRequest(),{draftId=it;draftNotice="✓ Guardado en tablet · Borrador"},{draftNotice=it})},
        onBack=onBack,draftNotice=draftNotice
    ) {
        item {
            SurveyHeader(survey, segment, route, roadbed, direction,
                onSegment={ segment=it },
                onRoute={ route=it; routeSource=LocationSource.MANUAL_OVERRIDE; locationSource=LocationSource.MANUAL_OVERRIDE; endLocationSource=LocationSource.MANUAL_OVERRIDE },
                onRoadbed={ roadbed=it; locationSource=LocationSource.edited(locationSource, startSuggestion != null); endLocationSource=LocationSource.edited(endLocationSource, endSuggestion != null) }, onDirection={ direction=it })
        }
        item {
            com.tuempresa.inventariovial.field.OptionalDeviceGps(location,onChange={fix->
                location=fix;latitude=fix?.latitude;longitude=fix?.longitude;gpsAccuracy=fix?.horizontalAccuracy
                locationSource=fix?.source ?: "CONTRACTUAL_PR_ONLY"
            },onError={gpsInputError=it})
        }

        locationError?.let { error ->

            item {
                ErrorText(error)
            }
        }

        item {
            HorizontalDivider()
        }

        // UBICACIÓN VIAL COMÚN
        item {

            SectionTitle(if(signalizationType==SignalizationType.VERTICAL) "Ubicación del elemento" else "Ubicación inicio")
            com.tuempresa.inventariovial.field.ContractualPrField(
                value = startPr,
                onValueChange = {
                    locationSource = LocationSource.edited(locationSource, startSuggestion != null)
                    startPr = it
                },
                label = {
                    Text(if (ContractualPrInput.catalog(prReference.prs,route,roadbed).isEmpty()) if(signalizationType==SignalizationType.VERTICAL) "Kilómetro de progresiva (PR)" else "Kilómetro de progresiva inicial (PR)" else if(signalizationType==SignalizationType.VERTICAL) "Código PR" else "Código PR inicial")
                },
                placeholder = {
                    Text("4 dígitos, ej. 0010")
                },
                modifier =
                    Modifier.fillMaxWidth()
            )

            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )

            com.tuempresa.inventariovial.field.PrDistanceField(
                value = startDistance,
                onValueChange = {
                    locationSource = LocationSource.edited(locationSource, startSuggestion != null)
                    startDistance = it
                },
                label = {
                    Text(if (ContractualPrInput.catalog(prReference.prs,route,roadbed).isEmpty()) if(signalizationType==SignalizationType.VERTICAL) "Metros desde el kilómetro" else "Metros desde el kilómetro inicial" else if(signalizationType==SignalizationType.VERTICAL) "Metros desde el PR" else "Metros desde el PR inicial")
                },
                placeholder = {
                    Text("Ej. 125.50")
                },
                modifier =
                    Modifier.fillMaxWidth()
            )
        }

        if (signalizationType != SignalizationType.VERTICAL) {
        item {

            SectionTitle("Ubicación fin")
            com.tuempresa.inventariovial.field.ContractualPrField(
                value = endPr,
                onValueChange = {
                    endLocationSource = LocationSource.edited(endLocationSource, endSuggestion != null)
                    endPr = it
                },
                label = {
                    Text(if (ContractualPrInput.catalog(prReference.prs,route,roadbed).isEmpty()) "Kilómetro de progresiva final (PR)" else "Código PR final")
                },
                placeholder = {
                    Text("4 dígitos, ej. 0010")
                },
                modifier =
                    Modifier.fillMaxWidth()
            )

            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )

            com.tuempresa.inventariovial.field.PrDistanceField(
                value = endDistance,
                onValueChange = {
                    endLocationSource = LocationSource.edited(endLocationSource, endSuggestion != null)
                    endDistance = it
                },
                label = {
                    Text(if (ContractualPrInput.catalog(prReference.prs,route,roadbed).isEmpty()) "Metros desde el kilómetro final" else "Metros desde el PR final")
                },
                placeholder = {
                    Text("Ej. 180.25")
                },
                modifier =
                    Modifier.fillMaxWidth()
            )
        }

        }

        item {

            Text(
                text = "Lado",
                fontWeight =
                    FontWeight.Bold
            )

            ChoiceSelector(
                options = listOf(
                    "D - Derecho",
                    "I - Izquierdo",
                    "S - Sin objeto"
                ),
                selected = side,
                onSelected = {
                    sideSource = "MANUAL"
                    side = it
                }
            )
        }

        item {
            HorizontalDivider()
        }

        // CAMPOS SEGÚN TIPO
        item {

            when (signalizationType) {

                SignalizationType.VERTICAL -> {

                    Sic22Fields(
                        state = sic22State,
                        onStateChange = {
                            sic22State = it
                        }
                    )
                }

                SignalizationType.HORIZONTAL_MARKS,
                SignalizationType.HORIZONTAL_STUDS,
                SignalizationType.SAFETY -> {

                    if(com.tuempresa.inventariovial.validation.TechnicalElementRules.linear(SicFormDetail.Sic21(sic21State)))
                        com.tuempresa.inventariovial.field.TechnicalLengthField(signalLength)

                    Sic21Fields(
                        state = sic21State,
                        isNew = draftRecord == null,
                        onStateChange = {
                            sic21State = it
                        }
                    )
                }
            }
        }

        item {
            HorizontalDivider()
        }



        item {
            com.tuempresa.inventariovial.field.LegacyInventoryPhotos(signalizationType.sicCode,capturedPhotos,photoCategories,
                assetType=signalizationType.name,classCode=sic21State.classCode,typeCode=sic21State.typeCode,
                hasEnds=signalizationType!=SignalizationType.VERTICAL,
                onPhotoMetadata={path,metadata->photoCaptures=photoCaptures+(path to metadata)},captureMetadata=photoCaptures) {p,c->
                capturedPhotos=p;photoCategories=c;photoPath=p.lastOrNull();photoCaptured=p.isNotEmpty()
            }
        }
        if (
            photoCaptured &&
            photoPath != null
        ) {

            item {

                val path = photoPath!!

                val bitmap by produceState<android.graphics.Bitmap?>(null, path) {
                    value = withContext(Dispatchers.IO) { loadCorrectlyOrientedBitmap(path) }
                }

                if (bitmap != null) {

                    Image(
                        bitmap =
                            bitmap!!.asImageBitmap(),

                        contentDescription =
                            "Fotografía capturada",

                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),

                        contentScale =
                            ContentScale.Fit
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )

                Text(
                    text =
                        "✓ Fotografía guardada",
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    text =
                        File(path).name,
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall
                )
            }
        }

        photoError?.let { error ->

            item {
                ErrorText(error)
            }
        }


        item {

            PhotoStampPanel(capturedPhotos,PhotoStampData(route,roadbed,"$startPr + $startDistance m",location,
                location?.timestamp ?: System.currentTimeMillis(),fieldSession?.operator.orEmpty(),signalizationType.title),stampedPaths, photoCaptures = photoCaptures) { stampedPaths=it }
        }
        item {

            SectionTitle("Registro")

            Text(
                text =
                    "Fecha: $registrationDate"
            )

            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )

            OutlinedTextField(
                value = observations,
                onValueChange = {
                    observations = it
                },
                label = {
                    Text(
                        "OBSERVACIONES"
                    )
                },
                minLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            )
        }
        formError?.let { error ->

            item {
                ErrorText(error)
            }
        }

        item {

            Button(
                onClick = {

                    when {

                        gpsInputError != null -> { formError=gpsInputError }

                        route.trim().isEmpty() -> {
                            formError =
                                "Debes ingresar el código de ruta."
                        }

                        roadbed.trim().isEmpty() -> {
                            formError =
                                "Debes ingresar el código de calzada."
                        }

                        startPr.trim().isEmpty() -> {
                            formError =
                                "Debes ingresar el PR de inicio."
                        }

                        startDistance.trim().isEmpty() -> {

                            formError =
                                "Debes ingresar la distancia desde el PR de inicio."
                        }


                        signalizationType != SignalizationType.VERTICAL && endPr.trim().isEmpty() -> {

                            formError =
                                "Debes ingresar el PR de fin."
                        }


                        signalizationType != SignalizationType.VERTICAL && endDistance.trim().isEmpty() -> {

                            formError =
                                "Debes ingresar la distancia desde el PR de fin."
                        }


                        signalizationType ==
                                SignalizationType.VERTICAL &&
                                sic22State.usesSignalCode &&
                                sic22State.signalCodeSource != com.tuempresa.inventariovial.model.form.SignalCodeSource.UNIDENTIFIABLE &&
                                sic22State.signalCode
                                    .trim()
                                    .isEmpty() -> {

                            formError =
                                "Debes ingresar el código de la señal."
                        }


                        else -> {
                            formError = null

                            run {

                                val detail =
                                    when (signalizationType) {

                                        SignalizationType.VERTICAL ->
                                            SicFormDetail.Sic22(
                                                sic22State
                                            )

                                        SignalizationType.HORIZONTAL_MARKS,
                                        SignalizationType.HORIZONTAL_STUDS,
                                        SignalizationType.SAFETY ->
                                            SicFormDetail.Sic21(
                                                sic21State
                                            )
                                    }

                                inventoryViewModel.saveRecord(

                                    request =
                                        InventorySaveRequest(

                                            sicCode =
                                                if (
                                                    signalizationType ==
                                                    SignalizationType.VERTICAL
                                                ) {
                                                    "SIC-22"
                                                } else {
                                                    "SIC-21"
                                                },

                                            assetType =
                                                signalizationType.name,

                                            routeCode = route,

                                            roadbedCode = roadbed,

                                            startPrCode = startPr,

                                            startDistanceM =
                                                startDistance,

                                            endPrCode = endPr,

                                            endDistanceM =
                                                endDistance,

                                            sideCode =
                                                codeFromOption(
                                                    side
                                                ),

                                            latitude = latitude ?: Double.NaN,

                                            longitude = longitude ?: Double.NaN,

                                            gpsAccuracyM =
                                                gpsAccuracy,

                                            surveyDate =
                                                registrationDate,

                                            observations =
                                                observations,

                                            photoPath = capturedPhotos.firstOrNull().orEmpty(),
                                            photoPaths = capturedPhotos, photoCategories = photoCategories,
                                            recordId = draftId,
                                            location = location,
                                            endLocation = endLocation,
                                            locationSource = locationSource,
                                            endLocationSource = endLocationSource,
                                            routeSource = routeSource, startSuggestion = startSuggestion, endSuggestion = endSuggestion,
                                            photoCaptures = photoCaptures,
                                            sideSource = sideSource,
                                            sessionId = draftRecord?.sessionId ?: fieldSession?.sessionId,
                                            segment = segment, direction = direction,
                                            stampedPaths = stampedPaths.filterKeys { it in capturedPhotos },
                                            endLatitude = endLocation?.latitude,
                                            endLongitude = endLocation?.longitude,
                                            endGpsAccuracyM = endLocation?.accuracyHorizontal,

                                            detail = detail
                                        ),

                                    onSuccess = {
                                        onSave()
                                    },

                                    onError = { error ->
                                        formError = error
                                    }
                                )
                            }
                        }
                    }
                },

                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
            ) {

                Text(
                    text =
                        "Revisar y guardar",
                    fontSize = 20.sp,
                    fontWeight =
                        FontWeight.Bold
                )
            }
        }
    }
}


// =========================================================
// SIC-22 · SEÑALIZACIÓN VERTICAL
// INVENTARIO VIAL CALIFICADO
// =========================================================

@Composable
fun Sic22Fields(state: Sic22FormState, onStateChange: (Sic22FormState) -> Unit) {
    com.tuempresa.inventariovial.field.SignalCodeFields(state, onStateChange)
}

@Composable
fun Sic21Fields(
    state: Sic21FormState,
    isNew: Boolean = false,
    onStateChange: (Sic21FormState) -> Unit
) {

    val typeOptions =
        when (state.classCode) {

            "18" -> SicCatalogRepository.options("sic21.type.0")

            "19" -> SicCatalogRepository.options("sic21.type.1")

            "20" -> SicCatalogRepository.options("sic21.type.2")

            else ->
                SicCatalogRepository.options("sic21.type.3")
        }


    val materialOptions = SicCatalogRepository.options("sic21.material.0")


    val conditionOptions =
        when (state.classCode) {

            "18" -> SicCatalogRepository.options("sic21.condition.0")

            "20" -> SicCatalogRepository.options("sic21.condition.1")

            "19" -> SicCatalogRepository.options("sic21.condition.2")

            else ->
                SicCatalogRepository.options("sic21.condition.3")
        }


    val classDescription =
        when (state.classCode) {

            "18" ->
                "18 - Señalización Horizontal - Marcas"

            "19" ->
                "19 - Seguridad"

            "20" ->
                "20 - Señalización Horizontal - Tachas"

            else ->
                state.classCode
        }


    SectionTitle(
        "SIC-21 · Seguridad y señalización horizontal"
    )

    Text(
        "Inventario Vial Calificado",
        fontWeight = FontWeight.Bold,
        style =
            MaterialTheme.typography.bodySmall
    )


    Text(
        text =
            "Clase: $classDescription",
        fontWeight =
            FontWeight.Bold
    )


    Text(
        "Tipo",
        fontWeight = FontWeight.Bold
    )

    ChoiceSelector(
        options = typeOptions,
        selected = optionForCode(
            typeOptions,
            state.typeCode
        ),
        onSelected = {

            onStateChange(
                state.copy(
                    typeCode = codeFromOption(it),
                    materialCode = com.tuempresa.inventariovial.validation.TechnicalElementRules.selectSafetyType(state,codeFromOption(it),isNew).materialCode
                )
            )
        }
    )


    if (state.classCode == "19") {
    Text(
        "Material",
        fontWeight = FontWeight.Bold
    )

    ChoiceSelector(
        options = materialOptions,
        selected = optionForCode(
            materialOptions,
            state.materialCode
        ),
        onSelected = {

            onStateChange(
                state.copy(
                    materialCode =
                        codeFromOption(it)
                )
            )
        }
    )


    }
    Text(
        "Condición",
        fontWeight = FontWeight.Bold
    )

    ChoiceSelector(
        options = conditionOptions,
        selected = optionForCode(
            conditionOptions,
            state.conditionCode
        ),
        onSelected = {

            onStateChange(
                state.copy(
                    conditionCode =
                        codeFromOption(it)
                )
            )
        }
    )
}


// =========================================================
// FORMULARIO GENERAL PARA ALCANTARILLA / CUNETA / BADÉN / PUENTE
// =========================================================

@Composable
fun AssetFormScreen(
    assetType: RoadAssetType,
    inventoryViewModel: InventoryViewModel,
    onBack: () -> Unit,
    onSave: () -> Unit,
    draftRecord: InventoryRecordEntity? = null
) {

    var culvertExtraRaw by rememberSaveable {mutableStateOf("{}")}
    var culvertExpanded by rememberSaveable {mutableStateOf(false)}
    var pendingCulvertCondition by remember {mutableStateOf<Sic18FormState?>(null)}
    var culvertDraftError by remember {mutableStateOf<String?>(null)}
    var culvertExtraDirty by rememberSaveable {mutableStateOf(false)}
    var showCulvertExit by remember {mutableStateOf(false)}
    fun culvertExtra() = org.json.JSONObject(culvertExtraRaw).let {j->j.keys().asSequence().associateWith {j.getString(it)}}
    var savedCulvert by remember {mutableStateOf<InventoryRecordEntity?>(null)}
    savedCulvert?.let {record->
        com.tuempresa.inventariovial.field.CulvertUnifiedScreen(inventoryViewModel,record.id,culvertExtra(),onSave)
        return
    }
    val context =
        LocalContext.current
    val culvertScope=rememberCoroutineScope()
    fun persistCulvertExtra(id:String,after:(()->Unit)?=null) {
        if(assetType==RoadAssetType.CULVERT && culvertExtra().isNotEmpty()) culvertScope.launch {
            runCatching {com.tuempresa.inventariovial.sic.SicFieldRepository(com.tuempresa.inventariovial.data.database.InventoryDatabase.getInstance(context))
                .saveCulvertDraft(id,culvertExtra(),inventoryViewModel.field.auth.sessions.requireUser().id)}
                .onSuccess {culvertExtraDirty=false;after?.invoke()}.onFailure {culvertDraftError=it.message}
        } else after?.invoke()
    }
    LaunchedEffect(draftRecord?.id) {
        if(assetType==RoadAssetType.CULVERT && draftRecord!=null) {
            com.tuempresa.inventariovial.data.database.InventoryDatabase.getInstance(context).inventoryDao().sicExtension(draftRecord.id)?.let {
                culvertExtraRaw=org.json.JSONObject(it.values()).toString();culvertExpanded=true
            }
        }
    }
    val captureContext = com.tuempresa.inventariovial.field.rememberFieldCaptureContext(
        inventoryViewModel.field.dao, inventoryViewModel.field.projectRoutes, draftRecord)
    if (captureContext == null) { androidx.activity.compose.BackHandler(onBack=onBack); Text("Cargando sesión de campo…"); return }
    val fieldSession = captureContext.session
    val initialLocation = captureContext.location
    val survey = remember { SurveyPreferences(context) }
    val prior = remember { com.tuempresa.inventariovial.field.FieldSessionDefaults.matchingPosition(fieldSession, survey.last()) }
    var segment by rememberSaveable { mutableStateOf(initialLocation.segment) }
    var direction by rememberSaveable { mutableStateOf(initialLocation.direction) }

    var draftId by rememberSaveable { mutableStateOf(draftRecord?.id) }
    var location by remember { mutableStateOf(draftRecord?.takeIf {it.latitude!=null && it.longitude!=null}?.let {
        GeoLocation(requireNotNull(it.latitude),requireNotNull(it.longitude),it.gpsAccuracyM?.toFloat() ?: Float.POSITIVE_INFINITY,it.altitudeM,source=it.locationSource,timestamp=it.gpsTimestamp ?: 0)
    }) }
    val prReference by inventoryViewModel.field.reference.collectAsState()
    val prSettings by inventoryViewModel.field.settings.collectAsState()
    var locationSource by rememberSaveable { mutableStateOf(draftRecord?.locationSource ?: "MANUAL") }
    var endLocationSource by rememberSaveable { mutableStateOf(draftRecord?.endLocationSource ?: "MANUAL") }
    var sideSource by rememberSaveable { mutableStateOf("MANUAL") }
    var stampedPaths by remember { mutableStateOf<Map<String,String>>(emptyMap()) }
    var photoCaptures by remember { mutableStateOf<Map<String, com.tuempresa.inventariovial.camera.PhotoCaptureMetadata>>(emptyMap()) }
    var photoGpsWarning by remember { mutableStateOf<String?>(null) }
    var routeSource by rememberSaveable { mutableStateOf(draftRecord?.routeSource ?: LocationSource.MANUAL) }
    var startSuggestion by remember { mutableStateOf<LocationSuggestion?>(null) }
    var endSuggestion by remember { mutableStateOf<LocationSuggestion?>(null) }


    var route by rememberSaveable {
        mutableStateOf(initialLocation.route)
    }

    var roadbed by rememberSaveable {
        mutableStateOf(initialLocation.roadbed)
    }

    var startPr by rememberSaveable {
        mutableStateOf(draftRecord?.startPrCode ?: prior?.pr.orEmpty())
    }

    var startDistance by rememberSaveable {
        mutableStateOf(draftRecord?.startDistanceM?.toString().orEmpty())
    }

    LaunchedEffect(route, roadbed, startPr, startDistance, location, fieldSession?.operator) {
        // A confirmed stamp must reflect the current form's location and operator.
        stampedPaths = emptyMap()
    }

    var endPr by remember {
        mutableStateOf("")
    }

    var endDistance by remember {
        mutableStateOf("")
    }

    var side by remember {
        mutableStateOf("D - Derecho")
    }

    var observations by remember {
        mutableStateOf("")
    }

    val registrationDate = remember {
        draftRecord?.surveyDate ?: SimpleDateFormat(
            "dd/MM/yyyy",
            Locale.getDefault()
        ).format(Date())
    }

    var capturedPhotos by remember { mutableStateOf<List<String>>(emptyList()) }
    var photoCategories by remember {mutableStateOf<Map<String,String>>(emptyMap())}
    var endLocation by remember { mutableStateOf<GeoLocation?>(null) }
    var photoPath by remember {
        mutableStateOf<String?>(null)
    }

    var photoCaptured by remember {
        mutableStateOf(false)
    }

    var photoError by remember {
        mutableStateOf<String?>(null)
    }

    var latitude by remember {
        mutableStateOf<Double?>(location?.latitude)
    }

    var longitude by remember {
        mutableStateOf<Double?>(location?.longitude)
    }

    var gpsAccuracy by remember {
        mutableStateOf<Float?>(location?.horizontalAccuracy)
    }

    var locating by remember {
        mutableStateOf(false)
    }

    var gpsInputError by remember {mutableStateOf<String?>(null)}
    var locationError by remember {
        mutableStateOf<String?>(null)
    }

    var formError by remember {
        mutableStateOf<String?>(null)
    }

    // =========================================================
    // ESTADOS SIC CALIFICADOS
    // =========================================================

    var sic17State by remember(assetType) {
        mutableStateOf(
            Sic17FormState()
        )
    }

    var sic18State by remember(assetType) {
        mutableStateOf(
            Sic18FormState()
        )
    }

    var sic19State by remember(assetType) {
        mutableStateOf(
            Sic19FormState()
        )
    }

    var sic20State by remember(assetType) {
        mutableStateOf(
            Sic20FormState(classCode = when(assetType) { RoadAssetType.TUNNEL -> "13"; RoadAssetType.WALL -> "14"; else -> "12" })
        )
    }


    var sic23State by remember(assetType) { mutableStateOf(Sic23FormState()) }

    fun hasLocationPermission(): Boolean {

        val fine =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) ==
                    PackageManager.PERMISSION_GRANTED

        val coarse =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) ==
                    PackageManager.PERMISSION_GRANTED

        return fine || coarse
    }


    fun captureLocation() {

        if (!hasLocationPermission()) return

        locating = true
        locationError = null

        TabletLocationProvider(context).getCurrentLocation(
            onSuccess = { fix ->
                latitude = fix.latitude
                longitude = fix.longitude
                gpsAccuracy = fix.horizontalAccuracy
                location = fix
                locating = false
            },

            onError = { error ->

                locationError = error
                locating = false
            }
        )
    }








    val takePictureLauncher =
        rememberLauncherForActivityResult(
            contract =
                com.tuempresa.inventariovial.core.FieldTakePicture()
        ) { success ->

            if (success) photoPath?.let { path ->
                capturedPhotos = capturedPhotos + path
                val capturedAt = System.currentTimeMillis()
                photoCaptures = photoCaptures + (path to com.tuempresa.inventariovial.camera.PhotoCaptureMetadata(capturedAt, null))
                com.tuempresa.inventariovial.camera.PhotoCaptureMetadata.capture(context, capturedAt) { metadata ->
                    photoCaptures = photoCaptures + (path to metadata)
                    photoGpsWarning = if (metadata.fix == null) "Fotografía guardada sin GNSS propio disponible." else null
                }
            }
            photoCaptured = capturedPhotos.isNotEmpty()
            photoPath = capturedPhotos.lastOrNull()

            if (success) {

                photoError = null



            } else {

                photoError =
                    "La fotografía fue cancelada o no pudo guardarse."
            }
        }


    fun openCamera() {

        try {

            val prefix = when (assetType) {

                RoadAssetType.RIGHT_OF_WAY -> "DERECHO_VIA"

                RoadAssetType.CULVERT ->
                    "ALCANTARILLA"

                RoadAssetType.DITCH ->
                    "CUNETA"

                RoadAssetType.FORD, RoadAssetType.TUNNEL, RoadAssetType.WALL ->
                    "SIC20"

                RoadAssetType.BRIDGE ->
                    "PUENTE"

                RoadAssetType.SIGNALIZATION ->
                    "SENALIZACION"
            }

            val photo =
                createPhotoFile(
                    context = context,
                    prefix = prefix
                )

            photoPath =
                photo.file.absolutePath

            photoError = null

            takePictureLauncher.launch(
                photo.uri
            )

        } catch (error: Exception) {

            photoError =
                error.message
                    ?: "No se pudo abrir la cámara."
        }
    }


    val cameraPermissionLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts
                    .RequestPermission()
        ) { granted ->

            if (granted) {

                openCamera()

            } else {

                photoError =
                    "Debes permitir el acceso a la cámara."
            }
        }


    val needsEndLocation =
        assetType == RoadAssetType.DITCH ||
                assetType in setOf(RoadAssetType.FORD, RoadAssetType.TUNNEL, RoadAssetType.WALL) ||
                assetType == RoadAssetType.BRIDGE ||
                assetType == RoadAssetType.RIGHT_OF_WAY

    val assetLength = com.tuempresa.inventariovial.validation.ContractualLength.calculate(
        com.tuempresa.inventariovial.validation.LengthEndpoint(route,roadbed,startPr,startDistance),
        com.tuempresa.inventariovial.validation.LengthEndpoint(route,roadbed,endPr,endDistance),prReference.prs,direction)
    var wallManual by rememberSaveable {mutableStateOf(draftRecord!=null)}
    LaunchedEffect(startPr,startDistance,endPr,endDistance,route,roadbed,direction,prReference,wallManual) {
        if(assetType==RoadAssetType.WALL && !wallManual)
            sic20State=sic20State.copy(wallLengthMeters=assetLength.metres?.toString().orEmpty(),lengthMode="AUTO")
    }

    val needsSide =
        assetType == RoadAssetType.DITCH ||
                assetType in setOf(RoadAssetType.FORD, RoadAssetType.TUNNEL, RoadAssetType.WALL) ||
                assetType == RoadAssetType.RIGHT_OF_WAY

    LaunchedEffect(assetType) {if(draftRecord==null && assetType==RoadAssetType.TUNNEL) side="S - Sin objeto"}

    val sideOptions =
        when (assetType) {

            RoadAssetType.DITCH ->
                listOf(
                    "D - Derecho",
                    "I - Izquierdo"
                )

            RoadAssetType.TUNNEL -> listOf("S - Sin objeto")
            RoadAssetType.WALL -> listOf("D - Derecho","I - Izquierdo")
            RoadAssetType.FORD, RoadAssetType.RIGHT_OF_WAY ->
                listOf(
                    "D - Derecho",
                    "I - Izquierdo",
                    "S - Sin objeto"
                )

            else ->
                emptyList()
        }


    val saveAsset = fun() {

                    when {

                        assetType==RoadAssetType.CULVERT && culvertExtra()["functionCode"].isNullOrBlank() -> {
                            formError="Selecciona la función de la alcantarilla antes de guardar la ficha completa."
                        }

                        gpsInputError != null -> { formError=gpsInputError }

                        route.trim().isEmpty() -> {
                            formError =
                                "Debes ingresar el código de ruta."
                        }

                        roadbed.trim().isEmpty() -> {
                            formError =
                                "Debes ingresar el código de calzada."
                        }

                        startPr.trim().isEmpty() -> {
                            formError =
                                "Debes ingresar el PR de inicio."
                        }

                        startDistance.trim().isEmpty() -> {

                            formError =
                                "Debes ingresar la distancia desde el PR de inicio."
                        }


                        needsEndLocation &&
                                endPr.trim().isEmpty() -> {

                            formError =
                                "Debes ingresar el PR de fin."
                        }


                        needsEndLocation &&
                                endDistance.trim().isEmpty() -> {

                            formError =
                                "Debes ingresar la distancia desde el PR de fin."
                        }


                        // =========================================================
                        // VALIDACIÓN SIC-17
                        // =========================================================

                        assetType == RoadAssetType.BRIDGE &&
                                sic17State.dimension1LengthM
                                    .trim()
                                    .isEmpty() -> {

                            formError =
                                "Debes ingresar la longitud del puente."
                        }


                        assetType == RoadAssetType.BRIDGE &&
                                sic17State.dimension2LowerHeightM
                                    .trim()
                                    .isEmpty() -> {

                            formError =
                                "Debes ingresar la altura libre inferior."
                        }


                        assetType == RoadAssetType.BRIDGE &&
                                sic17State.dimension3UpperHeightM
                                    .trim()
                                    .isEmpty() -> {

                            formError =
                                "Debes ingresar la altura libre superior. Si no existe limitación, usa 00.00."
                        }


                        // =========================================================
                        // VALIDACIÓN SIC-18
                        // =========================================================

                        assetType == RoadAssetType.CULVERT &&
                                sic18State.spans
                                    .trim()
                                    .isEmpty() -> {

                            formError =
                                "Debes ingresar el número de ojos / vanos."
                        }


                        assetType == RoadAssetType.CULVERT &&
                                sic18State.dimension1M
                                    .trim()
                                    .isEmpty() -> {

                            formError =
                                "Debes ingresar la Dimensión 1."
                        }


                        assetType == RoadAssetType.CULVERT && sic18State.usesDimension2 &&
                                sic18State.dimension2M
                                    .trim()
                                    .isEmpty() -> {

                            formError =
                                "Debes ingresar la Dimensión 2."
                        }


                        // =========================================================
                        // VALIDACIÓN SIC-20
                        // =========================================================

                        assetType in setOf(RoadAssetType.FORD, RoadAssetType.TUNNEL, RoadAssetType.WALL) &&
                                sic20State.dimension1M
                                    .trim()
                                    .isEmpty() -> {

                            formError =
                                "Debes ingresar la Dimensión 1."
                        }


                        assetType in setOf(RoadAssetType.FORD, RoadAssetType.TUNNEL, RoadAssetType.WALL) &&
                                sic20State.usesDimension2 &&
                                sic20State.dimension2M
                                    .trim()
                                    .isEmpty() -> {

                            formError =
                                "Debes ingresar la Dimensión 2."
                        }

                        else -> {
                            formError = null

                            run {

                                val detail =
                                    when (assetType) {

                                        RoadAssetType.RIGHT_OF_WAY -> SicFormDetail.Sic23(sic23State)

                                        RoadAssetType.BRIDGE ->
                                            SicFormDetail.Sic17(
                                                sic17State
                                            )

                                        RoadAssetType.CULVERT ->
                                            SicFormDetail.Sic18(
                                                sic18State
                                            )

                                        RoadAssetType.DITCH ->
                                            SicFormDetail.Sic19(
                                                sic19State
                                            )

                                        RoadAssetType.FORD, RoadAssetType.TUNNEL, RoadAssetType.WALL ->
                                            SicFormDetail.Sic20(
                                                sic20State
                                            )

                                        RoadAssetType.SIGNALIZATION -> {
                                            formError =
                                                "Utilice el módulo de señalización."
                                            return
                                        }
                                    }

                                val actualAssetType =
                                    when (assetType) {

                                        RoadAssetType.RIGHT_OF_WAY -> sic23State.assetName

                                        RoadAssetType.BRIDGE ->
                                            "PUENTE"

                                        RoadAssetType.CULVERT ->
                                            "ALCANTARILLA"

                                        RoadAssetType.DITCH ->
                                            "DRENAJE"

                                        RoadAssetType.FORD, RoadAssetType.TUNNEL, RoadAssetType.WALL ->
                                            when (
                                                sic20State.classCode
                                            ) {
                                                "12" -> "BADEN"
                                                "13" -> "TUNEL"
                                                "14" -> "MURO"
                                                else -> "SIC20"
                                            }

                                        else ->
                                            "SENALIZACION"
                                    }

                                inventoryViewModel.saveRecord(

                                    request =
                                        InventorySaveRequest(

                                            sicCode =
                                                assetType.sicCode,

                                            assetType =
                                                actualAssetType,

                                            routeCode = route,

                                            roadbedCode = roadbed,

                                            startPrCode = startPr,

                                            startDistanceM =
                                                startDistance,

                                            endPrCode =
                                                if (needsEndLocation) {
                                                    endPr
                                                } else {
                                                    null
                                                },

                                            endDistanceM =
                                                if (needsEndLocation) {
                                                    endDistance
                                                } else {
                                                    null
                                                },

                                            sideCode =
                                                if (needsSide) {
                                                    codeFromOption(
                                                        side
                                                    )
                                                } else {
                                                    null
                                                },

                                            latitude = latitude ?: Double.NaN,

                                            longitude = longitude ?: Double.NaN,

                                            gpsAccuracyM =
                                                gpsAccuracy,

                                            surveyDate =
                                                registrationDate,

                                            observations =
                                                observations,

                                            photoPath = capturedPhotos.firstOrNull().orEmpty(),
                                            photoPaths = capturedPhotos, photoCategories = photoCategories,
                                            recordId = draftId,
                                            location = location,
                                            endLocation = endLocation,
                                            locationSource = locationSource,
                                            endLocationSource = endLocationSource,
                                            routeSource = routeSource, startSuggestion = startSuggestion, endSuggestion = endSuggestion,
                                            photoCaptures = photoCaptures,
                                            sideSource = sideSource,
                                            sessionId = draftRecord?.sessionId ?: fieldSession?.sessionId,
                                            segment = segment, direction = direction,
                                            stampedPaths = stampedPaths.filterKeys { it in capturedPhotos },
                                            endLatitude = endLocation?.latitude,
                                            endLongitude = endLocation?.longitude,
                                            endGpsAccuracyM = endLocation?.accuracyHorizontal,

                                            detail = detail
                                        ),

                                    onSuccess = {
                                        if(assetType==RoadAssetType.CULVERT) {
                                            savedCulvert=inventoryViewModel.lastSavedRecord
                                        } else onSave()
                                    },
                                    onError = { error -> formError = error }
                                )
                            }
                        }
                    }
    }

    fun currentDraftRequest():InventorySaveRequest { return InventorySaveRequest(
        sicCode=assetType.sicCode,assetType=when(assetType) { RoadAssetType.BRIDGE->"PUENTE";RoadAssetType.CULVERT->"ALCANTARILLA";RoadAssetType.DITCH->"DRENAJE";RoadAssetType.RIGHT_OF_WAY->sic23State.assetName;else->assetType.name },routeCode=route,roadbedCode=roadbed,
        startPrCode=startPr,startDistanceM=startDistance,endPrCode=endPr,endDistanceM=endDistance,
        sideCode=codeFromOption(side),latitude=latitude ?: Double.NaN,longitude=longitude ?: Double.NaN,
        gpsAccuracyM=gpsAccuracy,surveyDate=registrationDate,observations=observations,
        photoPath=capturedPhotos.firstOrNull().orEmpty(),photoPaths=capturedPhotos,recordId=draftId ?: java.util.UUID.randomUUID().toString().also { draftId=it },
        detail=when(assetType) { RoadAssetType.RIGHT_OF_WAY->SicFormDetail.Sic23(sic23State);RoadAssetType.BRIDGE->SicFormDetail.Sic17(sic17State);RoadAssetType.CULVERT->SicFormDetail.Sic18(sic18State);RoadAssetType.DITCH->SicFormDetail.Sic19(sic19State);else->SicFormDetail.Sic20(sic20State) },location=location,endLocation=endLocation,locationSource=locationSource,
        endLocationSource=endLocationSource,routeSource=routeSource,sideSource=sideSource,
        sessionId=draftRecord?.sessionId ?: fieldSession?.sessionId,segment=segment,direction=direction,
        photoCaptures=photoCaptures,photoCategories=photoCategories) }
    var draftNotice by remember { mutableStateOf("") }
    LaunchedEffect(draftRecord?.id) {
        val raw=com.tuempresa.inventariovial.core.InventoryDraftStore.read(context,draftRecord?.id)
        if(raw!=null) {
            startDistance=raw.getString("startDistanceM")
            endDistance=if(raw.isNull("endDistanceM")) "" else raw.getString("endDistanceM")
            observations=draftRecord?.observations.orEmpty()
            side=draftRecord?.sideCode ?: side
            endPr=draftRecord?.endPrCode.orEmpty()
            if(draftRecord?.locationSource=="DRAFT_LOCATION_PENDING") { latitude=null;longitude=null;location=null }
            location=com.tuempresa.inventariovial.core.InventoryDraftStore.restoreFix(raw.optJSONObject("location")) ?: location
            endLocation=com.tuempresa.inventariovial.core.InventoryDraftStore.restoreFix(raw.optJSONObject("endLocation"))
            sideSource=raw.optString("sideSource",sideSource)
            val paths=raw.getJSONArray("photoPaths");capturedPhotos=(0 until paths.length()).map { paths.getString(it) }
            photoCaptured=capturedPhotos.isNotEmpty();photoPath=capturedPhotos.lastOrNull()
            sic17State=com.tuempresa.inventariovial.core.InventoryDraftStore.restore(raw,sic17State)
            sic18State=com.tuempresa.inventariovial.core.InventoryDraftStore.restore(raw,sic18State)
            sic19State=com.tuempresa.inventariovial.core.InventoryDraftStore.restore(raw,sic19State)
            sic20State=com.tuempresa.inventariovial.core.InventoryDraftStore.restore(raw,sic20State)
            if(!raw.getJSONObject("detail").getJSONObject("fields").has("lengthMode")) sic20State=sic20State.copy(lengthMode="HISTORICAL")
            wallManual=sic20State.lengthMode!="AUTO"
            sic23State=com.tuempresa.inventariovial.core.InventoryDraftStore.restore(raw,sic23State)
            val cats=raw.getJSONObject("photoCategories");photoCategories=cats.keys().asSequence().associateWith { cats.getString(it) }
            val captures=raw.getJSONObject("photoCaptures")
            photoCaptures=captures.keys().asSequence().associateWith { key ->
                val m=captures.getJSONObject(key)
                val fix=com.tuempresa.inventariovial.core.InventoryDraftStore.restoreFix(m.optJSONObject("fix")) ?: if(m.isNull("latitude")||m.isNull("longitude")) null else GeoLocation(m.getDouble("latitude"),m.getDouble("longitude"),m.optDouble("accuracy",Double.NaN).toFloat(),
                    if(m.isNull("altitude"))null else m.getDouble("altitude"),timestamp=m.optLong("timestamp"))
                com.tuempresa.inventariovial.camera.PhotoCaptureMetadata(if(m.isNull("capturedAt"))null else m.getLong("capturedAt"),fix,m.getString("source"),if(m.isNull("replacesPath"))null else m.getString("replacesPath"))
            }
        }
    }
    LaunchedEffect(capturedPhotos,photoCaptures,photoCategories) {
        if(capturedPhotos.isNotEmpty() || draftId!=null) {
            kotlinx.coroutines.delay(400)
            if(gpsInputError==null) inventoryViewModel.saveDraft(currentDraftRequest(),{draftId=it;persistCulvertExtra(it);draftNotice="✓ Guardado en tablet · Borrador"},{draftNotice=it})
        }
    }

    com.tuempresa.inventariovial.field.LegacyInventoryFormScaffold(
        title="Registrar ${assetType.title}",subtitle="${assetType.sicCode} · ${assetType.subtitle}",
        onSaveDraft={if(gpsInputError==null) inventoryViewModel.saveDraft(currentDraftRequest(),{draftId=it;persistCulvertExtra(it);draftNotice="✓ Guardado en tablet · Borrador"},{draftNotice=it})},
        onBack={if(assetType==RoadAssetType.CULVERT && culvertExtraDirty) showCulvertExit=true else onBack()},draftNotice=draftNotice,
        draftErrors={culvertDraftError?.let {ErrorText(it)}}
    ) {
        // FOTO
        item {
            SurveyHeader(survey, segment, route, roadbed, direction,
                onSegment={ segment=it },
                onRoute={ route=it; routeSource=LocationSource.MANUAL_OVERRIDE; locationSource=LocationSource.MANUAL_OVERRIDE; endLocationSource=LocationSource.MANUAL_OVERRIDE },
                onRoadbed={ roadbed=it; locationSource=LocationSource.edited(locationSource, startSuggestion != null); endLocationSource=LocationSource.edited(endLocationSource, endSuggestion != null) }, onDirection={ direction=it })
        }
        item {
            com.tuempresa.inventariovial.field.OptionalDeviceGps(location,onChange={fix->
                location=fix;latitude=fix?.latitude;longitude=fix?.longitude;gpsAccuracy=fix?.horizontalAccuracy
                locationSource=fix?.source ?: "CONTRACTUAL_PR_ONLY"
            },onError={gpsInputError=it})
        }

        locationError?.let { error ->

            item {
                ErrorText(error)
            }
        }

        item {
            HorizontalDivider()
        }

        item {
            com.tuempresa.inventariovial.field.LegacyLocationSection(prReference,prSettings.matchConfig(),route,roadbed,
                location,startPr,startDistance,locationSource,"inicio",
                {if(it!=null && (locationSource!=LocationSource.MANUAL_OVERRIDE || (startSuggestion==null && draftRecord?.suggestedRouteCode==null && draftRecord?.suggestedPrCode==null)))startSuggestion=it},
                {suggestion->
                    startSuggestion=suggestion;route=suggestion.suggestedRouteCode ?: route
                    segment=inventoryViewModel.field.projectRoutes.find(route)?.segmentCode.orEmpty()
                    routeSource=LocationSource.GNSS_CONFIRMED
                    suggestion.pr?.let {startPr=it.suggestedPrCode;startDistance=it.distanceText}
                    locationSource=if(suggestion.pr==null)LocationSource.GNSS_CONFIRMED else LocationSource.GNSS_PR_CONFIRMED
                },{locationSource=LocationSource.MANUAL_OVERRIDE;routeSource=LocationSource.MANUAL_OVERRIDE},
                {locationSource=LocationSource.edited(locationSource,startSuggestion!=null);startPr=it},
                {locationSource=LocationSource.edited(locationSource,startSuggestion!=null);startDistance=it},inventoryViewModel.field)
        }
        if(needsEndLocation) item {
            com.tuempresa.inventariovial.field.LegacyLocationSection(prReference,prSettings.matchConfig(),route,roadbed,
                endLocation,endPr,endDistance,endLocationSource,"fin",
                {if(it!=null && (endLocationSource!=LocationSource.MANUAL_OVERRIDE || (endSuggestion==null && draftRecord?.endSuggestedPrCode==null)))endSuggestion=it},
                {suggestion->if(route==suggestion.suggestedRouteCode) {
                    endSuggestion=suggestion;suggestion.pr?.let {endPr=it.suggestedPrCode;endDistance=it.distanceText}
                    endLocationSource=if(suggestion.pr==null)LocationSource.GNSS_CONFIRMED else LocationSource.GNSS_PR_CONFIRMED
                }},{endLocationSource=LocationSource.MANUAL_OVERRIDE},
                {endLocationSource=LocationSource.edited(endLocationSource,endSuggestion!=null);endPr=it},
                {endLocationSource=LocationSource.edited(endLocationSource,endSuggestion!=null);endDistance=it})
        }
        if(assetType in setOf(RoadAssetType.WALL,RoadAssetType.DITCH,RoadAssetType.FORD)) item {
            com.tuempresa.inventariovial.field.TechnicalLengthField(assetLength,
                if(assetType==RoadAssetType.WALL) {{metres:Double->wallManual=false;sic20State=sic20State.copy(wallLengthMeters=metres.toString(),lengthMode="AUTO")}} else null)
        }

        if (needsSide) {

            item {

                Text(
                    text = "Lado",
                    fontWeight =
                        FontWeight.Bold
                )

                ChoiceSelector(
                    options = sideOptions,
                    selected = side,
                    onSelected = {
                        sideSource = "MANUAL"
                        side = it
                    }
                )
            }
        }

        item {
            HorizontalDivider()
        }

        item {

            when (assetType) {

                RoadAssetType.RIGHT_OF_WAY -> Sic23Fields(sic23State) { sic23State = it }

                RoadAssetType.CULVERT -> {

                    Sic18Fields(
                        state = sic18State,
                        onCompleteSic18A = {culvertExpanded=true},
                        beforeConditions = {
                            com.tuempresa.inventariovial.field.CulvertFunctionField(culvertExtra()) {values->
                                culvertExtraRaw=org.json.JSONObject(values).toString();culvertExtraDirty=true
                            }
                        },
                        onStateChange = {
                            if(com.tuempresa.inventariovial.sic.InventoryElements.requiresCulvertDetail(sic18State.structuralConditionCode,sic18State.functionalConditionCode) &&
                                !com.tuempresa.inventariovial.sic.InventoryElements.requiresCulvertDetail(it.structuralConditionCode,it.functionalConditionCode) && culvertExtra().isNotEmpty()) pendingCulvertCondition=it
                            else sic18State = it
                        }
                    )
                    if(com.tuempresa.inventariovial.sic.InventoryElements.requiresCulvertDetail(sic18State.structuralConditionCode,sic18State.functionalConditionCode) || culvertExpanded) {
                        Text("$route · $roadbed · PR $startPr + $startDistance m · ${sic18State.spans} vanos")
                        com.tuempresa.inventariovial.field.CulvertSupplementFields(culvertExtra(),true) {values->culvertExtraRaw=org.json.JSONObject(values).toString();culvertExtraDirty=true}
                    }
                }

                RoadAssetType.DITCH -> {

                    Sic19Fields(
                        state = sic19State,
                        onStateChange = {
                            sic19State = it
                        }
                    )
                }

                RoadAssetType.FORD, RoadAssetType.TUNNEL, RoadAssetType.WALL -> {

                    Sic20Fields(
                        state = sic20State,
                        onManualLength = {wallManual=true;sic20State=sic20State.copy(lengthMode="MEASURED")},
                        onStateChange = {
                            sic20State = it.copy(lengthMode=if(wallManual) "MEASURED" else "AUTO")
                        }
                    )
                }

                RoadAssetType.BRIDGE -> {

                    Sic17Fields(
                        state = sic17State,
                        onStateChange = {
                            sic17State = it
                        }
                    )
                }

                RoadAssetType.SIGNALIZATION -> {

                    Text(
                        "Use el módulo Señalización y seguridad."
                    )
                }
            }
        }

        item {
            HorizontalDivider()
        }



        item {
            com.tuempresa.inventariovial.field.LegacyInventoryPhotos(assetType.sicCode,capturedPhotos,photoCategories,
                assetType=assetType.name,classCode=if(assetType.sicCode=="SIC-23")sic23State.classCode else sic20State.classCode,typeCode=sic20State.typeCode,
                onPhotoMetadata={path,metadata->photoCaptures=photoCaptures+(path to metadata)},captureMetadata=photoCaptures) {p,c->
                capturedPhotos=p;photoCategories=c;photoPath=p.lastOrNull();photoCaptured=p.isNotEmpty()
            }
        }
        if (
            photoCaptured &&
            photoPath != null
        ) {

            item {

                val path =
                    photoPath!!

                val bitmap by produceState<android.graphics.Bitmap?>(null, path) {
                    value = withContext(Dispatchers.IO) { loadCorrectlyOrientedBitmap(path) }
                }

                if (bitmap != null) {

                    Image(
                        bitmap =
                            bitmap!!.asImageBitmap(),

                        contentDescription =
                            "Fotografía capturada",

                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),

                        contentScale =
                            ContentScale.Fit
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )

                Text(
                    text =
                        "✓ Fotografía guardada",
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    text =
                        File(path).name,
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall
                )
            }
        }

        photoError?.let { error ->

            item {
                ErrorText(error)
            }
        }


        item {

            PhotoStampPanel(capturedPhotos,PhotoStampData(route,roadbed,"$startPr + $startDistance m",location,
                location?.timestamp ?: System.currentTimeMillis(),fieldSession?.operator.orEmpty(),assetType.title),stampedPaths, photoCaptures = photoCaptures) { stampedPaths=it }
        }
        item {

            SectionTitle("Registro")

            Text(
                text =
                    "Fecha: $registrationDate"
            )

            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )

            OutlinedTextField(
                value = observations,
                onValueChange = {
                    observations = it
                },
                label = {
                    Text(
                        "OBSERVACIONES"
                    )
                },
                minLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            )
        }
        formError?.let { error ->

            item {
                ErrorText(error)
            }
        }

        item {com.tuempresa.inventariovial.field.LegacyInventoryReviewButton(onReview=saveAsset)}
    }
    pendingCulvertCondition?.let {next->androidx.compose.material3.AlertDialog(onDismissRequest={pendingCulvertCondition=null},title={Text("Conservar detalle existente")},
        text={Text("Esta alcantarilla ya contiene un detalle de condición mala. La condición actual ya no requiere SIC-18A.")},
        confirmButton={TextButton(onClick={sic18State=next;pendingCulvertCondition=null}) {Text("Conservar como antecedente")}},
        dismissButton={TextButton(onClick={pendingCulvertCondition=null}) {Text("Cancelar cambio")}})}

    androidx.activity.compose.BackHandler(enabled=assetType==RoadAssetType.CULVERT && culvertExtraDirty) {showCulvertExit=true}
    if(showCulvertExit) androidx.compose.material3.AlertDialog(onDismissRequest={showCulvertExit=false},title={Text("Guardar cambios")},
        text={Text("Conserva el detalle de condición mala como borrador antes de volver.")},
        confirmButton={TextButton(onClick={if(gpsInputError==null) inventoryViewModel.saveDraft(currentDraftRequest(),{draftId=it;persistCulvertExtra(it){showCulvertExit=false;onBack()}},{culvertDraftError=it})}) {Text("Guardar borrador y volver")}},
        dismissButton={TextButton(onClick={showCulvertExit=false}) {Text("Seguir editando")}})

}


// =========================================================
// SIC-18 · ALCANTARILLAS
// INVENTARIO VIAL CALIFICADO
// =========================================================

@Composable
fun Sic18Fields(
    state: Sic18FormState,
    onCompleteSic18A: (() -> Unit)? = null,
    beforeConditions: @Composable () -> Unit = {},
    onStateChange: (Sic18FormState) -> Unit
) {

    val classOptions = SicCatalogRepository.options("sic18.class.0")

    val typeOptions =
        if (state.classCode == "06") {

            SicCatalogRepository.options("sic18.type.0")

        } else {

            SicCatalogRepository.options("sic18.type.1")
        }

    val sectionOptions = SicCatalogRepository.options("sic18.section.0")

    val structuralOptions = SicCatalogRepository.options("sic18.structural.0")

    val functionalOptions = SicCatalogRepository.options("sic18.functional.0")


    SectionTitle(
        "Datos de la alcantarilla · SIC-18"
    )

    Text(
        "Inventario Vial Calificado",
        fontWeight = FontWeight.Bold,
        style =
            MaterialTheme.typography.bodySmall
    )


    Text(
        "Clase",
        fontWeight = FontWeight.Bold
    )

    ChoiceSelector(
        options = classOptions,
        selected = optionForCode(
            classOptions,
            state.classCode
        ),
        onSelected = {

            onStateChange(
                state.copy(
                    classCode =
                        codeFromOption(it),
                    typeCode = "1"
                )
            )
        }
    )


    Text(
        "Tipo / material",
        fontWeight = FontWeight.Bold
    )

    ChoiceSelector(
        options = typeOptions,
        selected = optionForCode(
            typeOptions,
            state.typeCode
        ),
        onSelected = {

            onStateChange(
                state.copy(
                    typeCode =
                        codeFromOption(it)
                )
            )
        }
    )


    OutlinedTextField(
        value = state.spans,
        onValueChange = { value ->

            onStateChange(
                state.copy(
                    spans =
                        value.filter {
                            it.isDigit()
                        }
                )
            )
        },
        label = {
            Text(
                "Número de ojos / vanos"
            )
        },
        modifier =
            Modifier.fillMaxWidth()
    )


    Text(
        "Sección transversal",
        fontWeight = FontWeight.Bold
    )

    ChoiceSelector(
        options = sectionOptions,
        selected = optionForCode(
            sectionOptions,
            state.crossSectionCode
        ),
        onSelected = {

            onStateChange(
                state.copy(
                    crossSectionCode =
                        codeFromOption(it)
                )
            )
        }
    )


    if (state.crossSectionCode == "2") {
        Text("Forma de la sección")
        ChoiceSelector(listOf("Circular", "Ovalada"), if(state.sectionShape == "CIRCULAR") "Circular" else "Ovalada") {
            onStateChange(state.copy(sectionShape = if(it == "Circular") "CIRCULAR" else "OVAL",
                dimension2M = if(it == "Circular") "" else state.dimension2M))
        }
    }
    OutlinedTextField(
        value = state.dimension1M,
        onValueChange = {

            onStateChange(
                state.copy(
                    dimension1M = it
                )
            )
        },
        label = {
            Text(
                if(!state.usesDimension2) "Diámetro interior (m)" else "Dimensión 1 · ancho interior (m)"
            )
        },
        modifier =
            Modifier.fillMaxWidth()
    )


    if (state.usesDimension2) {
    OutlinedTextField(
        value = state.dimension2M,
        onValueChange = {

            onStateChange(
                state.copy(
                    dimension2M = it
                )
            )
        },
        label = {
            Text(
                "Dimensión 2 · altura (m)"
            )
        },
        modifier =
            Modifier.fillMaxWidth()
    )

    } else Text("Dimensión 2: no aplica a sección circular")


    beforeConditions()
    ConditionSelector("CONDICIÓN ESTRUCTURAL",state.structuralConditionCode,emptyMap()) {onStateChange(state.copy(structuralConditionCode=it))}
    com.tuempresa.inventariovial.sic.TechnicalCriterion("Ver criterio estructural") {SicCatalogRepository.sic18StructuralDescriptions.forEach {(code,description)->Text("$code · $description")}}
    ConditionSelector("Condición funcional",state.functionalConditionCode,emptyMap()) {onStateChange(state.copy(functionalConditionCode=it))}
    com.tuempresa.inventariovial.sic.TechnicalCriterion("Ver criterio funcional") {com.tuempresa.inventariovial.catalog.EngineeringConditions.functional.forEach {(code,description)->Text("$code · $description")}}
    if(com.tuempresa.inventariovial.catalog.EngineeringConditions.culvertFailuresRequired(state.structuralConditionCode,state.functionalConditionCode)) {
        Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.errorContainer)) {
            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("CONDICIÓN REGULAR O MALA",style=MaterialTheme.typography.titleLarge)
                Text("Corresponde completar SIC-18A")
                Text("Se requiere información complementaria SIC-18A.")
                if(onCompleteSic18A!=null) Button(onClick=onCompleteSic18A) {Text("Completar fallas SIC-18A")}
            }
        }
    }
    Text("La condición estructural y funcional la selecciona el ingeniero.")

}


// =========================================================
// SIC-19 · CUNETAS Y DRENAJE
// INVENTARIO VIAL CALIFICADO
// =========================================================

@Composable
fun Sic19Fields(
    state: Sic19FormState,
    onStateChange: (Sic19FormState) -> Unit
) {

    val classOptions = SicCatalogRepository.options("sic19.class.0")

    val typeOptions = SicCatalogRepository.options("sic19.type.0")

    val sectionOptions = SicCatalogRepository.options("sic19.section.0")

    val structuralOptions = SicCatalogRepository.options("sic19.structural.0")

    val functionalOptions = SicCatalogRepository.options("sic19.functional.0")


    SectionTitle(
        "SIC-19 · Cunetas y drenaje"
    )

    Text(
        "Inventario Vial Calificado",
        fontWeight = FontWeight.Bold,
        style =
            MaterialTheme.typography.bodySmall
    )


    Text(
        "Clase",
        fontWeight = FontWeight.Bold
    )

    ChoiceSelector(
        options = classOptions,
        selected = optionForCode(
            classOptions,
            state.classCode
        ),
        onSelected = {

            onStateChange(
                state.copy(
                    classCode =
                        codeFromOption(it)
                )
            )
        }
    )


    Text(
        "Tipo",
        fontWeight = FontWeight.Bold
    )

    ChoiceSelector(
        options = typeOptions,
        selected = optionForCode(
            typeOptions,
            state.typeCode
        ),
        onSelected = {

            onStateChange(
                state.copy(
                    typeCode =
                        codeFromOption(it)
                )
            )
        }
    )


    Text(
        "Sección transversal",
        fontWeight = FontWeight.Bold
    )

    ChoiceSelector(
        options = sectionOptions,
        selected = optionForCode(
            sectionOptions,
            state.crossSectionCode
        ),
        onSelected = {

            onStateChange(
                state.copy(
                    crossSectionCode =
                        codeFromOption(it)
                )
            )
        }
    )


    if(state.typeCode=="4") {
        Text("Criterio de condición estructural · interno")
        com.tuempresa.inventariovial.scap.ui.ScapChoice("Criterio para Otro",when(state.structuralCriterion){"EARTH"->"Tierra";"PAVED"->"Pavimentado";else->""},listOf("Pavimentado","Tierra")) {
            onStateChange(state.copy(structuralCriterion=when(it){"Tierra"->"EARTH";"Pavimentado"->"PAVED";else->""}))
        }
    }
    val criterion=com.tuempresa.inventariovial.catalog.EngineeringConditions.criterion(state.typeCode,state.structuralCriterion)
    Text(when(criterion){"EARTH"->"Elemento en tierra";"PAVED"->"Elemento pavimentado";else->"! Selecciona el criterio antes de evaluar."})
    if(criterion!=null) ConditionSelector("Condición estructural",state.structuralConditionCode,com.tuempresa.inventariovial.catalog.EngineeringConditions.structural(state.typeCode,state.structuralCriterion)) {onStateChange(state.copy(structuralConditionCode=it))}
    ConditionSelector("Condición funcional",state.functionalConditionCode,com.tuempresa.inventariovial.catalog.EngineeringConditions.functional) {onStateChange(state.copy(functionalConditionCode=it))}

    Text(
        text =
            "La ubicación inicio y fin identifica el tramo. Usa el recorrido para estimar la longitud de elementos curvos.",
        style =
            MaterialTheme.typography.bodySmall
    )
}


// =========================================================
// SIC-20 · BADENES, TÚNELES Y MUROS
// INVENTARIO VIAL CALIFICADO
// =========================================================

@Composable
fun Sic20Fields(
    state: Sic20FormState,
    onManualLength: () -> Unit = {},
    onStateChange: (Sic20FormState) -> Unit
) {

    val classOptions = SicCatalogRepository.options("sic20.class.0")

    val typeOptions =
        when (state.classCode) {

            "12" -> SicCatalogRepository.options("sic20.type.0")

            "13" -> SicCatalogRepository.options("sic20.type.1")

            "14" -> SicCatalogRepository.options("sic20.type.2")

            else ->
                SicCatalogRepository.options("sic20.type.3")
        }


    val structuralOptions = SicCatalogRepository.options("sic20.structural.0")

    val functionalOptions = SicCatalogRepository.options("sic20.functional.0")


    SectionTitle(
        "SIC-20 · Badenes, túneles y muros"
    )

    Text(
        "Inventario Vial Calificado",
        fontWeight = FontWeight.Bold,
        style =
            MaterialTheme.typography.bodySmall
    )


    Text(optionForCode(classOptions, state.classCode), fontWeight = FontWeight.Bold)


    Text(
        "Tipo",
        fontWeight = FontWeight.Bold
    )

    ChoiceSelector(
        options = typeOptions,
        selected = optionForCode(
            typeOptions,
            state.typeCode
        ),
        onSelected = {

            onStateChange(
                state.copy(
                    typeCode =
                        codeFromOption(it)
                )
            )
        }
    )


    OutlinedTextField(
        value = state.dimension1M,
        onValueChange = {

            onStateChange(
                state.copy(
                    dimension1M = it
                )
            )
        },
        label = {

            Text(
                when (state.classCode) {

                    "12" ->
                        "Dimensión 1 · ancho de rodadura (m)"

                    "13" ->
                        "Dimensión 1 · ancho del túnel (m)"

                    "14" ->
                        "Dimensión 1 · altura promedio del muro (m)"

                    else ->
                        "Dimensión 1 (m)"
                }
            )
        },

        modifier =
            Modifier.fillMaxWidth()
    )


    if (state.usesDimension2) {

        OutlinedTextField(
            value =
                state.dimension2M,

            onValueChange = {

                onStateChange(
                    state.copy(
                        dimension2M = it
                    )
                )
            },

            label = {

                Text(
                    when (state.classCode) {

                        "12" ->
                            "Dimensión 2 · ancho total con protección contra erosión (m)"

                        "13" ->
                            "Dimensión 2 · altura útil (m)"

                        else ->
                            "Dimensión 2 (m)"
                    }
                )
            },

            modifier =
                Modifier.fillMaxWidth()
        )
    }


    if(state.classCode=="14") OutlinedTextField(state.wallLengthMeters,{onManualLength();onStateChange(state.copy(wallLengthMeters=it))},label={Text("Longitud técnica del muro (m)")},supportingText={Text("Medida validada por el ingeniero; se incluye en Excel complementario, sin cambiar columnas SIC")},keyboardOptions=androidx.compose.foundation.text.KeyboardOptions(keyboardType=androidx.compose.ui.text.input.KeyboardType.Decimal),modifier=Modifier.fillMaxWidth())
    ConditionSelector("Condición estructural",state.structuralConditionCode,mapOf("1" to "No tiene problema.","2" to "Puede tener problemas.","3" to "Necesita repararse.")) {onStateChange(state.copy(structuralConditionCode=it))}
    ConditionSelector("Condición funcional",state.functionalConditionCode,com.tuempresa.inventariovial.catalog.EngineeringConditions.functional) {onStateChange(state.copy(functionalConditionCode=it))}

}


// =========================================================
// SIC-17 · PUENTES
// INVENTARIO VIAL CALIFICADO
// =========================================================

@Composable
fun Sic17Fields(
    state: Sic17FormState,
    onStateChange: (Sic17FormState) -> Unit
) {

    val classOptions = SicCatalogRepository.options("sic17.class.0")

    val typeOptions =
        when (state.classCode) {

            "01" -> SicCatalogRepository.options("sic17.type.0")

            "02" -> SicCatalogRepository.options("sic17.type.1")

            "03" -> SicCatalogRepository.options("sic17.type.2")

            "04" -> SicCatalogRepository.options("sic17.type.3")

            else -> SicCatalogRepository.options("sic17.type.4")
        }

    val inventoriedOptions = SicCatalogRepository.options("sic17.inventoried.0")

    val structuralOptions = SicCatalogRepository.options("sic17.structural.0")

    val functionalOptions = SicCatalogRepository.options("sic17.functional.0")

    val serviceOptions = SicCatalogRepository.options("sic17.service.0")

    val singularityOptions = SicCatalogRepository.options("sic17.singularity.0")


    SectionTitle(
        "SIC-17 · Puentes"
    )

    Text(
        "Inventario Vial Calificado",
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.bodySmall
    )


    Text(
        "Clase",
        fontWeight = FontWeight.Bold
    )

    ChoiceSelector(
        options = classOptions,
        selected = optionForCode(
            classOptions,
            state.classCode
        ),
        onSelected = {

            onStateChange(
                state.copy(
                    classCode =
                        codeFromOption(it),

                    typeCode = "1"
                )
            )
        }
    )


    Text(
        "Tipo",
        fontWeight = FontWeight.Bold
    )

    ChoiceSelector(
        options = typeOptions,
        selected = optionForCode(
            typeOptions,
            state.typeCode
        ),
        onSelected = {

            onStateChange(
                state.copy(
                    typeCode =
                        codeFromOption(it)
                )
            )
        }
    )


    OutlinedTextField(
        value = state.bridgeCode,
        onValueChange = {

            onStateChange(
                state.copy(
                    bridgeCode =
                        it.uppercase()
                )
            )
        },
        label = {
            Text("Código del puente")
        },
        modifier =
            Modifier.fillMaxWidth()
    )


    Text(
        "Inventariado",
        fontWeight = FontWeight.Bold
    )

    ChoiceSelector(
        options = inventoriedOptions,
        selected = optionForCode(
            inventoriedOptions,
            state.inventoriedCode
        ),
        onSelected = {

            onStateChange(
                state.copy(
                    inventoriedCode =
                        codeFromOption(it)
                )
            )
        }
    )


    OutlinedTextField(
        value = state.spans,
        onValueChange = { value ->

            onStateChange(
                state.copy(
                    spans =
                        value.filter {
                            it.isDigit()
                        }
                )
            )
        },
        label = {
            Text("Número de vanos")
        },
        modifier =
            Modifier.fillMaxWidth()
    )


    OutlinedTextField(
        value =
            state.dimension1LengthM,
        onValueChange = {

            onStateChange(
                state.copy(
                    dimension1LengthM = it
                )
            )
        },
        label = {
            Text(
                "Dimensión 1 · longitud total (m)"
            )
        },
        modifier =
            Modifier.fillMaxWidth()
    )


    OutlinedTextField(
        value =
            state.dimension2LowerHeightM,
        onValueChange = {

            onStateChange(
                state.copy(
                    dimension2LowerHeightM = it
                )
            )
        },
        label = {
            Text(
                "Dimensión 2 · altura libre inferior (m)"
            )
        },
        modifier =
            Modifier.fillMaxWidth()
    )


    Text(
        "Condición estructural",
        fontWeight = FontWeight.Bold
    )

    ChoiceSelector(
        options = structuralOptions,
        selected = optionForCode(
            structuralOptions,
            state.structuralConditionCode
        ),
        onSelected = {

            onStateChange(
                state.copy(
                    structuralConditionCode =
                        codeFromOption(it)
                )
            )
        }
    )


    Text(
        "Condición funcional",
        fontWeight = FontWeight.Bold
    )

    ChoiceSelector(
        options = functionalOptions,
        selected = optionForCode(
            functionalOptions,
            state.functionalConditionCode
        ),
        onSelected = {

            onStateChange(
                state.copy(
                    functionalConditionCode =
                        codeFromOption(it)
                )
            )
        }
    )


    Text(
        "Tipo de servicio",
        fontWeight = FontWeight.Bold
    )

    ChoiceSelector(
        options = serviceOptions,
        selected = optionForCode(
            serviceOptions,
            state.serviceTypeCode
        ),
        onSelected = {

            onStateChange(
                state.copy(
                    serviceTypeCode =
                        codeFromOption(it)
                )
            )
        }
    )


    Text(
        "Singularidad salvada",
        fontWeight = FontWeight.Bold
    )

    ChoiceSelector(
        options = singularityOptions,
        selected = optionForCode(
            singularityOptions,
            state.singularityCode
        ),
        onSelected = {

            onStateChange(
                state.copy(
                    singularityCode =
                        codeFromOption(it)
                )
            )
        }
    )


    OutlinedTextField(
        value =
            state.singularityName,
        onValueChange = {

            onStateChange(
                state.copy(
                    singularityName =
                        it.uppercase()
                )
            )
        },
        label = {
            Text(
                "Nombre de la singularidad"
            )
        },
        placeholder = {
            Text(
                "Ej. RÍO RÍMAC"
            )
        },
        modifier =
            Modifier.fillMaxWidth()
    )


    OutlinedTextField(
        value =
            state.dimension3UpperHeightM,
        onValueChange = {

            onStateChange(
                state.copy(
                    dimension3UpperHeightM = it
                )
            )
        },
        label = {
            Text(
                "Dimensión 3 · altura libre superior (m)"
            )
        },
        supportingText = {
            Text(
                "Si no existe limitación de altura, el manual indica 00.00."
            )
        },
        modifier =
            Modifier.fillMaxWidth()
    )
}



// =========================================================
// CAPTURA DE UBICACIÓN GPS FINAL
// =========================================================



// =========================================================
// COMPONENTES AUXILIARES
// =========================================================
internal fun codeFromOption(
    option: String
): String {

    return option
        .substringBefore(" - ")
        .trim()
}


internal fun optionForCode(
    options: List<String>,
    code: String
): String {

    return options.firstOrNull {
        codeFromOption(it) == code
    } ?: options.first()
}
@Composable
fun ChoiceSelector(options: List<String>, selected: String, onSelected: (String) -> Unit) =
    ChoiceSelector(options, selected, true, onSelected)

@Composable
fun ChoiceSelector(
    options: List<String>,
    selected: String,
    enabled: Boolean,
    onSelected: (String) -> Unit
) {

    var expanded by remember { mutableStateOf(false) }
    if(options.size > 3 || options.any { it.length > 40 }) {
        Box(Modifier.fillMaxWidth()) {
            OutlinedButton(enabled=enabled,onClick={expanded=true}, modifier=Modifier.fillMaxWidth()) { Text(selected.ifBlank { "Seleccionar" }) }
            DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) {
                options.forEach { option -> DropdownMenuItem(text={Text(if(option==selected) "✓ $option" else option)},
                    onClick={onSelected(option);expanded=false}) }
            }
        }
    } else LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        items(options) { option -> FilterChip(enabled=enabled,selected=selected==option,onClick={onSelected(option)},label={Text(option)}) }
    }

}


@Composable
fun SectionTitle(
    text: String
) {

    Text(
        text = text,
        fontSize = 20.sp,
        fontWeight =
            FontWeight.Bold
    )
}


@Composable
fun ErrorText(
    text: String
) {

    Text(
        text = text,
        color =
            MaterialTheme.colorScheme.error,
        fontWeight =
            FontWeight.Bold
    )
}


// =========================================================
// PREVIEW
// =========================================================

@Preview(
    showBackground = true,
    widthDp = 800,
    heightDp = 1280
)
@Composable
fun PreviewInventarioVial() {

    InventarioVialTheme {

        HomeScreen(
            recordsToday = 0,
            pendingSync = 0,
            onAssetSelected = {}
        )
    }
}
