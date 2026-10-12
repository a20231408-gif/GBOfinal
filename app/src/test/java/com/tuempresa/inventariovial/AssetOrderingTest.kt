package com.tuempresa.inventariovial

import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.road.*
import org.junit.Assert.*
import org.junit.Test

class AssetOrderingTest {
    private fun asset(id: String, axis: Double?, created: Long, sic: String = "SIC-18", route: String = "PE-22A") =
        InventoryRecordWithPhotos(testRecord(id, created = created, sic = sic, route = route).copy(axisMeasureM = axis), emptyList())
    private fun photo(id: String, record: String, index: Int) = PhotoEntity(id, record, index, "/$id.jpg", index == 1,
        null, null, null, "PENDING", 1)

    @Test fun retroactiveCulvertKeepsIdentityPhotosAndConfirmedV4Number() {
        val captured = listOf(asset("A", 10000.0, 1), asset("B", 15000.0, 2), asset("C", 20000.0, 3),
            asset("D", 17000.0, 4)).map { it.copy(photos = listOf(photo("photo-${it.record.id}", it.record.id, 1))) }
        val before = RoadOrdering.orderAssetsForRoute(captured.take(3))
        val after = RoadOrdering.orderAssetsForRoute(captured)
        assertEquals(listOf("A", "B", "C"), before.map { it.recordId })
        // V4 technical ties use UUID, independently of the geometric display order.
        assertEquals(listOf("A", "B", "C", "D"), after.map { it.recordId })
        assertEquals(listOf("A", "B", "D", "C"), after.sortedBy { it.axisMeasureM }.map { it.recordId })
        assertEquals(listOf(1, 2, 3, 4), after.map { it.sequence })
        assertEquals(listOf("ALC-A", "ALC-B", "ALC-C", "ALC-D"), after.map { it.shortCode })
        val confirmed=before.single {it.recordId=="C"}.copy(remoteLogicalPath="PE-22A/ALCANTARILLAS/ALCANTARILLA 7")
        assertEquals("ALC-7",confirmed.copy(sequence=99).shortCode)
        assertEquals("ALCANTARILLA 7",confirmed.copy(sequence=99).folderName)
        for (original in captured) {
            val actual = after.single { it.recordId == original.record.id }.item
            assertEquals(original, actual)
            assertEquals(original.record.id, actual.photos.single().recordId)
            assertEquals(original.record.createdAt, actual.record.createdAt)
        }
    }

    @Test fun mixedFamiliesUseSeparateCountersAndContractualProgressiveWithoutRenamingIdentity() {
        val records = listOf(asset("p1", 5000.0, 90, "SIC-17"), asset("a1", 6000.0, 50),
            asset("p2", 10000.0, 10, "SIC-17"), asset("a2", 12000.0, 1))
            .mapIndexed { i, item -> item.copy(record = item.record.copy(startPrCode = "${100 - i}")) }
        val sorted = RoadOrdering.orderAssetsForRoute(records.reversed())
        // 99b15aa made numeric PR the technical order; the old test expected axis order.
        assertEquals(listOf("a2", "p2", "a1", "p1"), sorted.map {it.recordId})
        assertEquals(listOf(1,1,2,2),sorted.map {it.sequence})
        assertEquals(listOf("ALC-a2", "P-p2", "ALC-a1", "P-p1"), sorted.map { it.shortCode })
        val changedGeometry=records.map {it.copy(record=it.record.copy(axisMeasureM=0.0,createdAt=999))}
        assertEquals(sorted.map {it.recordId},RoadOrdering.orderAssetsForRoute(changedGeometry).map {it.recordId})
        val inserted = RoadOrdering.orderAssetsForRoute(records + asset("a0", 5500.0, 999))
        assertEquals(sorted.filter { it.assetFamily == AssetFamily.PUENTE }.map { it.shortCode },
            inserted.filter { it.assetFamily == AssetFamily.PUENTE }.map { it.shortCode })
    }

