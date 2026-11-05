package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.core.*
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.localfiles.*
import com.tuempresa.inventariovial.scap.catalog.ScapCatalog
import com.tuempresa.inventariovial.scap.data.*
import com.tuempresa.inventariovial.scap.domain.*
import com.tuempresa.inventariovial.scap.export.*
import com.tuempresa.inventariovial.supplementary.FieldKind
import kotlinx.coroutines.runBlocking
import org.json.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
import org.robolectric.shadows.ShadowStatFs
import java.io.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.zip.*

internal object ScapP0Fixture {
    suspend fun create(c:Context,db:InventoryDatabase,count:Int=54,onlyDevice:Boolean=false):String {
        val repo=ScapRepository(db,ScapCatalog.load(c));val id=repo.create("TEST_P0","TEST_TABLET",null,ScapBridgeType.SLAB)
        for((k,v) in mapOf("singularityName" to "PUENTE_FICTICIO_P0","bridgeCode" to "TEST_P0","route" to "PE-22A","progressive" to "16+618"))repo.setField(id,"inspection",k,v)
        repo.saveSicLocation(id,"PE-22A","CD","0016","618","0016","650")
        for(format in BridgeSicContract.formats)for(key in BridgeSicContract.fields(format).map{it.key}.filter{it !in BridgeSicContract.unresolved(format)&&!(format=="SIC-17"&&it in BridgeSicContract.locationKeys)}) {
            if(format=="SIC-17" && key=="surveyDate")continue // Exercise the persisted capture default, never seed the same date twice.
            val f=BridgeSicContract.fields(format,BridgeSicContract.manualValues(repo.dao.snapshot(id)!!,format)).single{it.key==key}
            val v=when(f.kind){
                FieldKind.CODE,FieldKind.DESCRIBED_CODE->f.options.first();FieldKind.INTEGER->"2";FieldKind.YEAR->"2000"
                FieldKind.DECIMAL,FieldKind.SIGNED_DECIMAL->"8.4";FieldKind.LATITUDE->"-12.1";FieldKind.LONGITUDE->"-77.1"
                else->when(key){"surveyDate"->"27/10/2026";else->"TEST_$key"}
            };repo.setSicField(id,format,key,v)
        }
        for(n in 1..count) {
            val f=File(c.filesDir,"p0/$id/FOTO_$n.jpg").apply{parentFile!!.mkdirs()}
            val bitmap=Bitmap.createBitmap(24,16,Bitmap.Config.ARGB_8888);bitmap.eraseColor(0xff000000.toInt()+n*1000)
            f.outputStream().use{bitmap.compress(Bitmap.CompressFormat.JPEG,95,it)};bitmap.recycle()
            val p=repo.addPhotoRecord(id,ScapPhotoPanel.categories[(n-1)%20].label,"TEST_FOTO_$n");repo.replacePhoto(id,p,f.path)
        }
        for(type in ScapRepository.SKETCH_TYPES){
            val f=File(c.filesDir,"p0/$id/$type.png").apply{parentFile!!.mkdirs()};val bitmap=Bitmap.createBitmap(16,16,Bitmap.Config.ARGB_8888)
            f.outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle();repo.addSketch(id,type,f.path)
        }
        if(onlyDevice)db.localFilesDao().saveChoice(DeviceDeliveryChoice("SCAP/$id","DEVICE","TEST_USER",System.currentTimeMillis()))
        repo.complete(id,warningsConfirmed=true)
        return id
    }
}

