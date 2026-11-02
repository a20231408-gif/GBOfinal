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
    @Test fun regeneratedCopyHidesLegacyListingButTwoAttemptHistoryRemainsAccessible(){
        val d=InventoryDatabase.getInstance(c);val key="SCAP/TEST_UI_LOCAL_HISTORY"
        val old=LocalExportJob("UI_LEGACY",key,"SCAP","TEST_UI_LOCAL_HISTORY",1,scope="FULL",state="READY",createdAt=1,completed=1,total=1)
        val fresh=old.copy(id="UI_REGENERATED",scope="FULL_LAYOUT_V4",createdAt=2)
        val previous=LocalSharedFile("UI_LEGACY_FILE",old.id,key,"Download/GBO_Ruta_Clara/UUID_FICTICIO/","anterior.pdf","application/pdf",1,"a".repeat(64),"FICTITIOUS_OLD","content://fictitious/old","READY")
        val current=previous.copy(id="UI_CURRENT_FILE",jobId=fresh.id,relativePath="Download/GBO Inventario Vial/_PROVISIONALES/Puente Ficticio/",name="actual.pdf",uri="content://fictitious/current")
        runBlocking{d.localFilesDao().saveJob(old);d.localFilesDao().saveJob(fresh);d.localFilesDao().saveFile(previous);d.localFilesDao().saveFile(current)}
        ui.setContent{MaterialTheme{androidx.compose.foundation.layout.Column{LocalFilesList(key)}}}
        ui.waitUntil(5000){ui.onAllNodesWithText("ARCHIVO GUARDADO · actual.pdf").fetchSemanticsNodes().isNotEmpty()}
        ui.onNodeWithText("ARCHIVO GUARDADO · anterior.pdf").assertDoesNotExist()
        ui.onNodeWithText("Ver historial y todas las copias (2)").performClick()
        ui.onNodeWithText("ARCHIVO GUARDADO · anterior.pdf").assertExists()
        ui.onNodeWithText("Ocultar copias anteriores").performClick()
        ui.onNodeWithText("ARCHIVO GUARDADO · anterior.pdf").assertDoesNotExist()
        runBlocking{assertEquals(previous,d.localFilesDao().files(old.id).single());assertEquals(old,d.localFilesDao().job(old.id));assertTrue(d.automaticSyncDao().all().none{it.key==key})}
    }
    @Test fun failedRegenerationKeepsLastReadyCopyVisibleUntilReplacementIsPublished(){
        val d=InventoryDatabase.getInstance(c);val key="SCAP/TEST_UI_FAILED_REGENERATION"
        val old=LocalExportJob("UI_READY_BEFORE_FAILURE",key,"SCAP","TEST_UI_FAILED_REGENERATION",1,
            scope="FULL",state="READY",createdAt=1,completed=1,total=1)
        val replacement=old.copy(id="UI_FAILED_REGENERATION",scope="FULL_LAYOUT_V4",state="FAILED",
            createdAt=2,completed=0,total=0,message="No hay espacio para guardar la copia local.")
        val previous=LocalSharedFile("UI_READY_BEFORE_FAILURE_FILE",old.id,key,"Download/GBO_Ruta_Clara/Puente Ficticio/",
            "conservado.pdf","application/pdf",1,"a".repeat(64),"FICTITIOUS_ORIGINAL","content://fictitious/retained","READY")
        runBlocking{d.localFilesDao().saveJob(old);d.localFilesDao().saveJob(replacement);d.localFilesDao().saveFile(previous)}
        ui.setContent{MaterialTheme{androidx.compose.foundation.layout.Column{LocalFilesList(key)}}}
        ui.waitUntil(5000){ui.onAllNodesWithText(replacement.message!!).fetchSemanticsNodes().isNotEmpty()}
        ui.onNodeWithText("Reintentar copia local").assertExists()
        ui.onNodeWithText("ARCHIVO GUARDADO · conservado.pdf").assertExists()
        ui.onNodeWithText("Ver historial y todas las copias (2)").performClick()
        ui.onNodeWithText("ARCHIVO GUARDADO · conservado.pdf").assertExists()
        ui.onNodeWithText("Ocultar copias anteriores").performClick()

        // A reserved but unverified replacement must not hide the usable document either.
        val current=previous.copy(id="UI_REPLACEMENT_FILE",jobId=replacement.id,name="regenerado.pdf",
            uri="content://fictitious/replacement",state="WRITING")
        runBlocking{d.localFilesDao().saveFile(current);d.localFilesDao().saveJob(replacement.copy(state="RUNNING",total=1))}
        ui.waitUntil(5000){ui.onAllNodesWithText("Guardando copia local: 0/1 verificados").fetchSemanticsNodes().isNotEmpty()}
        ui.onNodeWithText("ARCHIVO GUARDADO · conservado.pdf").assertExists()
        ui.onNodeWithText("ARCHIVO GUARDADO · regenerado.pdf").assertDoesNotExist()

        runBlocking{d.localFilesDao().saveFile(current.copy(state="READY"));d.localFilesDao().saveJob(replacement.copy(state="READY",completed=1,total=1))}
        ui.waitUntil(5000){ui.onAllNodesWithText("ARCHIVO GUARDADO · regenerado.pdf").fetchSemanticsNodes().isNotEmpty()}
        ui.onNodeWithText("ARCHIVO GUARDADO · conservado.pdf").assertDoesNotExist()
        ui.onNodeWithText("Ver historial y todas las copias (2)").performClick()
        ui.onNodeWithText("ARCHIVO GUARDADO · conservado.pdf").assertExists()
        ui.onNodeWithText("ARCHIVO GUARDADO · regenerado.pdf").assertExists()
        runBlocking{
            assertEquals(old,d.localFilesDao().job(old.id));assertEquals(previous,d.localFilesDao().files(old.id).single())
            assertTrue(d.automaticSyncDao().all().none{it.key==key})
        }
    }
    @Test fun savedFileButtonsAndMissingAccess(){
        val f=LocalSharedFile("TEST","JOB","SIC/TEST","Download/GBO_Ruta_Clara/SIC/","test.pdf","application/pdf",1,"a".repeat(64),"TEST_SOURCE","content://missing.document/test","READY")
        ui.setContent{MaterialTheme{SavedFileActions(f)}}
        for(text in listOf("ABRIR ARCHIVO","Compartir","VER UBICACIÓN","Guardar como…"))ui.onNodeWithText(text).assertExists()
        ui.onNodeWithText("ABRIR ARCHIVO").performClick();ui.waitUntil(5000){ui.onAllNodesWithText("El archivo fue movido",substring=true).fetchSemanticsNodes().isNotEmpty()}
        ui.onNodeWithText("Recuperar copia en Descargas").assertExists()
    }
    @Test fun automaticRequiresVisibleConsent(){
        ui.setContent{MaterialTheme{LocalCopiesSettings()}}
        ui.onNode(isToggleable()).performClick();assertFalse(LocalCopySettings.enabled(c));ui.onNodeWithText("Activar copias automáticas").assertExists();ui.onNodeWithText("Activar",useUnmergedTree=true).performClick()
        ui.waitUntil(5000){LocalCopySettings.enabled(c)};ui.onNodeWithText("¿Dónde están mis archivos?").assertExists()
    }
}