    @Test fun fallbackIsExplicitAndDoesNotMixContractualAndGeometricOrigins() {
        val axis = asset("axis", 900000.0, 90)
        val legacy = asset("legacy", null, 50)
        val unknown = asset("unknown", null, 1).let { it.copy(record = it.record.copy(startPrCode = "?")) }
        val nan = asset("nan", Double.NaN, 2).let { it.copy(record = it.record.copy(startPrCode = "?")) }
        val rows = RoadOrdering.orderAssetsForRoute(listOf(unknown, legacy, axis, nan))
        assertEquals(listOf("axis", "legacy", "nan", "unknown"), rows.map { it.recordId })
        assertEquals(listOf(AssetOrderSource.GNSS_MAP_MATCH, AssetOrderSource.MANUAL_CHAINAGE_FALLBACK,
            AssetOrderSource.CREATED_AT_FALLBACK, AssetOrderSource.CREATED_AT_FALLBACK), rows.map { it.orderSource })
        assertNull(rows[1].axisMeasureM)
        assertNull(rows[2].axisMeasureM)
        assertEquals(rows,RoadOrdering.orderAssetsForRoute(listOf(nan,axis,legacy,unknown)))
        listOf(axis,legacy,unknown,nan).forEach {original->assertEquals(original,rows.single {it.recordId==original.record.id}.item)}
    }

    @Test fun tiesAreDeterministicAndV4RouteFamilyCountersDoNotDefineRemoteNumbers() {
        val a = asset("a", 10.0, 1)
        val b = asset("b", 10.0, 1).let { it.copy(record = it.record.copy(roadbedCode = "UC")) }
        val r = asset("r", 10.0, 1, route = "PE-3N")
        val rows = RoadOrdering.orderAssetsForRoute(listOf(r, b, a))
        assertEquals(listOf("a", "b", "r"), rows.map { it.recordId })
        assertEquals(listOf(1,2,1),rows.map {it.sequence})
        assertEquals(listOf("ALC-a", "ALC-b", "ALC-r"), rows.map { it.shortCode })
        assertTrue(rows.all {it.sibCode==null})
        val sameFamilyDifferentSib = listOf(asset("x", 1.0, 1, "SIC-21"), asset("y", 2.0, 2, "SIC-20"))
            .map { it.copy(record = it.record.copy(assetType = "MURO")) }
        val sameFamily=RoadOrdering.orderAssetsForRoute(sameFamilyDifferentSib)
        assertEquals(listOf(1,2),sameFamily.map {it.sequence})
        assertEquals(listOf("MUR-x", "MUR-y"),sameFamily.map {it.shortCode})
        assertEquals(rows,RoadOrdering.orderAssetsForRoute(listOf(a,r,b)))
        assertEquals(1, RoadOrdering.orderAssetsForRoute(listOf(a, b.copy(record = b.record.copy(status = "ANNULLED")))) .size)
    }

    @Test fun folderPlanUsesStableV4UuidAndRealPhotoIndexWithoutMutatingAnything() {
        val original = asset("id", 1.0, 1, route = "PE-3N").copy(photos = listOf(photo("second", "id", 9), photo("first", "id", 2)))
        val plan = DrivePathPlanner.plan(listOf(original))
        assertTrue(plan.warnings.isEmpty())
        val photos = plan.folders.single().photos
        assertEquals(listOf("first", "second"), photos.map { it.photoId })
        assertEquals(listOf("PE-3N/ALCANTARILLAS/ALCANTARILLA UUID_id/GENERAL/001_F02.jpg", "PE-3N/ALCANTARILLAS/ALCANTARILLA UUID_id/GENERAL/002_F09.jpg"), photos.map { it.plannedPath })
        assertEquals(listOf(2,9),photos.map {it.photoIndex})
        assertEquals(listOf("id","id"),photos.map {it.recordId})
        assertEquals(listOf("second", "first"), original.photos.map { it.id })
        val next = DrivePathPlanner.plan(listOf(original, asset("earlier", 0.0, 50, route = "PE-3N")))
        val retained=next.folders.single {it.asset.recordId=="id"}
        assertEquals(plan.folders.single().plannedPath,retained.plannedPath)
        assertEquals(photos,retained.photos)
        assertFalse(retained.plannedPath!!.contains("SIB-"))
    }

