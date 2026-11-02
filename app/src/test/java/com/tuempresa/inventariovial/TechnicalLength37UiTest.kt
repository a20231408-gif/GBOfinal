package com.tuempresa.inventariovial

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.tuempresa.inventariovial.field.TechnicalLengthField
import com.tuempresa.inventariovial.validation.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*

@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],application=Application::class) @GraphicsMode(GraphicsMode.Mode.NATIVE)
class TechnicalLength37UiTest {
    @get:Rule val ui=createComposeRule()
    private val start=LengthEndpoint("PE-12A","CD","42","995")
    private val end=start.copy(pr="43",distance="5")
    @Test fun knownKilometreReferenceImmediatelyUsesTenMetresWithoutContractualErrorOrExtraTap() {
        val contractual=ContractualLength.calculate(start,end,emptyList(),"INCREASING")
        ui.setContent{MaterialTheme{Column{TechnicalLengthField(contractual,kilometreStart=start,kilometreEnd=end,lengthReference="EXPLICIT_KILOMETRES",onReferenceChange={})}}}
        ui.onNodeWithText("Longitud calculada (m): 10.00").assertExists()
        ui.onNodeWithText(contractual.warning!!).assertDoesNotExist()
    }
    @Test fun unknownProvenanceShowsDifferenceImmediatelyButDoesNotInventContractualEquivalence() {
        val contractual=ContractualLength.calculate(start,end,emptyList(),"INCREASING")
        ui.setContent{MaterialTheme{Column{TechnicalLengthField(contractual,kilometreStart=start,kilometreEnd=end,onReferenceChange={})}}}
        ui.onNodeWithText("Diferencia kilométrica auxiliar (m): 10.00").assertExists()
        ui.onNodeWithText(contractual.warning!!).assertExists()
        ui.onNodeWithText("Usar longitud calculada").assertDoesNotExist()
    }
}
