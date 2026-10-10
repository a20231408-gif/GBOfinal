package com.tuempresa.inventariovial

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.auth.UserCacheEntity
import com.tuempresa.inventariovial.data.entity.FieldSession
import com.tuempresa.inventariovial.data.entity.InventoryRecordEntity
import com.tuempresa.inventariovial.model.*
import com.tuempresa.inventariovial.sic.*
import com.tuempresa.inventariovial.viewmodel.InventoryViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],qualifiers="w800dp-h1200dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FieldSessionFormsTest {
    private val models=androidx.lifecycle.ViewModelStore()
    private var inventoryModel:InventoryViewModel?=null
    @get:Rule(order=0) val resources=object:org.junit.rules.TestWatcher() {
        override fun finished(description:org.junit.runner.Description) {
            models.clear()
            inventoryModel?.field?.auth?.sessions?.logout()
            inventoryModel=null
        }
    }
    @get:Rule(order=1) val compose=createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private fun model():InventoryViewModel {
        val app=ApplicationProvider.getApplicationContext<Application>()
        android.provider.Settings.Secure.putString(app.contentResolver,android.provider.Settings.Secure.ANDROID_ID,"1234567890abcdef")
        try {androidx.work.WorkManager.getInstance(app)} catch(_:IllegalStateException) {
            androidx.work.WorkManager.initialize(app,androidx.work.Configuration.Builder().build())
        }
        return InventoryViewModel(app).also {vm->
            inventoryModel=vm
            models.put("inventory",vm)
            vm.field.auth.sessions.logout()
            runBlocking {withContext(Dispatchers.IO) {
                com.tuempresa.inventariovial.data.database.InventoryDatabase.getInstance(app).clearAllTables()
            }}
            app.getSharedPreferences("survey_context",android.content.Context.MODE_PRIVATE).edit().clear().commit()
            val user=UserCacheEntity("session-engineer","session-engineer","Engineer","ENGINEER",true,"",1,1,1)
            vm.field.auth.sessions.login(user)
        }
    }
    private fun start(vm:InventoryViewModel,route:String,bed:String):FieldSession {
        val result=CompletableDeferred<String?>()
        compose.runOnIdle {vm.field.startSession("Project",route,"",bed,"INCREASING",
            {result.complete(null)},{result.complete(it)})}
        compose.waitUntil(30_000) {compose.waitForIdle();org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();result.isCompleted}
        assertNull(runBlocking {result.await()})
        return runBlocking {vm.field.dao.currentSession()!!}.also {assertEquals(route,it.road);assertEquals(bed,it.roadbed)}
    }
    private fun assertHeader(route:String,bed:String) {
        compose.waitUntil(20_000) {compose.onAllNodesWithTag("legacy-inventory-form").fetchSemanticsNodes().isNotEmpty()}
        compose.onAllNodesWithText(route).onFirst().performScrollTo().assertIsDisplayed()
        val catalog=com.tuempresa.inventariovial.road.ProjectRouteCatalog.load(ApplicationProvider.getApplicationContext())
        val selected=catalog.requireRoute(route)
        compose.onAllNodesWithText("${selected.itemLabel} · ${selected.segmentCode}").onFirst().performScrollTo().assertIsDisplayed()
        compose.onAllNodesWithText(bed).onFirst().performScrollTo().assertIsDisplayed()
    }

    @Test fun bothSessionChoicesReachSignalsAssetsAndAllStandaloneSicFormsAfterReturningHome() {
        val vm=model()
        var screen by mutableStateOf<String?>(null)
        var generation by mutableIntStateOf(0)
        compose.setContent {MaterialTheme {
            val current=screen
            if(current==null) Text("Inicio de prueba")
            else key(current,generation) {
                val back:()->Unit={screen=null}
                when {
                    current.startsWith("SIGNAL:")->SignalizationFormScreen(SignalizationType.valueOf(current.substringAfter(':')),vm,back,back)
                    current.startsWith("ASSET:")->AssetFormScreen(RoadAssetType.valueOf(current.substringAfter(':')),vm,back,back)
                    else->SicFieldWorkspace(vm,SicFieldFormat.valueOf(current),onBack=back)
                }
            }
        }}
        val screens=listOf("SIGNAL:VERTICAL","SIGNAL:HORIZONTAL_MARKS","ASSET:CULVERT","ASSET:DITCH","ASSET:FORD",
            "ASSET:TUNNEL","ASSET:WALL","ASSET:RIGHT_OF_WAY","SIC08","SIC09","SIC13","SIC34")
        for((route,bed) in listOf("PE-22A" to "CD","PE-28H" to "A2")) {
            val active=start(vm,route,bed)
            for(target in screens) {
                compose.runOnIdle {generation++;screen=target}
                assertHeader(route,bed)
                compose.onNodeWithText("← Volver").performScrollTo().performClick()
                compose.onNodeWithText("Inicio de prueba").assertExists()
                assertEquals(active,runBlocking {vm.field.dao.currentSession()})
            }
        }
    }

    @Test fun manualChangeSavedAsDraftSurvivesReopenAndNextFormReturnsToSessionDefaults() {
        val vm=model()
        var screen by mutableStateOf(false)
        var existing by mutableStateOf<InventoryRecordEntity?>(null)
        compose.setContent {MaterialTheme {
            if(screen) SignalizationFormScreen(SignalizationType.VERTICAL,vm,{screen=false},{screen=false},existing)
            else Text("Inicio de prueba")
        }}
        val active=start(vm,"PE-22A","CD")
        compose.runOnIdle {screen=true}
        assertHeader("PE-22A","CD")
        compose.onAllNodesWithText("PE-22A").onFirst().performClick()
        compose.onNodeWithText("PE-3NG").performClick()
        compose.onAllNodesWithText("CD").onFirst().performClick()
        compose.onNodeWithText("UC").performClick()
        compose.onNodeWithText("Guardar borrador").performScrollTo().performClick()
        var saved:InventoryRecordEntity?=null
        compose.waitUntil(20_000) {
            saved=runBlocking {vm.field.dao.observeDrafts().first().firstOrNull {it.routeCode=="PE-3NG" && it.sicCode=="SIC-22"}}
            saved!=null
        }
        val id=saved!!.id
        compose.onNodeWithText("← Volver").performScrollTo().performClick()
        compose.runOnIdle {existing=vm.field.let {runBlocking {it.dao.recordById(id)}};screen=true}
        assertHeader("PE-3NG","UC")
        compose.onNodeWithText("← Volver").performScrollTo().performClick()
        compose.runOnIdle {existing=null;screen=true}
        assertHeader("PE-22A","CD")
        assertEquals("PE-3NG",runBlocking {vm.field.dao.recordById(id)}!!.routeCode)
        assertEquals("UC",runBlocking {vm.field.dao.recordById(id)}!!.roadbedCode)
        assertEquals(active,runBlocking {vm.field.dao.currentSession()})
    }
}
