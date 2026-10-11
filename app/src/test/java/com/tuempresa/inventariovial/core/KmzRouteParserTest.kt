package com.tuempresa.inventariovial.core

import com.tuempresa.inventariovial.road.KmzRouteParser
import org.junit.Test
import org.junit.Assert.*
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class KmzRouteParserTest {
    private val kml = """<kml xmlns="http://www.opengis.net/kml/2.2"><Document><Placemark><LineString><coordinates>-77.03,-12.04,0 -77.031,-12.041,0 -77.032,-12.042,0</coordinates></LineString></Placemark></Document></kml>"""
    private fun zip(payload: String): ByteArray = ByteArrayOutputStream().also { output ->
        ZipOutputStream(output).use { it.putNextEntry(ZipEntry("doc.kml")); it.write(payload.toByteArray()); it.closeEntry() }
    }.toByteArray()

    @Test fun validKmzHasVersionAndIncreasingMeasuredGeometry() {
        val result = KmzRouteParser.parseKmz(zip(kml), "PE-22A")
        assertEquals("PE-22A", result.routeCode)
        assertEquals(3, result.vertices)
        assertEquals(64, result.sha256.length)
        assertTrue(result.segments.single().points[2].chainageM > result.segments.single().points[1].chainageM)
    }
    @Test fun invalidDoesNotProduceGeometry() {
        assertThrows(Exception::class.java) { KmzRouteParser.parseKmz(zip("not xml"), "PE-22A") }
        assertThrows(Exception::class.java) { KmzRouteParser.parseKmz(byteArrayOf(1,2), "PE-22A") }
        assertThrows(Exception::class.java) { KmzRouteParser.parseKml("<!DOCTYPE foo [ <!ENTITY xx SYSTEM 'file:///etc/passwd'> ]><kml>&xx;</kml>".toByteArray(), "PE-22A") }
    }
}
