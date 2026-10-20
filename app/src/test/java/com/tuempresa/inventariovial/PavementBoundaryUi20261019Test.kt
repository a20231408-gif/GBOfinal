package com.tuempresa.inventariovial

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
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
class PavementBoundaryUi20261019Test {
    @get:Rule val ui=createComposeRule()
    @Test fun shoulderCannotSaveUntilOperatorConfirmsTheActualExteriorAndCancelDoesNotSave() {
        val s=axisSample();val d=visualDamage(27,1,.1,1.0,.1,4.0,1)
        var saved=0;var key="";var canceled=false
        ui.setContent{MaterialTheme{DamageDialog(d,"",{canceled=true},{saved++},sample=s,patchSave={_,k->saved++;key=k})}}
        ui.onNodeWithText("Confirma qué borde exterior",substring=true).performScrollTo().assertExists()
        ui.onNodeWithText("Aceptar").performClick();ui.runOnIdle{assertEquals(0,saved)}
        ui.onNodeWithText("Berma en X=0",substring=true).performScrollTo().performClick()
        ui.onNodeWithText("Proyección válida",substring=true).performScrollTo().assertExists()
        ui.onNodeWithTag("boundary-segment-preview").performScrollTo().assertExists()
        ui.onNodeWithText("Cancelar").performClick();ui.runOnIdle{assertTrue(canceled);assertEquals(0,saved)}
        ui.onNodeWithText("Aceptar").performClick();ui.runOnIdle{assertEquals(1,saved);assertEquals("EXTERIOR_X0",key)}
    }
    @Test fun jointIntersectionShowsBothChoicesAndRequiresConfirmation() {
        val s=axisSample().copy(anchoM=18.0,largoM=18.0,slabRows=3,slabColumns=3)
        val d=visualDamage(25,1,6.1,6.1,6.2,6.2,5);var key=""
        ui.setContent{MaterialTheme{DamageDialog(d,"",{},{},sample=s,patchSave={_,k->key=k})}}
        ui.onNodeWithText("Cruce de juntas ambiguo",substring=true).performScrollTo().assertExists()
        ui.onNodeWithText("Junta izquierda de losa 5").performScrollTo().performClick()
        ui.onNodeWithText("Proyección válida",substring=true).performScrollTo().assertExists()
        ui.onNodeWithText("Aceptar").performClick();ui.runOnIdle{assertEquals("V:1:1",key)}
    }
}
