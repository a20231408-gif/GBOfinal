package com.tuempresa.inventariovial.dashboard

import android.content.Context
import android.net.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.InventoryRecordEntity
import com.tuempresa.inventariovial.field.*
import com.tuempresa.inventariovial.model.RoadAssetType
import com.tuempresa.inventariovial.sic.*
import com.tuempresa.inventariovial.viewmodel.InventoryViewModel
import java.time.LocalDate

@Composable private fun connection():Boolean {
    val context=LocalContext.current
    val manager=remember(context){context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager}
    fun online()=manager.getNetworkCapabilities(manager.activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)==true
    var connected by remember {mutableStateOf(online())}
    DisposableEffect(manager) {
        val callback=object:ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network:Network) {connected=online()}
            override fun onLost(network:Network) {connected=online()}
            override fun onCapabilitiesChanged(network:Network,capabilities:NetworkCapabilities) {connected=online()}
        }
        manager.registerDefaultNetworkCallback(callback)
        onDispose {manager.unregisterNetworkCallback(callback)}
    }
    return connected
}

/** Single presentation surface; form callbacks retain the saved UUID and existing routing. */
@Composable fun DashboardHome(vm:InventoryViewModel?,onContinue:(InventoryRecordEntity)->Unit,onPavement:()->Unit,
    onRecords:()->Unit,onExport:()->Unit,onMap:()->Unit,onAsset:(RoadAssetType)->Unit,
    onSic:(SicFieldFormat)->Unit,message:String?) {
    if(vm==null) return
    val context=LocalContext.current
    val db=remember(context){InventoryDatabase.getInstance(context)}
    val session by vm.field.session.collectAsState()
    val sessions by vm.field.dao.observeSessions().collectAsState(emptyList())
    val drafts by vm.field.drafts.collectAsState()
    val extensions by vm.field.dao.observeSicExtensions().collectAsState(emptyList())
    val user by vm.field.auth.sessions.current.collectAsState()
    val reference by vm.field.reference.collectAsState()
    val intents by db.automaticSyncDao().observe().collectAsState(emptyList())
    // Preserve pre-existing ownership checks and explicit SIC-08/09 parent relationships.
    val accessible=drafts.filter {user!=null && it.sicCode!="SCAP" && com.tuempresa.inventariovial.core.InventoryDraftStore.accessible(context,it.id)}
    val rows=accessible.filter {InventoryElements.parentId(it,extensions,accessible)==it.id}
    var screen by rememberSaveable {mutableStateOf("HOME")}
    var showSession by rememberSaveable {mutableStateOf(false)}
    var showDrive by rememberSaveable {mutableStateOf(false)}
    var today by remember {mutableStateOf(LocalDate.now())}
    LaunchedEffect(Unit) {while(true) {today=LocalDate.now();kotlinx.coroutines.delay(30_000)}}
    val connected=connection()
    if(screen!="HOME") BackHandler {screen="HOME"}
    when(screen) {
        "DRAFTS"->AllDraftsScreen(rows,sessions,reference.prs,onContinue) {screen="HOME"}
        "NEW"->NewRecordChooser(onAsset,onSic) {screen="HOME"}
        "TOOLS"->DashboardToolsScreen(onBack={screen="HOME"},onMap=onMap,onExport=onExport,
            onBridge={onAsset(RoadAssetType.BRIDGE)},secondary={FieldSecondaryTools(vm)},
            core={com.tuempresa.inventariovial.core.CoreFieldTools(vm)},
            account={com.tuempresa.inventariovial.auth.AuthHomePanel(vm.field.auth)},message=message)
        else->DashboardScreen(session,sessions,rows,reference.prs,DashboardDeliveryCounts.from(intents),connected,today,
            onSession={showSession=true},onNew={screen="NEW"},onRecords=onRecords,onPavement=onPavement,
            onDrive={showDrive=true},onTools={screen="TOOLS"},onAllDrafts={screen="DRAFTS"},onContinue=onContinue,onMap=onMap)
    }
    if(showSession) SessionDialog(vm.field,{showSession=false},session)
    if(showDrive) com.tuempresa.inventariovial.core.DriveSyncCenter(vm) {showDrive=false}
}

@Composable fun NewRecordChooser(onAsset:(RoadAssetType)->Unit,onSic:(SicFieldFormat)->Unit,onBack:()->Unit) {
    LazyColumn(Modifier.fillMaxSize().background(Color.White),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        item {TextButton(onClick=onBack) {Text("VOLVER")};RutaClaraSectionHeader("Nuevo registro",com.tuempresa.inventariovial.R.drawable.gbo_dashboard_banner)}
        InventoryElements.home.groupBy {it.family}.forEach {(family,entries)->
            item {Text(family,style=MaterialTheme.typography.titleMedium)}
            items(entries) {entry->OutlinedButton(onClick={entry.asset?.let(onAsset);entry.format?.let(onSic)},modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)) {
                Column {Text(entry.title);Text(entry.subtitle,style=MaterialTheme.typography.bodySmall)}
            }}
        }
    }
}
