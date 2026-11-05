package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.export.*
import com.tuempresa.inventariovial.localfiles.*
import com.tuempresa.inventariovial.scap.export.TemplateWorkbook
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowStatFs
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=Application::class)
class SicPrimaryPublicationTest {
    private val c get()=ApplicationProvider.getApplicationContext<Context>()
    private fun db()=Room.inMemoryDatabaseBuilder(c,InventoryDatabase::class.java).build()
    @Before fun setup(){initializeIdleUiWorkers();ShadowStatFs.registerStats(c.filesDir.path,1_000_000,900_000,900_000)}
    private suspend fun culvert(db:InventoryDatabase,id:String,pr:String="0010",revision:Long=1,status:String="ACTIVE") {
        db.inventoryDao().insertRecord(testRecord(id,pr,120.0,created=revision,status=status,route="PE-22A",sic="SIC-18").copy(assetType="CULVERT"))
        db.inventoryDao().insertSic18(Sic18Entity(id,"06","3",2,"2",1.25,null,"3","1"))
        db.inventoryDao().saveSic18A(Sic18AEntity(id,functionCode="1",failureLocationCode="1",failureTypeCode="1",functionalStateCode="1",probableCauseCode="1"))
    }
    private fun job(id:String,revision:Long=1,scope:String="FULL")=LocalExportJob("TEST_PRIMARY_$id-$revision-$scope","SIC/$id","SIC",id,revision,scope,createdAt=revision)
    @Test fun successiveCulvertsAndRevisionPublishOneWorkbookPerFormatWithAllRows()=runBlocking {db().use{d->
        val storage=SicTestStorage()
        for((n,id)in listOf("TEST_A","TEST_B","TEST_C").withIndex()) {
            culvert(d,id,"00${12-n}");val j=job(id);d.localFilesDao().saveJob(j)
            LocalCopyDrain.run(c,d,storage,20)
            val saved=d.localFilesDao().job(j.id)!!;assertEquals(saved.message,"READY",saved.state)
            val public=LocalPlan.parse(saved.manifestJson!!)
            assertFalse(public.files.any{it.name.matches(Regex("SIC-18A?(_PROVISIONAL)?\\.xlsx"))})
            for(format in listOf("SIC-18","SIC-18A")){
                val result=LocalSicConsolidates.current(c,"PE-22A",format,storage)
                assertEquals(n+1,result.rows);assertEquals(n+1,SicRouteWorkbook.inspect(result.file.readBytes(),format))
                assertEquals(1,storage.entries.values.count{it.name=="$format.xlsx"})
                assertEquals(result.file.length() to fileDigest(result.file).second,storage.digest(result.uri))
                assertTrue(storage.find(result.uri).parent.endsWith("/PE-22A/EXCEL_SIC/"))
                assertEquals(android.content.Intent.ACTION_VIEW,SharedFileActions.view(result.uri,SicExcelWriter.XLSX_MIME).action)
            }
            assertTrue(File(LocalExportBuilder.work(c,j),"SIC-18.xlsx").isFile)
            assertTrue(File(LocalExportBuilder.work(c,j),"SIC-18A.xlsx").isFile)
            val manifest=JSONObject(File(LocalExportBuilder.work(c,j),"MANIFIESTO_LOCAL.json").readText())
            assertEquals(2,manifest.getJSONArray("privateSicWorkbooks").length())
            assertEquals(2,manifest.getJSONArray("publicSicConsolidates").length())
            assertEquals(public.files.size,manifest.getInt("localFilesExpected"))
        }
        val old=LocalSicConsolidates.current(c,"PE-22A","SIC-18",storage)
        val original=old.file.readBytes();val prior=d.inventoryDao().recordById("TEST_A")!!
        d.inventoryDao().updateRecord(prior.copy(updatedAt=2))
        val detail=d.inventoryDao().snapshot("TEST_A")!!.sic18!!;d.inventoryDao().updateSic18(detail.copy(dimension1M=2.5))
        d.localFilesDao().saveJob(job("TEST_A",2));LocalCopyDrain.run(c,d,storage,20)
        val next=LocalSicConsolidates.current(c,"PE-22A","SIC-18",storage)
        assertEquals(3,next.rows);assertTrue(TemplateWorkbook(next.file.inputStream()).sheet(1).documentElement.textContent.contains("2.5"))
        assertArrayEquals(original,old.file.readBytes())
        assertEquals(1,storage.entries.values.count{it.name=="SIC-18.xlsx"})
        assertTrue(storage.entries.values.none{it.name.startsWith("SIC-18")&&!it.parent.endsWith("/EXCEL_SIC/")})
        assertTrue(d.automaticSyncDao().all().isEmpty())
    }}
    @Test fun retainedRowsAndPrivateVersionsSurviveAnUnavailableRecord()=runBlocking {db().use{d->
        val storage=SicTestStorage();culvert(d,"TEST_A");culvert(d,"TEST_B","0011")
        val first=LocalSicConsolidates.update(c,d,"PE-22A","SIC-18",storage)
        val previousBytes=first.file.readBytes()
        d.inventoryDao().updateRecord(d.inventoryDao().recordById("TEST_B")!!.copy(status="DRAFT",updatedAt=2))
        val next=LocalSicConsolidates.update(c,d,"PE-22A","SIC-18",storage)
        assertEquals(2,next.rows);assertEquals(1,next.retained);assertArrayEquals(previousBytes,first.file.readBytes())
        assertEquals(2,SicRouteWorkbook.inspect(next.file.readBytes(),"SIC-18"))
    }}
    @Test fun absentOrStaleConsolidateNeverHidesTheIndividualFallback()=runBlocking {db().use{d->
        val storage=SicTestStorage();culvert(d,"TEST_A")
        val first=job("TEST_A");val raw=LocalExportBuilder.build(c,d,first)
        assertEquals(raw,LocalSicConsolidates.preferConsolidates(c,d,first,raw,storage))
        LocalSicConsolidates.update(c,d,"PE-22A","SIC-18",storage)
        d.inventoryDao().updateRecord(d.inventoryDao().recordById("TEST_A")!!.copy(updatedAt=2))
        val newer=job("TEST_A",2);val newerPlan=LocalExportBuilder.build(c,d,newer)
        assertEquals(newerPlan,LocalSicConsolidates.preferConsolidates(c,d,newer,newerPlan,storage))
    }}
    @Test fun incompleteDraftKeepsItsPublicProvisionalWorkbook()=runBlocking {db().use{d->
        val storage=SicTestStorage();culvert(d,"TEST_DRAFT",status="DRAFT")
        val j=job("TEST_DRAFT");d.localFilesDao().saveJob(j);LocalCopyDrain.run(c,d,storage,20)
        val saved=d.localFilesDao().job(j.id)!!;assertEquals("PARTIAL",saved.state)
        assertTrue(d.localFilesDao().files(j.id).any{it.name=="SIC-18.xlsx"&&it.state=="READY"&&RecordDocuments.provisional(it)})
        assertTrue(storage.entries.values.none{it.name=="SIC-18.xlsx"&&it.parent.endsWith("/EXCEL_SIC/")})
    }}
    @Test fun publicEditCannotCauseSuppressionOfAnUnverifiedWorkbook()=runBlocking {db().use{d->
        val storage=SicTestStorage();culvert(d,"TEST_A")
        val result=LocalSicConsolidates.update(c,d,"PE-22A","SIC-18",storage)
        storage.find(result.uri).bytes="EDICION EXTERNA FICTICIA".toByteArray()
        val j=job("TEST_A");val raw=LocalExportBuilder.build(c,d,j)
        assertEquals(raw,LocalSicConsolidates.preferConsolidates(c,d,j,raw,storage))
        assertEquals("EDICION EXTERNA FICTICIA",storage.find(result.uri).bytes.toString(Charsets.UTF_8))
    }}
    @Test fun explicitZipAndSpecialFamiliesKeepTheirOwnDocuments()=runBlocking {db().use{d->
        val storage=SicTestStorage();culvert(d,"TEST_A")
        LocalSicConsolidates.update(c,d,"PE-22A","SIC-18",storage)
        val zip=job("TEST_A",scope="ZIP");val raw=LocalExportBuilder.build(c,d,zip)
        assertEquals(raw,LocalSicConsolidates.preferConsolidates(c,d,zip,raw,storage))
        val plan=LocalPlan(emptyList(),emptyList(),5)
        for(kind in listOf("SCAP","PAVEMENT"))assertEquals(plan,LocalSicConsolidates.preferConsolidates(c,d,job("TEST_A").copy(kind=kind),plan,storage))
        d.inventoryDao().insertRecord(testRecord("TEST_17",sic="SIC-17",route="PE-22A"))
        assertEquals(plan,LocalSicConsolidates.preferConsolidates(c,d,job("TEST_17"),plan,storage))
    }}
    @Test fun frozenHistoricalManifestAndItsExistingIndividualUrisAreNotMigrated()=runBlocking {db().use{d->
        val storage=SicTestStorage();culvert(d,"TEST_HISTORY")
        val old=job("TEST_HISTORY");val raw=LocalExportBuilder.build(c,d,old);val frozen=raw.json()
        d.localFilesDao().saveJob(old.copy(manifestJson=frozen));LocalCopyDrain.run(c,d,storage,20)
        val previous=d.localFilesDao().files(old.id).single{it.name=="SIC-18.xlsx"}
        LocalSicConsolidates.update(c,d,"PE-22A","SIC-18",storage)
        d.localFilesDao().saveJob(d.localFilesDao().job(old.id)!!.copy(state="QUEUED"));LocalCopyDrain.run(c,d,storage,20)
        assertEquals(frozen,d.localFilesDao().job(old.id)!!.manifestJson)
        assertEquals(previous,d.localFilesDao().files(old.id).single{it.id==previous.id})
        d.inventoryDao().updateRecord(d.inventoryDao().recordById("TEST_HISTORY")!!.copy(updatedAt=2))
        d.localFilesDao().saveJob(job("TEST_HISTORY",2));LocalCopyDrain.run(c,d,storage,20)
        assertFalse(d.localFilesDao().files(job("TEST_HISTORY",2).id).any{it.name=="SIC-18.xlsx"})
        assertEquals(previous.size to previous.sha256,storage.digest(Uri.parse(previous.uri)))
        assertEquals(previous.name,storage.find(Uri.parse(previous.uri)).name)
    }}
}
