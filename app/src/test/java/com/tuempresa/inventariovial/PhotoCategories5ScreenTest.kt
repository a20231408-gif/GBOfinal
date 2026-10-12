package com.tuempresa.inventariovial

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.tuempresa.inventariovial.field.LegacyInventoryPhotos
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],application=Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PhotoCategories5ScreenTest {
    @get:Rule val compose=createAndroidComposeRule<androidx.activity.ComponentActivity>()
    @Test @Config(qualifiers="w800dp-h1280dp-mdpi")
    fun portraitShowsCountersHistoricalGeneralReplacementAndConfirmedRemoval() {
        var paths by mutableStateOf(listOf("inicio-a","inicio-b","old-general"))
        var categories by mutableStateOf(mapOf("inicio-a" to "INICIO","inicio-b" to "INICIO","old-general" to "GENERAL"))
        compose.setContent {MaterialTheme {Column(Modifier.verticalScroll(rememberScrollState())) {
            LegacyInventoryPhotos("SIC-20",paths,categories) {p,c->paths=p;categories=c}
        }}}
        compose.onNodeWithText("FOTOGRAFÍAS").assertIsDisplayed()
        compose.onNodeWithText("INICIO — 2 fotos").assertIsDisplayed()
        compose.onAllNodesWithText("Reemplazar").onFirst().performClick()
        compose.onNodeWithText("Reemplazar fotografía").assertIsDisplayed()
        compose.onNodeWithText("Cancelar").performClick()
        compose.onAllNodesWithText("Eliminar").onFirst().performClick()
        compose.onNodeWithText("Retirar fotografía").assertIsDisplayed()
        compose.runOnIdle {Assert.assertEquals(3,paths.size)}
        compose.onNodeWithText("Retirar").performClick()
        compose.runOnIdle {Assert.assertEquals(listOf("inicio-b","old-general"),paths)}
        compose.onNodeWithText("HISTÓRICAS · GENERAL — 1 fotos").performScrollTo().assertIsDisplayed()
    }
    @Test @Config(qualifiers="w1280dp-h800dp-mdpi")
    fun landscapeShowsVerticalCategoriesAndMultiplePhotoActions() {
        compose.setContent {MaterialTheme {Column(Modifier.verticalScroll(rememberScrollState())) {
            LegacyInventoryPhotos("SIC-22",emptyList(),emptyMap()) {_,_->}
        }}}
        compose.onNodeWithText("PANORÁMICA — 0 fotos").assertIsDisplayed()
        compose.onNodeWithText("ELEMENTO — 0 fotos").performScrollTo().assertIsDisplayed()
        compose.onAllNodesWithText("Tomar foto").assertCountEquals(2)
        compose.onAllNodesWithText("Incorporar fotos").assertCountEquals(2)
    }
}
