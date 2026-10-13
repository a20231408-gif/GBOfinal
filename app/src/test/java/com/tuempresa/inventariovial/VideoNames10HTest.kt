package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.media.*
import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],application=Application::class)
class VideoNames10HTest {
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    @Before fun clearTestLabels(){context.getSharedPreferences("video_readable_names_v2",Context.MODE_PRIVATE).edit().clear().commit()}
    private fun video():VideoEntity {
        val f=File(context.filesDir,"TEST_AUTO_10H.mp4").apply {writeBytes(ByteArray(32){it.toByte()})}
        return VideoEntity("media10h","SCAP/parent10h","T_CATEGORY","AGUAS ARRIBA",f.path,"video/mp4",f.length(),VideoPolicy.hash(f),1)
    }
    private fun ack(v:VideoEntity)=JSONObject().put("ok",true).put("videoProtocol",1).put("complete",true)
        .put("mediaId",v.id).put("parentKey",v.parentKey).put("sha256",v.sha256).put("sizeBytes",v.sizeBytes).put("fileId","file10h")
        .put("videoNamingVersion",2).put("fileName","001_PUE7_ARR.mp4").put("categoryFolder","AGUAS_ARRIBA")
        .put("categoryCode","ARR").put("videoNumber",1).put("elementNumber",7)
    private fun response(request:Request,json:JSONObject)=Response.Builder().request(request).protocol(Protocol.HTTP_1_1)
        .code(200).message("synthetic").body(json.toString().toResponseBody()).build()
    @Test fun namingCapabilityIsNegotiatedSeparatelyAndOldServerStillUsesVideoOne() {
        val v=video()
        for(cap in listOf(0,1,2,3)) {
            var requested:Int?=null
            val client=OkHttpClient.Builder().addInterceptor {chain->
                val r=chain.request()
                val json=if(r.method=="GET")JSONObject().put("ok",true).put("videoProtocol",1).put("videoScope","SCAP_ONLY").put("readableVideoNamingVersion",cap)
                else {val bytes=Buffer();r.body!!.writeTo(bytes);val p=JSONObject(bytes.readUtf8());
                    assertEquals(1,p.getInt("videoProtocol"));requested=if(p.has("videoNamingVersion"))p.getInt("videoNamingVersion") else null
                    if(cap==2)ack(v) else ack(v).apply {remove("videoNamingVersion")}}
                response(r,json)
            }.build()
            val http=VideoHttp(client,"https://example.invalid/10h","synthetic")
            assertTrue(http.available());http.begin(v,20);assertEquals(if(cap==2)2 else null,requested)
        }
    }
    @Test fun negotiatedV2CannotSilentlyAcceptMissingNameButExplicitHistoricalBindingIsAllowed() {
        val v=video();var result=ack(v).apply {remove("videoNamingVersion")}
        val client=OkHttpClient.Builder().addInterceptor {chain->response(chain.request(),
            if(chain.request().method=="GET")JSONObject().put("ok",true).put("videoProtocol",1).put("videoScope","SCAP_ONLY").put("readableVideoNamingVersion",2) else result)}.build()
        val http=VideoHttp(client,"https://example.invalid/10h","synthetic");assertTrue(http.available())
        assertTrue(runCatching {http.begin(v,20)}.isFailure)
        result=ack(v).put("videoNamingVersion",1).put("historicalName",true)
        assertEquals(1,http.begin(v,20).getInt("videoNamingVersion"))
    }
    @Test fun labelsSurviveRecreationAndRejectReassignmentWithoutTouchingOriginalOrIdentity() {
        val v=video();val a=ack(v);VideoNames.accept(context,v,a)
        assertEquals("001_PUE7_ARR.mp4",VideoNames.display(context,v.id))
        VideoNames.accept(context,v,a)
        assertTrue(runCatching {VideoNames.accept(context,v,ack(v).put("videoNumber",2).put("fileName","002_PUE7_ARR.mp4"))}.isFailure)
        assertTrue(runCatching {VideoNames.accept(context,v,ack(v).put("fileId","other"))}.isFailure)
        assertEquals(v.sha256,VideoPolicy.hash(File(v.localPath)));assertEquals("media10h",v.id)
    }
    @Test fun wrongExtensionUnsafeFolderOrInconsistentNumberCannotConfirm() {
        val v=video()
        for(a in listOf(ack(v).put("fileName","001_PUE7_ARR.webm"),ack(v).put("categoryFolder","../AGUAS"),
            ack(v).put("videoNumber",2),ack(v).put("videoNumber",1000)))assertTrue(runCatching {VideoNames.validate(a,v,2)}.isFailure)
    }
    @Test fun lostFinalAckUsesSameNameOriginalBytesUuidAndFileIdThenConfirms()=runBlocking {
        Room.inMemoryDatabaseBuilder(context,VideoDatabase::class.java).build().use {db->
            val v=video();db.videos().put(v);val remote=Buffer();var lost=true;var done=false;var begins=0
            val ids=mutableSetOf<String>();val session="https://www.googleapis.com/upload/drive/v3/files/fixed?upload_id=synthetic10h"
            val client=OkHttpClient.Builder().addInterceptor {chain->
                val r=chain.request()
                val result=when(r.method) {
                    "GET"->JSONObject().put("ok",true).put("videoProtocol",1).put("videoScope","SCAP_ONLY").put("readableVideoNamingVersion",2)
                    "POST"->{val b=Buffer();r.body!!.writeTo(b);val p=JSONObject(b.readUtf8());
                        assertEquals(v.category,p.getString("category"));assertEquals(2,p.getInt("videoNamingVersion"));ids.add(p.getString("mediaId"))
                        if(p.getString("action")=="beginVideoV1"){begins++;ack(v).put("complete",done).put("sessionUrl",session)}
                        else {assertEquals("file10h",p.getString("fileId"));assertEquals(v.sha256,remote.snapshot().sha256().hex());done=true
                            if(lost){lost=false;throw java.io.IOException("synthetic lost ACK")};ack(v)}}
                    else->{if(!r.header("Content-Range")!!.startsWith("bytes */"))r.body!!.writeTo(remote);JSONObject()}
                }
                if(r.method=="PUT" && remote.size==0L)Response.Builder().request(r).protocol(Protocol.HTTP_1_1).code(308).message("synthetic").body("{}".toResponseBody()).build()
                else response(r,result)
            }.build()
            fun transport()=VideoHttp(client,"https://example.invalid/10h","synthetic")
            assertTrue(VideoSync.run(context,db.videos(),transport(),true,{20}))
            assertEquals("ERROR",db.videos().get(v.id)!!.state)
            assertFalse(VideoSync.run(context,db.videos(),transport(),true,{20}))
            val saved=db.videos().get(v.id)!!;assertEquals("SYNCED",saved.state);assertEquals("file10h",saved.fileId)
            assertEquals(2,begins);assertEquals(setOf(v.id),ids);assertEquals(v.sizeBytes,remote.size)
            assertEquals(1,db.videos().all().size);assertEquals(v.sha256,VideoPolicy.hash(File(saved.localPath)))
            assertEquals("001_PUE7_ARR.mp4",VideoNames.display(context,saved.id))
        }
    }
}
