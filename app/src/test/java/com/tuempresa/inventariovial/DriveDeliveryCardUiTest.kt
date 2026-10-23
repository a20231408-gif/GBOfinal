package com.tuempresa.inventariovial

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import com.tuempresa.inventariovial.ui.theme.InventarioVialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.tuempresa.inventariovial.core.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
import java.io.File

/** Local rendering of the actual Compose cards, fictitious records; never a tablet screenshot. */
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],application=Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DriveDeliveryCardUiTest {
    @get:Rule val ui=createAndroidComposeRule<androidx.activity.ComponentActivity>()
    @Before fun setup(){initializeIdleUiWorkers()}
    private fun capture(name:String) {
        ui.runOnIdle {
            val v=ui.activity.window.decorView
            val b=Bitmap.createBitmap(v.width,v.height,Bitmap.Config.ARGB_8888);v.draw(Canvas(b))
            val f=File("../docs/entregas-drive-final-20261020/previews/$name.png");f.parentFile!!.mkdirs()
            f.outputStream().use{assertTrue(b.compress(Bitmap.CompressFormat.PNG,100,it))};b.recycle()
        }
    }
    private fun mount(i:DriveSyncIntent=deliveryFixture(),font:Float=1f,onRetry:()->Unit={}) {
        val id=DriveDeliveryIdentity("Puente 001 - Correviento","PE-28H · Tramo 3",2,true)
        val view=DriveDeliveryPresentation.view(i,id,false,true,true)
        ui.setContent{CompositionLocalProvider(LocalDensity provides Density(1f,font)){InventarioVialTheme{Surface {
            BoxWithConstraints(Modifier.fillMaxSize(),contentAlignment=Alignment.TopCenter) {
            val inset=if(maxWidth<600.dp)56.dp else 24.dp
            Column(Modifier.widthIn(max=560.dp).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=inset,vertical=12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text("Entregas Drive",style=MaterialTheme.typography.headlineSmall)
                DriveDeliveryCard(i,id,view,onRetry=onRetry)
            }}
        }}}}
    }
    @Test @Config(qualifiers="w400dp-h900dp-mdpi") fun phoneLargeFontKeepsIdentityAndHidesTechnicalBlocks() {
        mount(font=1.3f)
        ui.onNodeWithText("Puente 001 - Correviento").assertIsDisplayed()
        ui.onNodeWithText("✓ Subido correctamente").assertIsDisplayed()
        ui.onNodeWithText("Archivos confirmados: 1 de 1").assertIsDisplayed()
        ui.onNodeWithText("Identidad:",substring=true).assertDoesNotExist()
        ui.onNodeWithText("Destino del último ACK:",substring=true).assertDoesNotExist()
        capture("telefono-fuente-grande")
        ui.onNodeWithText("+ Destino confirmado").performScrollTo().performClick()
        ui.onNodeWithText("PE-28H/PUENTES/PUENTE 1").performScrollTo().assertIsDisplayed()
        ui.onNodeWithText("+ Archivos").performScrollTo().performClick()
        ui.onNodeWithText("F01.jpg · Confirmado").performScrollTo().assertIsDisplayed()
        ui.onNodeWithText("+ Detalles técnicos").performScrollTo().performClick()
        ui.onNodeWithText("Identidad: SIC/FICTICIO_DRIVE").performScrollTo().assertIsDisplayed()
    }
    @Test @Config(qualifiers="w800dp-h1280dp-mdpi") fun tabletPendingRetryAndDetailsKeepTheirExistingActions() {
        var retries=0;val i=deliveryFixture().copy(state="ERROR",syncedRevision=0,error="Servidor ocupado (BUSY). Esperando para reintentar.")
        mount(i,onRetry={retries++})
        ui.onNodeWithText("… Pendiente de subir").assertIsDisplayed()
        ui.onNodeWithText("Esperando para reintentar.").assertIsDisplayed()
        ui.onNodeWithText("Archivos confirmados:",substring=true).assertDoesNotExist()
        ui.onNodeWithText("Reintentar ahora").performScrollTo().performClick();assertEquals(1,retries)
        capture("tablet-reintento-busy")
        ui.onNodeWithText("+ Archivos").performScrollTo().performClick()
        ui.onNodeWithText("F01.jpg · Sin confirmación final").performScrollTo().assertIsDisplayed()
        capture("tablet-detalle-archivos")
        ui.onNodeWithText("+ Destino confirmado").performScrollTo().performClick()
        ui.onNodeWithText("Esta revisión aún no tiene un destino confirmado verificable.").performScrollTo().assertIsDisplayed()
    }
    @Test @Config(qualifiers="w800dp-h1280dp-mdpi") fun twinNamesHaveDistinctCardsWithoutDisplayingUuid() {
        val a=deliveryFixture();val b=a.copy(key="SIC/FICTICIO_TWIN",recordId="FICTICIO_TWIN",state="QUEUED",syncedRevision=0)
        val id=DriveDeliveryIdentity("Puente - Correviento","PE-28H",2,true)
        ui.setContent{InventarioVialTheme{Column(Modifier.verticalScroll(rememberScrollState())){
            for(i in listOf(a,b))DriveDeliveryCard(i,id,DriveDeliveryPresentation.view(i,id,false,true,true),onRetry={})
        }}}
        ui.onAllNodesWithText(id.title).assertCountEquals(2)
        ui.onNodeWithTag("delivery-${a.key}").assertExists();ui.onNodeWithTag("delivery-${b.key}").assertExists()
        ui.onNodeWithText("Identidad:",substring=true).assertDoesNotExist()
    }
}
