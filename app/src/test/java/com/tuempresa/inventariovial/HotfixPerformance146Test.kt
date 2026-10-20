package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.core.*
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.diagnostics.*
import com.tuempresa.inventariovial.pavement.*
import com.tuempresa.inventariovial.repository.InventoryRepository
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
import java.io.File
import java.io.IOException
import java.util.UUID

/** Ordinary V2 build; every HTTP request is intercepted. Two fictitious independent Room devices. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=Application::class,shadows=[SyntheticPdf146::class])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HotfixPerformance146Test {
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    private fun db()=Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build()
    private fun id(value:String)=UUID.nameUUIDFromBytes(("TEST_AUTO_146/"+value).toByteArray()).toString()
    private fun photo(n:Int):File {
        val file=File(context.cacheDir,"TEST_AUTO_146_$n.jpg")
        val bitmap=Bitmap.createBitmap(128,96,Bitmap.Config.ARGB_8888)
        val random=java.util.Random(n.toLong());val pixels=IntArray(128*96){0xff000000.toInt() or random.nextInt(0xffffff)}
        bitmap.setPixels(pixels,0,128,0,0,128,96)
        file.outputStream().use {bitmap.compress(Bitmap.CompressFormat.JPEG,85,it)};bitmap.recycle();return file
    }
    private suspend fun sic(d:InventoryDatabase,n:Int) {
        val record=id("SIC/$n");val f=photo(n)
        InventoryRepository(d).saveSic22(InventoryRecordEntity(record,"SIC-22","VERTICAL","PE-22A","CD",(20-n).toString(),20.0,null,null,"D",
            null,null,null,null,"15/10/2026","TEST_AUTO_146","ACTIVE","PENDING","PENDING",1,1,sessionId=id("session/${n%2}")),
            Sic22Entity(record,"20","1","2","R-1",null,"1",null,null,null),
            listOf(PhotoEntity(id("photo/SIC/$n"),record,1,f.path,true,null,null,null,"PENDING",1,photoCategory="GENERAL")))
    }
    private suspend fun pavement(d:InventoryDatabase,n:Int) {
        val record=id("PAVEMENT/$n");val damage=id("damage/$n");val f=photo(100+n)
        var sample=PavementSampleEntity(record,"TEST_AUTO_146","PE-22A",n,"Derecho","CD","01+000","01+040",5.0,40.0,"ASPHALT",
            "2026-10-15","engineer-${n%2}","FICTICIO","CLOSED",1,1)
        for(format in listOf(PavementSicFormat.SIC14,PavementSicFormat.SIC14A))sample=sample.copy(sicDataJson=PavementSic.update(sample,format,damage,
            mapOf("faja" to "1","prInicio" to "0001","distanciaInicio" to "0","prFin" to "0001","distanciaFin" to "40")))
        d.pavementDao().saveSample(sample)
        d.pavementDao().saveDamage(PavementDamageEntity(damage,record,1,"ASPHALT",1,"TEST_AUTO_146","L","AREA",1.0,1.0,2.0,3.0,null,1.0,2.0,2.0,null,null,"FICTICIO"))
        d.pavementDao().savePhoto(PavementDamagePhotoEntity(id("photo/PAVEMENT/$n"),damage,f.path,1))
        AutomaticSync.request(d,"PAVEMENT",record,1)
    }
    private class Server {
        var offline=true;var busy=2;var disconnectStage=true;var disconnectCommit=true
        var gets=0;var wireBytes=0L;var calls=0;var stageCalls=0;var commitCalls=0
        val records=linkedMapOf<String,JSONObject>();val receipts=mutableMapOf<String,JSONObject>()
        val stages=mutableMapOf<String,String>();private val bindings=mutableMapOf<String,Pair<Int,String>>()
        private fun binding(p:JSONObject):Pair<Int,String> = bindings.getOrPut(p.getString("key")) {
            val stem=if(p.getString("kind")=="PAVEMENT")"${p.getString("routeCode")}/EVALUACION_SUPERFICIAL/${p.getString("tramo")}/${p.getString("surface")}/MUESTRA "
                else "${p.getString("routeCode")}/${p.getString("familyPath")}/${p.getString("family")} "
            val number=bindings.values.count{it.second.startsWith(stem)}+1;number to (stem+number)
        }
        fun transport()=AutomaticDriveHttp(OkHttpClient.Builder().addInterceptor {chain->
            calls++;Thread.sleep(2) // Deliberate, labelled simulated latency; never contacts a network.
            val req=chain.request()
            if(req.method=="GET")gets++
            if(offline)throw IOException("SYNTHETIC_OFFLINE")
            val response=if(req.method=="GET")JSONObject().put("ok",true).put("syncProtocol",3).put("pathVersion",4)
                .put("stableFamilyNumbering",true).put("photoCategoryVersion",1).put("readablePhotoNamingVersion",2).put("technicalDeliveryWithoutGps",true)
            else {
                val buffer=Buffer();req.body!!.writeTo(buffer);wireBytes+=buffer.size
                val p=JSONObject(buffer.readUtf8())
                when(p.getString("action")) {
                    "reservePhotoNamesV2"->{
                        if(busy>0){busy--;JSONObject().put("ok",false).put("code","BUSY")}
                        else {val (number,path)=binding(p);val photos=p.getJSONArray("photos")
                            JSONObject().put("ok",true).put("photoNamingVersion",2).put("key",p.getString("key")).put("revision",p.getLong("revision"))
                                .put("identityId",p.optString("identityId",p.getString("recordId"))).put("folderId","folder-$number-${p.getString("kind")}").put("logicalPath",path)
                                .put("photos",JSONArray((0 until photos.length()).map{n->val f=photos.getJSONObject(n)
                                    JSONObject().put("id",f.getString("id")).put("category",f.getString("category"))
                                        .put("name","${(n+1).toString().padStart(3,'0')}_${if(p.getString("kind")=="PAVEMENT")"PAF" else "SEN"}${number}_GEN.jpg").put("historical",false)}))
                        }
                    }
                    "stageRecordV3"->{
                        stageCalls++;val f=p.getJSONObject("file");val key=p.getString("key")+"/"+p.getLong("revision")+"/"+f.getString("key")+"/"+f.getString("sha256")
                        val bytes=android.util.Base64.decode(f.getString("base64"),android.util.Base64.NO_WRAP)
                        assertEquals(f.getString("sha256"),com.tuempresa.inventariovial.delivery.sha256(bytes))
                        if(f.getString("mimeType")=="image/jpeg")assertTrue(JpegEnvelope.valid(bytes))
                        val stage=stages.getOrPut(key){"stage-${stages.size+1}"}
                        if(disconnectStage){disconnectStage=false;throw IOException("SYNTHETIC_LOST_STAGE_ACK")}
                        JSONObject().put("ok",true).put("key",p.getString("key")).put("revision",p.getLong("revision"))
                            .put("fileKey",f.getString("key")).put("sha256",f.getString("sha256")).put("stageFileId",stage)
                    }
                    else->{
                        commitCalls++;val key=p.getString("key");val old=records[key]
                        if(old!=null){assertEquals(old.getString("contentHash"),p.getString("contentHash"));JSONObject(receipts.getValue(key).toString()).put("alreadyExists",true)}
                        else {
                            val (number,path)=binding(p);val files=p.getJSONArray("files");val rows=p.getJSONArray("sicRows")
                            val ack=JSONObject().put("ok",true).put("syncProtocol",3).put("pathVersion",4).put("key",key).put("revision",p.getLong("revision"))
                                .put("contentHash",p.getString("contentHash")).put("folderId","folder-$number-${p.getString("kind")}").put("logicalPath",path)
                                .put("technicalDeliveryStatus","DELIVERED").put("georeferencingStatus",p.optJSONObject("georeferencing")?.optString("status") ?: "UNASSESSED").put("externalGeoreferencingStatus","PENDING")
                                .put("files",JSONArray((0 until files.length()).map{n->val f=files.getJSONObject(n)
                                    JSONObject().put("key",f.getString("key")).put("sha256",f.getString("sha256")).put("fileId","file-"+key+"-"+f.getString("key"))
                                        .put("folderId","folder-$number-${p.getString("kind")}").put("requestedPath",f.getString("path")).put("logicalPath",path+"/"+f.getString("path"))}))
                                .put("sicFiles",JSONArray((0 until rows.length()).map{n->val row=rows.getJSONObject(n)
                                    JSONObject().put("format",row.getString("format")).put("sha256",row.getString("sha256")).put("fileId","excel-"+row.getString("format"))
                                        .put("folderId","excel-folder").put("logicalPath",p.getString("routeCode")+"/EXCEL_SIC/"+row.getString("format")+".xlsx")
                                        .put("recordIds",JSONArray(listOf(row.getString("recordId"))))}))
                            assertTrue("Synthetic ACK contract",AutomaticDelivery.validAck(p,ack))
                            records[key]=JSONObject(p.toString());receipts[key]=ack
                            if(disconnectCommit){disconnectCommit=false;throw IOException("SYNTHETIC_LOST_COMMIT_ACK")}
                            ack
                        }
                    }
                }
            }
            Response.Builder().request(req).protocol(Protocol.HTTP_1_1).code(200).message("SYNTHETIC").body(response.toString().toResponseBody("application/json".toMediaType())).build()
        }.build(),"https://example.invalid/hotfix146","FICTITIOUS_TOKEN_ONLY")
    }
    @Test fun load()=runBlocking(Dispatchers.IO) {db().use {a->db().use {b->
        assertTrue("Ordinary V2 configuration required",BuildConfig.GBO_PHOTO_NAMES_V2_ENABLED)
        for((index,d) in listOf(a,b).withIndex())d.inventoryDao().insertSession(FieldSession(id("session/$index"),"TEST_AUTO_146","engineer-$index","synthetic-$index","PE-22A","","CD",startTime=1))
        for(n in 1..17)sic(if(n%2==0)a else b,n)
        for(n in 1..11)pavement(if(n%2==0)a else b,n)
        val originals=(1..17).map{photoFilePath(it)}+(1..11).map{photoFilePath(100+it)}
        val hashes=originals.associateWith{com.tuempresa.inventariovial.delivery.sha256(File(it).readBytes())}
        val server=Server();var factories=0;var drains=0
        suspend fun drain(d:InventoryDatabase):Boolean {drains++;return AutomaticSyncDrain.run(context,d,{factories++;server.transport()},null)}
        val started=SyncPerformance.now()
        assertTrue(drain(a));assertTrue(drain(b));assertEquals(2,server.gets);assertTrue(server.records.isEmpty())
        server.offline=false
        var rounds=0
        while((a.automaticSyncDao().all()+b.automaticSyncDao().all()).any{!SyncIntentPresentation.confirmed(it)} && rounds++<10){drain(a);drain(b)}
        val intents=a.automaticSyncDao().all()+b.automaticSyncDao().all()
        assertEquals(intents.map{it.state+":"+it.error}.toString(),28,intents.count{SyncIntentPresentation.confirmed(it)})
        assertEquals(28,server.records.size);assertEquals(intents.size,intents.map{it.recordId}.distinct().size)
        assertEquals(factories,server.gets);assertTrue("No per-record capability GET",server.gets<28)
        val files=server.records.values.sumOf{it.getJSONArray("files").length()};assertEquals(files,server.stages.size)
        assertEquals("Only the lost staging response repeats",files+1,server.stageCalls)
        assertEquals(29,server.commitCalls)
        for((path,hash) in hashes)assertEquals(hash,com.tuempresa.inventariovial.delivery.sha256(File(path).readBytes()))
        val report=JSONObject().put("environment","ISOLATED ROOM + HTTP INTERCEPTORS; NO PRODUCTION").put("sic",17).put("pavements",11)
            .put("pdfBackend","Actual page renderer on bitmap canvases + TEST raster PDF wrapper; Android native PDF writer NOT VERIFIED")
            .put("confirmed",28).put("elapsedMs",SyncPerformance.now()-started).put("drains",drains).put("capabilityGets",server.gets)
            .put("files",files).put("stageCalls",server.stageCalls).put("commitCalls",server.commitCalls).put("wireBytes",server.wireBytes)
            .put("artificialLatencyPerHttpMs",2).put("metrics",SyncPerformance.report(context)).put("deviceA",SyncQueueDiagnostics.snapshot(a)).put("deviceB",SyncQueueDiagnostics.snapshot(b))
        val out=File("build/outputs/hotfix-1.4.6/load-android.json");out.parentFile!!.mkdirs();out.writeText(report.toString(2))
        assertFalse(report.toString().contains("FICTITIOUS_TOKEN_ONLY"));assertFalse(report.toString().contains("example.invalid"))
    }}}
    private fun photoFilePath(n:Int)=File(context.cacheDir,"TEST_AUTO_146_$n.jpg").path

    @Test fun preview()=runBlocking(Dispatchers.IO) {db().use{d->
        sic(d,1);pavement(d,1);val times=JSONArray()
        for(i in d.automaticSyncDao().all()) {
            val directory=File(context.filesDir,"automatic_drive/"+com.tuempresa.inventariovial.delivery.sha256(i.key.toByteArray())+"/"+i.revision)
            assertFalse(directory.exists())
            val fastStart=SyncPerformance.now();val fast=AutomaticDelivery.namingPreview(context,d,i);val fastMs=SyncPerformance.now()-fastStart
            assertFalse("Reservation preview must not render files",directory.exists())
            val oldStart=SyncPerformance.now();val old=AutomaticDelivery.prepare(context,d,i,readable=false,persist=false);val oldMs=SyncPerformance.now()-oldStart
            for(k in listOf("kind","key","recordId","revision","baseRevision","pathVersion","identityId","routeCode","familyPath","family","tramo","surface"))assertEquals(k,old.opt(k),fast.opt(k))
            fun photos(p:JSONObject)=(0 until p.getJSONArray("files").length()).map{p.getJSONArray("files").getJSONObject(it)}.filter{it.getString("key").startsWith("photo/")}
                .map{listOf(it.getString("key"),it.getString("photoCategory"),it.getString("path"))}
            assertEquals(photos(old),photos(fast));assertTrue(directory.listFiles()!!.isNotEmpty())
            times.put(JSONObject().put("kind",i.kind).put("newReservationPreviewMs",fastMs).put("oldFullRenderForReservationMs",oldMs))
        }
        val out=File("build/outputs/hotfix-1.4.6/reservation-measurements.json");out.parentFile!!.mkdirs();out.writeText(times.toString(2))
    }}
    @Test fun counts()=runBlocking(Dispatchers.IO) {db().use{d->
        sic(d,1)
        for((n,state) in SyncQueueDiagnostics.states.withIndex()) {
            val key="SIC/SECRET_UUID_$n";d.automaticSyncDao().put(DriveSyncIntent(key,"SIC","SECRET_UUID_$n",2,state=state,
                preparedJson=if(state=="QUEUED")"{\"revision\":2,\"files\":[{},{}],\"token\":\"SECRET_TOKEN\",\"localFile\":\"PRIVATE_PATH\"}" else null))
        }
        val json=SyncQueueDiagnostics.snapshot(d);val raw=json.toString()
        for(secret in listOf("SECRET_UUID","SECRET_TOKEN","PRIVATE_PATH"))assertFalse(raw.contains(secret))
        val families=json.getJSONArray("outboxByFamily");val counts=(0 until families.length()).map{families.getJSONObject(it)}.single{it.getString("family")=="SIC_OTHER"}.getJSONObject("states")
        for(state in SyncQueueDiagnostics.states)assertEquals(state,1,counts.getInt(state))
        assertEquals(2,json.getInt("knownFilesAwaitingRecordAck"));assertEquals(1,json.getInt("invalidSyncedReceipts"))
        for(n in 0..270)SyncPerformance.record(context,"SIC","PREPARE",n.toLong(),outcome="OK")
        SyncPerformance.record(context,"SECRET_TOKEN","PRIVATE_URL",1)
        val metrics=SyncPerformance.report(context);assertEquals(256,metrics.getJSONArray("samples").length());assertFalse(metrics.toString().contains("SECRET_TOKEN"))
        val fake=com.tuempresa.inventariovial.dashboard.DashboardDeliveryCounts.from(listOf(DriveSyncIntent("SIC/x","SIC","x",1,state="ERROR",error="Servidor ocupado (BUSY)")))
        assertEquals(0,fake.errors);assertEquals(1,fake.pending)
    }}
}
