package com.tuempresa.inventariovial

import com.tuempresa.inventariovial.camera.PhotoCaptureMetadata
import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.delivery.*
import com.tuempresa.inventariovial.location.GeoLocation
import com.tuempresa.inventariovial.road.*
import org.junit.Assert.*
import org.junit.Test

class DeliveryMetadataTest {
    private fun photo(id: String, record: String, index: Int) = PhotoEntity(id, record, index, "/data/private/$id.jpg", index == 1, null, null, null, "PENDING", 1)
    private fun item(id: String, axis: Double, created: Long, photos: List<PhotoEntity>) = InventoryRecordWithPhotos(
        testRecord(id, created = created, route = "PE-22A", sic = "SIC-18").copy(axisMeasureM = axis), photos)
    @Test fun routeSequenceUsesPhysicalOrderAndLocalIndicesWhileLateElementRetainsIdentities() {
        val a = item("A", 10000.0, 1, listOf(photo("p1", "A", 1), photo("p2", "A", 2)))
        val b = item("B", 15000.0, 2, listOf(photo("p3", "B", 1)))
        val c = item("C", 20000.0, 3, listOf(photo("p4", "C", 1)))
        val d = item("D", 17000.0, 4, listOf(photo("p5", "D", 1)))
        val plan = DrivePathPlanner.plan(listOf(c, b, d, a))
        assertEquals(listOf("A", "B", "C", "D"), plan.folders.map { it.asset.recordId })
        assertEquals(listOf("ALC-A", "ALC-B", "ALC-C", "ALC-D"), plan.folders.map { it.asset.shortCode })
        assertEquals(listOf("PE-22A/ALCANTARILLAS/ALCANTARILLA UUID_A/GENERAL/001_F01.jpg", "PE-22A/ALCANTARILLAS/ALCANTARILLA UUID_A/GENERAL/002_F02.jpg",
            "PE-22A/ALCANTARILLAS/ALCANTARILLA UUID_B/GENERAL/003_F01.jpg"), plan.folders.take(2).flatMap { it.photos }.map { it.plannedPath })
        assertEquals(listOf(1, 2, 3, 5, 4), plan.folders.flatMap { it.photos }.map { it.deliveryPhotoSequence })
        assertEquals("C", c.photos.single().recordId); assertEquals(1, c.photos.single().photoIndex)
        assertEquals(3L, c.record.createdAt)
    }
    @Test fun legacyNamesRemainCompleteAndWarnAbove20Characters() {
        val row = item("A", 1.0, 1, listOf(photo("p", "A", 1))).let { it.copy(record = it.record.copy(sideCode = "D", startPrCode = "0040", startDistanceM = 684.0)) }
        val compact = DrivePathPlanner.plan(listOf(row)); assertTrue(compact.warnings.isEmpty())
        val legacy = DrivePathPlanner.plan(listOf(row), policy = DeliveryNamingPolicy.LEGACY_COMPATIBLE)
        val name = legacy.folders.single().photos.single().photoName
        assertEquals("1_PE-22A_ALC_D_0040+684.jpg", name)
        assertNotNull(FileNameComplianceValidator.warning(name)); assertTrue(legacy.warnings.any { it.contains("20") })
        assertFalse(FileNameComplianceValidator.safeComponent("../ruta"))
        assertFalse(FileNameComplianceValidator.safeComponent("ruta\\otra"))
    }
    @Test fun separatePhotoFixesAndAuditedMetadataNeverCopyRecordLocationOrPrivatePaths() {
        val fix1 = GeoLocation(-12.1, -77.1, .8f, 100.0, timestamp = 1000)
        val fix2 = GeoLocation(-12.2, -77.2, 1.1f, 101.0, timestamp = 2000)
        val photos = listOf(PhotoCaptureMetadata(1000, fix1).applyTo(photo("p1", "A", 1)),
            PhotoCaptureMetadata(2000, fix2).applyTo(photo("p2", "A", 2)), photo("p3", "A", 3))
        val row = item("A", 12650.0, 1, photos).let { it.copy(record = it.record.copy(startPrCode = "0015", startDistanceM = 200.0,
            suggestedRouteCode = "PE-22A", suggestedPrCode = "0010", suggestedDistanceFromPrM = 2650.0, locationSource = LocationSource.MANUAL_OVERRIDE)) }
        val folder = DrivePathPlanner.plan(listOf(row)).folders.single()
        val meta = DeliveryMetadata.element(folder, folder.photos.mapIndexed { i, plan -> DeliveryMetadata.photo(photos[i], plan, "a".repeat(64)) })
        assertEquals("0015", meta.getJSONObject("location").getString("confirmedPr"))
        assertEquals("0010", meta.getJSONObject("suggestion").getString("suggestedPr"))
        assertEquals(12650.0, meta.getJSONObject("geometricLocation").getDouble("axisMeasureM"), 0.0)
        val ps = meta.getJSONArray("photos")
        assertEquals(-12.1, ps.getJSONObject(0).getDouble("latitude"), 0.0)
        assertEquals(-12.2, ps.getJSONObject(1).getDouble("latitude"), 0.0)
        assertTrue(ps.getJSONObject(2).isNull("latitude"))
        assertEquals("UNAVAILABLE_LEGACY", ps.getJSONObject(2).getString("photoLocationSource"))
        assertEquals("a".repeat(64), ps.getJSONObject(1).getString("sha256"))
        for (forbidden in listOf("/data/", "localPath", "originalPath", "stampedPath", "token", "password")) assertFalse(meta.toString().contains(forbidden))
    }
    @Test fun staleFixIsUnavailableWithoutBlockingThePhoto() {
        val stale = GeoLocation(0.0, 0.0, 1f, timestamp = 1)
        val metadata = PhotoCaptureMetadata(100_000, PhotoCaptureMetadata.validFix(stale, 100_000))
        val captured = metadata.applyTo(photo("p", "r", 1))
        assertEquals("p", captured.id); assertNull(captured.photoLatitude)
        assertEquals("UNAVAILABLE_AT_CAPTURE", captured.photoLocationSource)
    }
}
