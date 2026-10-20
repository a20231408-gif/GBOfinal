package com.tuempresa.inventariovial

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import com.tuempresa.inventariovial.dashboard.*
import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.sic.InventoryElements
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.*

@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],application=Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Dashboard6ScreenTest {
    @get:Rule val compose=createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private val today=LocalDate.of(2026,10,11)
    private val time=today.atTime(8,0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    private val session=FieldSession("s","Proyecto ficticio","Ingeniero de prueba","test","PE-12A",null,"CD","INCREASING",time)
    private fun draft(id:String="technical-uuid-413b",pr:String="0041")=testRecord(id,pr,325.5,time,"DRAFT","PE-12A",sic="SIC-18")
        .copy(sessionId="s",endPrCode=null,endDistanceM=null,observations="Observación conservada")
    private fun screenshot(name:String) {
        // Native Robolectric drawing of the actual Compose view; PixelCopy is unavailable here.
        compose.runOnIdle {
            val view=compose.activity.window.decorView
            val image=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
            view.draw(Canvas(image))
            val file=File("build/outputs/ruta-clara-etapa-b/renderizados-locales/$name.png");file.parentFile!!.mkdirs()
            file.outputStream().use {image.compress(Bitmap.CompressFormat.PNG,100,it)}
            image.recycle()
        }
    }
    private fun home(s:FieldSession?=session,rows:List<InventoryRecordEntity> = listOf(draft()),font:Float=1f,action:(String)->Unit={}) {
        compose.setContent {CompositionLocalProvider(LocalDensity provides Density(1f,font)) {MaterialTheme {
            DashboardScreen(s,listOf(session),rows,emptyList(),DashboardDeliveryCounts(2,3,1,1),false,today,
                {action("session")},{action("new")},{action("records")},{action("pavement")},{action("drive")},
                {action("tools")},{action("all")},{action(it.id)})
        }}}
    }
    @Test @Config(qualifiers="w800dp-h1280dp-mdpi")
    fun portraitSessionProgressiveAndAccessibleActions() {
        home()
        compose.onNodeWithText("GBO Inventario Vial").assertIsDisplayed()
        compose.onNodeWithText("Proyecto ficticio · Ingeniero de prueba").assertIsDisplayed()
        compose.onNodeWithTag("session-action").assertHasClickAction().assertIsDisplayed()
        compose.onNodeWithText("CONFIGURAR SESIÓN").assertIsDisplayed()
        compose.onNodeWithText("PR 0041+325.50").assertIsDisplayed()
        compose.onNodeWithText("technical-uuid-413b",substring=true).assertDoesNotExist()
        screenshot("01-dashboard-vertical-datos-ficticios")
    }
    @Test @Config(qualifiers="w1280dp-h800dp-mdpi")
    fun landscapeLargeFontNavigationOfFourMainActionsAndTools() {
        val actions=mutableListOf<String>();home(font=1.3f,action={actions+=it})
        compose.onNodeWithTag("session-action").performClick()
        listOf("NUEVO REGISTRO","MIS REGISTROS","PAVIMENTOS","ENTREGAS DRIVE").forEach {
            compose.onNodeWithTag(it).performScrollTo().assertHasClickAction().performClick()
        }
        compose.onNodeWithTag("dashboard").performScrollToNode(hasTestTag("tools"))
        compose.onNodeWithTag("tools").performClick()
        compose.runOnIdle {Assert.assertEquals(listOf("session","new","records","pavement","drive","tools"),actions)}
        compose.onNodeWithTag("dashboard").performScrollToIndex(0)
        screenshot("02-dashboard-horizontal-fuente-130-datos-ficticios")
    }
    @Test @Config(qualifiers="w600dp-h960dp-mdpi")
    fun noSessionShowsStartAndDriveStatesAreDistinct() {
        home(s=null,rows=emptyList())
        compose.onNodeWithText("INICIAR SESIÓN").assertIsDisplayed()
        compose.onNodeWithText("0 borradores del día").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Sincronizados: 2").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Pendientes: 3 · Actualizaciones pendientes: 1").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Errores: 1").performScrollTo().assertIsDisplayed()
    }
    @Test @Config(qualifiers="w800dp-h1280dp-mdpi")
    fun fiftyDraftsScrollInsideBoundedListAndResumeExactData() {
        val rows=(1..50).map {draft("draft-$it",it.toString())}
        var resumed="";home(rows=rows,action={resumed=it})
        compose.onNodeWithText("50 borradores del día").assertIsDisplayed()
        val bounds=compose.onNodeWithTag("daily-drafts").fetchSemanticsNode().boundsInRoot
        Assert.assertTrue(bounds.height<=360f)
        compose.onNodeWithTag("daily-drafts").performScrollToIndex(49)
        compose.onNodeWithTag("continue-draft-50").performClick()
        compose.runOnIdle {Assert.assertEquals("draft-50",resumed);Assert.assertEquals("Observación conservada",rows.last().observations)}
        compose.onNodeWithTag("all-drafts").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle {Assert.assertEquals("all",resumed)}
    }
    @Test @Config(qualifiers="w800dp-h1280dp-mdpi")
    fun allDraftsFiltersHistoricalRowsAndContinuesOriginal() {
        val old=draft("old").copy(sessionId=null,createdAt=time-86400000)
        var resumed:InventoryRecordEntity?=null
        compose.setContent {MaterialTheme {AllDraftsScreen(listOf(old,draft()),listOf(session),emptyList(),{resumed=it},{})}}
        compose.onNodeWithTag("filter-date").performTextInput("2026-10-10")
        compose.onNodeWithTag("filter-route").performTextInput("PE-12A")
        compose.onNodeWithTag("filter-bed").performTextInput("CD")
        compose.onNodeWithTag("filter-type").performTextInput("Alcantarilla")
        compose.onNodeWithTag("continue-old").performScrollTo().performClick()
        compose.runOnIdle {Assert.assertEquals(old,resumed)}
        screenshot("03-borradores-historicos-filtrados-datos-ficticios")
    }
    @Test @Config(qualifiers="w800dp-h1280dp-mdpi")
    fun newRecordChooserPreservesEveryExistingDestination() {
        val assets=mutableListOf<com.tuempresa.inventariovial.model.RoadAssetType>()
        val formats=mutableListOf<com.tuempresa.inventariovial.sic.SicFieldFormat>()
        compose.setContent {MaterialTheme {NewRecordChooser({assets+=it},{formats+=it},{})}}
        InventoryElements.home.forEach {entry->compose.onNodeWithText(entry.title).performScrollTo().performClick()}
        compose.runOnIdle {
            Assert.assertEquals(InventoryElements.home.mapNotNull {it.asset},assets)
            Assert.assertEquals(InventoryElements.home.mapNotNull {it.format},formats)
        }
    }
}
