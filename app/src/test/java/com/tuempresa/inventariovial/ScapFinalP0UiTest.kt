package com.tuempresa.inventariovial

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.core.*
import com.tuempresa.inventariovial.scap.catalog.ScapCatalog
import com.tuempresa.inventariovial.scap.data.*
import com.tuempresa.inventariovial.scap.domain.*
import com.tuempresa.inventariovial.scap.ui.*
import com.tuempresa.inventariovial.viewmodel.InventoryViewModel
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=Application::class,qualifiers="w1100dp-h1400dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScapFinalP0UiTest {
    private val databases=mutableListOf<com.tuempresa.inventariovial.data.database.InventoryDatabase>()
    // Keep Room alive until Compose's rule has disposed every asynchronous observer.
    @get:Rule(order=0) val resources=object:org.junit.rules.TestWatcher(){
        override fun finished(description:org.junit.runner.Description){databases.forEach{it.close()}}
    }
    @get:Rule(order=1) val ui=createAndroidComposeRule<androidx.activity.ComponentActivity>()
    @Before fun workers(){initializeIdleUiWorkers()}
    @Test fun actualFormShowsZeroAllowsEditingAndReopeningAndItsCloseDialogDoesNotRejectZero()=runBlocking {
        val c=ui.activity;val app=ApplicationProvider.getApplicationContext<Application>()
        SicConsolidationFixtures.db(c).also{databases+=it}.let{db->
            val cat=ScapCatalog.load(c);val r=ScapRepository(db,cat);val id=r.create("TEST","TEST",null,ScapBridgeType.SLAB)
            val s=r.dao.snapshot(id)!!;val e=s.elements.first().element
            val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate);val ctl=ScapController(c,db,scope)
            val models=androidx.lifecycle.ViewModelStore();val vm=InventoryViewModel(app);models.put("test",vm)
            val show=mutableStateOf(true)
            try {
                ui.setContent{MaterialTheme{
                    if(show.value) {
                    val observed by r.dao.observe(id).collectAsState(s)
                    val pending by ctl.pending.collectAsState();val error by ctl.error.collectAsState()
                    ScapEditor(observed!!,ctl,cat.forSnapshot(observed!!),vm,"Guardado",pending,error)
                    }
                }}
                ui.runOnIdle{ctl.navigate(ScapFieldRoute("G","G1",e.id,"percent0"))}
                fun input(n:Int)=ui.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag("scap:${e.id}:percent$n")))
                input(0).performScrollTo().assertTextContains("0")
                input(1).performScrollTo().performTextReplacement("60")
                ui.waitUntil(20_000){ctl.pending.value==0 && runBlocking{r.dao.snapshot(id)!!.values(e.id)["percent1"]=="60"}}
                input(2).performScrollTo().performTextReplacement("40")
                ui.waitUntil(20_000){ctl.pending.value==0 && runBlocking{r.dao.snapshot(id)!!.values(e.id)["percent2"]=="40"}}
                ui.onNodeWithText("Guardar borrador").performClick();ui.waitUntil(20_000){ctl.pending.value==0}
                ui.runOnIdle{show.value=false};ui.runOnIdle{show.value=true;ctl.navigate(ScapFieldRoute("G","G1",e.id,"percent1"))}
                input(1).performScrollTo().assertTextContains("60");input(2).performScrollTo().assertTextContains("40")
                ui.onNodeWithText("Revisar y finalizar").performClick()
                ui.onNodeWithText("Confirmar cierre SCAP").assertExists()
                assertTrue(ui.onAllNodesWithText("Porcentaje 0 inválido",substring=true).fetchSemanticsNodes().isEmpty())
                val current=r.dao.snapshot(id)!!
                assertFalse(ScapCompletion.assess(current,cat.forSnapshot(current)).pending.any{it.ownerId==e.id&&it.key=="percent0"})
                assertEquals(ScapElementState.UNSET,ScapElementView.state(current,e)) // Categorical exception, never forged.
                ui.onNodeWithText("Confirmar cierre con pendientes").performClick()
                ui.waitUntil(20_000){ctl.pending.value==0 && runBlocking{r.dao.inspection(id)!!.status=="COMPLETE"}}
                val queued=db.automaticSyncDao().get("SCAP/$id")!!
                assertEquals("QUEUED",queued.state);assertTrue(queued.warningsConfirmed);assertEquals(0,queued.syncedRevision)
                assertNull(ctl.error.value)
            }finally{scope.cancel();ui.runOnIdle{show.value=false;models.clear()};ui.waitForIdle()}
        }
    }
    @Test fun realDeliveryReviewActionTargetsTheInvalidWidthAndPersistsItsCorrection()=runBlocking {
        val c=ui.activity
        SicConsolidationFixtures.db(c).also{databases+=it}.let{db->
            val cat=ScapCatalog.load(c);val r=ScapRepository(db,cat);val id=r.create("TEST","TEST",null,ScapBridgeType.SLAB)
            r.setField(id,"inspection","sidewalkWidthM","INVALIDO");r.saveDraft(id)
            val s=r.dao.snapshot(id)!!;val i=DriveSyncIntent("SCAP/$id","SCAP",id,s.inspection.updatedAt,state="PENDING_VALIDATION")
            val identity=DriveDeliveryPresentation.identity(db,i,c);val v=DriveDeliveryPresentation.view(i,identity,false,true,true)
            val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main);val ctl=ScapController(c,db,scope)
            val show=mutableStateOf(true)
            try {
            ui.setContent{MaterialTheme{Surface{
                if(show.value) {
                val target by ctl.destination.collectAsState();val observed by r.dao.observe(id).collectAsState(s)
                CompositionLocalProvider(LocalScapDestination provides target){
                    if(target==null)Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())){
                        DriveDeliveryCard(i,identity,v,onReview={ctl.navigate(identity.target!!)},onRetry={error("No retry invalid")})
                    }else Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        ScapFields(observed!!,ctl,cat.forSnapshot(observed!!),"B",editable=true,keys=listOf("sidewalkWidthM"))
                    }
                }
                }
            }}}
            ui.onNodeWithText("Validación SCAP:",substring=true).assertExists()
            ui.onNodeWithTag("drive-review-fields").performScrollTo().performClick()
            assertEquals("sidewalkWidthM",ctl.destination.value!!.second.fieldKey)
            ui.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag("scap:inspection:sidewalkWidthM"))).performScrollTo().performTextReplacement("0")
            ui.waitUntil(20_000){ctl.pending.value==0 && runBlocking{r.dao.snapshot(id)!!.values()["sidewalkWidthM"]=="0"}}
            assertTrue(ScapCompletion.assess(r.dao.snapshot(id)!!,cat.forSnapshot(s)).pending.none{it.key=="sidewalkWidthM"})
            assertFalse(SyncIntentPresentation.confirmed(i));assertTrue(db.automaticSyncDao().all().isEmpty())
            }finally{scope.cancel();ui.runOnIdle{show.value=false};ui.waitForIdle()}
        }
    }
}
