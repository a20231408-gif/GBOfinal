package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.auth.*
import com.tuempresa.inventariovial.core.*
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.delivery.sha256
import com.tuempresa.inventariovial.pavement.*
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

/** All requests stop at the interceptor. No device, production endpoint, account or inventory is used. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=Application::class,shadows=[SyntheticPdf146::class])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PavementAnnotationSync147Test {
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    private fun sample()=PavementSampleEntity("TEST_AUTO_147_SYNC","T","PE-22A",1,"Derecho","CD","00+000","00+020",10.0,20.0,"RIGID","2026-10-16","synthetic","FICTICIO",slabRows=4,slabColumns=2)
    private fun global()=PavementDamageEntity("TEST_AUTO_147_GLOBAL",sample().id,1,"RIGID",26,"Daño del sello de junta","M","GLOBAL",null,null,null,null,null,null,null,null,null,null,"Observación literal")
    private fun corner()=global().copy(id="TEST_AUTO_147_CORNER",code=22,name="Rotura de esquina",geometry="SLABS",x1=.5,y1=2.0,x2=2.0,y2=.5,slabs="1")
    private fun sessions()=AuthSessionManager().apply {login(UserCacheEntity("synthetic","synthetic","FICTICIO","OPERATOR",true,"",0,0,0))}
    private class Server {
        var offline=true;var busy=1;var lostStage=true;var lostCommit=true
        val stages=linkedMapOf<String,String>();val bytes=linkedMapOf<String,ByteArray>()
        val records=linkedMapOf<Long,JSONObject>();val receipts=linkedMapOf<Long,JSONObject>()
        var stageCalls=0;var commitCalls=0
        fun http()=AutomaticDriveHttp(OkHttpClient.Builder().addInterceptor {chain->
            if(offline)throw IOException("SYNTHETIC_OFFLINE")
            val req=chain.request()
            val result=if(req.method=="GET")JSONObject().put("ok",true).put("syncProtocol",3).put("pathVersion",4)
                .put("stableFamilyNumbering",true).put("photoCategoryVersion",1).put("readablePhotoNamingVersion",2).put("technicalDeliveryWithoutGps",true)
            else {
                val body=Buffer();req.body!!.writeTo(body);val p=JSONObject(body.readUtf8())
                when(p.getString("action")) {
                    "reservePhotoNamesV2"->if(busy-->0)JSONObject().put("ok",false).put("code","BUSY") else {
                        val photos=p.getJSONArray("photos")
                        JSONObject().put("ok",true).put("photoNamingVersion",2).put("key",p.getString("key")).put("revision",p.getLong("revision"))
                            .put("identityId",p.getString("recordId")).put("folderId","fixed-sample-folder").put("logicalPath",prefix(p))
                            .put("photos",JSONArray((0 until photos.length()).map {n->val f=photos.getJSONObject(n)
                                JSONObject().put("id",f.getString("id")).put("category",f.getString("category")).put("name","001_PRG1_GEN.jpg").put("historical",false)}))
                    }
                    "stageRecordV3"->{
                        stageCalls++;val f=p.getJSONObject("file");val b=Base64.decode(f.getString("base64"),Base64.NO_WRAP)
                        assertEquals(f.getString("sha256"),sha256(b))
                        val identity="${p.getLong("revision")}/${f.getString("key")}/${f.getString("sha256")}"
                        val id=stages.getOrPut(identity){"stage-${stages.size}"};bytes[id]=b
                        if(lostStage){lostStage=false;throw IOException("SYNTHETIC_LOST_STAGE_ACK")}
                        JSONObject().put("ok",true).put("key",p.getString("key")).put("revision",p.getLong("revision"))
                            .put("fileKey",f.getString("key")).put("sha256",f.getString("sha256")).put("stageFileId",id)
                    }
                    "commitRecordV3"->{
                        commitCalls++;val rev=p.getLong("revision");val old=records[rev]
                        if(old!=null) {
                            assertEquals(old.getString("contentHash"),p.getString("contentHash"));JSONObject(receipts.getValue(rev).toString()).put("alreadyExists",true)
                        }else {
                            assertEquals(records.keys.maxOrNull() ?: 0L,p.getLong("baseRevision"))
                            val files=p.getJSONArray("files");assertEquals(0,p.getJSONArray("sicRows").length())
                            for(n in 0 until files.length()){val f=files.getJSONObject(n);assertEquals(f.getString("sha256"),sha256(bytes.getValue(f.getString("stageFileId"))))}
                            val ack=JSONObject().put("ok",true).put("syncProtocol",3).put("pathVersion",4).put("key",p.getString("key"))
                                .put("revision",rev).put("contentHash",p.getString("contentHash")).put("folderId","fixed-sample-folder").put("logicalPath",prefix(p))
                                .put("files",JSONArray((0 until files.length()).map {n->val f=files.getJSONObject(n)
                                    JSONObject().put("key",f.getString("key")).put("sha256",f.getString("sha256")).put("fileId","fixed-${f.getString("key")}")
                                        .put("folderId","fixed-sample-folder").put("requestedPath",f.getString("path")).put("logicalPath",prefix(p)+"/"+f.getString("path"))})).put("sicFiles",JSONArray())
                            records[rev]=JSONObject(p.toString());receipts[rev]=ack
                            if(lostCommit){lostCommit=false;throw IOException("SYNTHETIC_LOST_COMMIT_ACK")}
                            ack
                        }
                    }
                    else->error("Unexpected fake action")
                }
            }
            Response.Builder().request(req).protocol(Protocol.HTTP_1_1).code(200).message("SYNTHETIC").body(result.toString().toResponseBody("application/json".toMediaType())).build()
        }.build(),"https://example.invalid/rigid147","FICTITIOUS_ONLY")
        private fun prefix(p:JSONObject)="${p.getString("routeCode")}/EVALUACION_SUPERFICIAL/${p.getString("tramo")}/${p.getString("surface")}/MUESTRA 1"
        fun file(rev:Long,key:String):ByteArray {
            val a=records.getValue(rev).getJSONArray("files")
            val f=(0 until a.length()).map {a.getJSONObject(it)}.single {it.getString("key")==key}
            return bytes.getValue(f.getString("stageFileId"))
        }
        fun native(rev:Long)=JSONObject(file(rev,"sample_json").toString(Charsets.UTF_8))
    }
    @Test fun rigidAnnotationsAndPhotoSurviveOfflineBusyLostAckAndAuthorizedUpdate()=runBlocking(Dispatchers.IO) {
        assertTrue(BuildConfig.GBO_PHOTO_NAMES_V2_ENABLED)
        Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build().use {db->
            val s=sample();db.pavementDao().saveSample(s);val repo=PavementRepository(db,sessions())
            repo.saveDamage(global());repo.saveDamage(corner())
            val file=File(context.cacheDir,"TEST_AUTO_147_SYNC.jpg")
            val bitmap=Bitmap.createBitmap(24,24,Bitmap.Config.ARGB_8888)
            file.outputStream().use {assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG,95,it))};bitmap.recycle()
            val original=file.readBytes();repo.savePhoto(PavementDamagePhotoEntity("TEST_AUTO_147_PHOTO",global().id,file.path),s.id)
            val marks=setOf(PavementJointAnnotations.Mark("V",0,1),PavementJointAnnotations.Mark("N",1,1))
            repo.saveJointMarks(s.id,global().id,marks);repo.closeSample(s.id,PavementCalculator(context))
            val server=Server();suspend fun drain()=AutomaticSyncDrain.run(context,db,{server.http()},null)
            val key="PAVEMENT/${s.id}";val firstRevision=db.automaticSyncDao().get(key)!!.revision
            assertTrue(drain());assertEquals(0L,db.automaticSyncDao().get(key)!!.syncedRevision)
            server.offline=false;assertTrue(drain());assertEquals(firstRevision,db.automaticSyncDao().get(key)!!.revision)
            var attempts=0;while(!SyncIntentPresentation.confirmed(db.automaticSyncDao().get(key)) && attempts++<6)drain()
            val confirmed=db.automaticSyncDao().get(key)!!;assertTrue(confirmed.error,SyncIntentPresentation.confirmed(confirmed))
            val raw=server.native(firstRevision);val head=s.copy(sicDataJson=raw.getJSONObject("sample").getJSONObject("sicData").toString())
            assertEquals(marks,PavementJointAnnotations.marks(head,global().id));assertEquals(2,raw.getJSONArray("damages").length())
            assertEquals(4,server.records.getValue(firstRevision).getJSONArray("files").length());assertEquals(5,server.stageCalls)
            assertEquals(2,server.commitCalls);assertArrayEquals(original,file.readBytes())
            repo.reopenSample(s.id);repo.toggleJointMark(s.id,global().id,PavementJointAnnotations.Mark("H",1,0));repo.closeSample(s.id,PavementCalculator(context))
            val next=db.automaticSyncDao().get(key)!!;assertTrue(next.revision>firstRevision);assertEquals(firstRevision,next.syncedRevision)
            drain();val final=db.automaticSyncDao().get(key)!!;assertTrue(final.error,SyncIntentPresentation.confirmed(final))
            assertEquals(2,server.records.size);assertEquals(2,db.pavementDao().damages(s.id).size)
            val a=JSONObject(confirmed.receiptJson!!);val b=JSONObject(final.receiptJson!!)
            assertEquals(a.getString("folderId"),b.getString("folderId"));assertEquals(a.getString("logicalPath"),b.getString("logicalPath"))
            fun ids(j:JSONObject)=(0 until j.getJSONArray("files").length()).map {j.getJSONArray("files").getJSONObject(it).getString("fileId")}.toSet()
            assertEquals(ids(a),ids(b));assertArrayEquals(original,file.readBytes())
            val native=server.native(next.revision);assertEquals(3,PavementJointAnnotations.marks(s.copy(sicDataJson=native.getJSONObject("sample").getJSONObject("sicData").toString()),global().id).size)
            val out=File("D:/Downloads/GBO_PRUEBAS_8B/tmp/rigidos-1.4.7-20261016/entrega-ficticia").apply {mkdirs()}
            File(out,"Pavimentos-Rigidos-TEST.xlsx").writeBytes(server.file(next.revision,"sample_excel"))
            File(out,"Pavimentos-Rigidos-PDF_SIMULADO_NO_NATIVO.pdf").writeBytes(server.file(next.revision,"field"))
            File(out,"Muestra-Rigida-TEST.json").writeText(native.toString(2))
        }
    }
}
