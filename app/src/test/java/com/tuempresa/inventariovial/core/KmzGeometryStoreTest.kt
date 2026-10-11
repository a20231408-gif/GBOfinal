package com.tuempresa.inventariovial.core

import androidx.test.core.app.ApplicationProvider
import android.content.Context
import com.tuempresa.inventariovial.road.KmzGeometryStore
import com.tuempresa.inventariovial.road.KmzRouteParser
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk=[34])
class KmzGeometryStoreTest {
    @Test fun validVersionIsPreservedAndInvalidPreviewCannotReplaceIt() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = KmzGeometryStore(context)
        val route = "PE-22A"
        val valid = """<kml xmlns="http://www.opengis.net/kml/2.2"><Document><Placemark><LineString><coordinates>-77.03,-12.04 -77.031,-12.041</coordinates></LineString></Placemark></Document></kml>"""
        val prepared = KmzRouteParser.parseKml(valid.toByteArray(), route, "survey.kml")
        store.activate(prepared)
        assertEquals(prepared.sha256, store.active(route)?.sha256)
        assertTrue(store.versions(route).contains(prepared.sha256))
        assertThrows(Exception::class.java) { KmzRouteParser.parseKml("not KML".toByteArray(),route) }
        assertEquals(prepared.sha256, store.active(route)?.sha256)
    }
}
