package com.tuempresa.inventariovial

import android.Manifest
import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.field.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w800dp-h1200dp-mdpi",application=Application::class) @GraphicsMode(GraphicsMode.Mode.NATIVE)
class ContractualFields3ScreenTest {
    @get:Rule val compose=createAndroidComposeRule<androidx.activity.ComponentActivity>()
    @Test fun fifthPrDigitAndInvalidPasteShowImmediateErrorAndKeepZeros(){
        var value by mutableStateOf("0010")
        compose.setContent{MaterialTheme{ContractualPrField(value,{value=it},label={Text("PR")})}}
        compose.onNode(hasSetTextAction()).performTextReplacement("00010")
        compose.onNode(hasSetTextAction()).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
        compose.onNodeWithText("PR: solo dígitos, máximo 4. Entrada rechazada.").assertExists()
        compose.runOnIdle{assertEquals("0010",value)}
        compose.onNode(hasSetTextAction()).performTextReplacement("0011");compose.runOnIdle{assertEquals("0011",value)}
    }
    @Test fun fourthIntegerDistanceRejectedAndDecimalPrecisionPreserved(){
        var value by mutableStateOf("125.500000")
        compose.setContent{MaterialTheme{PrDistanceField(value,{value=it},label={Text("Distancia")})}}
        compose.onNode(hasSetTextAction()).performTextReplacement("1234.50")
        compose.onNode(hasSetTextAction()).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
        compose.runOnIdle{assertEquals("125.500000",value)}
        compose.onNode(hasSetTextAction()).performTextReplacement("999,750000");compose.runOnIdle{assertEquals("999,750000",value)}
    }
    @Test fun historicalInvalidValueDisplayedWithoutSilentTruncation(){
        var value by mutableStateOf("12345")
        compose.setContent{MaterialTheme{ContractualPrField(value,{value=it},label={Text("PR histórico")})}}
        compose.onNode(hasSetTextAction()).assertTextContains("12345")
        compose.onNode(hasSetTextAction()).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
        compose.runOnIdle{assertEquals("12345",value)}
    }
    @Test fun deniedPermissionGpsCollapsedOptionalManualAndClear(){
        val app=ApplicationProvider.getApplicationContext<Application>();shadowOf(app).denyPermissions(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION)
        var fix by mutableStateOf<com.tuempresa.inventariovial.location.GeoLocation?>(null)
        var error:String?=null
        compose.setContent{MaterialTheme{OptionalDeviceGps(fix,onChange={fix=it},onError={error=it})}}
        compose.onNodeWithText("Latitud").assertDoesNotExist();compose.runOnIdle{assertNull(fix)}
        compose.onNodeWithText("GPS del dispositivo (opcional)").performClick()
        compose.onNode(hasSetTextAction() and hasText("Latitud")).performTextReplacement("-12.5")
        compose.runOnIdle{assertNotNull(error);assertNull(fix)}
        compose.onNode(hasSetTextAction() and hasText("Longitud")).performTextReplacement("-76.5")
        compose.runOnIdle{assertNull(error);assertEquals("MANUAL_COORDINATES",fix!!.source);assertFalse(fix!!.isRtkFixed)}
        compose.onNodeWithText("Dejar sin coordenadas").performClick();compose.runOnIdle{assertNull(fix);assertNull(error)}
    }
}
