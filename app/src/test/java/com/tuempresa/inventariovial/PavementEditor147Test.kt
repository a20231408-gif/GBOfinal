package com.tuempresa.inventariovial

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.tuempresa.inventariovial.pavement.ui.PavementEditorBody
import com.tuempresa.inventariovial.ui.theme.InventarioVialTheme
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Hosts the real dialog body. Window/IME/device behaviour still needs Android instrumentation. */
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],application=Application::class)
class PavementEditor147Test {
    @get:Rule val compose=createComposeRule()
    @Before fun init()=initializeIdleUiWorkers()
    @Test fun boundedViewportKeepsValuesAndSettlesAfterResizeAndLargeFont() {
        var height by mutableStateOf(600);var width by mutableStateOf(500);var font by mutableStateOf(1f)
        var value by mutableStateOf("12.5");var accepted="";var cancelled=false
        compose.setContent {CompositionLocalProvider(LocalDensity provides Density(1f,font)) {InventarioVialTheme {
            Box(Modifier.width(width.dp).height(height.dp)) {
                PavementEditorBody("Cabecera de muestra",{cancelled=true},{accepted=value}) {
                    repeat(12){Text("Campo $it",Modifier.height(48.dp))}
                    OutlinedTextField(value,{value=it},label={Text("Largo de losa")})
                }
            }
        }}}
        compose.onNodeWithText("Largo de losa").performScrollTo().performClick()
        compose.onNodeWithText("Largo de losa").performTextReplacement("7.25")
        for((w,h,f) in listOf(Triple(500,350,1f),Triple(800,350,1.6f),Triple(450,650,1.6f),Triple(500,600,1f))) {
            compose.runOnIdle {width=w;height=h;font=f};compose.waitForIdle()
            val positions=(1..5).map {compose.waitForIdle();compose.onNodeWithTag("pavement-editor-scroll").fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()}
            assertTrue("Unexpected continuous scroll: $positions",positions.max()-positions.min()<1f)
            assertEquals("7.25",value)
        }
        compose.onNodeWithText("Aceptar").performClick();assertEquals("7.25",accepted)
        compose.onNodeWithText("Cancelar").performClick();assertTrue(cancelled)
    }
}
