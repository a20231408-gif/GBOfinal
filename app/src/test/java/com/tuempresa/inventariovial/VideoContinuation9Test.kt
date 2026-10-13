package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.media.*
import com.tuempresa.inventariovial.core.StablePhotoNames
import com.tuempresa.inventariovial.data.entity.PhotoEntity
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],application=Application::class)
class VideoContinuation9Test {
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    private fun video(id:String):VideoEntity {val f=File(context.filesDir,"$id.mp4");f.writeBytes(ByteArray(700000){(it%251).toByte()});return VideoEntity(id,"SCAP/parent9","GENERAL","General",f.path,"video/mp4",f.length(),VideoPolicy.hash(f),1)}
    private class Transport:VideoTransport {
        var calls=0;var sent=0L;var fail=false;var wrongAck=false;var expired=false;var advertised=true
        override fun available()=advertised.also{calls++}
        override fun begin(video:VideoEntity,revision:Long)=JSONObject().put("ok",true).put("mediaId",video.id).put("parentKey",video.parentKey).put("sha256",video.sha256).put("fileId","fixed-${video.id}").put("sessionUrl","https://www.googleapis.com/upload/drive/v3/files/fixed?upload_id=secret")
        override fun offset(video:VideoEntity):Long {if(expired){expired=false;throw ExpiredVideoSession()};return sent}
        override fun chunk(video:VideoEntity,start:Long,count:Int):Long {assertEquals(sent,start);require(count<=VideoPolicy.CHUNK_BYTES);sent+=count;if(fail){fail=false;throw java.io.IOException("lost response")};return sent}
        override fun finish(video:VideoEntity)=JSONObject().put("ok",true).put("complete",true).put("videoProtocol",1).put("mediaId",video.id).put("parentKey",video.parentKey).put("sizeBytes",video.sizeBytes).put("sha256",if(wrongAck)"wrong" else video.sha256).put("fileId",video.fileId)
    }
    @Test fun historicalNonBridgeVideosArePreservedAndNeverReachTransport()=runBlocking {
        Room.inMemoryDatabaseBuilder(context,VideoDatabase::class.java).build().use {db->
            for(kind in listOf("SIC","PAVEMENT")) {val v=video("legacy10-$kind").copy(parentKey="$kind/parent9");db.videos().put(v)}
            val t=Transport();VideoSync.run(context,db.videos(),t,true,{20})
            assertEquals(0,t.calls);assertEquals(2,db.videos().all().size)
            db.videos().all().forEach {assertEquals("SCOPE_RESTRICTED",it.state);assertTrue(File(it.localPath).isFile);assertFalse(it.retired)}
            assertFalse(VideoPolicy.bridgeOnly("SIC/parent"));assertFalse(VideoPolicy.bridgeOnly("PAVEMENT/parent"));assertTrue(VideoPolicy.bridgeOnly("SCAP/parent"))
        }
    }
    @Test fun disabledUploadNeverCallsRemoteOrPretendsToBeSynced()=runBlocking {Room.inMemoryDatabaseBuilder(context,VideoDatabase::class.java).build().use {db->val v=video("disabled9");db.videos().put(v);val t=Transport();assertFalse(VideoSync.run(context,db.videos(),t,false,{20}));assertEquals(0,t.calls);assertEquals("BACKEND_REQUIRED",db.videos().get(v.id)!!.state);assertTrue(File(v.localPath).isFile)}}
    @Test fun resumableRetryAfterLostChunkResponseUsesSameUuidAndFileId()=runBlocking {Room.inMemoryDatabaseBuilder(context,VideoDatabase::class.java).build().use {db->val v=video("resume9");db.videos().put(v);val t=Transport();t.fail=true;assertTrue(VideoSync.run(context,db.videos(),t,true,{20}));val failed=db.videos().get(v.id)!!;assertEquals("ERROR",failed.state);assertEquals("fixed-${v.id}",failed.fileId);assertFalse(VideoSync.run(context,db.videos(),t,true,{20}));val saved=db.videos().get(v.id)!!;assertEquals("SYNCED",saved.state);assertEquals(v.sizeBytes,t.sent);assertEquals(v.sha256,saved.sha256);assertEquals(1,db.videos().all().size)}}
    @Test fun waitingParentAndMissingCapabilityPreserveOriginalAndQueue()=runBlocking {Room.inMemoryDatabaseBuilder(context,VideoDatabase::class.java).build().use {db->val v=video("wait9");db.videos().put(v);val t=Transport();VideoSync.run(context,db.videos(),t,true,{null});assertEquals("WAITING_PARENT",db.videos().get(v.id)!!.state);assertEquals(0,t.calls);t.advertised=false;VideoSync.run(context,db.videos(),t,true,{20});assertEquals("BACKEND_REQUIRED",db.videos().get(v.id)!!.state);assertTrue(File(v.localPath).isFile)}}
    @Test fun badAckAndChangedOriginalCannotBecomeSynced()=runBlocking {Room.inMemoryDatabaseBuilder(context,VideoDatabase::class.java).build().use {db->val v=video("hash9");db.videos().put(v);val t=Transport();t.wrongAck=true;assertTrue(VideoSync.run(context,db.videos(),t,true,{20}));assertEquals("ERROR",db.videos().get(v.id)!!.state);File(v.localPath).appendText("changed");val before=t.calls;VideoSync.run(context,db.videos(),t,true,{20});assertEquals(before,t.calls);assertEquals("ERROR",db.videos().get(v.id)!!.state)}}
    @Test fun expiredSessionClearsSecretAndRetryKeepsId()=runBlocking {Room.inMemoryDatabaseBuilder(context,VideoDatabase::class.java).build().use {db->val v=video("expired9");db.videos().put(v);val t=Transport();t.expired=true;assertTrue(VideoSync.run(context,db.videos(),t,true,{20}));assertNull(db.videos().get(v.id)!!.sessionUrl);assertFalse(VideoSync.run(context,db.videos(),t,true,{20}));assertEquals("SYNCED",db.videos().get(v.id)!!.state)}}
    @Test fun retiredVideoCannotBeResurrectedByWorkerProgress()=runBlocking {Room.inMemoryDatabaseBuilder(context,VideoDatabase::class.java).build().use {db->val v=video("retired9");db.videos().put(v.copy(retired=true,state="RETIRED"));db.videos().saveSync(v.copy(state="SYNCED"));assertEquals("RETIRED",db.videos().get(v.id)!!.state);val t=Transport();VideoSync.run(context,db.videos(),t,true,{20});assertEquals(0,t.calls);assertTrue(File(v.localPath).isFile)}}
    @Test fun offlineDatabaseReopenRetainsCategoriesHashOriginalAndIdentity()=runBlocking {val name="video9-${System.nanoTime()}";val v=video("offline9");Room.databaseBuilder(context,VideoDatabase::class.java,name).build().use {it.videos().put(v)};Room.databaseBuilder(context,VideoDatabase::class.java,name).build().use {assertEquals(v,it.videos().get(v.id))};context.deleteDatabase(name);Unit}
    @Test fun sessionHostAndOffsetsRejectForeignOrMalformedResponses() {
        assertTrue(VideoPolicy.safeSession("https://www.googleapis.com/upload/drive/v3/files/id?upload_id=secret"))
        listOf("http://www.googleapis.com/upload/drive/v3/files/id","https://evil.test/upload/drive/v3/files/id","https://www.googleapis.com.evil.test/upload/drive/v3/files/id").forEach {assertFalse(VideoPolicy.safeSession(it))}
        assertEquals(0L,VideoPolicy.rangeOffset(null,100));assertEquals(50L,VideoPolicy.rangeOffset("bytes=0-49",100));assertTrue(runCatching {VideoPolicy.rangeOffset("bytes=1-49",100)}.isFailure);assertTrue(runCatching {VideoPolicy.rangeOffset("bytes=0-199",100)}.isFailure)
    }
    @Test fun photographicNumbersAreStableAcrossOrderRetryAndHistoricalFiles() {
        val p=PhotoEntity("name9","record9",1,"local",true,null,null,null,"PENDING",1)
        val first=StablePhotoNames.name(context,p,true);assertTrue(first.startsWith("001_F_"));assertEquals(first,StablePhotoNames.name(context,p.copy(photoIndex=7,driveFileId="fixed"),true))
        val second=StablePhotoNames.name(context,p.copy(id="name9b"),true);assertTrue(second.startsWith("002_F_"));assertNotEquals(first,second)
        assertTrue(StablePhotoNames.name(context,p.copy(id="historical9",driveFileId="old"),true).startsWith("F_"))
    }
    @Test fun importCopiesMultipleOriginalsAndRejectsMimeSizeWithoutFictitiousRows()=runBlocking {
        Room.inMemoryDatabaseBuilder(context,VideoDatabase::class.java).build().use {db->
            val directory=File(context.filesDir,"videos/capture").apply{mkdirs()}
            val source=File(directory,"source-import9.mp4").apply {writeBytes(byteArrayOf(0,0,0,24,102,116,121,112,105,115,111,109))}
            // Windows Robolectric cannot use Android FileProvider '/' root checks.
            // Exercise the resolver stream with a SAF-like provider; physical capture
            // and the manifest FileProvider remain tablet checks.
            val provider=object:android.content.ContentProvider() {
                override fun onCreate()=true
                override fun getType(uri:android.net.Uri)=if(uri.lastPathSegment!!.endsWith(".mp4"))"video/mp4" else "image/jpeg"
                override fun openFile(uri:android.net.Uri,mode:String)=android.os.ParcelFileDescriptor.open(File(directory,uri.lastPathSegment!!),android.os.ParcelFileDescriptor.MODE_READ_ONLY)
                override fun query(uri:android.net.Uri,projection:Array<out String>?,selection:String?,selectionArgs:Array<out String>?,sortOrder:String?):android.database.Cursor?=null
                override fun insert(uri:android.net.Uri,values:android.content.ContentValues?):android.net.Uri?=null
                override fun delete(uri:android.net.Uri,selection:String?,selectionArgs:Array<out String>?)=0
                override fun update(uri:android.net.Uri,values:android.content.ContentValues?,selection:String?,selectionArgs:Array<out String>?)=0
            }
            provider.attachInfo(context,android.content.pm.ProviderInfo().apply {authority="videos.test";exported=false;grantUriPermissions=true})
            org.robolectric.shadows.ShadowContentResolver.registerProviderInternal("videos.test",provider)
            fun uri(file:File)=android.net.Uri.parse("content://videos.test/${file.name}")
            val storage=VideoStorage(context,db.videos(),inspect={require(it.isFile)},schedule={})
            val a=storage.import("SCAP/import9","GENERAL","General",uri(source))
            val b=storage.import("SCAP/import9","GENERAL","General",uri(source))
            assertNotEquals(a.id,b.id);assertEquals(a.sha256,b.sha256);assertEquals(2,db.videos().all().size)
            assertEquals(source.readBytes().toList(),File(a.localPath).readBytes().toList());assertTrue(source.isFile)
            val invalid=File(directory,"mime9.jpg").apply {writeText("not a video")}
            assertTrue(runCatching {storage.import("SCAP/import9","GENERAL","General",uri(invalid))}.isFailure)
            val large=File(directory,"large9.mp4");java.io.RandomAccessFile(large,"rw").use {it.setLength(VideoPolicy.MAX_BYTES+1)}
            assertTrue(runCatching {storage.import("SCAP/import9","GENERAL","General",uri(large))}.isFailure)
            assertEquals(2,db.videos().all().size);assertTrue(large.isFile)
        }
    }
}
