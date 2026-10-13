package com.tuempresa.inventariovial

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.tuempresa.inventariovial.field.PhotoCategoryPolicy
import com.tuempresa.inventariovial.media.BridgeMediaCategory
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],application=Application::class)
class BridgeMedia10ScreenTest {
    @get:Rule val compose=createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private fun panel() {
        var photos=0
        compose.setContent {MaterialTheme {BridgeMediaCategory("SCAP/ui10",PhotoCategoryPolicy.Category("UPSTREAM","Vista de prueba","AGUAS_ARRIBA"),true,2,{photos++},{}) {Text("Fotografía existente")}}}
        compose.onAllNodesWithText("Vista de prueba").assertCountEquals(1)
        compose.onNodeWithText("2 fotos · 0 videos").assertExists()
        compose.onNodeWithText("Fotografía existente").assertExists()
        val photo=compose.onNodeWithText("FOTO").fetchSemanticsNode().boundsInRoot
        val video=compose.onNodeWithText("VIDEO").fetchSemanticsNode().boundsInRoot
        assertEquals(photo.top,video.top,1f);assertTrue(photo.right<=video.left)
        compose.onNodeWithText("FOTO").performClick();compose.runOnIdle {assertEquals(1,photos)}
        compose.onNodeWithText("VIDEO").performClick()
        compose.onNodeWithText("Grabar video").assertExists();compose.onNodeWithText("Importar videos").assertExists()
    }
    @Test @Config(qualifiers="w800dp-h1280dp-mdpi") fun singleCategoryPortraitHasSideBySideActionsAndSeparateCounters()=panel()
    @Test @Config(qualifiers="w1280dp-h800dp-mdpi") fun singleCategoryLandscapeHasSideBySideActionsAndSeparateCounters()=panel()
}
