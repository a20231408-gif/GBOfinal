package com.tuempresa.inventariovial

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.road.*
import com.tuempresa.inventariovial.scap.catalog.ScapCatalog
import com.tuempresa.inventariovial.scap.data.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ProjectRoutesTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()

    @Test fun fixedMappingAndOfficialAxesMatchAllSixRoutesStartingPe3nAtPallasca() = runBlocking {
        val catalog = ProjectRouteCatalog.load(context)
        assertEquals(mapOf("PE-3N" to ("1" to "Tramo 1"), "PE-22A" to ("1" to "Tramo 2"),
            "PE-28C" to ("1" to "Tramo 3"), "PE-12A" to ("2" to "Tramo 1"),
            "PE-3NG" to ("2" to "Tramo 2"), "PE-28H" to ("2" to "Tramo 3")),
            catalog.routes.associate { it.routeCode to (it.itemCode to it.segmentCode) })
        val reference = RoadReferenceRepository(context).loadFromAssets()
        val counts = mapOf("PE-3N" to 3972, "PE-22A" to 36904, "PE-28C" to 2397,
            "PE-12A" to 11737, "PE-3NG" to 12109, "PE-28H" to 1942)
        assertEquals(catalog.routeCodes.toSet(), reference.segments.map { it.routeCode }.toSet())
        for (axis in reference.segments) {
            assertEquals(counts.getValue(axis.routeCode), axis.points.size)
            assertEquals("EJE", axis.roadbedCode)
            assertTrue(axis.points.zipWithNext().all { (a,b) -> a.chainageM < b.chainageM })
            val p = axis.points[axis.points.size / 2]
            val match = requireNotNull(LinearReferenceEngine(reference).locate(p.latitude, p.longitude, 1.0))
            assertEquals(axis.routeCode, match.routeCode)
            assertEquals(p.chainageM, match.chainageM, 0.1)
            assertNull(match.prCode)
        }
        val start = reference.segments.single { it.routeCode == "PE-3N" }.points.first()
        assertTrue(GeoMath.distanceM(start.latitude, start.longitude, -8.253887590825283, -77.99548975924931) < 2)
        assertTrue(reference.prs.isEmpty())
    }

    @Test fun scapDerivesTramoFromRouteAndRetainsIdsAndHistoricalValuesUntilRouteEdit() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, InventoryDatabase::class.java).build()
        try {
            val catalog = ProjectRouteCatalog.load(context)
            val repo = ScapRepository(db, ScapCatalog.load(context), catalog)
            val id = repo.create("Synthetic", "test", null)
            repo.setField(id, "inspection", "bridgeCode", "CONTRACTUAL-42")
            val before = repo.dao.snapshot(id)!!
            repo.setField(id, "inspection", "route", "PE-3NG", "GNSS_CONFIRMED")
            var s = repo.dao.snapshot(id)!!
            assertEquals("Tramo 2", s.values()["segment"])
            assertEquals("Tramo 2", db.inventoryDao().recordById(s.inspection.roadRecordId)!!.segment)
            assertTrue(runCatching { repo.setField(id, "inspection", "segment", "Tramo 1") }.isFailure)
            repo.dao.putValue(ScapFieldValueEntity(id, "inspection", "segment", "Tramo histórico", "MANUAL"))
            assertEquals("Tramo 2", catalog.segment("PE-3NG", "Tramo histórico"))
            assertEquals("Tramo histórico", repo.dao.snapshot(id)!!.values()["segment"])
            repo.setField(id, "inspection", "route", "PE-28H")
            s = repo.dao.snapshot(id)!!
            assertEquals("Tramo 3", s.values()["segment"])
            assertEquals(before.inspection.id, s.inspection.id)
            assertEquals(before.inspection.roadRecordId, s.inspection.roadRecordId)
            assertEquals("CONTRACTUAL-42", s.inspection.bridgeCode)
        } finally { db.close() }
    }
}
