package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.core.*
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.export.*
import com.tuempresa.inventariovial.localfiles.*
import com.tuempresa.inventariovial.pavement.*
import com.tuempresa.inventariovial.scap.catalog.ScapCatalog
import com.tuempresa.inventariovial.scap.data.*
import com.tuempresa.inventariovial.scap.domain.*
import com.tuempresa.inventariovial.scap.export.*
import kotlinx.coroutines.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
import org.robolectric.shadows.ShadowStatFs
import java.io.*
import java.util.zip.ZipFile

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=Application::class,shadows=[SyntheticPdf146::class])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AutomaticDocuments149Test {
    private val c get()=ApplicationProvider.getApplicationContext<Context>()
    private val examples get()=File("../output/GBO_RUTA_CLARA_CANDIDATA_1.4.9_23/ejemplos-ficticios").apply{mkdirs()}
    @Before fun space(){ShadowStatFs.registerStats(c.filesDir.path,1_000_000,900_000,900_000)}
    private fun db(auto:Boolean=true)=Room.inMemoryDatabaseBuilder(c,InventoryDatabase::class.java).addCallback(object:RoomDatabase.Callback(){override fun onOpen(db:SupportSQLiteDatabase){AutomaticLocalDocuments.install(db,auto)}}).build()
    private fun sample(id:String="TEST_PAV_149",rev:Long=1)=PavementSampleEntity(id,"TEST_TRAMO","PE-3N",1,"Derecho","CD","00+000","00+020",10.0,20.0,"RIGID","2026-10-16","TEST_USER","FICTICIO",updatedAt=rev,slabRows=4,slabColumns=2)
    private fun image()=File(c.filesDir,"TEST_${java.util.UUID.randomUUID()}.jpg").apply{val b=Bitmap.createBitmap(24,18,Bitmap.Config.ARGB_8888);outputStream().use{b.compress(Bitmap.CompressFormat.JPEG,95,it)};b.recycle()}
    private suspend fun bridge(d:InventoryDatabase):String {val r=ScapRepository(d,ScapCatalog.load(c));val id=r.create("TEST_OPERATOR","TEST_DEVICE",null,ScapBridgeType.MODULAR)
        r.setField(id,"inspection","route","PE-3N");r.setField(id,"inspection","singularityName","TEST_LOCAL_149")
        listOf("ELEVATION","ELEVATION","PLAN","PLAN","CROSS_SECTION").forEach{r.addSketch(id,it,image().path)}
        r.addPhoto(id,image().path,ScapRepository.PHOTO_CATEGORIES.first(),null);return id
    }
    @Test fun defaultIsEnabledButExplicitOptOutIsPreserved(){
        c.getSharedPreferences("gbo_local_copies",0).edit().clear().commit();assertTrue(LocalCopySettings.enabled(c))
        c.getSharedPreferences("gbo_local_copies",0).edit().putBoolean("automatic",false).commit();assertFalse(LocalCopySettings.enabled(c))
    }
    @Test fun rollbackCannotCreateRequestAndCommittedDraftDoes()=runBlocking{db().use{d->
        assertTrue(runCatching{d.withTransaction{d.pavementDao().saveSample(sample());error("TEST_ROLLBACK")}}.isFailure)
        assertNull(d.pavementDao().sample(sample().id));assertTrue(d.localFilesDao().jobs().isEmpty())
        d.pavementDao().saveSample(sample());val job=d.localFilesDao().jobs().single();assertEquals("QUEUED",job.state);assertEquals(1,job.revision);assertTrue(d.automaticSyncDao().all().isEmpty())
    }}
    @Test fun nearbyEditsCoalesceWithoutDroppingPublishedRevisions()=runBlocking{db().use{d->
        d.pavementDao().saveSample(sample());val first=d.localFilesDao().jobs().single();d.localFilesDao().saveJob(first.copy(state="READY"))
        for(n in 2L..30L)d.pavementDao().saveSample(sample(rev=n))
        val jobs=d.localFilesDao().jobs();assertEquals(1,jobs.count{it.state=="QUEUED"});assertEquals(30,jobs.single{it.state=="QUEUED"}.revision);assertEquals("READY",d.localFilesDao().job(first.id)!!.state)
        assertTrue(d.automaticSyncDao().all().isEmpty())
    }}
    @Test fun noHistoryScanAndOptOutDoesNotChangeDrive()=runBlocking{db(false).use{d->
        d.pavementDao().saveSample(sample());AutomaticLocalDocuments.install(d.openHelper.writableDatabase,true)
        assertTrue(d.localFilesDao().jobs().isEmpty());d.pavementDao().saveSample(sample(rev=2));assertEquals(2,d.localFilesDao().jobs().single().revision)
        AutomaticLocalDocuments.install(d.openHelper.writableDatabase,false);d.pavementDao().saveSample(sample(rev=3));assertEquals(1,d.localFilesDao().jobs().size);assertTrue(d.automaticSyncDao().all().isEmpty())
    }}
    @Test fun pendingRequestSurvivesReopenWithoutAProcessObserver()=runBlocking{
        val name="auto149.db";c.deleteDatabase(name)
        fun open()=Room.databaseBuilder(c,InventoryDatabase::class.java,name).addCallback(object:RoomDatabase.Callback(){override fun onOpen(db:SupportSQLiteDatabase){AutomaticLocalDocuments.install(db,true)}}).build()
        open().use{it.pavementDao().saveSample(sample())};open().use{assertEquals("QUEUED",it.localFilesDao().jobs().single().state);assertEquals(20,it.openHelper.writableDatabase.version)}
    }
    @Test fun incompleteBridgeCreatesThreeRealXlsxWithPendingAndLiteralValues()=runBlocking{db().use{d->
        val id=bridge(d);val r=ScapRepository(d,ScapCatalog.load(c));r.setSicField(id,"SIC-17B","comments","=1+2 TEST_LITERAL")
        val s=d.scapDao().snapshot(id)!!;val before=ScapFieldBackup.capture(s).toString()
        for(format in BridgeSicContract.formats){val f=File(examples,"${format}_PROVISIONAL_SOLO_FICTICIO.xlsx");f.outputStream().use{ProvisionalSicExcel.write(it,s,format)}
            ZipFile(f).use{z->val book=z.getInputStream(z.getEntry("xl/workbook.xml")).reader().readText();assertTrue(book.contains("PENDIENTES"))
                val sheet=z.getInputStream(z.getEntry("xl/worksheets/sheet1.xml")).reader().readText();assertTrue(sheet.contains(ProvisionalSicExcel.MARKER));assertFalse(sheet.contains("<f>"))
                if(format=="SIC-17B")assertTrue(sheet.contains("=1+2 TEST_LITERAL"))
                val pending=z.getInputStream(z.getEntry("xl/worksheets/sheet2.xml")).reader().readText();assertTrue(pending.contains(id));assertTrue(pending.contains("TEST_OPERATOR"))
            }
            assertTrue(runCatching{ScapSicExporter.write(ByteArrayOutputStream(),s,format)}.isFailure)
        }
        assertEquals(before,ScapFieldBackup.capture(d.scapDao().snapshot(id)!!).toString());assertTrue(d.automaticSyncDao().all().isEmpty())
    }}
    @Test fun scapWithoutPrStillBuildsAllSketchesAndOriginalsOffline()=runBlocking{db().use{d->
        val id=bridge(d);val s=d.scapDao().snapshot(id)!!;assertTrue(ScapDriveLocation.issues(c,s).any{it.contains("PR")})
        val job=d.localFilesDao().jobs().last{it.recordId==id&&it.state=="QUEUED"};val plan=LocalExportBuilder.build(c,d,job)
        assertTrue(plan.problems.joinToString(),plan.problems.isEmpty());assertEquals(5,plan.files.count{it.relativePath.contains("CROQUIS")})
        assertEquals(3,plan.files.count{it.name.startsWith("SIC-")&&it.name.endsWith("PROVISIONAL.xlsx")});assertTrue(plan.files.any{it.name=="SCAP_PROVISIONAL.xlsx"})
        for(sk in s.sketches)assertTrue(plan.files.any{it.sha256==fileDigest(File(sk.localUri)).second})
        assertTrue(plan.files.all{it.relativePath.startsWith("Download/GBO_Ruta_Clara/PE-3N/PUENTES/")})
        val manifest=JSONObject(File(plan.files.single{it.name=="MANIFIESTO_LOCAL.json"}.source).readText());assertFalse(manifest.getBoolean("roomRestorable"));assertEquals("PROVISIONAL_UUID",manifest.getJSONObject("location").getString("identityStatus"))
    }}
    @Test fun incompleteLegacySicKeepsBlankPrAndCapturedObservations()=runBlocking{db().use{d->
        val record=testRecord("TEST_SIC_149",pr="",status="DRAFT",route="PE-3N",sic="SIC-18").copy(observations="=1+2 TEST_OBSERVACION")
        d.inventoryDao().insertRecord(record);val s=d.inventoryDao().snapshot(record.id)!!
        val job=d.localFilesDao().jobs().single();val plan=LocalExportBuilder.build(c,d,job);assertTrue(plan.problems.joinToString(),plan.problems.isEmpty())
        val file=plan.files.single{it.name.endsWith("PROVISIONAL.xlsx")};ZipFile(file.source).use{z->val text=z.getInputStream(z.getEntry("xl/worksheets/sheet1.xml")).reader().readText();assertTrue(text.contains("=1+2 TEST_OBSERVACION"));assertFalse(text.contains("<f>"));assertFalse(text.contains("PR 0000"))
            val workbook=TemplateWorkbook(File(file.source).inputStream());val header=workbook.sheet(1).nodes("c").single{it.getAttribute("r").endsWith("13")&&it.textContent=="OBSERVACIONES"}
            assertEquals("",header.getAttribute("s"));val column=header.getAttribute("r").takeWhile(Char::isLetter);assertEquals("",workbook.cell(1,column+"14").getAttribute("s"))
        }
        assertEquals(s,d.inventoryDao().snapshot(record.id));assertTrue(d.automaticSyncDao().all().isEmpty())
    }}
    @Test fun draftPavementDocumentsPreservePciGeometryAndNoRemoteWork()=runBlocking{db().use{d->
        val s=sample();d.pavementDao().saveSample(s);val before=PavementCalculator(c).calculate(s,emptyList());val job=d.localFilesDao().jobs().single()
        val plan=LocalExportBuilder.build(c,d,job);assertTrue(plan.problems.joinToString(),plan.problems.isEmpty());assertTrue(plan.files.any{it.name=="Inspeccion_BORRADOR.pdf"});assertTrue(plan.files.any{it.name=="Datos.xlsx"});assertTrue(plan.files.any{it.name=="Muestra.json"})
        assertEquals(before,PavementCalculator(c).calculate(d.pavementDao().sample(s.id)!!,emptyList()));assertEquals(s,d.pavementDao().sample(s.id));assertTrue(d.automaticSyncDao().all().isEmpty())
    }}
    @Test fun confirmedAckNamesNewCopiesButDoesNotMoveAnEarlierPlan()=runBlocking{db().use{d->
        val s=sample();d.pavementDao().saveSample(s);val job=d.localFilesDao().jobs().single();val before=LocalExportBuilder.build(c,d,job)
        val path="PE-3N/EVALUACION_SUPERFICIAL/TEST_TRAMO/RIGIDO/MUESTRA 42"
        val ack=JSONObject().put("ok",true).put("key",job.recordKey).put("revision",1).put("logicalPath",path).put("folderId","TEST_FOLDER")
        d.automaticSyncDao().put(DriveSyncIntent(job.recordKey,"PAVEMENT",s.id,1,1,"SYNCED",lastSyncedAt=1,receiptJson=ack.toString()))
        d.pavementDao().saveSample(s.copy(updatedAt=2));val next=d.localFilesDao().jobs().single{it.revision==2L};val after=LocalExportBuilder.build(c,d,next)
        assertTrue(before.files.all{it.relativePath.contains("UUID")});assertTrue(after.files.all{it.relativePath.contains("MUESTRA_42")});assertTrue(before.files.all{File(it.source).exists()})
    }}
    @Test fun inventoryUnifiesAndDeduplicatesBridgeParentAndSortsAbdc()=runBlocking{db(false).use{d->
        val id=bridge(d);val b=d.scapDao().inspection(id)!!;val parent=d.inventoryDao().recordById(b.roadRecordId)!!
        val records=listOf("A" to 100.0,"B" to 200.0,"C" to 400.0,"D" to 300.0).map{(id,pos)->InventoryRecordWithPhotos(testRecord(id,route="PE-3N").copy(axisMeasureM=pos),emptyList())}+InventoryRecordWithPhotos(parent,emptyList())
        val list=LocalInventoryIndex.project(records,listOf(b),listOf(sample()))
        assertEquals(listOf("A","B","D","C"),list.filter{it.kind=="SIC"}.map{it.id});assertEquals(1,list.count{it.kind=="SCAP"});assertEquals(1,list.count{it.kind=="PAVEMENT"});assertFalse(list.any{it.kind=="SIC"&&it.id==parent.id})
    }}
    private class Store:SharedDocumentStore {
        val entries=linkedMapOf<String,ByteArray>();var writes=0;var stop=false
        override fun reserve(file:LocalSharedFile,knownUri:Uri?):SharedDocumentStore.Slot {val uri=Uri.parse("content://test/"+android.net.Uri.encode(file.relativePath+file.name));return SharedDocumentStore.Slot(uri,entries.containsKey(uri.toString()))}
        override fun write(uri:Uri,source:File){entries[uri.toString()]=source.readBytes();writes++}
        override fun digest(uri:Uri)=entries.getValue(uri.toString()).let{it.size.toLong() to com.tuempresa.inventariovial.delivery.sha256(it)}
        override fun publish(slot:SharedDocumentStore.Slot,file:LocalSharedFile):Uri{if(stop){stop=false;throw CancellationException("TEST_STOP_AFTER_WRITE")};return slot.uri}
    }
    @Test fun interruptedLocalPublicationResumesWithoutDuplicatingMediaWhenDriveIsBusy()=runBlocking{db().use{d->
        val id=bridge(d);val job=d.localFilesDao().jobs().single{it.recordId==id&&it.state=="QUEUED"};d.localFilesDao().saveJob(job.copy(createdAt=0));val store=Store()
        d.automaticSyncDao().put(DriveSyncIntent(job.recordKey,"SCAP",id,job.revision,state="ERROR",error="BUSY"))
        store.stop=true;assertTrue(runCatching{LocalCopyDrain.run(c,d,store,100)}.isFailure);LocalCopyDrain.run(c,d,store,100)
        assertEquals("READY",d.localFilesDao().job(job.id)!!.state);val files=d.localFilesDao().files(job.id);assertEquals(files.size,store.entries.size)
        val writes=store.writes;val repo=ScapRepository(d,ScapCatalog.load(c));repo.setField(id,"G2","observations","TEST_NEXT")
        val next=d.localFilesDao().jobs().single{it.recordId==id&&it.state=="QUEUED"};d.localFilesDao().saveJob(next.copy(createdAt=0));LocalCopyDrain.run(c,d,store,100)
        assertEquals("READY",d.localFilesDao().job(next.id)!!.state);val media=store.entries.keys.count{it.contains("FOTOGRAFIAS")||it.contains("CROQUIS")};assertEquals(6,media)
        assertEquals("ERROR",d.automaticSyncDao().get(job.recordKey)!!.state);assertTrue(store.writes>writes)
    }}
    @Test fun validPrAllowsDrivePreparationWithCabinetPendingWithoutProvisionalRows()=runBlocking{db(false).use{d->
        val id=bridge(d);val repo=ScapRepository(d,ScapCatalog.load(c));repo.saveSicLocation(id,"PE-3N","CD","0010","3.5");repo.complete(id,true)
        val snapshot=d.scapDao().snapshot(id)!!;assertTrue(ScapDriveLocation.issues(c,snapshot).toString(),ScapDriveLocation.issues(c,snapshot).isEmpty())
        assertEquals("PE-3N",ScapHumanDelivery.prepare(c,d,snapshot).routeCode)
        val i=d.automaticSyncDao().get("SCAP/$id")!!;val payload=AutomaticDelivery.prepare(c,d,i,readable=false)
        assertEquals(0,payload.getJSONArray("sicRows").length());assertTrue(payload.getJSONArray("files").length()>5)
    }}
    @Test fun newlyCreatedPavementWithoutDimensionsStillCreatesReadableDraftDocuments()=runBlocking{db().use{d->
        val s=PavementHeader.newSample("TEST_EMPTY_149",PavementSurface.RIGID,"TEST_USER","FICTICIO",emptyList())
        d.pavementDao().saveSample(s);val plan=LocalExportBuilder.build(c,d,d.localFilesDao().jobs().single())
        assertTrue(plan.problems.joinToString(),plan.problems.isEmpty());assertTrue(plan.files.any{it.name=="Inspeccion_BORRADOR.pdf"});assertTrue(plan.files.any{it.name=="Datos.xlsx"})
        val json=JSONObject(File(plan.files.single{it.name=="Muestra.json"}.source).readText());assertTrue(json.toString().contains("Dimensiones"))
    }}
    @Test fun videoChangeOutboxIsDurableIdempotentAndDoesNotAlterParentOrDrive()=runBlocking{
        c.getSharedPreferences("gbo_local_copies",0).edit().putBoolean("automatic",true).commit()
        val d=InventoryDatabase.getInstance(c);val id=bridge(d);val before=d.scapDao().snapshot(id)!!;val remote=d.automaticSyncDao().all()
        for(job in d.localFilesDao().jobs().filter{it.recordId==id&&it.state=="QUEUED"})d.localFilesDao().saveJob(job.copy(state="READY"))
        val videos=com.tuempresa.inventariovial.media.VideoDatabase.get(c);val f=File(c.filesDir,"TEST_LOCAL_149.mp4").apply{writeBytes(byteArrayOf(0,0,0,16,102,116,121,112))}
        val hash=fileDigest(f);videos.videos().put(com.tuempresa.inventariovial.media.VideoEntity("TEST_VIDEO_149","SCAP/$id","UPSTREAM","Aguas arriba",f.path,"video/mp4",hash.first,hash.second,System.currentTimeMillis()))
        videos.openHelper.writableDatabase.query("SELECT count(*) FROM gbo_local_video_changes").use{assertTrue(it.moveToFirst());assertEquals(1,it.getInt(0))}
        VideoDocumentOutbox.transfer(c);val jobs=d.localFilesDao().jobs().filter{it.recordId==id&&it.scope.startsWith("FULL_MEDIA_")};assertEquals(1,jobs.size)
        VideoDocumentOutbox.transfer(c);assertEquals(jobs,d.localFilesDao().jobs().filter{it.recordId==id&&it.scope.startsWith("FULL_MEDIA_")})
        videos.videos().put(com.tuempresa.inventariovial.media.VideoEntity("TEST_VIDEO_149_B","SCAP/$id","DOWNSTREAM","Aguas abajo",f.path,"video/mp4",hash.first,hash.second,System.currentTimeMillis()))
        VideoDocumentOutbox.transfer(c);assertEquals("SUPERSEDED",d.localFilesDao().job(jobs.single().id)!!.state)
        val latest=d.localFilesDao().jobs().single{it.recordId==id&&it.scope.startsWith("FULL_MEDIA_")&&it.state=="QUEUED"}
        val plan=LocalExportBuilder.build(c,d,latest);assertTrue(plan.problems.joinToString(),plan.problems.isEmpty());assertEquals(2,plan.files.count{it.mimeType=="video/mp4"&&it.sha256==hash.second})
        assertEquals(before,d.scapDao().snapshot(id));assertEquals(remote,d.automaticSyncDao().all());assertEquals(hash,fileDigest(f))
    }

    @Test fun openingAnUpToDateHistoricalDocumentDoesNotRegenerateIt()=runBlocking{db().use{d->
        val s=sample();d.pavementDao().saveSample(s);val job=d.localFilesDao().jobs().single();d.localFilesDao().saveJob(job.copy(state="READY"))
        LocalCopies.requestCurrent(c,d,"PAVEMENT",s.id,automatic=true)
        assertEquals("READY",d.localFilesDao().job(job.id)!!.state);assertEquals(1,d.localFilesDao().jobs().size)
    }}

    @Test fun automaticCopyRetainsLegacySicAppendicesAndProvisionalSupplements()=runBlocking{db(false).use{d->
        val r=testRecord("TEST_LENGTH_149",route="PE-3N").copy(observations="TEST_NOTES")
        d.inventoryDao().insertRecord(r);d.inventoryDao().insertSic19(Sic19Entity(r.id,"08","2","1","1","1"))
        val first=LocalExportBuilder.build(c,d,LocalExportJob("TEST_LENGTH_JOB","SIC/${r.id}","SIC",r.id,r.updatedAt))
        assertTrue(first.problems.joinToString(),first.problems.isEmpty());assertTrue(first.files.any{it.name=="LONGITUD.xlsx"});assertTrue(first.files.any{it.name=="OBSERVACIONES.html"});assertTrue(first.files.any{it.name=="SIC-19.xlsx"})
        val culvert=testRecord("TEST_SUPPLEMENT_149",sic="SIC-18",route="PE-3N",status="DRAFT");d.inventoryDao().insertRecord(culvert)
        d.inventoryDao().insertSic18(Sic18Entity(culvert.id,"06","1",null,"1",null,null,"3","3"));d.inventoryDao().saveSic18A(Sic18AEntity(culvert.id,failureLocationCode="TEST_RAW"))
        val plan=LocalExportBuilder.build(c,d,LocalExportJob("TEST_18A_JOB","SIC/${culvert.id}","SIC",culvert.id,culvert.updatedAt))
        assertTrue(plan.problems.joinToString(),plan.problems.isEmpty());val f=plan.files.single{it.name=="SIC-18A_PROVISIONAL.xlsx"}
        ZipFile(f.source).use{z->val main=z.getInputStream(z.getEntry("xl/worksheets/sheet1.xml")).reader().readText();assertTrue(main.contains("TEST_RAW"));assertFalse(main.contains("OBSERVACIONES"))}
        val bridge=testRecord("TEST_MANUAL_BRIDGE_149",sic="SIC-17",route="PE-3N",status="DRAFT");d.inventoryDao().insertRecord(bridge)
        val bridgePlan=LocalExportBuilder.build(c,d,LocalExportJob("TEST_MANUAL_BRIDGE_JOB","SIC/${bridge.id}","SIC",bridge.id,bridge.updatedAt))
        assertTrue(bridgePlan.problems.joinToString(),bridgePlan.problems.isEmpty());assertEquals(3,bridgePlan.files.count{it.name.startsWith("SIC-17")&&it.name.endsWith("PROVISIONAL.xlsx")})
    }}

}
