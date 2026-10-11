package com.tuempresa.inventariovial

import android.content.Context
import android.graphics.Bitmap
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.road.*
import com.tuempresa.inventariovial.scap.catalog.ScapCatalog
import com.tuempresa.inventariovial.scap.data.*
import com.tuempresa.inventariovial.scap.export.*
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.util.UUID
import java.util.zip.ZipFile

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScapHumanDeliveryTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private fun row(index: Int, measure: Double): ScapBridgeRow {
        val id = "00000000-0000-0000-0000-${index.toString().padStart(12, '0')}"
        val inspection = ScapInspectionEntity(id, "road-$index", "Bridge $index", "CONTRACT-$index",
            index.toLong(), index.toLong(), "Synthetic", "test", "IN_PROGRESS", "PENDING", 0.0, 0.0, 1.0, 1, "GNSS")
        val snapshot = ScapInspectionSnapshot(inspection, listOf(ScapFieldValueEntity(id,"inspection","route","PE-3N","MANUAL")))
        val record = testRecord("road-$index", created=index.toLong(), route="PE-3N", sic="SCAP", status="DRAFT")
            .copy(assetType="PUENTE", axisMeasureM=measure)
        return ScapBridgeRow(snapshot, record)
    }

    @Test fun capture10Then30Then20KmUsesP1P3P2AndHumanPathsWithoutChangingIdentities() {
        val rows = listOf(row(1, 10000.0), row(2, 30000.0), row(3, 20000.0))
        val plan = ScapHumanDelivery.plan(rows)
        assertEquals(listOf("P-1", "P-3", "P-2"), rows.map { plan.getValue(it.snapshot.inspection.id).shortCode })
        assertEquals(listOf("PE-3N/SIB-08/PUENTE 1", "PE-3N/SIB-08/PUENTE 3", "PE-3N/SIB-08/PUENTE 2"),
            rows.map { plan.getValue(it.snapshot.inspection.id).path })
        rows.forEachIndexed { i, row ->
            assertEquals("CONTRACT-${i+1}", row.snapshot.inspection.bridgeCode)
            assertEquals("SCAP", row.record.sicCode); assertEquals("DRAFT", row.record.status)
            assertEquals("code:CONTRACT-${i+1}", plan.getValue(row.snapshot.inspection.id).bridgeIdentity)
        }
        assertEquals(plan, ScapHumanDelivery.plan(rows.reversed()))
        val repeated = rows[0].copy(snapshot=rows[0].snapshot.copy(inspection=rows[0].snapshot.inspection.copy(id=UUID.randomUUID().toString(),updatedAt=10)))
        val revised = ScapHumanDelivery.plan(rows + repeated)
        assertEquals(revised[rows[0].snapshot.inspection.id], revised[repeated.snapshot.inspection.id])
    }

    @Test fun preparationProjectsSavedScapGnssOnOfficialAxisAndKeepsParentContractualData() = runBlocking {
        val db=Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build()
        try {
            val repo=ScapRepository(db,ScapCatalog.load(context),ProjectRouteCatalog.load(context))
            val points=RoadReferenceRepository(context).loadFromAssets().segments.single {it.routeCode=="PE-22A"}.points
            val snapshots=listOf(10000.0,30000.0,20000.0).map {measure->
                val id=repo.create("Synthetic","test",null)
                repo.setField(id,"inspection","route","PE-22A")
                repo.setField(id,"inspection","bridgeCode","CONTRACT-$measure")
                val p=points.minBy {kotlin.math.abs(it.chainageM-measure)}
                repo.setLocation(id,com.tuempresa.inventariovial.location.GeoLocation(p.latitude,p.longitude,1.0f))
                repo.dao.snapshot(id)!!
            }
            val parents=snapshots.map {db.inventoryDao().recordById(it.inspection.roadRecordId)!!}
            val layouts=snapshots.map {ScapHumanDelivery.prepare(context,db,it)}
            assertEquals(listOf("P-1","P-3","P-2"),layouts.map {it.shortCode})
            assertEquals(listOf("PUENTE 1","PUENTE 3","PUENTE 2"),layouts.map {it.elementFolder})
            snapshots.forEachIndexed {i,s->
                assertEquals(parents[i],db.inventoryDao().recordById(s.inspection.roadRecordId))
                assertEquals(s,repo.dao.snapshot(s.inspection.id))
            }
        } finally {db.close()}
    }

    @Test fun humanZipRetriesExactBytesKeepsPriorRevisionAndAcknowledgesOnlyMatchingPathsAndRevision() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, InventoryDatabase::class.java).build()
        val source = File(context.cacheDir, "human-${UUID.randomUUID()}.png")
        val bundles = mutableListOf<File>()
        try {
            val bitmap = Bitmap.createBitmap(100, 80, Bitmap.Config.ARGB_8888)
            source.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }; bitmap.recycle()
            val sourceHash = ScapDelivery.sha256(source)
            val repo = ScapRepository(db, ScapCatalog.load(context), ProjectRouteCatalog.load(context))
            val id = repo.create("Synthetic", "test", null)
            repo.setField(id,"inspection","route","PE-3N")
            repo.setField(id,"inspection","bridgeCode","CONTRACT-A")
            repo.addPhoto(id,source.path,"GENERAL",null)
            val sketch = ScapSketchEntity(UUID.randomUUID().toString(),id,"PLAN",source.path)
            repo.dao.insertSketch(sketch)
            val original = repo.dao.snapshot(id)!!
            val parent = db.inventoryDao().recordById(original.inspection.roadRecordId)!!.copy(axisMeasureM=10000.0)
            val layout = ScapHumanDelivery.plan(listOf(ScapBridgeRow(original,parent))).getValue(id)
            assertEquals(listOf("fotos/001_F01.jpg"),layout.photoPaths.values.toList())
            assertEquals(listOf("croquis/CROQUIS_1.png"),layout.sketchPaths.values.toList())
            repo.dao.putValue(ScapFieldValueEntity(id,"delivery","layout",layout.json().toString(),"REVIEWED"))
            repo.dao.putValue(ScapFieldValueEntity(id,"delivery","revision",repo.dao.inspection(id)!!.updatedAt.toString(),"REVIEWED"))
            repo.dao.updateInspection(repo.dao.inspection(id)!!.copy(status="COMPLETE"))
            val s = repo.dao.snapshot(id)!!
            val bundle = ScapDelivery.bundle(context,s,layout).also {bundles += it}
            val hash = ScapDelivery.sha256(bundle)
            assertEquals(hash,ScapDelivery.sha256(ScapDelivery.bundle(context,s,layout)))
            val paths = setOf("SCAP.xlsx","manifest.json","fotos/001_F01.jpg","croquis/CROQUIS_1.png")
            ZipFile(bundle).use {zip ->
                assertEquals(paths,zip.entries().asSequence().map {it.name}.toSet())
                val manifest=JSONObject(zip.getInputStream(zip.getEntry("manifest.json")).bufferedReader().readText())
                assertEquals(2,manifest.getInt("protocol"))
                assertEquals(ScapDelivery.SCAP_BUNDLE_VERSION,manifest.getInt("scapBundleVersion"))
                assertEquals(layout,ScapHumanLayout.parse(manifest.getJSONObject("delivery")))
                assertEquals(id,manifest.getString("scapInspectionId"))
                assertEquals("CONTRACT-A",manifest.getString("bridgeCode"))
                val files=manifest.getJSONArray("files")
                for(i in 0 until files.length()) {
                    val f=files.getJSONObject(i)
                    assertEquals(f.getString("sha256"),ScapDelivery.sha256(zip.getInputStream(zip.getEntry(f.getString("path"))).readBytes()))
                }
            }
            val ack=JSONObject().put("ok",true).put("protocol",2).put("recordKind","SCAP")
                .put("scapInspectionId",id).put("revision",s.inspection.updatedAt).put("sha256",hash)
                .put("fileId","zip-id").put("folderId","version-id").put("bridgeFolderId","bridge-id").put("path",layout.path)
                .put("files",JSONArray(paths.map {JSONObject().put("path",it).put("fileId","file-$it").put("folderId","folder-$it")}))
            assertTrue(ScapDriveSync.validAck(ack,s,hash))
            assertFalse(ScapDriveSync.validAck(JSONObject(ack.toString()).put("path","PE-3N/SIB-08/PUENTE 2"),s,hash))
            assertFalse(ScapDriveSync.validAck(JSONObject(ack.toString()).put("protocol",1),s,hash))
            assertTrue(ScapDriveSync.acknowledge(db,s,ack,hash))
            assertEquals("SYNCED",repo.dao.snapshot(id)!!.photos.single().syncStatus)
            repo.dao.updateInspection(repo.dao.inspection(id)!!.copy(status="IN_PROGRESS"))
            repo.setField(id,"inspection","bridgeName","New saved revision")
            val current=repo.dao.snapshot(id)!!
            assertFalse(ScapDriveSync.acknowledge(db,s,ack,hash))
            val next=ScapDelivery.bundle(context,current,layout).also {bundles += it}
            assertNotEquals(bundle.path,next.path)
            assertEquals(hash,ScapDelivery.sha256(bundle))
            assertEquals(sourceHash,ScapDelivery.sha256(source))
            assertEquals(s.inspection.id,current.inspection.id)
            assertEquals(s.inspection.bridgeCode,current.inspection.bridgeCode)
        } finally {bundles.forEach {it.delete()};source.delete();db.close()}
    }
}
