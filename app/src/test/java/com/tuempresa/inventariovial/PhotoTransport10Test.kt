package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.media.ExifInterface
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.core.*
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.PhotoEntity
import com.tuempresa.inventariovial.delivery.sha256
import com.tuempresa.inventariovial.sic.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.json.JSONArray
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],application=Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PhotoTransport10Test {
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    private fun image(name:String,format:Bitmap.CompressFormat):File {
        val file=File(context.cacheDir,name);val b=Bitmap.createBitmap(48,32,Bitmap.Config.ARGB_8888)
        b.eraseColor(android.graphics.Color.BLUE)
        try {file.outputStream().use {assertTrue(b.compress(format,95,it))}}finally {b.recycle()}
        return file
    }
    @Test fun jpegCameraWithTrailingBytesReproducesServerRejectionAndCopyRecovers() {
        val original=image("camera10.jpg",Bitmap.CompressFormat.JPEG)
        original.appendBytes(byteArrayOf(0,0,0,0));val bytes=original.readBytes()
        // Exact check in v3Store.isJpeg: valid decoded camera JPEG, rejected because EOI is not at EOF.
        assertFalse((bytes[bytes.size-2].toInt() and 255)==255 && (bytes.last().toInt() and 255)==217)
        val target=File(context.cacheDir,"camera-delivery10.jpg");DeliveryJpeg.copy(original,target)
        assertTrue(JpegEnvelope.valid(target.readBytes()));assertArrayEquals(bytes,original.readBytes())
    }
    @Test fun pngAndWebpMislabeledJpgBecomeVerifiedJpegAndRetainOriginal() {
        listOf(Bitmap.CompressFormat.PNG,Bitmap.CompressFormat.WEBP_LOSSLESS).forEachIndexed {n,format->
            val original=image("import10-$n.jpg",format);val bytes=original.readBytes()
            val target=File(context.cacheDir,"converted10-$n.jpg");DeliveryJpeg.copy(original,target)
            assertTrue(JpegEnvelope.valid(target.readBytes()));assertArrayEquals(bytes,original.readBytes())
            assertEquals(sha256(target.readBytes()),sha256(android.util.Base64.decode(android.util.Base64.encodeToString(target.readBytes(),2),2)))
        }
    }
    @Test fun allExifOrientationsAndCapturedMetadataSurviveWithoutChangingPixelsOrOriginal() {
        for(orientation in 1..8) {
            val original=image("orientation10-$orientation.jpg",Bitmap.CompressFormat.JPEG)
            ExifInterface(original.path).apply {setAttribute("Orientation",orientation.toString());saveAttributes()}
            val bytes=original.readBytes();val target=File(context.cacheDir,"exif10-$orientation.jpg")
            DeliveryJpeg.copy(original,target,PhotoEntity("id","record",1,original.path,true,null,null,null,"PENDING",1,capturedAt=1000))
            assertTrue(JpegEnvelope.valid(target.readBytes()));assertEquals(orientation,ExifInterface(target.path).getAttributeInt("Orientation",0));assertArrayEquals(bytes,original.readBytes())
        }
    }
    @Test fun videoCorruptionAndTruncatedJpegNeverBecomePhotos() {
        val original=image("truncated10.jpg",Bitmap.CompressFormat.JPEG);original.writeBytes(original.readBytes().copyOf(12))
        for(file in listOf(original,File(context.cacheDir,"video10.jpg").apply {writeText("ftypisom not a photo")})) {
            assertTrue(runCatching {DeliveryJpeg.copy(file,File(context.cacheDir,"invalid10.jpg"))}.isFailure)
            assertTrue(file.isFile)
        }
        assertNull(JpegEnvelope.end(byteArrayOf(-1,-40,-1,-39)))
    }
    @Test fun rejectedCachedRevisionIsBackedUpAndRebuiltWithSameIdentityAndPhoto()=runBlocking {
        Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build().use {db->
            val id="jpeg-recovery10";SicFieldRepository(db).save(id,fajaState(SicFieldFormat.SIC08),"engineer",true)
            val original=image("historic10.png",Bitmap.CompressFormat.PNG);val originalBytes=original.readBytes()
            db.inventoryDao().insertPhoto(PhotoEntity("photo-recovery10",id,1,original.path,true,null,null,null,"PENDING",1,photoCategory="GENERAL"))
            val intent=db.automaticSyncDao().get("SIC/$id")!!
            assertTrue("This regression must exercise the ordinary V2 profile",BuildConfig.GBO_PHOTO_NAMES_V2_ENABLED)
            val preview=AutomaticDelivery.namingPreview(context,db,intent)
            assertEquals("FAJA",preview.getString("family"))
            var reservations=0
            val client=okhttp3.OkHttpClient.Builder().addInterceptor {chain->
                val response=if(chain.request().method=="GET")JSONObject().put("ok",true).put("syncProtocol",3)
                    .put("pathVersion",4).put("stableFamilyNumbering",true).put("photoCategoryVersion",1).put("readablePhotoNamingVersion",2)
                else {
                    val buffer=okio.Buffer();chain.request().body!!.writeTo(buffer)
                    val request=JSONObject(buffer.readUtf8());assertEquals("reservePhotoNamesV2",request.getString("action"));reservations++
                    val photos=request.getJSONArray("photos")
                    JSONObject().put("ok",true).put("photoNamingVersion",2).put("key",request.getString("key"))
                        .put("revision",request.getLong("revision")).put("identityId",request.optString("identityId",request.getString("recordId")))
                        .put("folderId","FICTITIOUS_JPEG_FOLDER")
                        .put("logicalPath","${request.getString("routeCode")}/${request.getString("familyPath")}/${request.getString("family")} 1")
                        .put("photos",JSONArray((0 until photos.length()).map {n->val photo=photos.getJSONObject(n)
                            JSONObject().put("id",photo.getString("id")).put("category",photo.getString("category"))
                                .put("name","001_FAJ1_GEN.jpg").put("historical",false)}))
                }
                okhttp3.Response.Builder().request(chain.request()).protocol(okhttp3.Protocol.HTTP_1_1).code(200).message("SYNTHETIC")
                    .body(response.toString().toResponseBody("application/json".toMediaType())).build()
            }.build()
            val transport=AutomaticDriveHttp(client,"https://example.invalid/jpeg-recovery","FICTITIOUS_ONLY")
            ReadablePhotoNames.remember(context,preview,transport.reservePhotoNames(preview))
            val reserved=ReadablePhotoNames.name(context,"photo-recovery10","GENERAL")
            assertEquals(1,reservations)
            val prepared=AutomaticDelivery.prepare(context,db,intent)
            assertEquals(AutomaticDelivery.contentHash(prepared),prepared.getString("contentHash"))
            val file=prepared.getJSONArray("files").let {a->(0 until a.length()).map {a.getJSONObject(it)}.single {it.getString("key").startsWith("photo/")}}
            File(file.getString("localFile")).writeBytes(originalBytes);file.put("sha256",sha256(originalBytes))
            prepared.put("contentHash",AutomaticDelivery.contentHash(prepared))
            val dir=File(context.filesDir,"automatic_drive/${sha256(intent.key.toByteArray())}/${intent.revision}")
            File(dir,"prepared-path4.json").writeText(prepared.toString());db.automaticSyncDao().prepared(intent.key,intent.revision,prepared.toString())
            db.automaticSyncDao().state(intent.key,intent.revision,"BLOCKED","El servidor rechazó la revisión (NOT_JPEG). La evidencia local se conserva.")
            assertEquals(1,AutomaticSync.recoverJpegBlocked(context,db));val recovered=db.automaticSyncDao().get(intent.key)!!
            assertEquals(intent.key,recovered.key);assertEquals(intent.recordId,recovered.recordId)
            assertEquals(intent.revision,recovered.revision);assertEquals(intent.syncedRevision,recovered.syncedRevision);assertFalse(SyncIntentPresentation.confirmed(recovered))
            val next=AutomaticDelivery.prepare(context,db,recovered)
            val repaired=next.getJSONArray("files").let {a->(0 until a.length()).map {a.getJSONObject(it)}.single {it.getString("key")==file.getString("key")}}
            assertTrue(JpegEnvelope.valid(File(repaired.getString("localFile")).readBytes()));assertEquals(file.getString("path"),repaired.getString("path"))
            assertEquals(reserved,repaired.getString("path").substringAfterLast('/'))
            assertEquals(reserved,ReadablePhotoNames.name(context,"photo-recovery10","GENERAL"))
            assertEquals(sha256(File(repaired.getString("localFile")).readBytes()),repaired.getString("sha256"))
            assertEquals(AutomaticDelivery.contentHash(next),next.getString("contentHash"))
            assertArrayEquals(originalBytes,original.readBytes());assertEquals(1,db.inventoryDao().snapshot(id)!!.photos.size)
            assertTrue(dir.parentFile!!.listFiles()!!.any {it.name.contains("NOT_JPEG") && File(it,"prepared-path4.json").isFile})
            val rejected=dir.parentFile!!.listFiles()!!.single {it.name.contains("NOT_JPEG")}
            val rejectedManifest=JSONObject(File(rejected,"prepared-path4.json").readText())
            assertEquals(prepared.toString(),rejectedManifest.toString())
            assertArrayEquals(originalBytes,File(rejected,File(file.getString("localFile")).name).readBytes())
            assertEquals(next.toString(),AutomaticDelivery.prepare(context,db,recovered).toString());assertEquals(0,AutomaticSync.recoverJpegBlocked(context,db))
            assertEquals(1,reservations)
        }
    }
    @Test fun unknownAckAndConflictsAreNeverRebuilt()=runBlocking {
        Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build().use {db->
            for(state in listOf("ERROR","CONFLICT","UPLOADING")) {
                val id="lost-$state";SicFieldRepository(db).save(id,fajaState(SicFieldFormat.SIC08),"engineer",true)
                val old=db.automaticSyncDao().get("SIC/$id")!!;db.automaticSyncDao().state(old.key,old.revision,state,"Lost ACK (NOT_JPEG)")
            }
            assertEquals(0,AutomaticSync.recoverJpegBlocked(context,db))
        }
    }
}