    @Test fun catalogUsesV4DetailFamiliesAndReportsUnknownsWithoutInventingSib() {
        val drainage = asset("d", 1.0, 1, "SIC-19")
        for ((code, abbreviation) in mapOf("08" to "CUN", "09" to "CAN", "10" to "BAJ", "11" to "ZD", "12" to "ZC", "13" to "CB")) {
            val row = drainage.copy(sic19 = Sic19Entity("d", code, "1", "1", "1", "1"))
            assertEquals(abbreviation, AssetPresentationCatalog.resolve(row).abbreviation)
        }
        for ((code, abbreviation) in mapOf("12" to "BAD", "13" to "TUN", "14" to "MUR")) {
            val row = asset("s", 1.0, 1, "SIC-20").copy(sic20 = Sic20Entity("s", code, "1", 1.0, 1.0, "1", "1"))
            assertEquals(abbreviation, AssetPresentationCatalog.resolve(row).abbreviation)
        }
        val unknown = asset("u", 1.0, 1, "UNKNOWN")
        val plan = DrivePathPlanner.plan(listOf(unknown))
        assertEquals("ELM-u", plan.folders.single().asset.shortCode)
        assertEquals("PE-22A/OTROS/ELEMENTO UUID_u",plan.folders.single().plannedPath)
        assertNull(plan.folders.single().asset.sibCode)
        assertFalse(plan.warnings.any { it.contains("SIB") })
        assertTrue(plan.warnings.any { it.contains("ELM") })
        assertEquals("SV", AssetPresentationCatalog.resolve(asset("v", 1.0, 1, "SIC-22")).abbreviation)
        assertEquals("SH", AssetPresentationCatalog.resolve(asset("h", 1.0, 1, "SIC-21").let { it.copy(record = it.record.copy(assetType = "HORIZONTAL_MARKS")) }).abbreviation)
        assertEquals("DV", AssetPresentationCatalog.resolve(asset("v", 1.0, 1, "SIC-23").copy(sic23 = Sic23Entity("v", "21", "1", 3.0, ""))).abbreviation)
    }

    @Test fun selectedRouteIsMatchedBeforeNearerRoadAndContractualDataIsUntouched() {
        val reference = testAxis()
        val nearby = reference.segments.single().copy(routeCode = "OTHER", points = reference.segments.single().points.map { it.copy(routeCode = "OTHER", latitude = 0.00005) })
        val data = reference.copy(segments = reference.segments + nearby)
        val record = testRecord().copy(latitude = 0.00005, startPrCode = "9999", startDistanceM = 777.0)
        val matched = RoadAxisPositioner(data).position(record)
        assertEquals("R", matched.routeCode)
        assertEquals(1500.0, matched.axisMeasureM!!, 0.01)
        assertEquals(5.56, matched.distanceToRoadAxisM!!, 0.05)
        assertEquals(record, matched.copy(axisMeasureM = null, distanceToRoadAxisM = null, roadMatchConfidence = null,
            matchedSegmentId = null, projectedLatitude = null, projectedLongitude = null))
        assertNull(RoadAxisPositioner(data).position(record.copy(routeCode = "MISSING")).axisMeasureM)
        assertNull(RoadAxisPositioner(data).position(record.copy(routeCode = "", latitude = 0.000025, gpsAccuracyM = 5.0)).axisMeasureM)
        val global = RoadAxisPositioner(reference).position(record.copy(routeCode = ""))
        assertNotNull(global.axisMeasureM)
        assertEquals("", global.routeCode)
    }

    @Test fun invalidFixOrDistantPointClearsDerivedValuesButRetainsRecord() {
        val positioner = RoadAxisPositioner(testAxis())
        for (bad in listOf(testRecord().copy(gpsAccuracyM = null), testRecord().copy(gpsAccuracyM = 100.0),
            testRecord().copy(latitude = 80.0), testRecord().copy(longitude = Double.NaN))) {
            val result = positioner.position(bad.copy(axisMeasureM = 123.0))
            assertNull(result.axisMeasureM)
            assertEquals(bad, result)
        }
    }
}
