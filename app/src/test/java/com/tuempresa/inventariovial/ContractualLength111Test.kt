package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.core.*
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.export.TechnicalLengthExport
import com.tuempresa.inventariovial.repository.InventoryRepository
import com.tuempresa.inventariovial.road.*
import com.tuempresa.inventariovial.validation.*
import kotlinx.coroutines.runBlocking
import org.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ContractualLength111Test {
    private val c get()=ApplicationProvider.getApplicationContext<Context>()
    private val a=LengthEndpoint("PE-28H","CD","0010","10")
    private val b=LengthEndpoint("PE-28H","CD","0011","180")
    private fun pr(code:String,at:Double)=RoadPr("PE-28H","CD",code,-12.0,-75.0,at)
    @Test fun bundledCatalogHasNoContractualEndpointsForReportedRoute()=runBlocking {
        val reference=RoadReferenceRepository(c).loadFromAssets()
        assertTrue(reference.prs.isEmpty())
        assertTrue(reference.segments.any{it.routeCode=="PE-28H"})
        val value=ContractualLength.calculate(a,b,reference.prs,"INCREASING")
        assertNull(value.metres);assertEquals("UNRESOLVED",value.source)
        assertTrue(value.warning!!.contains("equivalencia contractual"))
    }
    @Test fun realCodeRequiresBothEndpointsAndDoesNotAssumeOneKilometre() {
        assertNull(ContractualLength.calculate(a,b,emptyList(),"INCREASING").metres)
        assertNull(ContractualLength.calculate(a,b,listOf(pr("0010",52500.0)),"INCREASING").metres)
        val prs=listOf(pr("0010",52500.0),pr("0011",54860.0)) // Deliberately fictitious, non-kilometric separation.
        assertEquals(2530.0,ContractualLength.calculate(a,b,prs,"INCREASING").metres!!,0.0)
        assertEquals(2530.0,ContractualLength.calculate(a.copy(pr="10"),b.copy(pr="11"),prs,"INCREASING").metres!!,0.0)
        assertEquals(2530.0,ContractualLength.calculate(b,a,prs,"DECREASING").metres!!,0.0)
        assertNull(ContractualLength.calculate(b,a,prs,"INCREASING").metres)
        assertNull(ContractualLength.calculate(a,b.copy(bed="CI"),prs,"INCREASING").metres)
        assertNull(ContractualLength.calculate(a,b,prs.map{it.copy(roadbedCode="CI")},"INCREASING").metres)
        assertNull(ContractualLength.calculate(a,b,prs+pr("10",53000.0),"INCREASING").metres)
        assertEquals(170.0,ContractualLength.calculate(a,a.copy(distance="180"),emptyList(),"INCREASING").metres!!,0.0)
    }
    @Test fun localPrRowsCanExplainDifferentResultsWithoutDifferentApkButDoNotProveOfficialProvenance()=runBlocking {
        Room.inMemoryDatabaseBuilder(c,InventoryDatabase::class.java).build().use{db->
            val repo=FieldPrRepository(db.inventoryDao());val bundled=RoadReferenceRepository(c).loadFromAssets()
            assertNull(ContractualLength.calculate(a,b,repo.merge(bundled).prs,"INCREASING").metres)
            for((code,at)in listOf("0010" to 9000.0,"0011" to 9900.0))db.inventoryDao().insertFieldPr(
                FieldPrEntity("TEST111_$code","PE-28H","CD",code,"Virtual",null,-12.0,-75.0,null,5.0,1,at,"TEST_AXIS",createdAt=1))
            val merged=repo.merge(bundled)
            assertEquals(1070.0,ContractualLength.calculate(a,b,merged.prs,"INCREASING").metres!!,0.0)
            assertEquals("FIELD_CAPTURED",db.inventoryDao().fieldPrs().first().source)
            // Existing API labels any supplied RoadPr as OFFICIAL; a local projection alone does not certify the contract.
            assertEquals("OFFICIAL_PR_CHAINAGE",ContractualLength.calculate(a,b,merged.prs,"INCREASING").source)
            val diagnostic=com.tuempresa.inventariovial.diagnostics.ContractualReferenceDiagnostics.snapshot(c,db)
            assertEquals(0,diagnostic.getInt("bundledPrCount"));assertEquals(2,diagnostic.getInt("localFieldPrCount"))
            assertEquals(1,diagnostic.getInt("reportedCaseStartLocalMatches"));assertEquals(1,diagnostic.getInt("reportedCaseEndLocalMatches"))
            val text=diagnostic.toString()
            for(secret in listOf("TEST111_0010","TEST111_0011","latitude","longitude","description","-12.0","-75.0"))assertFalse(text.contains(secret))
        }
    }
    @Test fun pendingWorkbookLeavesCalculatedAndApplicableCellsEmptyWithoutChangingProgressives() {
        val r=testRecord("TEST111_XLSX",sic="SIC-20",route="PE-28H",pr="0010",distance=10.0).copy(assetType="BADEN",endPrCode="0011",endDistanceM=180.0,surveyDirection="INCREASING")
        val item=InventoryRecordForExport(r,sic20=Sic20Entity(r.id,"12","2",2.0,3.0,"1","1"))
        val bytes=java.io.ByteArrayOutputStream().also{TechnicalLengthExport.write(it,item,emptyList())}.toByteArray()
        val xml=java.util.zip.ZipInputStream(bytes.inputStream()).use{z->var found="";while(true){val e=z.nextEntry?:break;if(e.name=="xl/worksheets/sheet1.xml")found=z.readBytes().toString(Charsets.UTF_8)};found}
        val doc=javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xml.byteInputStream())
        val rows=doc.getElementsByTagName("row")
        val row=(0 until rows.length).map{rows.item(it) as org.w3c.dom.Element}.single{it.textContent.contains(r.id)}
        val nodes=row.getElementsByTagName("c")
        val cells=(0 until nodes.length).map{nodes.item(it) as org.w3c.dom.Element}.associate{it.getAttribute("r").filter(Char::isLetter) to it.textContent}
        assertEquals("0010",cells["E"]);assertEquals("10.0",cells["F"]);assertEquals("0011",cells["G"]);assertEquals("180.0",cells["H"])
        for(col in listOf("I","J","K"))assertTrue("Empty length cell $col",cells[col].isNullOrEmpty())
        assertEquals("UNRESOLVED",cells["L"]);assertTrue(cells["M"]!!.contains("equivalencia contractual"))
        assertFalse(com.tuempresa.inventariovial.export.SicExportFormat.SIC20.columns.any{it.label.contains("Longitud",true)})
    }
    @Test fun pendingLengthExportsNullAndAValidTransportAckDoesNotInventOrConfirmACalculation()=runBlocking {
        Room.inMemoryDatabaseBuilder(c,InventoryDatabase::class.java).build().use{db->
            val id="TEST111_FORD";val photo=File(c.filesDir,"$id.jpg")
            val bitmap=Bitmap.createBitmap(24,24,Bitmap.Config.ARGB_8888)
            photo.outputStream().use{bitmap.compress(Bitmap.CompressFormat.JPEG,95,it)};bitmap.recycle()
            val r=testRecord(id,sic="SIC-20",route="PE-28H",pr="0010",distance=10.0).copy(assetType="BADEN",roadbedCode="CD",endPrCode="0011",endDistanceM=180.0,sideCode="D",surveyDirection="INCREASING",latitude=null,longitude=null,observations="TEST111 OBS")
            InventoryRepository(db).saveSic20(r,Sic20Entity(id,"12","2",2.0,3.0,"1","1"),listOf(PhotoEntity("TEST111_PHOTO",id,1,photo.path,true,null,null,null,"PENDING",1,photoCategory="GENERAL")))
            val before=db.inventoryDao().snapshot(id)!!
            val transport=object:AutomaticDriveTransport {
                override fun supportsV3()=true
                override fun supportsFamilyPaths()=true
                override fun supportsReadablePhotoNames()=true
                override fun supportsTechnicalDeliveryWithoutGps()=true
                override fun reservePhotoNames(payload:JSONObject)=ReleasePhotoV2Fixture.reservation(payload,"TEST111_FOLDER")
                override fun post(p:JSONObject):JSONObject {
                    val prefix="${p.getString("routeCode")}/${p.getString("familyPath")}/${p.getString("family")} 1"
                    val fs=p.getJSONArray("files");val rows=p.getJSONArray("sicRows")
                    val native=(0 until fs.length()).map{fs.getJSONObject(it)}.single{it.getString("key")=="metadata"}
                    val data=JSONObject(File(native.getString("localFile")).readText())
                    assertTrue(data.getJSONObject("technicalLength").isNull("calculatedM"))
                    assertTrue(data.getJSONObject("technicalLength").isNull("applicableM"))
                    assertTrue((0 until fs.length()).any{fs.getJSONObject(it).getString("key")=="technical-length"})
                    return JSONObject().put("ok",true).put("syncProtocol",3).put("pathVersion",4).put("key",p.getString("key"))
                        .put("revision",p.getLong("revision")).put("contentHash",p.getString("contentHash")).put("folderId","TEST111_FOLDER").put("logicalPath",prefix)
                        .put("technicalDeliveryStatus","DELIVERED").put("georeferencingStatus",p.getJSONObject("georeferencing").getString("status")).put("externalGeoreferencingStatus","PENDING")
                        .put("files",JSONArray((0 until fs.length()).map{n->val f=fs.getJSONObject(n);JSONObject().put("key",f.getString("key")).put("sha256",f.getString("sha256")).put("fileId","TEST111_FILE_$n").put("folderId","TEST111_FOLDER").put("requestedPath",f.getString("path")).put("logicalPath",prefix+"/"+f.getString("path"))}))
                        .put("sicFiles",JSONArray((0 until rows.length()).map{n->val row=rows.getJSONObject(n);JSONObject().put("format",row.getString("format")).put("sha256","a".repeat(64)).put("fileId","TEST111_EXCEL").put("folderId","TEST111_XLSX_FOLDER").put("logicalPath","PE-28H/EXCEL_SIC/${row.getString("format")}.xlsx").put("recordIds",JSONArray(listOf(id)))}))
                }
            }
            val retry=AutomaticSyncDrain.run(c,db,{transport},null)
            val diagnostic=db.automaticSyncDao().all().single()
            assertFalse("Fictitious ACK drain: state=${diagnostic.state}, error=${diagnostic.error}",retry)
            assertTrue(SyncIntentPresentation.confirmed(db.automaticSyncDao().all().single()))
            val after=db.inventoryDao().snapshot(id)!!
            assertEquals(before.record.copy(photoSyncStatus=after.record.photoSyncStatus,excelSyncStatus=after.record.excelSyncStatus),after.record)
            assertEquals("SYNCED",after.record.photoSyncStatus);assertEquals("SYNCED",after.record.excelSyncStatus)
            assertEquals(before.sic20,after.sic20)
            val row=db.inventoryDao().recordsForExport("SIC-20",null).single()
            val length=TechnicalLengthExport.metadata(row,emptyList())
            assertTrue(length.isNull("calculatedM"));assertTrue(length.isNull("applicableM"));assertEquals("12",row.sic20!!.classCode)
            assertEquals("0010",row.record.startPrCode);assertEquals("0011",row.record.endPrCode);assertTrue(photo.isFile)
        }
    }
}
