package com.tuempresa.inventariovial

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.road.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class LocationSuggestionTest {
    @Test fun globalRealSixRouteReferenceSuggestsPe22WithoutContractualMutation() = runBlocking {
        val data = RoadReferenceRepository(ApplicationProvider.getApplicationContext<Context>()).loadFromAssets()
        val point = data.segments.single { it.routeCode == "PE-22A" }.points[1700]
        val manual = testRecord().copy(routeCode = "", startPrCode = "0042", startDistanceM = 684.0,
            latitude = point.latitude, longitude = point.longitude)
        val result = LocationSuggestionEngine(data).suggest("", "CD", point.latitude, point.longitude, .8)
        assertEquals("PE-22A", result.suggestedRouteCode)
        assertFalse(result.routeMatchAmbiguous)
        assertNull(result.suggestedPrCode)
        assertTrue(result.message!!.contains("falta catálogo"))
        val audited = manual.withSuggestions(result)
        assertEquals("", audited.routeCode)
        assertEquals("0042", audited.startPrCode)
        assertEquals(684.0, audited.startDistanceM, 0.0)
        assertEquals("PE-22A", audited.suggestedRouteCode)
    }
    @Test fun closeRoutesRemainAmbiguousAndGnssAndManualValuesSurvive() {
        val axis = testAxis().segments.single()
        val other = axis.copy(routeCode = "OTHER", points = axis.points.map { it.copy(routeCode = "OTHER", latitude = .00002) })
        val result = LocationSuggestionEngine(testAxis().copy(segments = listOf(axis, other))).suggest("", "CD", .00001, .005, 1.0)
        assertTrue(result.routeMatchAmbiguous)
        assertNull(result.suggestedRouteCode)
        assertEquals(setOf("R", "OTHER"), result.candidates.map { it.routeCode }.toSet())
        val manual = testRecord().copy(latitude = .00001, routeCode = "")
        val saved = manual.withSuggestions(result)
        assertEquals(requireNotNull(manual.latitude), requireNotNull(saved.latitude), 0.0)
        assertTrue(saved.routeMatchAmbiguous)
        assertEquals("", saved.routeCode)
    }
    @Test fun confirmedRouteIsPrimaryAndStrongDiscrepancyDoesNotReplaceIt() {
        val axis = testAxis().segments.single()
        val other = axis.copy(routeCode = "OTHER", points = axis.points.map { it.copy(routeCode = "OTHER", latitude = .01) })
        val engine = LocationSuggestionEngine(testAxis().copy(segments = listOf(axis, other)))
        val result = engine.suggest("R", "CD", .01, .005, 1.0)
        assertEquals("OTHER", result.discrepancyRoute)
        assertNull(result.match)
        assertEquals("R", testRecord().withSuggestions(result).routeCode)
    }
    @Test fun prProposalAndManualOverrideAreStoredSeparatelyAndEndIsIndependent() {
        val engine = LocationSuggestionEngine(prSuggestionReference())
        val start = engine.suggest("R", "CD", 0.0, .0053, 1.0)
        val end = engine.suggest("R", "CD", 0.0, .012, 1.0)
        val manual = testRecord().copy(startPrCode = "0015", startDistanceM = 200.0,
            locationSource = LocationSource.edited(LocationSource.MANUAL, true)).withSuggestions(start, end)
        assertEquals("0015", manual.startPrCode)
        assertEquals(200.0, manual.startDistanceM, 0.0)
        assertEquals("0010", manual.suggestedPrCode)
        assertEquals(2650.0, manual.suggestedDistanceFromPrM!!, .001)
        assertEquals("0015", manual.endSuggestedPrCode)
        assertEquals(1000.0, manual.endSuggestedDistanceFromPrM!!, .001)
        assertEquals(LocationSource.MANUAL_OVERRIDE, manual.locationSource)
        assertEquals(manual, manual.withSuggestions(null, null))
        val resumed = testRecord().withSavedSuggestionAudit(manual).withSuggestions(null, null)
        assertEquals(manual.suggestedPrCode, resumed.suggestedPrCode)
        assertEquals(manual.suggestedDistanceFromPrM, resumed.suggestedDistanceFromPrM)
        assertEquals(manual.endSuggestedPrCode, resumed.endSuggestedPrCode)
    }
    @Test fun invalidGnssCannotInventRouteOrPr() {
        val engine = LocationSuggestionEngine(prSuggestionReference())
        for (accuracy in listOf(null, -1.0, Double.NaN, 100.0)) {
            val result = engine.suggest("", "CD", 0.0, .0053, accuracy)
            assertNull(result.suggestedRouteCode); assertNull(result.suggestedPrCode)
        }
    }
}
