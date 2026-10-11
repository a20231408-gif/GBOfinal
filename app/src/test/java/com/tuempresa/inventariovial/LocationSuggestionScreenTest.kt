package com.tuempresa.inventariovial

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.tuempresa.inventariovial.field.LocationSuggestionPanel
import com.tuempresa.inventariovial.road.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class LocationSuggestionScreenTest {
    @get:Rule val compose = createComposeRule()
    @Test fun oneButtonConfirmsWholeBlockAndEditKeepsProposalForAudit() {
        var route by mutableStateOf("")
        var pr by mutableStateOf("0042")
        var metres by mutableStateOf("684")
        var routeSource = LocationSource.MANUAL
        var source = LocationSource.MANUAL
        var proposal: LocationSuggestion? = null
        compose.setContent { Column {
            Text("Confirmado: $route / $pr + $metres")
            LocationSuggestionPanel(prSuggestionReference(), MatchConfig(), route, "CD", 0.0, .0053, 1.0, "inicio",
                { if (it != null) proposal = it }, { result ->
                    route = result.suggestedRouteCode!!; pr = result.suggestedPrCode!!; metres = result.pr!!.distanceText
                    routeSource = LocationSource.GNSS_CONFIRMED; source = LocationSource.GNSS_PR_CONFIRMED
                }, { pr = "0015"; metres = "200"; source = LocationSource.MANUAL_OVERRIDE })
        } }
        compose.waitUntil(10_000) { proposal?.suggestedPrCode != null }
        compose.runOnIdle { assertEquals("", route); assertEquals("0042", pr) }
        compose.onNodeWithText("CONFIRMAR UBICACIÓN").performClick()
        compose.runOnIdle {
            assertEquals("R", route); assertEquals("0010", pr); assertEquals("2650", metres)
            assertEquals(LocationSource.GNSS_CONFIRMED, routeSource); assertEquals(LocationSource.GNSS_PR_CONFIRMED, source)
        }
        compose.onNodeWithText("EDITAR").performClick()
        compose.runOnIdle {
            assertEquals("0015", pr); assertEquals("200", metres)
            assertEquals("0010", proposal!!.suggestedPrCode); assertEquals(LocationSource.MANUAL_OVERRIDE, source)
        }
    }
    @Test fun ambiguityRequiresCandidateSelectionAndConfirmation() {
        val axis = testAxis().segments.single()
        val other = axis.copy(routeCode = "OTHER", points = axis.points.map { it.copy(routeCode = "OTHER", latitude = .00002) })
        val reference = testAxis().copy(segments = listOf(axis, other))
        var confirmed: LocationSuggestion? = null
        compose.setContent { LocationSuggestionPanel(reference, MatchConfig(), "", "CD", .00001, .005, 1.0,
            "inicio", {}, { confirmed = it }, {}) }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("R").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("CONFIRMAR UBICACIÓN").assertIsNotEnabled()
        compose.onNodeWithText("R").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Ruta: R").fetchSemanticsNodes().isNotEmpty() }
        assertNull(confirmed)
        compose.onNodeWithText("CONFIRMAR UBICACIÓN").performClick()
        assertEquals("R", confirmed!!.suggestedRouteCode)
        assertTrue(confirmed!!.routeMatchAmbiguous)
    }
}
