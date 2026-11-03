package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.core.*
import com.tuempresa.inventariovial.export.*
import com.tuempresa.inventariovial.localfiles.*
import com.tuempresa.inventariovial.scap.data.*
import com.tuempresa.inventariovial.scap.export.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
import org.robolectric.shadows.ShadowStatFs
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipFile

@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],application=Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RealEvidenceP0Test {
    private val c get()=ApplicationProvider.getApplicationContext<Context>()
    @Test fun incompleteThreeSicFormatsKeepCellsBlankAndPublishWithoutLosingEarlierRows()=runBlocking {
        SicConsolidationFixtures.db(c).use{db->
            val r=SicConsolidationFixtures.repo(c,db);val a=SicConsolidationFixtures.bridge(r,"TEST_A");val b=SicConsolidationFixtures.bridge(r,"TEST_B","10+200")
            r.setSicField(a,"SIC-17","surveyDate","")
            for(key in listOf("lanes","deckWidthM","superstructureWidthM","alignmentCode"))r.setSicField(a,"SIC-17A",key,"")
            r.setSicField(a,"SIC-17B","wearingSurface","")
            val before=r.dao.snapshot(a)!!;val store=SicTestStorage()
            for(format in listOf("SIC-17","SIC-17A","SIC-17B")){
                val p=ScapSicExporter.previews(before).single{it.format==format};assertTrue(p.missingRequired.isNotEmpty())
                val bytes=ByteArrayOutputStream();ScapSicExporter.writeLocal(bytes,before,format)
                val book=TemplateWorkbook(bytes.toByteArray().inputStream())
                assertTrue(p.values.indices.filter{p.values[it]==null}.all{index->book.cell(1,"${('A'+index)}14").textContent.isBlank()})
                val result=LocalSicConsolidates.update(c,db,"PE-28H",format,store,a)
                assertEquals(2,result.rows);assertTrue(result.pending>0);assertTrue(TemplateWorkbook(result.file.inputStream()).parts.containsKey("docProps/custom.xml"))
                assertTrue(runCatching{ScapSicExporter.write(ByteArrayOutputStream(),before,format)}.isFailure)
            }
            assertEquals(before,r.dao.snapshot(a));assertTrue(db.automaticSyncDao().all().isEmpty());assertNotEquals(a,b)
        }
    }
    @Test fun correctingAndClearingAFieldUpdatesOneIdentityWithoutErasingKnownCells()=runBlocking {
        SicConsolidationFixtures.db(c).use{db->
            val r=SicConsolidationFixtures.repo(c,db);val a=SicConsolidationFixtures.bridge(r,"TEST_RETAIN");SicConsolidationFixtures.bridge(r,"TEST_OTHER","10+300")
            val storage=SicTestStorage();val first=LocalSicConsolidates.update(c,db,"PE-28H","SIC-17A",storage,a)
            val original=TemplateWorkbook(first.file.inputStream());val cells=original.sheet(1).nodes("row").single{it.getAttribute("r")=="14"}.textContent
            r.setSicField(a,"SIC-17A","lanes","")
            val second=LocalSicConsolidates.update(c,db,"PE-28H","SIC-17A",storage,a)
            assertEquals(2,second.rows);assertTrue(second.retained>0);assertEquals(cells,TemplateWorkbook(second.file.inputStream()).sheet(1).nodes("row").single{it.getAttribute("r")=="14"}.textContent)
            r.setSicField(a,"SIC-17A","lanes","4")
            val third=LocalSicConsolidates.update(c,db,"PE-28H","SIC-17A",storage,a);assertEquals(2,third.rows)
            assertEquals(1,storage.entries.values.count{it.name=="SIC-17A.xlsx"})
        }
    }
    @Test fun missingPrExportsASeparateProvisionalWorkbookAndNeverInventsARouteRow()=runBlocking {
        SicConsolidationFixtures.db(c).use{db->
            val r=SicConsolidationFixtures.repo(c,db);val id=r.create("TEST_NO_LOCATION","TEST",null,com.tuempresa.inventariovial.scap.domain.ScapBridgeType.SLAB)
            val s=r.dao.snapshot(id)!!;val storage=SicTestStorage();assertFalse(LocalBridgeDocument.located(s))
            val uri=LocalBridgeDocument.provisional(c,db,s,"SIC-17A",storage)
            assertTrue(storage.find(uri).name.contains("PROVISIONAL"));assertTrue(storage.find(uri).parent.contains("/PUENTES/"))
            assertEquals(1,SicRouteWorkbook.inspect(storage.find(uri).bytes,"SIC-17A"));assertEquals(s,r.dao.snapshot(id))
        }
    }
    @Test fun sevenPhotosSevenSketchesHaveOneVerifiedReadableZipAndNoRemoteAck()=runBlocking {
        ShadowStatFs.registerStats(c.filesDir.path,1_000_000,900_000,900_000)
        SicConsolidationFixtures.db(c).use{db->
            val id=ScapP0Fixture.create(c,db,7);val r=SicConsolidationFixtures.repo(c,db);r.reopen(id)
            val old=r.dao.snapshot(id)!!
            repeat(4){n->r.addSketch(id,ScapRepository.SKETCH_TYPES.first(),old.sketches.first().localUri)}
            r.complete(id,true);val s=r.dao.snapshot(id)!!;assertEquals(7,s.sketches.size)
            val job=LocalCopies.request(c,db,"SCAP",id,s.inspection.updatedAt,"ZIP");val store=SicTestStorage();LocalCopyDrain.run(c,db,store,20)
            val result=db.localFilesDao().job(job.id)!!;assertEquals(result.message,"READY",result.state)
            val f=db.localFilesDao().files(job.id).single();assertFalse(f.name.contains(id));assertTrue(f.name.endsWith(".zip"));assertNotNull(f.uri)
            File(System.getProperty("gbo.p0.evidence"),"EJEMPLO_NOMBRE_LOCAL.json").apply{parentFile!!.mkdirs();writeText(org.json.JSONObject().put("name",f.name).put("relativePath",f.relativePath).put("verified",true).toString(2))}

            ZipFile(f.source).use{zip->val names=zip.entries().asSequence().map{it.name}.toList();assertEquals(7,names.count{"FOTOGRAFIAS" in it.split('/')});assertEquals(7,names.count{"CROQUIS" in it.split('/')})}
            assertEquals(0,db.automaticSyncDao().get("SCAP/$id")!!.syncedRevision)
        }
    }
    @Test fun oldCapabilityBlockCanBeScheduledWithoutChangingIdentityOrFrozenPreparation()=runBlocking {
        SicConsolidationFixtures.db(c).use{db->
            val id=ScapP0Fixture.create(c,db,0);val i=db.automaticSyncDao().get("SCAP/$id")!!.copy(state="PENDING_VALIDATION",error="Pendiente de compatibilidad del servidor: panel fotográfico SCAP.",preparedJson="{\"revision\":1}")
            db.automaticSyncDao().put(i);val selected=mutableListOf<String>()
            assertEquals(1,UnifiedSyncAdapter.enqueue(db,false){kind,recordId,_->selected+="$kind/$recordId"})
            assertEquals(listOf(i.key),selected);assertEquals(i,db.automaticSyncDao().get(i.key))
            val v=DriveDeliveryPresentation.view(i,DriveDeliveryIdentity("TEST","TEST",i.revision,true),false,true,true);assertTrue(v.retryAllowed);assertTrue(v.message.contains("última consulta"))
        }
    }
    @Test fun processedSixteenIsStagingAndCannotBecomeAConfirmationWithoutReceipt(){
        val old=deliveryFixture();val i=old.copy(state="UPLOADING",syncedRevision=0,receiptJson=null)
        val p=SyncProgress(i.revision,"CONFIRMING",16,16,1)
        val view=DriveDeliveryPresentation.view(i,DriveDeliveryIdentity("TEST","TEST",i.revision,true),true,true,true,progress=p)
        assertEquals(DriveDeliveryStatus.VERIFYING,view.status);assertFalse(view.confirmed)
        assertTrue(SyncProgressPresentation.files(p).contains("staging"));assertTrue(SyncProgressPresentation.files(p).contains("ACK"))
    }
    @Test fun remoteV21LimitsRemainIndependentFromLocalExports(){
        RemoteRevisionBudget.check(List(59){1024L},4096)
        assertTrue(runCatching{RemoteRevisionBudget.check(List(81){1L})}.exceptionOrNull() is SyncPending)
        assertTrue(runCatching{RemoteRevisionBudget.check(listOf(25L*1024*1024))}.exceptionOrNull() is SyncPending)
        assertTrue(runCatching{RemoteRevisionBudget.check(List(8){24L*1024*1024})}.exceptionOrNull() is SyncPending)
    }
    @Test fun independentSic17LocalSelectionDoesNotBlockOrInventMissingTechnicalCells()=runBlocking {
        SicConsolidationFixtures.db(c).use{db->
            val r=testRecord("TEST_NATIVE_MISSING",sic="SIC-17",route="PE-28H").copy(status="ACTIVE",surveyDate="",endPrCode=null,endDistanceM=null)
            db.inventoryDao().insertRecord(r)
            val invalid=db.inventoryDao().snapshot(r.id)!!.copy(record=r.copy(surveyDate="31/02/2026"))
            assertNull(LocalSicWorkbookPolicy.primary17Values(invalid)["surveyDate"])
            val scope=kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob()+kotlinx.coroutines.Dispatchers.IO)
            val review=LocalSicExport(c,db,scope).review(SicExportFormat.SIC17,"PE-28H")
            assertTrue(review.pending.isNotEmpty());assertTrue(review.blocking.isEmpty())
            val b=ByteArrayOutputStream();LocalSicWorkbookPolicy.selection(b,db,db.inventoryDao().recordsForExport("SIC-17","PE-28H"),SicExportFormat.SIC17,ExportHeading())
            val book=TemplateWorkbook(b.toByteArray().inputStream());assertEquals(1,SicRouteWorkbook.inspect(b.toByteArray(),"SIC-17"))
            for(ref in listOf("K14","L14","M14","P14","T14"))assertTrue(ref,book.cell(1,ref).textContent.isBlank())
            assertTrue(book.parts.containsKey("docProps/custom.xml"));assertEquals(r,db.inventoryDao().recordById(r.id))
            val provider=SicDownloadsProvider().apply{attachInfo(c,android.content.pm.ProviderInfo().apply{authority="media"})}
            org.robolectric.shadows.ShadowContentResolver.registerProviderInternal("media",provider)
            val exporter=LocalSicExport(c,db,scope);val prepared=exporter.prepareRoute(SicExportFormat.SIC17,"PE-28H")
            assertEquals(prepared.localPath,exporter.cachedRoute(SicExportFormat.SIC17,"PE-28H")!!.localPath)
            assertEquals(1,SicRouteWorkbook.inspect(File(prepared.localPath).readBytes(),"SIC-17"))
            assertEquals("QUEUED",db.automaticSyncDao().get("SIC/${r.id}")!!.state)
            assertEquals(0L,db.automaticSyncDao().get("SIC/${r.id}")!!.syncedRevision)
            scope.coroutineContext[kotlinx.coroutines.Job]?.cancel();Unit
        }
    }

    @Test fun verifiedUriActionsKeepMimeReadGrantAndAccessibleLocationFallback(){
        val uri=android.net.Uri.parse("content://media/external/downloads/42")
        for(mime in listOf("application/pdf","application/zip",SicExcelWriter.XLSX_MIME,"image/jpeg","video/mp4")){
            val intent=SharedFileActions.view(uri,mime)
            assertEquals(uri,intent.data);assertEquals(mime,intent.type)
            assertTrue(intent.flags and android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION!=0)
            assertEquals(uri,intent.clipData!!.getItemAt(0).uri)
            assertEquals(android.app.DownloadManager.ACTION_VIEW_DOWNLOADS,SharedFileActions.location(uri)!!.action)
        }
        val other=android.net.Uri.parse("content://fictitious.provider/document/42")
        assertNull(SharedFileActions.location(other));assertEquals(other,SharedFileActions.view(other,"application/zip").data)
    }

}
