package com.tuempresa.inventariovial

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.scap.catalog.ScapCatalog
import com.tuempresa.inventariovial.scap.data.ScapRepository
import com.tuempresa.inventariovial.scap.domain.ScapPhotoCategories
import com.tuempresa.inventariovial.scap.ui.*
import com.tuempresa.inventariovial.ui.theme.InventarioVialTheme
import kotlinx.coroutines.*
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],application=android.app.Application::class)
class Engineering2ScreenTest {
    // Dispose Compose collectors before closing Room; otherwise teardown races the final query.
    @get:Rule(order=0) val resources=object:org.junit.rules.TestWatcher() {
        override fun finished(description:org.junit.runner.Description) {
            if(::scope.isInitialized) scope.cancel()
            if(::db.isInitialized) db.close()
        }
    }
    @get:Rule(order=1) val compose=createComposeRule()
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db:InventoryDatabase
    private lateinit var repo:ScapRepository
    private lateinit var controller:ScapController
    private lateinit var catalog:ScapCatalog
    private lateinit var scope:CoroutineScope
    private lateinit var id:String
    @Before fun setup():Unit=runBlocking {
        initializeIdleUiWorkers()
        db=Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build()
        catalog=ScapCatalog.load(context);repo=ScapRepository(db,catalog);id=repo.create("Inspector","Tablet",null)
        scope=CoroutineScope(SupervisorJob()+Dispatchers.Main);controller=ScapController(context,db,scope)
    }
    @Test fun c4ShowsFixedStructuresAndSevenIndependentlyEditablePiers():Unit=runBlocking {
        repeat(7) {repo.addRow(id,"PIER")}
        val initial=repo.dao.snapshot(id)!!
        compose.setContent {InventarioVialTheme {
            val s by repo.dao.observe(id).collectAsState(initial)
            ScapTechnicalSection(s!!,controller,catalog,"C4",true)
        }}
        for(title in listOf("Estribo izquierdo","Estribo derecho","Macizo / cámara de anclaje izquierda","Macizo / cámara de anclaje derecha")+(1..7).map {"Pilar $it"})
            compose.onNodeWithText(title).performScrollTo().assertExists()
        compose.onNodeWithText("Elemento para agregar").assertDoesNotExist()
        compose.onNodeWithText("Elemento").assertDoesNotExist()
        compose.onNodeWithText("+ AÑADIR PILAR").performScrollTo().performClick()
        compose.waitUntil(10000) {compose.waitForIdle();Assert.assertNull(controller.error.value);runBlocking {repo.dao.snapshot(id)!!.substructures.count {it.kind=="PIER"}==8}}
        compose.onNodeWithText("Pilar 8").performScrollTo().assertExists()
    }
    @Test fun f2ShowsPendingDefaultsAndEditingAnObservationPersistsWithoutAssertingPresence():Unit=runBlocking {
        val initial=repo.dao.snapshot(id)!!
        var showPresence by mutableStateOf(false)
        compose.setContent {InventarioVialTheme {
            val s by repo.dao.observe(id).collectAsState(initial)
            if(showPresence) ScapElementPresencePanel(s!!,controller,catalog,true)
            else ScapElements(s!!,controller,catalog,true){}
        }}
        compose.onNodeWithText("F.2 · CONDICIÓN GLOBAL DEL PUENTE").assertExists()
        // General observations are created explicitly; no fake element/defect is prefilled.
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("+ Añadir observación / falla"))
        compose.onNodeWithText("+ Añadir observación / falla").performClick()
        compose.waitUntil(10000) {
            compose.waitForIdle()
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            Assert.assertNull(controller.error.value)
            runBlocking {repo.dao.snapshot(id)!!.defects.isNotEmpty()}
        }
        compose.waitUntil(15000) {
            compose.waitForIdle()
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            compose.onAllNodesWithText("Descripción observada").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Descripción observada"))
        compose.onAllNodesWithText("Descripción observada").onFirst().performTextInput("Observación de prueba")
        compose.waitUntil(10000) {compose.waitForIdle();Assert.assertNull(controller.error.value);runBlocking {repo.dao.snapshot(id)!!.defects.any {it.description=="Observación de prueba"}}}
        Assert.assertTrue(repo.dao.snapshot(id)!!.elements.isEmpty())
        Assert.assertFalse(scapSections.containsKey("F3"))
        compose.runOnIdle {showPresence=true}
        for(group in listOf("II. SUBESTRUCTURA","III. DETALLES","IV. CAUCE","V. ACCESOS")) {
            compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(group));compose.onNodeWithText(group).assertExists()
        }
    }
    @Test fun photoPanelHasDirectCaptureAndImportForEveryPrimaryCategory():Unit=runBlocking {
        val s=repo.dao.snapshot(id)!!
        compose.setContent {InventarioVialTheme {ScapMedia(s,controller,catalog,true,false)}}
        for(title in ScapPhotoCategories.primary.values) {
            compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(title))
            compose.onNodeWithText(title).assertExists()
        }
        Assert.assertEquals(9,ScapPhotoCategories.primary.size)
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("+ AÑADIR FOTO / CATEGORÍA ADICIONAL"))
        compose.onNodeWithText("+ AÑADIR FOTO / CATEGORÍA ADICIONAL").performClick()
        compose.onNodeWithText("Categoría adicional").assertExists()
    }
    @Test fun sic17ComplementUsesExistingCalzadaAndClassChoices() {
        val s=runBlocking {
            repo.saveSicLocation(id,"PE-3N","UC","0042","125.5","0043","10")
            repo.dao.snapshot(id)!!
        }
        compose.setContent {InventarioVialTheme {Column(Modifier.verticalScroll(rememberScrollState())) {ScapSicSupplementPanel(s,controller,true)}}}
        compose.onNodeWithText("DATOS PARA SIC").assertExists()
        compose.onNodeWithText("Completar SIC-17").performScrollTo().performClick()
        compose.onNodeWithText("No forman parte de la ficha SCAP").assertExists()
        compose.onNodeWithText("Clase").performScrollTo()
        compose.onNode(hasText("Sin completar") and hasAnyAncestor(hasTestTag("sic-field-classCode"))).performScrollTo().performClick()
        compose.onNodeWithText("01 - Puente Definitivo").performClick()
        compose.waitUntil(10000) {compose.waitForIdle();Assert.assertNull(controller.error.value);runBlocking {repo.dao.snapshot(id)!!.values()["sic17.classCode"]=="01"}}
        Assert.assertEquals("UC",runBlocking {repo.dao.snapshot(id)!!.roadRecord!!.roadbedCode})
    }
    @Test fun badCulvertDisplaysImmediateSic18aActionBesideConditionFields() {
        var clicked=false
        compose.setContent {InventarioVialTheme {Column(Modifier.verticalScroll(rememberScrollState())) {
            Sic18Fields(com.tuempresa.inventariovial.model.form.Sic18FormState(functionalConditionCode="3"),onCompleteSic18A={clicked=true}) {}
        }}}
        compose.onNodeWithText("Completar fallas SIC-18A").performScrollTo().performClick()
        Assert.assertTrue(clicked)
        compose.onNodeWithText("CONDICIÓN REGULAR O MALA").assertExists()
    }

}
