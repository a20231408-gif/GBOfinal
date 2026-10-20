package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.scap.catalog.ScapCatalog
import com.tuempresa.inventariovial.scap.data.*
import com.tuempresa.inventariovial.scap.domain.*
import com.tuempresa.inventariovial.scap.export.*
import com.tuempresa.inventariovial.scap.ui.ScapController
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.ZipFile

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],application=Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScapUrgentIntegrityTest {
    private val c get()=ApplicationProvider.getApplicationContext<Context>()
    private val evidence get()=File("../tmp/scap-urgente-20261016/ensayo-ficticio").apply{mkdirs()}
    private fun db()=Room.inMemoryDatabaseBuilder(c,InventoryDatabase::class.java).build()
    private fun repo(d:InventoryDatabase)=ScapRepository(d,ScapCatalog.load(c))
    private fun image():File=File(c.filesDir,"TEST_URGENT_${java.util.UUID.randomUUID()}.jpg").apply{
        val b=Bitmap.createBitmap(24,18,Bitmap.Config.ARGB_8888);outputStream().use{b.compress(Bitmap.CompressFormat.JPEG,95,it)};b.recycle()
    }
    private suspend fun five(r:ScapRepository):String {
        val id=r.create("TEST_OPERATOR","TEST_DEVICE",null,ScapBridgeType.MODULAR)
        r.setField(id,"inspection","route","PE-3N");r.setField(id,"inspection","singularityName","TEST_SCAP_URGENT")
        listOf("ELEVATION","ELEVATION","PLAN","PLAN","CROSS_SECTION").forEach{r.addSketch(id,it,image().path)}
        return id
    }
    @Test fun fiveSketchesAreWarningsAndAllReachTheBundleWithoutDuplicates()=runBlocking{db().use{d->
        val r=repo(d);val id=five(r);repeat(19){r.addPhoto(id,image().path,ScapRepository.PHOTO_CATEGORIES.first(),null)}
        assertTrue(runCatching{r.complete(id)}.isFailure);r.complete(id,true)
        val s=r.dao.snapshot(id)!!;val originals=(s.sketches.map{it.localUri}+s.photos.map{it.localPath}).associateWith{ScapDelivery.sha256(File(it))}
        val review=ScapDelivery.exporter(c,s).review(s);assertTrue(review.errors.joinToString(),review.errors.isEmpty())
        assertTrue(review.warnings.any{it.contains("#REF!")});assertEquals(2,ScapSketchSelection.warnings(s).size)
        val bundle=ScapDelivery.bundle(c,s);val hash=ScapDelivery.sha256(bundle);bundle.copyTo(File(evidence,"SCAP_19_FOTOS_5_CROQUIS_SOLO_FICTICIO.zip"),overwrite=true)
        ZipFile(bundle).use{z->val names=z.entries().asSequence().map{it.name}.toList();assertEquals(5,names.count{it.startsWith("croquis/")});assertEquals(19,names.count{it.startsWith("fotos/")});assertEquals(names.size,names.toSet().size)
            val manifest=JSONObject(z.getInputStream(z.getEntry("manifest.json")).reader().readText());assertEquals("INCOMPLETE",manifest.getString("completionStatus"));assertTrue(manifest.getBoolean("uploadedWithWarnings"));assertEquals(5,manifest.getJSONObject("fieldCapture").getJSONArray("sketches").length());assertTrue(manifest.getJSONArray("pending").length()>0)
        }
        assertEquals(hash,ScapDelivery.sha256(ScapDelivery.bundle(c,s)));originals.forEach{(p,h)->assertEquals(h,ScapDelivery.sha256(File(p)))}
    }}
    @Test fun explicitPrincipalChangesOnlyANewRevisionAndLeavesAllAnnexes()=runBlocking{db().use{d->
        val r=repo(d);val id=five(r);val s=r.dao.snapshot(id)!!;assertNull(ScapSketchSelection.principal(s,"ELEVATION"))
        val pick=s.sketches.last{it.type=="ELEVATION"};r.selectPrincipalSketch(id,pick.type,pick.id)
        val chosen=r.dao.snapshot(id)!!;assertEquals(pick.id,ScapSketchSelection.principal(chosen,pick.type)!!.id);assertTrue(chosen.inspection.updatedAt>s.inspection.updatedAt);assertEquals(s.sketches,chosen.sketches)
        r.complete(id,true);assertTrue(runCatching{r.selectPrincipalSketch(id,pick.type,pick.id)}.isFailure)
        r.reopen(id);r.selectPrincipalSketch(id,pick.type,null);val reopened=r.dao.snapshot(id)!!;assertNull(ScapSketchSelection.principal(reopened,pick.type));assertEquals(5,reopened.sketches.size)
        assertTrue(runCatching{r.selectPrincipalSketch(id,"PLAN",pick.id)}.isFailure)
    }}
    @Test fun onlyExplicitPrincipalOccupiesTheOriginalFrame()=runBlocking{db().use{d->
        val r=repo(d);val id=five(r);var s=r.dao.snapshot(id)!!
        fun book(s:ScapInspectionSnapshot):TemplateWorkbook{val bytes=ByteArrayOutputStream();ScapDelivery.exporter(c,s).write(bytes,s){File(it).readBytes()};return TemplateWorkbook(bytes.toByteArray().inputStream())}
        val unselected=book(s);assertFalse(unselected.parts.containsKey("xl/media/scap_2_1.jpg"));assertFalse(unselected.parts.containsKey("xl/media/scap_2_2.jpg"))
        r.selectPrincipalSketch(id,"ELEVATION",s.sketches.last{it.type=="ELEVATION"}.id);s=r.dao.snapshot(id)!!;val selected=book(s)
        assertTrue(selected.parts.containsKey("xl/media/scap_2_1.jpg"));assertFalse(selected.parts.containsKey("xl/media/scap_2_2.jpg"))
        val source=TemplateWorkbook(c.assets.open(ScapBridgeType.MODULAR.templateAsset));assertArrayEquals(source.parts.getValue("xl/styles.xml"),selected.parts.getValue("xl/styles.xml"));assertEquals(source.sheet(2).nodes("mergeCell").map{it.getAttribute("ref")},selected.sheet(2).nodes("mergeCell").map{it.getAttribute("ref")})
    }}
    @Test fun provisionalThreeSicFormatsKeepUnknownsAndNeverProduceContractualXlsx()=runBlocking{db().use{d->
        val r=repo(d);val id=five(r);val s=r.dao.snapshot(id)!!;val before=d.automaticSyncDao().all()
        for(format in BridgeSicContract.formats){val preview=ScapSicExporter.previews(s).single{it.format==format};assertTrue(preview.missingRequired.isNotEmpty());assertTrue(runCatching{ScapSicExporter.write(ByteArrayOutputStream(),s,format)}.isFailure)
            val f=File(c.filesDir,"TEST_${format}_PROVISIONAL.zip");ScapFieldBackup.write(c,s,format,f);f.copyTo(File(evidence,"${format}_PROVISIONAL_SOLO_FICTICIO.zip"),overwrite=true)
            ZipFile(f).use{z->assertFalse(z.entries().asSequence().any{it.name.endsWith("xlsx")});val j=JSONObject(z.getInputStream(z.getEntry("MANIFIESTO_PROVISIONAL.json")).reader().readText());assertFalse(j.getBoolean("contractual"));assertFalse(j.getBoolean("complete"));assertEquals(s.inspection.id,j.getString("inspectionId"));assertEquals(s.inspection.updatedAt,j.getLong("revision"));assertTrue(j.getJSONArray("pending").length()>0)
                val html=z.getInputStream(z.getEntry("ABRIR_PROVISIONAL.html")).reader().readText();assertTrue(html.contains(ScapFieldBackup.STATUS));assertTrue(html.contains("PENDIENTE"));assertEquals(5,z.entries().asSequence().count{it.name.startsWith("CROQUIS/")})
            }
        };assertEquals(s,r.dao.snapshot(id));assertEquals(before,d.automaticSyncDao().all())
    }}
    @Test fun unknownProgressiveHasLocalBackupButNoInventedTechnicalDelivery()=runBlocking{db().use{d->
        val r=repo(d);val id=five(r);r.complete(id,true);val s=r.dao.snapshot(id)!!
        val failure=runCatching{ScapHumanDelivery.prepare(c,d,s)}.exceptionOrNull();assertNotNull(failure);assertTrue(failure!!.message!!.contains("PR"))
        val f=File(c.filesDir,"TEST_NO_PR_PROVISIONAL.zip");ScapFieldBackup.write(c,s,"SIC-17",f);assertTrue(f.length()>0);assertTrue(s.roadRecord!!.startPrCode.isBlank());assertEquals(s,r.dao.snapshot(id))
    }}
    @Test fun corruptAnnexIsSpecificAndProvisionalListsItsExactIdentity()=runBlocking{db().use{d->
        val r=repo(d);val id=five(r);val s=r.dao.snapshot(id)!!;val bad=s.sketches.first();File(bad.localUri).writeText("TEST_BAD_IMAGE")
        val error=runCatching{ScapDelivery.bundle(c,s)}.exceptionOrNull();assertNotNull(error);assertTrue(error!!.message!!.contains(bad.id));assertTrue(File(bad.localUri).isFile)
        val f=File(c.filesDir,"TEST_PARTIAL_PROVISIONAL.zip");ScapFieldBackup.write(c,s,"SIC-17",f);ZipFile(f).use{z->val j=JSONObject(z.getInputStream(z.getEntry("MANIFIESTO_PROVISIONAL.json")).reader().readText());assertFalse(j.getBoolean("mediaVerified"));assertTrue(j.getJSONArray("missingFiles").toString().contains(bad.id));assertEquals(4,z.entries().asSequence().count{it.name.startsWith("CROQUIS/")})}
    }}
    @Test fun rawSectionsMediaAndRevisionSurviveDatabaseReopenOffline()=runBlocking{
        val name="urgent.db";c.deleteDatabase(name);var d=Room.databaseBuilder(c,InventoryDatabase::class.java,name).build()
        try {var r=repo(d);val id=five(r);val values=mapOf("bridgeCode" to "TEST_A","totalLengthM" to "42.5","soil" to "TEST_D","observations" to "TEST_G_OFFLINE")
            values.forEach{(k,v)->r.setField(id,if(k=="observations")"G2" else "inspection",k,v)};r.addPhoto(id,image().path,ScapRepository.PHOTO_CATEGORIES.first(),null);r.saveDraft(id)
            val before=r.dao.snapshot(id)!!;val raw=ScapFieldBackup.capture(before).toString();val hashes=(before.photos.map{it.localPath}+before.sketches.map{it.localUri}).associateWith{ScapDelivery.sha256(File(it))}
            d.close();d=Room.databaseBuilder(c,InventoryDatabase::class.java,name).build();r=repo(d);val after=r.dao.snapshot(id)!!;File(evidence,"ANTES_REAPERTURA_SOLO_FICTICIO.json").writeText(raw);File(evidence,"DESPUES_REAPERTURA_SOLO_FICTICIO.json").writeText(ScapFieldBackup.capture(after).toString());File(evidence,"HASHES_ORIGINALES_SOLO_FICTICIO.json").writeText(JSONObject(hashes).toString(2));assertEquals(raw,ScapFieldBackup.capture(after).toString());hashes.forEach{(p,h)->assertEquals(h,ScapDelivery.sha256(File(p)))}
            r.complete(id,true);val old=r.dao.inspection(id)!!;r.reopen(id);r.setField(id,"G2","observations","TEST_G_NEWER");assertEquals(0,r.dao.syncState(id,old.updatedAt,"SYNCED"));assertEquals("TEST_G_NEWER",r.dao.snapshot(id)!!.values("G2")["observations"])
        }finally{d.close()}
    }
    private suspend fun idle(controller:ScapController){withTimeout(15000){controller.catalog.first{it!=null};while(controller.pending.value!=0)delay(10)}}
    private fun stop(controller:ScapController){(controller.javaClass.getDeclaredField("writer").apply{isAccessible=true}.get(controller) as CoroutineScope).cancel()}
    @Test fun failedQueuePreservesOrderAndNeverClaimsSavedUntilRetryCompletes()=runBlocking{db().use{d->
        val r=repo(d);val id=r.create("TEST_OPERATOR","TEST_DEVICE",null);val ui=CoroutineScope(SupervisorJob()+Dispatchers.Default);val controller=ScapController(c,d,ui)
        try {idle(controller);val fail=AtomicBoolean(true);controller.submit{if(fail.getAndSet(false))throw java.io.IOException("TEST_ONLY");it.setField(id,"inspection","bridgeName","OLD")};controller.edit(id,"inspection","bridgeName","LATEST")
            idle(controller);assertNotNull(controller.error.value);assertFalse(controller.saveState.value.contains("Ficha guardada"));assertNotEquals("LATEST",r.dao.snapshot(id)!!.values()["bridgeName"]);assertTrue(runCatching{controller.savedSnapshot(id)}.isFailure)
            controller.retry();idle(controller);assertNull(controller.error.value);assertEquals("LATEST",controller.savedSnapshot(id).values()["bridgeName"]);assertTrue(ScapEditJournal(c).pending().isEmpty())
        }finally{stop(controller);ui.cancel()}
    }}
    @Test fun journalRestoresPendingInputWithoutInternetOrUiScopeAndDoesNotReplayOlderTokens()=runBlocking{db().use{d->
        val r=repo(d);val id=r.create("TEST_OPERATOR","TEST_DEVICE",null);val j=ScapEditJournal(c);val a=j.new(id,"inspection","bridgeName","OLD","MANUAL",false);val b=j.new(id,"inspection","bridgeName","RECOVERED","MANUAL",false);j.put(a);j.put(b);j.committed(a);assertEquals(b,j.pending().single())
        val ui=CoroutineScope(SupervisorJob()+Dispatchers.Default);val controller=ScapController(c,d,ui);ui.cancel()
        try {idle(controller);assertEquals("RECOVERED",controller.savedSnapshot(id).values()["bridgeName"]);assertTrue(j.pending().isEmpty())}finally{stop(controller)}
    }}
    @Test fun scapFiveAnnexesKeepUuidPreparedBytesAndAckAcrossOfflineBusyAndLostResponse()=runBlocking{db().use{d->
        val r=repo(d);val id=five(r);r.saveSicLocation(id,"PE-3N","CD","0001","25")
        r.addPhoto(id,image().path,ScapRepository.PHOTO_CATEGORIES.first(),null);r.complete(id,true)
        val key="SCAP/$id";val saved=r.dao.snapshot(id)!!;val raw=ScapFieldBackup.capture(saved).toString()
        val server=object:com.tuempresa.inventariovial.core.AutomaticDriveTransport {
            var offline=true;var busy=true;var lose=true;var accepted:JSONObject?=null;var calls=0
            override fun supportsV3():Boolean{if(offline)throw java.io.IOException("TEST_OFFLINE");return true}
            override fun supportsFamilyPaths()=true
            override fun supportsReadablePhotoNames()=true
            override fun supportsTechnicalDeliveryWithoutGps()=true
            fun prefix(p:JSONObject)="${p.getString("routeCode")}/PUENTES/PUENTE 1"
            override fun reservePhotoNames(p:JSONObject):JSONObject {
                if(busy){busy=false;return JSONObject().put("ok",false).put("code","BUSY")}
                val files=p.getJSONArray("files");val photos=(0 until files.length()).map{files.getJSONObject(it)}.filter{it.getString("key").startsWith("photo/")}
                return JSONObject().put("ok",true).put("photoNamingVersion",2).put("key",p.getString("key")).put("revision",p.getLong("revision"))
                    .put("identityId",p.getString("identityId")).put("folderId","TEST_FIXED_FOLDER").put("logicalPath",prefix(p)).put("photos",org.json.JSONArray(photos.mapIndexed{n,f->JSONObject().put("id",f.getString("key").substringAfter("photo/")).put("category",f.getString("photoCategory")).put("name","%03d_PUE1_GEN.jpg".format(n+1)).put("historical",false)}))
            }
            override fun post(p:JSONObject):JSONObject {
                calls++;accepted?.let{assertEquals(it.getString("contentHash"),p.getString("contentHash"))};accepted=JSONObject(p.toString())
                val files=p.getJSONArray("files");assertEquals(5,(0 until files.length()).count{files.getJSONObject(it).getString("key").startsWith("sketch/")});assertEquals(0,p.getJSONArray("sicRows").length())
                val ack=JSONObject().put("ok",true).put("syncProtocol",3).put("pathVersion",4).put("key",key).put("revision",p.getLong("revision")).put("contentHash",p.getString("contentHash"))
                    .put("folderId","TEST_FIXED_FOLDER").put("logicalPath",prefix(p)).put("technicalDeliveryStatus","DELIVERED").put("georeferencingStatus","PENDING_EXTERNAL").put("externalGeoreferencingStatus","PENDING")
                    .put("files",org.json.JSONArray((0 until files.length()).map{n->val f=files.getJSONObject(n);JSONObject().put("key",f.getString("key")).put("sha256",f.getString("sha256")).put("fileId","TEST_FILE_"+f.getString("key")).put("folderId","TEST_FIXED_FOLDER").put("requestedPath",f.getString("path")).put("logicalPath",prefix(p)+"/"+f.getString("path"))})).put("sicFiles",org.json.JSONArray())
                assertTrue(com.tuempresa.inventariovial.core.AutomaticDelivery.validAck(p,ack))
                if(lose){lose=false;throw java.io.IOException("TEST_LOST_RESPONSE_AFTER_COMMIT")};return ack
            }
        }
        suspend fun drain()=com.tuempresa.inventariovial.core.AutomaticSyncDrain.run(c,d,{server},null)
        assertTrue(drain());assertEquals(0L,d.automaticSyncDao().get(key)!!.syncedRevision)
        server.offline=false;assertTrue(drain());assertTrue(d.automaticSyncDao().get(key)!!.error!!.contains("BUSY"));assertEquals(raw,ScapFieldBackup.capture(r.dao.snapshot(id)!!).toString())
        assertTrue(drain());val pending=d.automaticSyncDao().get(key)!!;val prepared=pending.preparedJson!!;assertEquals(0L,pending.syncedRevision)
        drain();val ack=d.automaticSyncDao().get(key)!!;assertTrue(ack.error,com.tuempresa.inventariovial.core.SyncIntentPresentation.confirmed(ack));assertEquals(pending.revision,ack.syncedRevision);assertEquals(prepared,ack.preparedJson);assertEquals(2,server.calls);assertEquals(5,r.dao.snapshot(id)!!.sketches.size)
        val files=server.accepted!!.getJSONArray("files");assertEquals(files.length(),(0 until files.length()).map{files.getJSONObject(it).getString("key")}.toSet().size)
    }}

    @Test fun captureUsesStableExplicitLabelsAndDiagnosticsNeverIncludeCapturedValues()=runBlocking{db().use{d->
        val r=repo(d);val id=five(r);r.setField(id,"G2","observations","TEST_PRIVATE_VALUE_NOT_FOR_DIAGNOSTICS")
        val s=r.dao.snapshot(id)!!;val data=ScapFieldBackup.capture(s);assertEquals(id,data.getJSONObject("inspection").getString("id"));assertEquals(s.inspection.createdBy,data.getJSONObject("inspection").getString("createdBy"));assertTrue(data.toString().contains("TEST_PRIVATE_VALUE_NOT_FOR_DIAGNOSTICS"));assertTrue(data.getJSONObject("roadRecord").has("startPrCode"));assertTrue(data.getJSONObject("roadRecord").has("locationSource"))
        com.tuempresa.inventariovial.diagnostics.ScapSaveDiagnostics.record(c,3,1,2)
        val report=com.tuempresa.inventariovial.diagnostics.ScapSaveDiagnostics.report(c).toString();assertFalse(report.contains(id));assertFalse(report.contains(s.inspection.createdBy));assertFalse(report.contains("TEST_PRIVATE_VALUE"));assertFalse(report.contains("content://"));assertFalse(report.contains("http"))
    }}

    @Test fun inaccessibleJournalReportsRecoveryErrorWithoutCrashingOrClaimingSaved()=runBlocking{db().use{d->
        val path=File(c.filesDir,"scap_pending_edits");assertFalse(path.exists());path.writeText("TEST_BLOCKED_DIRECTORY")
        val j=ScapEditJournal(c);assertTrue(runCatching{j.pending()}.isFailure);assertTrue(runCatching{j.put(j.new("TEST","inspection","bridgeName","VALUE","MANUAL",false))}.isFailure)
        val ui=CoroutineScope(SupervisorJob()+Dispatchers.Default);val controller=ScapController(c,d,ui)
        try {idle(controller);assertNotNull(controller.error.value);assertFalse(controller.saveState.value.contains("Ficha guardada"));assertTrue(runCatching{controller.savedSnapshot("TEST")}.isFailure)}finally{stop(controller);ui.cancel()}
    }}

    @Test fun cabinetCompletionEnablesAllThreeContractualSicWithoutChangingThePriorProvisional()=runBlocking{db().use{d->
        val r=repo(d);val id=five(r);val initial=r.dao.snapshot(id)!!
        val provisional=File(c.filesDir,"TEST_BEFORE_CABINET.zip");ScapFieldBackup.write(c,initial,"SIC-17",provisional);val hash=ScapDelivery.sha256(provisional)
        r.complete(id,true);r.reopen(id);r.saveSicLocation(id,"PE-3N","CD","0001","25","0002","50")
        for(format in BridgeSicContract.formats){
            for(key in BridgeSicContract.fields(format).map{it.key}.filter{it !in BridgeSicContract.unresolved(format) && !(format=="SIC-17"&&it in BridgeSicContract.locationKeys)}){
                val field=BridgeSicContract.fields(format,BridgeSicContract.manualValues(r.dao.snapshot(id)!!,format)).single{it.key==key}
                val v=when(field.kind){
                    com.tuempresa.inventariovial.supplementary.FieldKind.CODE,com.tuempresa.inventariovial.supplementary.FieldKind.DESCRIBED_CODE->field.options.first()
                    com.tuempresa.inventariovial.supplementary.FieldKind.INTEGER->"2"
                    com.tuempresa.inventariovial.supplementary.FieldKind.YEAR->"2001"
                    com.tuempresa.inventariovial.supplementary.FieldKind.DECIMAL,com.tuempresa.inventariovial.supplementary.FieldKind.SIGNED_DECIMAL->"8.4"
                    com.tuempresa.inventariovial.supplementary.FieldKind.LATITUDE->"-12.1"
                    com.tuempresa.inventariovial.supplementary.FieldKind.LONGITUDE->"-77.1"
                    else->if(key=="surveyDate")"16/10/2026"else"TEST_$key"
                };r.setSicField(id,format,key,v)
            }
        }
        val after=r.dao.snapshot(id)!!;assertEquals(initial.inspection.id,after.inspection.id);assertTrue(after.inspection.updatedAt>initial.inspection.updatedAt);assertEquals(initial.sketches,after.sketches)
        for(format in BridgeSicContract.formats){val p=ScapSicExporter.previews(after).single{it.format==format};assertTrue("$format ${p.missingRequired}",p.missingRequired.isEmpty());val b=ByteArrayOutputStream();ScapSicExporter.write(b,after,format);val target=File(evidence,"${format}_CONTRACTUAL_DATOS_FICTICIOS.xlsx");target.writeBytes(b.toByteArray());ZipFile(target).use{assertNotNull(it.getEntry("xl/workbook.xml"))}}
        assertEquals(hash,ScapDelivery.sha256(provisional));r.complete(id,true);assertEquals("COMPLETE",r.dao.inspection(id)!!.status)
    }}

    @Test fun repeatedCategoryEditPreservesExistingDependentResetSemantics()=runBlocking{db().use{d->
        val r=repo(d);val id=r.create("TEST_OPERATOR","TEST_DEVICE",null);r.addRow(id,"SPAN");val span=r.dao.snapshot(id)!!.spans.single();r.setField(id,span.id,"category","DEFINITIVO");r.setField(id,span.id,"type","Losa")
        val ui=CoroutineScope(SupervisorJob()+Dispatchers.Default);val controller=ScapController(c,d,ui)
        try {idle(controller);controller.edit(id,span.id,"category","DEFINITIVO");idle(controller);assertNull(controller.error.value);val s=controller.savedSnapshot(id);assertEquals("",s.values(span.id)["type"]);assertEquals("",s.spans.single().type)}finally{stop(controller);ui.cancel()}
    }}

    @Test fun delayedPhotoGnssCannotFailTheQueueOrRewriteAClosedRevision()=runBlocking{db().use{d->
        val r=repo(d);val id=five(r);val photo=image();r.addPhoto(id,photo.path,ScapRepository.PHOTO_CATEGORIES.first(),null);r.complete(id,true)
        val before=r.dao.snapshot(id)!!;val original=ScapDelivery.sha256(photo);val outbox=d.automaticSyncDao().all()
        val ui=CoroutineScope(SupervisorJob()+Dispatchers.Default);val controller=ScapController(c,d,ui)
        try {idle(controller);controller.submit{it.updatePhotoCapture(id,photo.path,com.tuempresa.inventariovial.camera.PhotoCaptureMetadata(System.currentTimeMillis(),null))};idle(controller);assertNull(controller.error.value);assertEquals(before,r.dao.snapshot(id));assertEquals(outbox,d.automaticSyncDao().all());assertEquals(original,ScapDelivery.sha256(photo))}finally{stop(controller);ui.cancel()}
    }}

}
