package com.tuempresa.inventariovial

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.tuempresa.inventariovial.core.*
import com.tuempresa.inventariovial.ui.theme.InventarioVialTheme
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
import java.io.File

/** Actual shared list/card components, fictitious data and native Robolectric drawing. */
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],application=Application::class,qualifiers="w800dp-h1280dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DriveDeliveryListsUiTest {
    @get:Rule val ui=createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private var rows by mutableStateOf(emptyList<DriveDeliveryEntry>())
    private var retries=0
    @Before fun setup(){initializeIdleUiWorkers()}
    private fun mount(font:Float=1f){ui.setContent{
        CompositionLocalProvider(LocalDensity provides Density(1f,font)){InventarioVialTheme{Surface{Box(Modifier.fillMaxSize(),contentAlignment=Alignment.TopCenter){
            Column(Modifier.widthIn(max=560.dp).fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp).testTag("drive-list-root"),verticalArrangement=Arrangement.spacedBy(8.dp)){
                Text("Entregas Drive",style=MaterialTheme.typography.headlineSmall)
                DriveDeliveryLists(rows){retries++}
            }
        }}}}
    }}
    private fun section(section:DriveDeliverySection){ui.onNodeWithTag("drive-section-${section.name}").performScrollTo().performClick()}
    private fun capture(name:String){ui.runOnIdle{
        val v=ui.activity.window.decorView;val b=Bitmap.createBitmap(v.width,v.height,Bitmap.Config.ARGB_8888);v.draw(Canvas(b))
        val f=File("../docs/cola-historial-drive-20261021/previews/$name.png");f.parentFile!!.mkdirs()
        f.outputStream().use{assertTrue(b.compress(Bitmap.CompressFormat.PNG,100,it))};b.recycle()
    }}
    @Test fun confirmationDisappearsFromQueueButRemainsInHistoryAndEditedRevisionReturns() {
        var row=listEntry("LIVE","Muro - PE-28C - PR 10+060");rows=listOf(row);mount()
        ui.onNodeWithTag("delivery-${row.intent.key}").assertExists()
        section(DriveDeliverySection.HISTORY);ui.onNodeWithTag("delivery-${row.intent.key}").assertExists()
        section(DriveDeliverySection.QUEUE)
        row=listEntry("LIVE","Muro - PE-28C - PR 10+060",state="UPLOADING",running=true)
        ui.runOnIdle{rows=listOf(row)};ui.onNodeWithText("↑ Subiendo").assertExists()
        section(DriveDeliverySection.HISTORY);ui.onNodeWithText("↑ Subiendo").assertExists()
        section(DriveDeliverySection.QUEUE)
        row=listEntry("LIVE","Muro - PE-28C - PR 10+060",state="UPLOADING")
        ui.runOnIdle{rows=listOf(row)};ui.onNodeWithText("◌ Verificando entrega").assertExists()
        row=listEntry("LIVE","Muro - PE-28C - PR 10+060",state="SYNCED")
        ui.runOnIdle{rows=listOf(row)}
        ui.onNodeWithTag("delivery-${row.intent.key}").assertDoesNotExist()
        ui.onNodeWithText(DriveDeliveryListPresentation.EMPTY_QUEUE).assertExists()
        section(DriveDeliverySection.HISTORY)
        ui.onNodeWithTag("delivery-${row.intent.key}").assertExists();ui.onNodeWithText("✓ Subido correctamente").assertExists()
        val i=row.intent.copy(revision=3,state="UPDATE_PENDING");val id=row.identity.copy(currentRevision=3)
        row=DriveDeliveryEntry(i,id,DriveDeliveryPresentation.view(i,id,false,true,true))
        ui.runOnIdle{rows=listOf(row)}
        ui.onNodeWithText("✓ Subido correctamente").assertDoesNotExist()
        section(DriveDeliverySection.QUEUE);ui.onNodeWithTag("delivery-${row.intent.key}").assertExists()
        assertEquals(0,retries)
    }
    @Test fun queueAndHistoryPreviewsSearchFiltersTwinNamesAndDetailsDoNotEnqueue() {
        val pending=listEntry("PENDING");val done=listEntry("DONE",state="SYNCED")
        rows=listOf(done,pending);mount()
        ui.onNodeWithTag("delivery-${done.intent.key}").assertDoesNotExist()
        ui.onNodeWithTag("delivery-${pending.intent.key}").assertExists()
        ui.onNodeWithText("SIC-17").assertExists();capture("01-en-la-carga")
        section(DriveDeliverySection.HISTORY)
        ui.onAllNodesWithText("Puente - Correviento").assertCountEquals(2)
        capture("02-mis-entregas")
        ui.onNodeWithTag("drive-history-search").performScrollTo().performTextReplacement("PE-28H 10+060")
        ui.onAllNodesWithText("Puente - Correviento").assertCountEquals(2)
        ui.onNodeWithTag("drive-history-filter-COMPLETE").performScrollTo().performClick()
        ui.onNodeWithTag("delivery-${done.intent.key}").assertExists();ui.onNodeWithTag("delivery-${pending.intent.key}").assertDoesNotExist()
        ui.onNodeWithText("+ Archivos").performScrollTo().performClick();ui.onNodeWithText("F01.jpg · Confirmado").assertExists()
        ui.onNodeWithText("+ Destino confirmado").performScrollTo().performClick();ui.onNodeWithText("PE-28H/PUENTES/PUENTE 1").assertExists()
        section(DriveDeliverySection.QUEUE)
        ui.onNodeWithTag("delivery-${pending.intent.key}").assertExists() // history query/filter cannot suppress the queue
        section(DriveDeliverySection.HISTORY)
        ui.onNodeWithTag("drive-history-search").performScrollTo().performTextReplacement("NO COINCIDE")
        ui.onNodeWithText("No hay entregas que coincidan con la búsqueda o el filtro.").assertExists()
        ui.onNodeWithText("Identidad:",substring=true).assertDoesNotExist();assertEquals(0,retries)
    }
    @Test @Config(qualifiers="w400dp-h900dp-mdpi") fun emptyQueueAndAttentionHistoryRemainReadableWithLargeFont() {
        rows=listOf(listEntry("DONE",state="SYNCED"));mount(1.3f)
        ui.onNodeWithText(DriveDeliveryListPresentation.EMPTY_QUEUE).assertIsDisplayed();capture("03-cola-vacia-telefono")
        section(DriveDeliverySection.HISTORY);ui.onNodeWithText("✓ Subido correctamente").assertExists()
        val base=listEntry("ERROR","Badén - PE-28C - PR 12+150",state="BLOCKED")
        val i=base.intent.copy(error="Fotografía rechazada (NOT_JPEG). Original conservado.")
        ui.runOnIdle{rows=rows+base.copy(intent=i,view=DriveDeliveryPresentation.view(i,base.identity,false,true,true))}
        ui.onNodeWithTag("drive-history-filter-ATTENTION").performScrollTo().performClick()
        ui.onNodeWithText("! Necesita atención").assertExists()
        ui.onNodeWithText("+ Detalles técnicos").performScrollTo().performClick()
        ui.onNodeWithText(i.error!!).assertExists()
        section(DriveDeliverySection.QUEUE);ui.onNodeWithTag("delivery-${i.key}").assertExists()
        assertEquals(0,retries)
    }
}
