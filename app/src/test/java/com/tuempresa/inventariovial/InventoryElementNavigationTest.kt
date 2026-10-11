package com.tuempresa.inventariovial

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.tuempresa.inventariovial.field.CulvertSupplementFields
import com.tuempresa.inventariovial.model.form.Sic18FormState
import com.tuempresa.inventariovial.sic.*
import com.tuempresa.inventariovial.ui.theme.InventarioVialTheme
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class) @Config(sdk=[28])
class InventoryElementNavigationTest {
    @get:Rule val compose=createComposeRule()
    @Test fun dashboardHasOneElementEntryForFajasAndAlcantarillasAndNoIndependentSibling() {
        compose.setContent {InventarioVialTheme {HomeScreen(0,0,{})}}
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Fajas de las calzadas"))
        compose.onNodeWithText("Fajas de las calzadas").assertExists()
        compose.onNodeWithText("SIC-08").assertExists();compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Ancho de las fajas"));compose.onNodeWithText("SIC-09").assertExists()
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Alcantarillas"))
        compose.onNodeWithText("Alcantarillas").assertExists()
        compose.onNodeWithText("SIC-18A").assertDoesNotExist()
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Puntos crÃ­ticos"))
        compose.onNodeWithText("Puntos crÃ­ticos").assertExists()
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Accidentes"))
        compose.onNodeWithText("Accidentes").assertExists()
    }
    @Test fun fajasDashboardOpensCharacteristicsDestinationAndKeepsCodesSecondary() {
        var opened:SicFieldFormat?=null
        compose.setContent {InventarioVialTheme {InventoryElementDashboard {opened=it}}}
        compose.onNodeWithText("Fajas de las calzadas").performClick()
        Assert.assertEquals(SicFieldFormat.SIC08,opened)
        compose.onNodeWithText("SIC-08 Â· SIC-09").assertExists()
    }
    @Test fun culvertBadConditionRevealsSupplementInsideSameFormWithoutIndependentCommonInputs() {
        compose.setContent {InventarioVialTheme {
            var state by remember {mutableStateOf(Sic18FormState(structuralConditionCode="2",functionalConditionCode="1"))}
            var expanded by remember {mutableStateOf(false)}
            var values by remember {mutableStateOf(emptyMap<String,String>())}
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Sic18Fields(state,onCompleteSic18A={expanded=true}) {state=it}
                if(expanded) CulvertSupplementFields(values,true) {values=it}
            }
        }}
        compose.onNodeWithText("Completar detalle de condiciÃ³n mala").assertDoesNotExist()
        compose.onAllNodes(hasText("3 Â· MALO",substring=true))[0].performScrollTo().performClick()
        compose.onNodeWithText("Completar detalle de condiciÃ³n mala").performScrollTo().performClick()
        compose.onNodeWithText("Detalle de condiciÃ³n mala").performScrollTo().assertExists()
        compose.onNodeWithText("Falla estructural").performScrollTo().assertExists()
        compose.onNodeWithText("Ruta").assertDoesNotExist();compose.onNodeWithText("Calzada").assertDoesNotExist()
        compose.onNodeWithText("Falla funcional").performScrollTo().assertExists()
    }
    @Test fun culvertTechnicalCriteriaStartCollapsedAndRemainAvailableOnDemand() {
        val criterion=com.tuempresa.inventariovial.catalog.SicCatalogRepository.sic18StructuralDescriptions.entries.first()
        compose.setContent {InventarioVialTheme {Column(Modifier.verticalScroll(rememberScrollState())) {Sic18Fields(Sic18FormState()) {}}}}
        compose.onNodeWithText("${criterion.key} Â· ${criterion.value}").assertDoesNotExist()
        compose.onNodeWithText("Ver criterio estructural").performScrollTo().performClick()
        compose.onNodeWithText("${criterion.key} Â· ${criterion.value}").performScrollTo().assertExists()
    }

}
