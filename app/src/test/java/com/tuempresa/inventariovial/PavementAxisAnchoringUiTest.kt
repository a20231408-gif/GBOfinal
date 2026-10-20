package com.tuempresa.inventariovial

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.tuempresa.inventariovial.pavement.ui.DamageDialog
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=Application::class,qualifiers="w1280dp-h800dp-land")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PavementAxisAnchoringUiTest {
    @get:Rule val ui=createComposeRule()
    @Test fun oversizedAnchorShowsReasonInsteadOfCrashingOrSilentlyShrinkingAndInteriorRemainsSelectable() {
        val s=axisSample().copy(anchoM=12.0,slabColumns=2)
        val d=visualDamage(29,1,.5,1.0,7.5,2.0,1).copy(correlativo=0,slabs="1,2")
        ui.setContent{MaterialTheme{DamageDialog(d,"",{},{},sample=s,patchSave={_,_->})}}
        ui.onNodeWithText("El parche no cabe",substring=true).performScrollTo().assertExists()
        ui.onAllNodesWithTag("patch-projection-preview").assertCountEquals(0)
        ui.onNodeWithText("Interior: conservar ubicación").performScrollTo().performClick()
        ui.onNodeWithTag("patch-projection-preview").performScrollTo().assertExists()
        ui.onAllNodesWithText("El parche no cabe",substring=true).assertCountEquals(0)
    }
}
