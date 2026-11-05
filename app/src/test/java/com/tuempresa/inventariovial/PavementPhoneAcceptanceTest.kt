package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.room.Room
import com.tuempresa.inventariovial.auth.*
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.pavement.*
import com.tuempresa.inventariovial.pavement.ui.*
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.*

/** Real Compose controls with synthetic Room rows; physical phone acceptance remains separate. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=Application::class,qualifiers="w411dp-h900dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PavementPhoneAcceptanceTest {
    @get:Rule val ui=createAndroidComposeRule<ComponentActivity>()
    @Before fun init()=initializeIdleUiWorkers()
    private fun sample()=PavementSampleEntity("PHONE_FICTICIO","TRAMO FICTICIO","PE-22A",1,"Derecho","UC","00+000","00+040",5.0,40.0,"ASPHALT","2026-11-03","TEST","Inspector ficticio")
    private fun repo(db:InventoryDatabase)=PavementRepository(db,AuthSessionManager().apply{login(UserCacheEntity("TEST","test","FICTICIO","OPERATOR",true,"",0,0,0))})

    @Test fun titleContextAndReviewRemainIndependentAtPhoneWidthsAndLargeFont() {
        var width by mutableStateOf(320);var font by mutableStateOf(1f);var reviewed=0;var back=0
        ui.setContent{CompositionLocalProvider(LocalDensity provides Density(1f,font)){MaterialTheme{
            Box(Modifier.width(width.dp).height(800.dp)){PavementEditorHeader(sample(),true,false,{back++},{reviewed++},{})}
        }}}
        for((w,f) in listOf(320 to 1f,360 to 1.5f,411 to 1.8f,320 to 1.8f)){
            ui.runOnIdle{width=w;font=f};ui.waitForIdle()
            val title=ui.onNodeWithTag("pavement-editor-title").fetchSemanticsNode().boundsInRoot
            val review=ui.onNodeWithTag("pavement-review").fetchSemanticsNode().boundsInRoot
            assertTrue("Title compressed into an icon column: $title",title.width>=220f)
            assertTrue("Action overlaps title: $title / $review",review.top>=title.bottom)
            ui.onNodeWithText("Fallas en pavimentos").assertIsDisplayed()
            ui.onNodeWithTag("pavement-editor-context").assertIsDisplayed()
            ui.onNodeWithTag("pavement-review").assertIsDisplayed().performClick()
            ui.onNodeWithText("Volver a Muestras").performClick()
        }
        assertEquals(4,reviewed);assertEquals(4,back)
    }

    @Test fun phoneSchemeCanCreateDamageThenEditViewPhotosAndRequestNativePdf()=runBlocking {
        val c=ui.activity as Context;val db=Room.inMemoryDatabaseBuilder(c,InventoryDatabase::class.java).build()
        val repo=repo(db);val s=sample();db.pavementDao().saveSample(s);var showing by mutableStateOf(true)
        try{
            ui.setContent{if(showing)CompositionLocalProvider(LocalDensity provides Density(1f,1.2f)){MaterialTheme{
                Box(Modifier.width(360.dp).height(800.dp)){PavementEditor(s,repo,"TEST",onBack={})}
            }}}
            ui.onNodeWithTag("pavement-scheme").performScrollTo().assertIsDisplayed()
            val canvas=ui.onNodeWithTag("pavement-scheme").fetchSemanticsNode().boundsInRoot
            assertTrue(canvas.height>=180f)
            val viewport=PavementViewport.fit(s.anchoM,s.largoM,canvas.width,canvas.height,64f,1f,0f,0f)
            fun point(x:Double,y:Double)=Offset(viewport.originX+(x*viewport.scale).toFloat(),viewport.canvasY(y))
            ui.onNodeWithTag("pavement-scheme").performTouchInput{click(point(1.0,10.0));click(point(2.0,20.0))}
            ui.onNodeWithText("Aceptar").performClick()
            ui.waitUntil(10000){runBlocking{db.pavementDao().damages(s.id).size==1}}
            val damage=db.pavementDao().damages(s.id).single();assertEquals(10.0,damage.areaM2!!,0.02)
            ui.onNodeWithTag("damage-row-${damage.id}").performScrollTo().performClick()
            ui.onNodeWithText("Modificar",substring=true).performScrollTo().performClick()
            ui.onNodeWithText("Cancelar",useUnmergedTree=true).performClick()
            ui.onNodeWithTag("pavement-damage-list").performScrollTo().performScrollToNode(hasTestTag("damage-photos-${damage.id}"))
            ui.onNodeWithTag("damage-photos-${damage.id}").performScrollTo()
            val photoBounds=ui.onNodeWithTag("damage-photos-${damage.id}").fetchSemanticsNode().boundsInRoot
            assertTrue(photoBounds.width>=100f&&photoBounds.height>=48f)
            ui.onNodeWithTag("damage-photos-${damage.id}").assertIsDisplayed().performClick()
            ui.waitUntil(5000){ui.onAllNodesWithText("Volver a la muestra").fetchSemanticsNodes().isNotEmpty()}
            ui.onNodeWithText("Volver a la muestra").performScrollTo().assertIsDisplayed().performClick()
            ui.onNodeWithText("Reporte PDF").performScrollTo().assertIsDisplayed().performClick()
            val request=shadowOf(ui.activity).nextStartedActivityForResult
            assertEquals(Intent.ACTION_CREATE_DOCUMENT,request.intent.action);assertEquals("application/pdf",request.intent.type)
            assertTrue(db.automaticSyncDao().all().isEmpty());assertEquals(1,db.pavementDao().damages(s.id).size)
        }finally{ui.runOnIdle{showing=false};ui.waitForIdle();db.close()}
    }
}
