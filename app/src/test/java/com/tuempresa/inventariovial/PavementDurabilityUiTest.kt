package com.tuempresa.inventariovial

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.tuempresa.inventariovial.pavement.*
import com.tuempresa.inventariovial.pavement.ui.DamageDialog
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=Application::class,qualifiers="w1280dp-h800dp-land")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PavementDurabilityUiTest {
    @get:Rule val ui=createComposeRule()
    @Test fun ambiguousCornerShowsCorrectablePreviewAndExistingFieldsRetainNormalSave() {
        val s=axisSample();val d=visualDamage(24,1,2.0,2.0,3.0,3.0,1)
        var saved:PavementDamageEntity?=null
        ui.setContent{MaterialTheme{DamageDialog(d,"",{},{saved=it},sample=s)}}
        ui.onNodeWithText("Esquina D24 no determinada",substring=true).performScrollTo().assertExists()
        ui.onNodeWithTag("d24-corner-preview").performScrollTo().assertExists()
        for(label in listOf("X1 / centro X (m)","Y1 / centro Y (m)"))
            ui.onNode(hasText(label) and hasSetTextAction()).performScrollTo().performTextReplacement("0.1")
        ui.onNodeWithText("Abanico referencial ajustado",substring=true).performScrollTo().assertExists()
        ui.onAllNodesWithText("Esquina D24 no determinada",substring=true).assertCountEquals(0)
        ui.onNodeWithText("Aceptar").performClick()
        ui.runOnIdle{assertNotNull(saved);assertEquals(d.id,saved!!.id);assertEquals(.1,saved!!.x1!!,0.0);assertEquals(3.0,saved!!.x2!!,0.0)}
    }
}
