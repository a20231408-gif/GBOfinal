package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.export.*
import com.tuempresa.inventariovial.localfiles.*
import kotlinx.coroutines.runBlocking
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
class SicProvisionalOpeningTest {
    private val c get()=ApplicationProvider.getApplicationContext<Context>()
    private fun db()=Room.inMemoryDatabaseBuilder(c,InventoryDatabase::class.java).build()
    @Before fun setup(){initializeIdleUiWorkers();ShadowStatFs.registerStats(c.filesDir.path,1_000_000,900_000,900_000)}
    private fun job(id:String,rev:Long)=LocalExportJob("OPEN_$id-$rev","SIC/$id","SIC",id,rev,createdAt=rev)
    private suspend fun complete(d:InventoryDatabase,id:String,metres:Double) {
        val old=d.inventoryDao().recordById(id)
        val r=testRecord(id,"0010",metres,created=2,route="PE-22A",sic="SIC-18").copy(assetType="CULVERT",updatedAt=2)
        if(old==null)d.inventoryDao().insertRecord(r)else d.inventoryDao().updateRecord(r)
        d.inventoryDao().insertSic18(Sic18Entity(id,"06","3",2,"2",1.25,null,"3","1"))
        d.inventoryDao().saveSic18A(Sic18AEntity(id,functionCode="1",failureLocationCode="1",failureTypeCode="1",functionalStateCode="1",probableCauseCode="1"))
    }
    @Test fun currentRecordOpeningMustNotFallBackToTheEarlierEmptyProvisional()=runBlocking {db().use{d->
        val store=SicTestStorage();val id="TEST_200"
        d.inventoryDao().insertRecord(testRecord(id,"0010",200.0,created=1,status="DRAFT",route="PE-22A",sic="SIC-18"))
        d.localFilesDao().saveJob(job(id,1));LocalCopyDrain.run(c,d,store,30)
        assertTrue(d.localFilesDao().files(job(id,1).id).any{it.name=="SIC-18_PROVISIONAL.xlsx"})
        complete(d,"TEST_120",120.0);complete(d,id,200.0)
        val fresh=LocalExportBuilder.build(c,d,job(id,2))
        assertTrue(fresh.files.any{it.name=="SIC-18.xlsx"})
        assertFalse(fresh.files.any{it.name=="SIC-18_PROVISIONAL.xlsx"})
        d.localFilesDao().saveJob(job(id,2));LocalCopyDrain.run(c,d,store,30)
        assertEquals(2,LocalSicConsolidates.current(c,"PE-22A","SIC-18",store).rows)
        val docs=d.localFilesDao().filesForRecord("SIC/$id").filter{it.state=="READY"&&it.mimeType==SicExcelWriter.XLSX_MIME}
        val principal=LocalSicConsolidates.principals(c,d,"SIC",id,store)
        val opening=RecordDocuments.current(d.localFilesDao().jobs(),docs,principal)
        assertTrue("Opening must select EXCEL_SIC instead of the earlier empty provisional",opening.isNotEmpty()&&opening.all{it.relativePath.endsWith("/EXCEL_SIC/")})
        for(element in listOf("TEST_120",id)){
            val files=LocalSicConsolidates.principals(c,d,"SIC",element,store)
            assertEquals(setOf("SIC-18.xlsx","SIC-18A.xlsx"),files.map{it.name}.toSet())
            for(f in files)assertEquals(f.uri,RecordDocuments.openTarget(c,d,"SIC",element,f,storage=store).uri)
        }
        val previous=docs.single{it.name=="SIC-18_PROVISIONAL.xlsx"}
        assertEquals("SIC-18.xlsx",RecordDocuments.openTarget(c,d,"SIC",id,previous,storage=store).name)
        assertEquals(previous.uri,RecordDocuments.openTarget(c,d,"SIC",id,previous,history=true,storage=store).uri)
        assertTrue(RecordDocuments.provisional(previous))
        assertTrue(RecordDocuments.current(d.localFilesDao().jobs(),docs,principal,history=true).any{it.id==previous.id})
    }}
    @Test fun frozenPlanIsRecheckedWithoutRewritingItsPrivateRevision()=runBlocking {db().use{d->
        val store=SicTestStorage();val id="TEST_FROZEN"
        d.inventoryDao().insertRecord(testRecord(id,"0010",200.0,created=1,status="DRAFT",route="PE-22A",sic="SIC-18"))
        val old=job(id,1);val raw=LocalExportBuilder.build(c,d,old);val frozen=raw.json()
        val individual=raw.files.single{it.name=="SIC-18_PROVISIONAL.xlsx"};val original=File(individual.source).readBytes()
        complete(d,id,200.0);complete(d,"TEST_OTHER",120.0)
        for(format in listOf("SIC-18","SIC-18A"))LocalSicConsolidates.update(c,d,"PE-22A",format,store)
        d.localFilesDao().saveJob(old.copy(manifestJson=frozen));LocalCopyDrain.run(c,d,store,30)
        assertEquals(frozen,d.localFilesDao().job(old.id)!!.manifestJson)
        assertArrayEquals(original,File(individual.source).readBytes())
        assertTrue(store.entries.values.none{it.name.startsWith("SIC-18")&&!it.parent.endsWith("/EXCEL_SIC/")})
        assertTrue(d.automaticSyncDao().all().isEmpty())
    }}
    @Test fun asynchronousRevisionRequiresFreshProofAndOpeningResolvesTheLatestUri()=runBlocking {db().use{d->
        val store=SicTestStorage();complete(d,"TEST_A",120.0);complete(d,"TEST_B",200.0)
        for(format in listOf("SIC-18","SIC-18A"))LocalSicConsolidates.update(c,d,"PE-22A",format,store)
        val cached=LocalSicConsolidates.principals(c,d,"SIC","TEST_A",store)
        d.inventoryDao().updateRecord(d.inventoryDao().recordById("TEST_A")!!.copy(updatedAt=3,observations="TEST_CORRECCION"))
        assertTrue(LocalSicConsolidates.principals(c,d,"SIC","TEST_A",store).isEmpty())
        assertTrue(runCatching{RecordDocuments.openTarget(c,d,"SIC","TEST_A",cached.first(),storage=store)}.isFailure)
        for(format in listOf("SIC-18","SIC-18A")){
            val actual=LocalSicConsolidates.update(c,d,"PE-22A",format,store)
            assertEquals(2,actual.rows)
            val selected=cached.single{it.name=="$format.xlsx"}
            assertEquals(actual.uri.toString(),RecordDocuments.openTarget(c,d,"SIC","TEST_A",selected,storage=store).uri)
        }
        for(id in listOf("TEST_A","TEST_B"))assertEquals(2,LocalSicConsolidates.principals(c,d,"SIC",id,store).size)
    }}
    @Test fun failedConsolidationRetainsTheClearlyProvisionalFallback()=runBlocking {db().use{d->
        val store=SicTestStorage();val id="TEST_FAILURE"
        d.inventoryDao().insertRecord(testRecord(id,"0010",200.0,created=1,status="DRAFT",route="PE-22A",sic="SIC-18"))
        d.localFilesDao().saveJob(job(id,1));LocalCopyDrain.run(c,d,store,30)
        val old=d.localFilesDao().files(job(id,1).id).single{it.name=="SIC-18_PROVISIONAL.xlsx"}
        complete(d,id,200.0);val result=LocalSicConsolidates.update(c,d,"PE-22A","SIC-18",store)
        store.find(result.uri).bytes="MODIFICACION EXTERNA FICTICIA".toByteArray()
        val principals=LocalSicConsolidates.principals(c,d,"SIC",id,store);assertTrue(principals.isEmpty())
        val fallback=RecordDocuments.current(d.localFilesDao().jobs(),listOf(old),principals).single()
        assertTrue(RecordDocuments.provisional(fallback));assertEquals(old.uri,RecordDocuments.openTarget(c,d,"SIC",id,fallback,storage=store).uri)
        assertEquals("MODIFICACION EXTERNA FICTICIA",store.find(result.uri).bytes.toString(Charsets.UTF_8))
    }}
}
