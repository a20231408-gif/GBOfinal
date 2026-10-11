package com.tuempresa.inventariovial

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.tuempresa.inventariovial.field.TrafficSignImage
import com.tuempresa.inventariovial.field.TrafficSignVisualSelector
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class) @Config(sdk=[28])
class TrafficSignVisualSelectorTest {
    @get:Rule val compose=createComposeRule()
    @Test fun searchPareShowsR1CodeNameAndCorrespondingAccessibleImage() {
        var selected=""
        compose.setContent {MaterialTheme {var code by remember {mutableStateOf("")}
            Column {TrafficSignVisualSelector("1",code) {code=it;selected=it}}
        }}
        compose.onNodeWithText("Buscar señal por código o nombre").performClick()
        compose.onNodeWithText("Buscar señal…").performTextInput("PARE")
        compose.onNodeWithTag("signal-option-R-1").assertExists().performClick()
        assertEquals("R-1",selected)
        compose.onNodeWithText("R-1").assertExists()
        compose.waitUntil(10000) {compose.onAllNodesWithContentDescription("Código R-1, PARE").fetchSemanticsNodes().isNotEmpty()}
    }
    @Test fun selectingP1ChangesPreviewAndSearchResultsAreLazy() {
        val names=TrafficSignCatalog.signsForType("2").associate {it.code to it.name}
        compose.setContent {MaterialTheme {var code by remember {mutableStateOf("P-1A")}
            Column {TrafficSignVisualSelector("2",code) {code=it}}
        }}
        compose.waitUntil(10000) {compose.onAllNodesWithContentDescription("Código P-1A, ${names.getValue("P-1A")}").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("Buscar señal por código o nombre").performClick()
        compose.onNodeWithText("Buscar señal…").performTextInput("P-1")
        compose.onNodeWithTag("signal-option-P-1B").performClick()
        compose.waitUntil(10000) {compose.onAllNodesWithContentDescription("Código P-1B, ${names.getValue("P-1B")}").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithTag("signal-image-P-1A").assertDoesNotExist()
        compose.onNodeWithText("Buscar señal por código o nombre").performClick()
        compose.onNodeWithText("Buscar señal…").performTextClearance()
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(TrafficSignCatalog.signsForType("2").size-1)
        compose.onNodeWithTag("signal-option-${TrafficSignCatalog.signsForType("2").last().code}").assertIsDisplayed()
    }
    @Test fun manualAndUnidentifiedReplacePreviousImageAndI2aKeepsRealAsset() {
        val source=mutableStateOf("CATALOG")
        compose.setContent {MaterialTheme {Column {TrafficSignImage("R-1","PARE",source.value)}}}
        compose.waitUntil(10000) {compose.onAllNodesWithContentDescription("Código R-1, PARE").fetchSemanticsNodes().isNotEmpty()}
        compose.runOnIdle {source.value="MANUAL"}
        compose.onNodeWithText("Imagen no disponible").assertExists()
        compose.onNodeWithContentDescription("Código R-1, PARE").assertDoesNotExist()
        compose.runOnIdle {source.value="UNIDENTIFIABLE"}
        compose.onNodeWithText("Sin código identificable").assertExists()
        compose.onNodeWithContentDescription("Código R-1, PARE").assertDoesNotExist()
    }
}
