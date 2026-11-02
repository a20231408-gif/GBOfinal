package com.tuempresa.inventariovial

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.auth.*
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.localfiles.*
import com.tuempresa.inventariovial.pavement.*
import com.tuempresa.inventariovial.scap.catalog.ScapCatalog
import com.tuempresa.inventariovial.scap.data.ScapRepository
import com.tuempresa.inventariovial.viewmodel.InventoryViewModel
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class UnifiedRecords149UiTest {
    @get:Rule val ui=createComposeRule()
    private val app get()=ApplicationProvider.getApplicationContext<Application>()
    @Test fun allVisibleDraftsRespectCombinedFilters()=discardFilteredDrafts(all=true)
    @Test fun selectedVisibleDraftsRespectCombinedFilters()=discardFilteredDrafts(all=false)
    private fun discardFilteredDrafts(all:Boolean) {
        val prefix=if(all)"ALL_SCOPE_"else"SELECTED_SCOPE_"
        val road=if(all)"PE-ALL"else"PE-SELECTED"
        initializeIdleUiWorkers()
        android.provider.Settings.Secure.putString(app.contentResolver,android.provider.Settings.Secure.ANDROID_ID,"FICTITIOUS_SCOPE_39")
        val graph=AuthGraph.get(app);graph.sessions.logout();graph.sessions.login(UserCacheEntity("TEST_SCOPE_39","test","FICTICIO","ENGINEER",true,"",0,0,0))
        val db=InventoryDatabase.getInstance(app)
        runBlocking {
            LocalCopySettings.enabled(app,false)
            for(id in listOf("first","second","other_route","other_pr","other_date","excluded_search")) {
                db.inventoryDao().insertRecord(testRecord(if(id=="excluded_search")road+id else prefix+id,status="DRAFT",route=if(id=="other_route")"OTHER"else road,pr=if(id=="other_pr")"0020"else"0010").copy(surveyDate=if(id=="other_date")"2025-09-23"else"2026-09-23"))
            }
        }
        lateinit var vm:InventoryViewModel
        ui.runOnUiThread{vm=InventoryViewModel(app)}
        ui.setContent{MaterialTheme{UnifiedRecordsScreen(vm){}}}
        fun click(text:String)=ui.onNodeWithText(text).performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick){it()}
        ui.onNodeWithText("Buscar elemento o UUID").performTextInput(prefix)
        click("SIC")
        ui.onNodeWithText("Carretera").performTextInput(road)
        ui.onNodeWithText("Progresiva").performTextInput("0010")
        click("Año: Todos");click("2026");click("Borradores")
        ui.onNodeWithText("2 registros").assertExists()
        if(!all)click("Seleccionar borradores visibles")
        click(if(all)"Descartar todos los borradores visibles"else"Descartar seleccionados")
        ui.onNodeWithText("Descartar 2 borradores locales").assertExists()
        click("Confirmar descarte reversible")
        ui.waitUntil(5000){runBlocking{db.inventoryDao().recordById(prefix+"first")!!.status=="DISCARDED_LOCAL"}}
        runBlocking {
            for(id in listOf("first","second"))org.junit.Assert.assertEquals("DISCARDED_LOCAL",db.inventoryDao().recordById(prefix+id)!!.status)
            for(id in listOf("other_route","other_pr","other_date"))org.junit.Assert.assertEquals("DRAFT",db.inventoryDao().recordById(prefix+id)!!.status)
            org.junit.Assert.assertEquals("DRAFT",db.inventoryDao().recordById(road+"excluded_search")!!.status)
            org.junit.Assert.assertTrue(db.automaticSyncDao().all().isEmpty())
        }
    }
    @Test fun discardedViewExposesIndividualRestoreAndNeverEnqueuesDrive() {
        initializeIdleUiWorkers()
        android.provider.Settings.Secure.putString(app.contentResolver,android.provider.Settings.Secure.ANDROID_ID,"FICTITIOUS_RESTORE_UI_37")
        val graph=AuthGraph.get(app);graph.sessions.logout();graph.sessions.login(UserCacheEntity("TEST_RESTORE_37","test","FICTICIO","ENGINEER",true,"",0,0,0))
        val db=InventoryDatabase.getInstance(app)
        runBlocking {
            LocalCopySettings.enabled(app,false)
            db.inventoryDao().insertRecord(testRecord("FICTITIOUS_DISCARDED_UI_37",status="DISCARDED_LOCAL"))
        }
        lateinit var vm:InventoryViewModel
        ui.runOnUiThread{vm=InventoryViewModel(app)}
        ui.setContent{MaterialTheme{UnifiedRecordsScreen(vm){}}}
        // Accessibility action avoids a synthetic tap outside the viewport of nested scrolls.
        ui.onNodeWithText("Descartados").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick){it()}
        ui.waitForIdle()
        ui.onNodeWithText("Descartados").assertIsSelected()
        ui.onNodeWithTag("mis-registros").performScrollToNode(hasText("Restaurar borrador local"))
        ui.onNodeWithText("Restaurar borrador local").performClick()
        ui.onNodeWithText("Recuperar",useUnmergedTree=true).performClick()
        ui.waitUntil(5000){runBlocking{db.inventoryDao().recordById("FICTITIOUS_DISCARDED_UI_37")!!.status=="DRAFT"}}
        runBlocking{org.junit.Assert.assertNull(db.automaticSyncDao().get("SIC/FICTITIOUS_DISCARDED_UI_37"))}
    }
    @Test fun familiesAndDocumentActionsAreAvailableWithoutAnEditor() {
        initializeIdleUiWorkers()
        val graph=AuthGraph.get(app);graph.sessions.logout();graph.sessions.login(UserCacheEntity("TEST_UI_149","test","FICTICIO","ENGINEER",true,"",0,0,0))
        runBlocking {
            LocalCopySettings.enabled(app,false)
            val db=InventoryDatabase.getInstance(app)
            db.inventoryDao().insertRecord(testRecord("TEST_SIC_UI_149",sic="SIC-18",route="PE-3N",status="DRAFT"))
            val repo=ScapRepository(db,ScapCatalog.load(app));val bridge=repo.create("TEST_OPERATOR","TEST_DEVICE",null)
            repo.setField(bridge,"inspection","bridgeName","TEST_UI_149_BRIDGE")
            db.pavementDao().saveSample(PavementSampleEntity("TEST_PAV_UI_149","TRAMO_TEST","PE-3N",1,"Derecho","CD","00+000","00+020",10.0,20.0,"RIGID","2026-10-16","TEST_UI_149","FICTICIO",slabRows=4,slabColumns=2))
        }
        lateinit var vm:InventoryViewModel
        ui.runOnUiThread{vm=InventoryViewModel(app)}
        ui.setContent{MaterialTheme{UnifiedRecordsScreen(vm){}}}
        ui.onNodeWithText("Mis registros").assertExists()
        ui.onNodeWithText("Buscar elemento o UUID").performTextInput("UI_149")
        ui.onNodeWithText("SIC",useUnmergedTree=true).performClick()
        ui.onNodeWithTag("mis-registros").performScrollToNode(hasText("Ver detalle"))
        for(label in listOf("Ver detalle","Abrir archivo","Mostrar ubicación","Compartir"))ui.onNodeWithText(label).assertExists()
        ui.onNodeWithText("Abrir archivo").assertIsNotEnabled()
        ui.onNodeWithTag("mis-registros").performScrollToIndex(0)
        ui.onNodeWithText("SCAP",useUnmergedTree=true).performClick()
        ui.onNodeWithTag("mis-registros").performScrollToNode(hasText("SCAP / Puentes",substring=true))
        ui.onNodeWithText("SCAP / Puentes",substring=true).assertExists()
        ui.onNodeWithTag("mis-registros").performScrollToIndex(0)
        ui.onNodeWithText("Pavimentos",useUnmergedTree=true).performClick()
        ui.onNodeWithTag("mis-registros").performScrollToNode(hasText("Pavimentos · Rígido",substring=true))
        ui.onNodeWithText("Pavimentos · Rígido",substring=true).assertExists()
    }
}
