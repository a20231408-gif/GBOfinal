package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.pavement.*
import com.tuempresa.inventariovial.pavement.ui.*
import com.tuempresa.inventariovial.core.*
import com.tuempresa.inventariovial.ui.theme.InventarioVialTheme
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],application=Application::class)
@GraphicsMode(GraphicsMode.Mode.LEGACY)
class PavementFieldUi10ITest {
    @get:Rule val compose=createComposeRule()
    private fun damage(code:Int=15)=PavementDamageEntity("test-damage","test-sample",1,if(code==15)"ASPHALT" else "RIGID",code,if(code==15)"Ahuellamiento" else "Parcheo pequeño","M",if(code==15)"AREA" else "SLABS",.5,1.0,1.5,2.0,null,1.0,1.0,1.0,null,null,"",slabs=if(code==15)"" else "1")
    private fun capture(name:String) {
        // Screens are real composables with fictitious data, never the owner's tablet.
        runCatching {val dir=File("../docs/manual-10i/figuras").apply {mkdirs()};val image=compose.onNode(isDialog()).captureToImage().asAndroidBitmap()
            val colors=mutableSetOf<Int>();for(y in 0 until image.height step 20)for(x in 0 until image.width step 20)colors+=image.getPixel(x,y)
            check(colors.size>3){"Capture is blank in this renderer"}
            File(dir,name).outputStream().use {image.compress(Bitmap.CompressFormat.PNG,100,it)}}
    }
    @Test fun rutFields() {
        compose.mainClock.autoAdvance=false
        compose.setContent {InventarioVialTheme {DamageDialog(damage(),"",{}, {})}}
        compose.mainClock.advanceTimeBy(1000)
        compose.onNodeWithText("Ancho de fisura (mm), si aplica").assertDoesNotExist()
        compose.onNodeWithText("Profundidad media del ahuellamiento (mm)").performScrollTo().assertIsDisplayed()
        capture("ahuellamiento.png")
    }
    @Test fun slabFields() {
        compose.mainClock.autoAdvance=false
        compose.setContent {InventarioVialTheme {DamageDialog(damage(30),"",{}, {})}}
        compose.mainClock.advanceTimeBy(1000)
        compose.onNodeWithText("Localizar dentro de las losas indicadas").assertExists()
        compose.onNodeWithText("X1 / centro X (m)").assertExists();compose.onNodeWithText("X2 (m)").assertExists()
        capture("losa-localizada.png")
    }
    @Test fun photoButtonUsesTheImageContractAndKeepsTheExactDamageUuid() {
        val context=ApplicationProvider.getApplicationContext<Context>();FieldCameraSelection.select(context,null)
        assertEquals(MediaStore.ACTION_IMAGE_CAPTURE,FieldTakePicture().createIntent(context,Uri.parse("content://test/10i.jpg")).action)
        val calls=mutableListOf<String>();val d=damage()
        compose.setContent {InventarioVialTheme {PavementDamageTable(listOf(d),null,{},mapOf(d.id to 0),{calls+=it},{})}}
        compose.onNodeWithTag("damage-camera-${d.id}").performClick()
        assertEquals(listOf(d.id),calls)
        compose.onAllNodes(hasText("video",substring=true,ignoreCase=true)).assertCountEquals(0)
    }
}
