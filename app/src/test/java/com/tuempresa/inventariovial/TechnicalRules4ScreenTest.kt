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
import com.tuempresa.inventariovial.field.*
import com.tuempresa.inventariovial.model.form.Sic18FormState
import com.tuempresa.inventariovial.sic.*
import com.tuempresa.inventariovial.validation.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w800dp-h1600dp-mdpi",application=Application::class) @GraphicsMode(GraphicsMode.Mode.NATIVE)
class TechnicalRules4ScreenTest {
    @get:Rule val compose=createAndroidComposeRule<androidx.activity.ComponentActivity>()
    @Test fun functionIsAlwaysBeforeConditionAndRemainsEditable(){
        var values by mutableStateOf(mapOf("functionCode" to "3"))
        var base by mutableStateOf(Sic18FormState(spans="1",dimension1M="1",dimension2M="1"))
        compose.setContent {MaterialTheme {Column(Modifier.verticalScroll(rememberScrollState())) {
            Sic18Fields(base,beforeConditions={CulvertFunctionField(values) {values=it}}) {base=it}
        }}}
        compose.onNodeWithText("Función").assertExists()
        val function=compose.onNodeWithText("Función").fetchSemanticsNode().boundsInRoot.top
        val condition=compose.onNodeWithText("CONDICIÓN ESTRUCTURAL").fetchSemanticsNode().boundsInRoot.top
        assertTrue(function<condition)
        compose.runOnIdle {base=base.copy(structuralConditionCode="2")}
        compose.onNodeWithText("Función").assertExists();compose.onNodeWithText("Completar fallas SIC-18A").assertDoesNotExist()
        compose.runOnIdle {assertEquals("3",values["functionCode"])}
    }
    @Test fun failureFieldsOfCompletedSupplementRemainEnabled(){
        var values by mutableStateOf(mapOf("failureLocationCode" to "1","failureTypeCode" to "2","functionalStateCode" to "2","probableCauseCode" to "3"))
        compose.setContent {MaterialTheme {CulvertSupplementFields(values,true) {values=it}}}
        compose.onNodeWithText("Falla estructural: tipo").assertExists()
        compose.onNodeWithText("Falla funcional: causa probable").assertExists()
        compose.onNodeWithText("2 · Deformación").assertIsEnabled()
    }
    @Test fun lengthUpdatesWhenFullProgressiveChangesAndDoesNotInventMissingValues(){
        var offset by mutableStateOf("200")
        compose.setContent {MaterialTheme {TechnicalLengthField(ContractualLength.calculate(
            LengthEndpoint("R","CD","0010","100"),LengthEndpoint("R","CD","0010",offset),emptyList(),"INCREASING"))}}
        compose.onNodeWithText("Longitud calculada (m): 100.00").assertExists()
        compose.runOnIdle {offset="250,25"}
        compose.onNodeWithText("Longitud calculada (m): 150.25").assertExists()
        compose.runOnIdle {offset=""}
        compose.onNodeWithText("Longitud calculada (m): Pendiente").assertExists()
    }
}
