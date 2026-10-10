package com.tuempresa.inventariovial

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.tuempresa.inventariovial.pavement.*
import com.tuempresa.inventariovial.pavement.ui.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class) @Config(sdk=[28])
class PavementParallelCUiTest {
    @get:Rule val compose=createComposeRule()
    private fun sample()=PavementSampleEntity("s","T","R",1,"Derecho","UC","00+000","00+040",5.0,40.0,"ASPHALT","2026-10-06","u","Inspector")
    @Test fun lanesAndRoadbedsAreDropdownsWithExistingCodes() {
        compose.setContent { MaterialTheme { SampleDialog(sample(),"",{}, {}) } }
        compose.onNodeWithText("Carril: Derecho").performScrollTo().performClick()
        compose.onNodeWithText("Izquierdo").performClick()
        compose.onNodeWithText("Carril: Izquierdo").assertExists()
        compose.onNodeWithText("Calzada: UC").performClick()
        compose.onNodeWithText("CD").performClick()
        compose.onNodeWithText("Calzada: CD").assertExists()
        assertEquals(0,compose.onAllNodes(hasSetTextAction() and hasText("Carril",substring=true)).fetchSemanticsNodes().size)
    }
    @Test fun fitButtonRestoresCoordinatesAfterZoomWithoutMovingMeasuredDamage() {
        val points=mutableListOf<Pair<Double,Double>>()
        compose.setContent { Box(Modifier.width(400.dp).height(420.dp)) { PavementScheme(sample(),emptyList(),emptySet(),null,true) { x,y -> points+=x to y } } }
        val scheme=compose.onNodeWithTag("pavement-scheme")
        fun tap() { scheme.performTouchInput { click(Offset(64*compose.density.density+10,height/2f)) } }
        tap();scheme.performTouchInput { pinch(Offset(width*.3f,height*.45f),Offset(width*.5f,height*.55f),Offset(width*.2f,height*.3f),Offset(width*.7f,height*.7f),500) }
        compose.onNodeWithText("Ajustar").performClick();tap()
        compose.runOnIdle { assertEquals(2,points.size);assertEquals(points[0].first,points[1].first,1e-5);assertEquals(points[0].second,points[1].second,1e-5) }
    }
}
