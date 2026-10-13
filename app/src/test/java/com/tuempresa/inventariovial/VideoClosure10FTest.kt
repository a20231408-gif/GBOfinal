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
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],application=Application::class)
class VideoClosure10FTest {
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    private fun video(id:String):VideoEntity {
        val file=File(context.filesDir,"$id.mp4").apply {writeBytes(ByteArray(700000){(it%251).toByte()})}
        return VideoEntity(id,"SCAP/parent10f","UPSTREAM","Aguas arriba",file.path,"video/mp4",file.length(),VideoPolicy.hash(file),1)
    }
    @Test fun capabilityRequiresSuccessfulResponseProtocolOneAndScapOnly() {
        for((ok,protocol,scope,expected) in listOf(
            listOf(true,1,"SCAP_ONLY",true),listOf(false,1,"SCAP_ONLY",false),
            listOf(true,0,"SCAP_ONLY",false),listOf(true,1,"ALL",false))) {
            var posts=0
            val client=OkHttpClient.Builder().addInterceptor {chain->
                if(chain.request().method!="GET")posts++
                Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("mock")
                    .body(JSONObject().put("ok",ok).put("videoProtocol",protocol).put("videoScope",scope).toString().toResponseBody()).build()
            }.build()
            assertEquals(expected,VideoHttp(client,"https://example.invalid/10f","synthetic-token").available());assertEquals(0,posts)
        }
    }
    @Test fun networkErrorCannotExposeResumableSessionCredentialInRoomOrUi()=runBlocking {
        Room.inMemoryDatabaseBuilder(context,VideoDatabase::class.java).build().use {db->
            val video=video("private-error-10f");db.videos().put(video)
            val transport=object:VideoTransport {
                override fun available():Boolean=throw java.io.IOException("Connection failed: https://www.googleapis.com/upload/drive/v3/files/id?upload_id=fictitious-private-session-10f")
                override fun begin(video:VideoEntity,revision:Long):JSONObject=error("Unexpected begin")
                override fun offset(video:VideoEntity):Long=error("Unexpected offset")
                override fun chunk(video:VideoEntity,start:Long,count:Int):Long=error("Unexpected chunk")
                override fun finish(video:VideoEntity):JSONObject=error("Unexpected finish")
            }
            assertTrue(VideoSync.run(context,db.videos(),transport,true,{20}))
            val saved=db.videos().get(video.id)!!;assertEquals("ERROR",saved.state)
            assertFalse(saved.error.orEmpty().contains("upload_id"));assertFalse(saved.error.orEmpty().contains("https://"))
            assertTrue(File(saved.localPath).isFile);assertEquals(video.sha256,saved.sha256)
        }
    }
    @Test fun realHttpTransportResumesLostChunkAndLostFinalAckWithoutDuplicatingBytesOrFile()=runBlocking {
        Room.inMemoryDatabaseBuilder(context,VideoDatabase::class.java).build().use {db->
            val video=video("http-recovery-10f");db.videos().put(video)
            val remote=Buffer();var loseChunk=true;var loseAck=true;var complete=false
            var starts=0;var finishes=0;val mediaIds=mutableSetOf<String>();val fileIds=mutableSetOf<String>()
            val session="https://www.googleapis.com/upload/drive/v3/files/fixed?upload_id=synthetic-10f"
            val client=OkHttpClient.Builder().addInterceptor {chain->
                val request=chain.request();var code=200;var range:String?=null
                val result=when(request.method) {
                    "GET" -> JSONObject().put("ok",true).put("videoProtocol",1).put("videoScope","SCAP_ONLY")
                    "POST" -> {
                        val bytes=Buffer();request.body!!.writeTo(bytes);val p=JSONObject(bytes.readUtf8())
                        assertEquals("synthetic-token",p.getString("token"));assertEquals(video.parentKey,p.getString("parentKey"))
                        assertEquals(video.category,p.getString("category"));mediaIds.add(p.getString("mediaId"))
                        val ack=JSONObject().put("ok",true).put("videoProtocol",1).put("parentKey",video.parentKey)
                            .put("mediaId",video.id).put("sha256",video.sha256).put("sizeBytes",video.sizeBytes).put("fileId","fixed-10f")
                        if(p.getString("action")=="beginVideoV1") {starts++;ack.put("complete",complete);if(!complete)ack.put("sessionUrl",session)}
                        else {finishes++;fileIds.add(p.getString("fileId"));assertEquals(video.sizeBytes,remote.size)
                            assertEquals(video.sha256,remote.snapshot().sha256().hex());complete=true
                            if(loseAck){loseAck=false;throw java.io.IOException("Synthetic lost final ACK")}
                            ack.put("complete",true)}
                        ack
                    }
                    else -> {
                        assertEquals(session,request.url.toString());val contentRange=request.header("Content-Range")!!
                        if(!contentRange.startsWith("bytes */")) {
                            val start=contentRange.substringAfter("bytes ").substringBefore('-').toLong();assertEquals(remote.size,start)
                            request.body!!.writeTo(remote)
                            if(loseChunk){loseChunk=false;throw java.io.IOException("Synthetic lost chunk response")}
                        }
                        code=if(remote.size==video.sizeBytes)200 else 308
                        if(remote.size>0)range="bytes=0-${remote.size-1}"
                        JSONObject()
                    }
                }
                Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(code).message("mock")
                    .apply {range?.let {header("Range",it)}}.body(result.toString().toResponseBody()).build()
            }.build()
            fun transport()=VideoHttp(client,"https://example.invalid/10f","synthetic-token")
            assertTrue(VideoSync.run(context,db.videos(),transport(),true,{20}))
            assertEquals(VideoPolicy.CHUNK_BYTES.toLong(),remote.size);assertEquals("ERROR",db.videos().get(video.id)!!.state)
            assertTrue(VideoSync.run(context,db.videos(),transport(),true,{20}));assertEquals("ERROR",db.videos().get(video.id)!!.state)
            assertFalse(VideoSync.run(context,db.videos(),transport(),true,{20}))
            val saved=db.videos().get(video.id)!!;assertEquals("SYNCED",saved.state);assertNull(saved.sessionUrl)
            assertEquals(video.sizeBytes,saved.uploadedBytes);assertEquals(video.sha256,saved.sha256)
            assertEquals(1,db.videos().all().size);assertEquals(setOf(video.id),mediaIds);assertEquals(setOf("fixed-10f"),fileIds)
            assertEquals(3,starts);assertEquals(2,finishes);assertTrue(File(video.localPath).isFile)
        }
    }
}
