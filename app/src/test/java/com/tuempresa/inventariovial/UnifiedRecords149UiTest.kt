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
        for(label in listOf("Ver detalle","Abrir archivo","Ver ubicación","Compartir"))ui.onNodeWithText(label).assertExists()
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
