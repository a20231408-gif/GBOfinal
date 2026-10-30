package com.tuempresa.inventariovial

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.room.Room
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.scap.catalog.ScapCatalog
import com.tuempresa.inventariovial.scap.data.ScapRepository
import com.tuempresa.inventariovial.scap.domain.*
import com.tuempresa.inventariovial.scap.ui.*
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=Application::class,qualifiers="w800dp-h1000dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScapPhotoPanelUiTest {
    @get:Rule val ui=createAndroidComposeRule<androidx.activity.ComponentActivity>()
    @Before fun workers(){initializeIdleUiWorkers()}
    @Test fun realPanelShowsEveryNumberedCategoryAndCreatesAnOfficialDraftRecordThroughItsNormalDialog()=runBlocking {
        val context=ui.activity
        Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build().use{db->
            val catalog=ScapCatalog.load(context);val repo=ScapRepository(db,catalog)
            val id=repo.create("FICTICIO","FICTICIO",null,ScapBridgeType.MODULAR)
            val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main);val controller=ScapController(context,db,scope)
            controller.select(id)
            ui.setContent{val s by controller.snapshot.collectAsState();MaterialTheme{Surface{Column(Modifier.fillMaxSize()){
                s?.let{ScapMedia(it,controller,catalog.forSnapshot(it),true,false)}
            }}}}
            ui.waitUntil(20_000){ui.onAllNodesWithTag("scap-photo-panel").fetchSemanticsNodes().isNotEmpty()}
            for(cat in ScapPhotoPanel.categories){
                ui.onNodeWithTag("scap-photo-panel").performScrollToNode(hasText(cat.displayLabel))
                ui.onNodeWithText(cat.displayLabel).assertIsDisplayed()
            }
            ui.onNodeWithTag("scap-photo-panel").performScrollToNode(hasText("+ Añadir registro fotográfico"))
            ui.onNodeWithText("+ Añadir registro fotográfico").performClick()
            ui.onNodeWithText("Sin completar").performClick()
            ui.onNodeWithText("VISTA PANORAMICA LADO DERECHO",useUnmergedTree=true).performClick()
            ui.onNodeWithText("Añadir",useUnmergedTree=true).performClick()
            ui.waitUntil(20_000){controller.pending.value==0 && runBlocking{repo.dao.snapshot(id)!!.photos.size==1}}
            val s=repo.dao.snapshot(id)!!;assertEquals("SCAP20_01",s.photos.single().photoCategory)
            assertEquals("VISTA PANORAMICA LADO DERECHO",s.values(s.photos.single().id)["photoName"])
            assertEquals(1,repo.dao.allSnapshots().size);assertTrue(db.automaticSyncDao().all().isEmpty());scope.cancel()
        }
    }
}
