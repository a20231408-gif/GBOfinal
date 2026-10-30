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

/** Actual A input -> persistent controller journal -> Room -> actual SIC panel. No device/Google. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=Application::class,qualifiers="w800dp-h1000dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScapProgressiveUiTest {
    @get:Rule val ui=createAndroidComposeRule<androidx.activity.ComponentActivity>()
    @Before fun workers(){initializeIdleUiWorkers()}

    @Test fun typingConventionalKilometresThenOpeningSicUsesAllDigits()=runBlocking {
        val context=ui.activity
        Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build().use { db->
            val catalog=ScapCatalog.load(context);val repo=ScapRepository(db,catalog)
            val id=repo.create("FICTICIO","FICTICIO",null,ScapBridgeType.MODULAR)
            repo.setField(id,"inspection","route","PE-28H")
            repo.setField(id,"inspection","sic17.roadbedCode","CD")
            val state=mutableStateOf(repo.dao.snapshot(id)!!);val sic=mutableStateOf(false)
            val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main)
            val controller=ScapController(context,db,scope)
            controller.select(id)
            ui.setContent { MaterialTheme { Surface { Column(Modifier.fillMaxSize()) {
                val observed by controller.snapshot.collectAsState()
                val current=observed ?: state.value
                Button(onClick={sic.value=true}){Text("Abrir SIC de prueba")}
                if(sic.value) ScapBridgeSicPanel(current,controller,true)
                else ScapFields(current,controller,catalog,"A",editable=true,keys=listOf("progressive"))
            } } } }
            for(text in listOf("1","10","10+","10+1","10+10","10+100")) {
                ui.onNode(hasSetTextAction()).performTextReplacement(text)
                ui.waitUntil(20_000){controller.pending.value==0 && runBlocking {repo.dao.snapshot(id)!!.values()["progressive"]==text}}
                state.value=repo.dao.snapshot(id)!!
                if(text=="1") {
                    assertEquals("0000",state.value.roadRecord!!.startPrCode)
                    assertEquals(1.0,state.value.roadRecord!!.startDistanceM,0.0)
                }
            }
            ui.onNodeWithText("Abrir SIC de prueba").performClick()
            val saved=repo.dao.snapshot(id)!!
            assertEquals("10+100",saved.values()["progressive"])
            assertEquals("0010",saved.roadRecord!!.startPrCode)
            assertEquals(100.0,saved.roadRecord!!.startDistanceM,0.0)
            ui.onNodeWithText("PE-28H · CD · PR 0010 + 100.0 m").assertExists()
            assertEquals(1,repo.dao.allSnapshots().size)
            assertTrue(db.automaticSyncDao().all().isEmpty())
            ui.onNodeWithText("Heredado de kilometraje SCAP · pendiente de revisión SIC").assertExists()
            scope.cancel()
        }
    }
    @Test fun manualConflictIsVisibleAndOnlyExplicitChoiceChangesTheLinkedLocation()=runBlocking {
        val context=ui.activity
        Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build().use{db->
            val repo=ScapRepository(db,ScapCatalog.load(context))
            val id=repo.create("FICTICIO","FICTICIO",null,ScapBridgeType.MODULAR)
            repo.setField(id,"inspection","route","PE-28H")
            repo.setField(id,"inspection","progressive","10+100")
            repo.saveSicLocation(id,"PE-28H","CD","0020","35","0021","12")
            val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main);val controller=ScapController(context,db,scope)
            controller.select(id)
            ui.setContent{val s by controller.snapshot.collectAsState();MaterialTheme{Surface{Column(Modifier.fillMaxSize()){
                s?.let{ScapBridgeSicPanel(it,controller,true)}
            }}}}
            ui.waitUntil(20_000){ui.onAllNodesWithTag("sic-location-discrepancy").fetchSemanticsNodes().isNotEmpty()}
            ui.onNodeWithTag("sic-location-discrepancy").assertTextContains("Se conserva la ubicación confirmada manualmente",substring=true)
            ui.onNodeWithText("PE-28H · CD · PR 0020 + 35.0 m").assertExists()
            assertEquals("0020",repo.dao.snapshot(id)!!.roadRecord!!.startPrCode)
            ui.onNodeWithText("Usar kilometraje SCAP").performScrollTo().performClick()
            ui.waitUntil(20_000){controller.pending.value==0 && runBlocking{repo.dao.snapshot(id)!!.roadRecord!!.locationSource==ScapSicLocation.INHERITED}}
            ui.waitUntil(20_000){ui.onAllNodesWithText("PE-28H · CD · PR 0010 + 100.0 m").fetchSemanticsNodes().size==1}
            ui.onNodeWithText("PE-28H · CD · PR 0010 + 100.0 m").assertExists()
            ui.onNodeWithTag("sic-location-discrepancy").assertDoesNotExist()
            // The manual editor must now seed from the selected source, not its old remembered PR.
            ui.onNodeWithText("Modificar ubicación SIC").performClick()
            ui.onNode(hasSetTextAction() and hasText("0010")).assertExists()
            assertEquals("0021",repo.dao.snapshot(id)!!.roadRecord!!.endPrCode)
            assertEquals(1,repo.dao.allSnapshots().size);assertTrue(db.automaticSyncDao().all().isEmpty());scope.cancel()
        }
    }
}