@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],application=Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScapP0Test {
    private val c get()=ApplicationProvider.getApplicationContext<Context>()
    private val evidence get()=File(System.getProperty("gbo.p0.evidence","../docs/scap-p0-20261027/evidencias")).apply{mkdirs()}
    @Before fun space(){ShadowStatFs.registerStats(c.filesDir.path,1_000_000,900_000,900_000)}
    @Test fun fiftyFourPhotosThreeSketchesPublishARealZipAndSurviveDatabaseReopen()=runBlocking {
        val name="p0.db";val store=SicTestStorage();lateinit var id:String;lateinit var job:LocalExportJob;lateinit var old:ScapInspectionSnapshot
        Room.databaseBuilder(c,InventoryDatabase::class.java,name).build().use{db->
            id=ScapP0Fixture.create(c,db);old=db.scapDao().snapshot(id)!!
            assertEquals(54,old.photos.size);assertEquals(3,old.sketches.size);assertEquals("COMPLETE",old.inspection.status)
            assertTrue(ScapCompletion.assess(old,ScapCatalog.load(c).forSnapshot(old)).pending.isNotEmpty())
            val hashes=(old.photos.map{File(it.originalPath)}+old.sketches.map{File(it.localUri)}).associate{it.path to fileDigest(it)}
            job=LocalCopies.request(c,db,"SCAP",id,old.inspection.updatedAt,"ZIP")
            assertEquals(job.id,LocalCopies.request(c,db,"SCAP",id,old.inspection.updatedAt,"ZIP").id)
            assertEquals(1,db.localFilesDao().jobs().count{it.scope=="ZIP"})
            LocalCopyDrain.run(c,db,store,limit=20)
            val result=db.localFilesDao().job(job.id)!!;assertEquals(result.message,"READY",result.state)
            val f=db.localFilesDao().files(job.id).single();assertEquals("READY",f.state);assertNotNull(f.uri)
            assertEquals(f.size to f.sha256,store.digest(android.net.Uri.parse(f.uri)))
            ZipFile(f.source).use {z->
                val all=z.entries().asSequence().toList();assertEquals(all.size,all.map{it.name}.distinct().size)
                val photos=all.filter{"FOTOGRAFIAS" in it.name.split('/')};val sketches=all.filter{"CROQUIS" in it.name.split('/')}
                assertEquals(54,photos.size);assertEquals(3,sketches.size)
                assertTrue(all.any{it.name.endsWith("CONTINUACION_FOTOGRAFICA.html")});assertTrue(all.any{it.name.endsWith("INSPECCION.json")})
                val xlsx=all.single{it.name.endsWith(ScapDocumentNames.excel(old))}
                val book=TemplateWorkbook(z.getInputStream(xlsx));assertNotNull(book.parts["xl/workbook.xml"])
                assertTrue(book.cell(1,"C4").textContent.contains("BORRADOR"))
                assertTrue(book.parts.keys.any{it.startsWith("xl/worksheets/sheet4")})
                val captions=(11..42).map{book.cell(4,"D$it").textContent}
                assertEquals(32,captions.count{it.contains("TEST_FOTO_")})
                val declared=JSONObject(z.getInputStream(all.single{it.name.endsWith("MANIFIESTO_LOCAL.json")}).bufferedReader().readText())
                assertTrue(declared.getBoolean("generationComplete"));assertTrue(declared.getJSONArray("pending").length()>0)
                assertEquals("PARENT_NOT_CONFIRMED_FOR_THIS_REVISION",declared.getString("remoteState"))
            }
            hashes.forEach{(p,h)->assertEquals(h,fileDigest(File(p)))};assertEquals(old,db.scapDao().snapshot(id))
            File(evidence,"SCAP_54_FOTOS_3_CROQUIS_FICTICIO.zip").writeBytes(File(f.source).readBytes())
        }
        Room.databaseBuilder(c,InventoryDatabase::class.java,name).build().use{db->
            assertEquals(old,db.scapDao().snapshot(id));assertEquals("READY",db.localFilesDao().job(job.id)!!.state)
            val f=db.localFilesDao().files(job.id).single();assertEquals(f.size to f.sha256,store.digest(android.net.Uri.parse(f.uri)))
            val intent=db.automaticSyncDao().get("SCAP/$id")!!;assertFalse(SyncIntentPresentation.confirmed(intent));assertEquals(0,intent.syncedRevision)
        }
    }
    @Test fun currentDateIsPersistedEditableAndHistoricalAntecedentsAreNeverInvented()=runBlocking {
        SicConsolidationFixtures.db(c).use{db->
            val r=ScapRepository(db,ScapCatalog.load(c));val id=r.create("TEST","TEST",null,ScapBridgeType.SLAB)
            val expected=LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            var s=r.dao.snapshot(id)!!;assertEquals(expected,s.values()["sic17.surveyDate"])
            assertEquals(expected,ScapToSicMapper.mapToSic17(s).fields.single{it.sicField=="surveyDate"}.value)
            r.setField(id,"inspection","lastInspection","01/01/2015");r.setField(id,"inspection","fieldClearanceDate","02/01/2016")
            r.setSicField(id,"SIC-17","surveyDate","13/09/2024");r.ensureBridgeDefaults(id)
            s=r.dao.snapshot(id)!!;assertEquals("13/09/2024",ScapToSicMapper.mapToSic17(s).fields.single{it.sicField=="surveyDate"}.value)
            r.setSicField(id,"SIC-17","surveyDate","") // explicit missing historical date stays missing
            s=r.dao.snapshot(id)!!
            assertTrue(ScapSicExporter.previews(s).single{it.format=="SIC-17"}.missingRequired.contains("Fecha"))
            assertEquals("01/01/2015",s.values()["lastInspection"]);assertEquals("02/01/2016",s.values()["fieldClearanceDate"])
            assertTrue(ScapToSicMapper.mapToSic17(s).fields.none{it.sicField=="surveyDate"})
        }
    }
    @Test fun validSurveyDateIsInContractualColumnAndDifferentSicCountsAreVerifiedNotInvented()=runBlocking {
        SicConsolidationFixtures.db(c).use{db->
            val r=SicConsolidationFixtures.repo(c,db);val a=SicConsolidationFixtures.bridge(r,"FICTICIO_A",route="PE-22A")
            val b=SicConsolidationFixtures.bridge(r,"FICTICIO_B",route="PE-22A")
            r.setSicField(a,"SIC-17","surveyDate","27/10/2026");r.setSicField(b,"SIC-17A","lanes","")
            val store=SicTestStorage()
            val first=com.tuempresa.inventariovial.export.LocalSicConsolidates.update(c,db,"PE-22A","SIC-17",store,a)
            val book=TemplateWorkbook(first.file.inputStream())
            assertTrue((14..15).any{book.cell(1,"P$it").textContent=="46322"})
            assertEquals("0010",book.cell(1,"C14").textContent)
            for((format,count) in listOf("SIC-17A" to 2,"SIC-17B" to 2)) {
                val out=com.tuempresa.inventariovial.export.LocalSicConsolidates.update(c,db,"PE-22A",format,store,a)
                assertEquals(count,out.rows);assertEquals(count,com.tuempresa.inventariovial.export.SicRouteWorkbook.inspect(out.file.readBytes(),format))
                assertEquals(out.uri,com.tuempresa.inventariovial.export.LocalSicConsolidates.update(c,db,"PE-22A",format,store,a).uri)
            }
            assertEquals(2,r.dao.allSnapshots().size);assertTrue(db.automaticSyncDao().all().isEmpty())
        }
    }
    @Test fun failedPublicWriteNeverClaimsSuccessAndSameFrozenPackageRetries()=runBlocking {
        SicConsolidationFixtures.db(c).use{db->
            val id=ScapP0Fixture.create(c,db,count=1);val s=db.scapDao().snapshot(id)!!
            val originals=s.photos.map{File(it.originalPath)}.associate{it.path to fileDigest(it)}
            val job=LocalCopies.request(c,db,"SCAP",id,s.inspection.updatedAt,"ZIP");val store=SicTestStorage().apply{shortWrite=true}
            LocalCopyDrain.run(c,db,store);assertEquals("PARTIAL",db.localFilesDao().job(job.id)!!.state)
            assertTrue(db.localFilesDao().files(job.id).none{it.state=="READY"})
            val frozen=db.localFilesDao().job(job.id)!!.manifestJson
            store.shortWrite=false;LocalCopies.retry(c,db,job.id);LocalCopyDrain.run(c,db,store)
            assertEquals("READY",db.localFilesDao().job(job.id)!!.state);assertEquals(frozen,db.localFilesDao().job(job.id)!!.manifestJson)
            originals.forEach{(p,h)->assertEquals(h,fileDigest(File(p)))};assertEquals(s,db.scapDao().snapshot(id))
            assertFalse(SyncIntentPresentation.confirmed(db.automaticSyncDao().get("SCAP/$id")))
        }
    }
    private fun receipt(p:JSONObject):JSONObject {
        val prefix="${p.getString("routeCode")}/${p.getString("familyPath")}/${p.getString("family")} 1"
        val files=p.getJSONArray("files");val rows=p.getJSONArray("sicRows")
        return JSONObject().put("ok",true).put("syncProtocol",3).put("pathVersion",4)
            .put("key",p.getString("key")).put("revision",p.getLong("revision")).put("contentHash",p.getString("contentHash"))
            .put("folderId","TEST_FOLDER").put("logicalPath",prefix)
            .put("technicalDeliveryStatus","DELIVERED").put("georeferencingStatus","PENDING_EXTERNAL").put("externalGeoreferencingStatus","PENDING")
            .put("files",JSONArray((0 until files.length()).map{n->val f=files.getJSONObject(n)
                JSONObject().put("key",f.getString("key")).put("sha256",f.getString("sha256")).put("fileId","TEST_FILE_$n").put("folderId","TEST_FOLDER")
                    .put("requestedPath",f.getString("path")).put("logicalPath",prefix+"/"+f.getString("path"))}))
            .put("sicFiles",JSONArray((0 until rows.length()).map{n->val row=rows.getJSONObject(n)
                JSONObject().put("format",row.getString("format")).put("fileId","TEST_EXCEL_$n").put("folderId","TEST_ROUTE_FOLDER").put("sha256","a".repeat(64))
                    .put("logicalPath","${p.getString("routeCode")}/EXCEL_SIC/${row.getString("format")}.xlsx").put("recordIds",JSONArray(listOf(row.getString("recordId"))))}))
    }
    @Test fun closingScapQueuesOfflineAndLostResponseReplaysOneSnapshotWithoutManualExports()=runBlocking {
        SicConsolidationFixtures.db(c).use{db->
            val id=ScapP0Fixture.create(c,db,54);val key="SCAP/$id";val original=db.scapDao().snapshot(id)!!
            val photoHashes=original.photos.associate{it.id to fileDigest(File(it.originalPath))}
            val queued=db.automaticSyncDao().get(key)!!;assertEquals("QUEUED",queued.state)
            assertEquals("AUTOMATIC",queued.origin);assertEquals("Pendiente de conexión",SyncIntentPresentation.label(queued,true,false,true))
            val bodies=mutableListOf<String>();val committed=mutableSetOf<String>();var lost=true
            val transport=object:AutomaticDriveTransport {
                override fun supportsV3()=true
                override fun supportsFamilyPaths()=true
                override fun supportsTechnicalDeliveryWithoutGps()=true
                override fun supportsReadablePhotoNames()=true
                override fun reservePhotoNames(payload:JSONObject)=ReleasePhotoV2Fixture.reservation(payload,"TEST_FOLDER").also {ack->
                    val photos=ack.getJSONArray("photos")
                    for(n in 0 until photos.length()){
                        val photo=photos.getJSONObject(n);val cat=ScapPhotoPanel.forCode(photo.getString("category"))!!
                        photo.put("name",cat.fileName(n+1,"jpg",1))
                    }
                }
                override fun post(payload:JSONObject):JSONObject {
                    bodies+=payload.toString();committed+=payload.getString("key")+"/"+payload.getLong("revision")
                    if(lost){lost=false;throw SyncCommunicationFailure(IOException("TEST_LOST_RESPONSE"))}
                    return receipt(payload)
                }
            }
            assertTrue(AutomaticSyncDrain.run(c,db,{transport},configurationError=null))
            val interrupted=db.automaticSyncDao().get(key)!!;val frozen=interrupted.preparedJson
            assertNotNull(frozen);assertEquals(0,interrupted.syncedRevision);assertFalse(SyncIntentPresentation.confirmed(interrupted))
            AutomaticSyncDrain.run(c,db,{transport},configurationError=null)
            val done=db.automaticSyncDao().get(key)!!
            assertTrue(done.error.orEmpty(),SyncIntentPresentation.confirmed(done));assertEquals(done.revision,done.syncedRevision)
            assertEquals(2,bodies.size);assertEquals(bodies[0],bodies[1]);assertEquals(1,committed.size)
            val after=db.scapDao().snapshot(id)!!;assertEquals(original.photos.map{it.id}.toSet(),after.photos.map{it.id}.toSet())
            after.photos.forEach{assertEquals(photoHashes.getValue(it.id),fileDigest(File(it.originalPath)))}
            assertEquals(original.sketches,after.sketches)
            assertTrue(db.localFilesDao().jobs().none{it.scope=="ZIP"});assertTrue(db.localFilesDao().jobs().none{it.kind=="SIC"})
        }
    }
    @Test fun onlyDeviceClosureDoesNotEnrollDriveAndLocationUsesRealAndroidUris()=runBlocking {
        SicConsolidationFixtures.db(c).use{db->
            val id=ScapP0Fixture.create(c,db,0,onlyDevice=true)
            assertNull(db.automaticSyncDao().get("SCAP/$id"));assertEquals("COMPLETE",db.scapDao().inspection(id)!!.status)
        }
        assertEquals(android.app.DownloadManager.ACTION_VIEW_DOWNLOADS,SharedFileActions.location(android.net.Uri.parse("content://media/external/downloads/1"))!!.action)
        val tree=android.net.Uri.parse("content://com.android.externalstorage.documents/tree/primary%3ATEST/document/primary%3ATEST%2Fsub%2Fdoc.zip")
        val intent=SharedFileActions.location(tree)!!
        assertEquals("vnd.android.document/directory",intent.type)
        assertEquals("primary:TEST",android.provider.DocumentsContract.getDocumentId(intent.data!!))
        assertNull(SharedFileActions.location(android.net.Uri.parse("file:///data/private/doc.zip")))
    }
    @Test fun remoteBundleKeepsAllFiftyFourOriginalsWhileWorkbookHasThirtyTwoAndProtocolIsUnchanged()=runBlocking {
        SicConsolidationFixtures.db(c).use{db->
            val id=ScapP0Fixture.create(c,db);val s=db.scapDao().snapshot(id)!!
            ScapDriveSync.reviewForUpload(s,ScapCatalog.load(c).forSnapshot(s),true)
            val hashes=s.photos.associate{it.id to fileDigest(File(it.originalPath))}
            val bundle=ScapDelivery.bundle(c,s)
            ZipFile(bundle).use{z->
                val all=z.entries().asSequence().toList();assertEquals(54,all.count{it.name.startsWith("fotos/")});assertEquals(3,all.count{it.name.startsWith("croquis/")})
                val manifest=JSONObject(z.getInputStream(z.getEntry("manifest.json")).bufferedReader().readText())
                assertEquals(1,manifest.getInt("protocol"));assertEquals(s.photos.size,manifest.getJSONObject("fieldCapture").getJSONArray("photos").length())
                val book=TemplateWorkbook(z.getInputStream(z.getEntry("SCAP.xlsx")))
                assertEquals(32,(11..42).count{book.cell(4,"D$it").textContent.contains("TEST_FOTO_")})
                for(p in s.photos)assertEquals(hashes.getValue(p.id),streamDigest(z.getInputStream(z.getEntry(ScapPhotoPanel.photoPath(s,p)))))
            }
            assertEquals(s,db.scapDao().snapshot(id));assertFalse(SyncIntentPresentation.confirmed(db.automaticSyncDao().get("SCAP/$id")))
        }
    }
}
