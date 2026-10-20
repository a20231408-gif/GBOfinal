package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.auth.*
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.localfiles.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*

@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],application=Application::class) @GraphicsMode(GraphicsMode.Mode.NATIVE)
class LocalFiles148UiTest {
    @get:Rule val ui=createComposeRule()
    private val c get()=ApplicationProvider.getApplicationContext<Context>()
    @Before fun prepare()=kotlinx.coroutines.runBlocking{initializeIdleUiWorkers();AuthGraph.get(c).sessions.logout();AuthGraph.get(c).sessions.login(UserCacheEntity("TEST_UI_148","test_ui","FICTICIO","ENGINEER",true,"",0,0,0));LocalCopySettings.enabled(c,false)}
    @Test fun choiceConfirmation(){
        ui.setContent {MaterialTheme{RecordDeliveryChoice("SIC","TEST_UI_148",false)}}
        ui.onNodeWithText("Elegir Solo dispositivo").performClick();ui.onNodeWithText("Confirmar destino").assertExists();ui.onNodeWithText("Confirmar",useUnmergedTree=true).performClick()
        ui.waitUntil(5000){ui.onAllNodesWithText("Solo dispositivo · no se programa Drive").fetchSemanticsNodes().isNotEmpty()}
        runBlocking {assertTrue(DeliveryDestination.deviceOnly(InventoryDatabase.getInstance(c),"SIC/TEST_UI_148"));assertNull(InventoryDatabase.getInstance(c).automaticSyncDao().get("SIC/TEST_UI_148"))}
    }
    @Test fun savedFileButtonsAndMissingAccess(){
        val f=LocalSharedFile("TEST","JOB","SIC/TEST","Download/GBO_Ruta_Clara/SIC/","test.pdf","application/pdf",1,"a".repeat(64),"TEST_SOURCE","content://missing.document/test","READY")
        ui.setContent{MaterialTheme{SavedFileActions(f)}}
        for(text in listOf("Abrir archivo","Compartir","Ver ubicación","Guardar como…"))ui.onNodeWithText(text).assertExists()
        ui.onNodeWithText("Abrir archivo").performClick();ui.waitUntil(5000){ui.onAllNodesWithText("El archivo fue movido",substring=true).fetchSemanticsNodes().isNotEmpty()}
        ui.onNodeWithText("Recuperar copia en Descargas").assertExists()
    }
    @Test fun automaticRequiresVisibleConsent(){
        ui.setContent{MaterialTheme{LocalCopiesSettings()}}
        ui.onNode(isToggleable()).performClick();assertFalse(LocalCopySettings.enabled(c));ui.onNodeWithText("Activar copias automáticas").assertExists();ui.onNodeWithText("Activar",useUnmergedTree=true).performClick()
        ui.waitUntil(5000){LocalCopySettings.enabled(c)};ui.onNodeWithText("¿Dónde están mis archivos?").assertExists()
    }
}
