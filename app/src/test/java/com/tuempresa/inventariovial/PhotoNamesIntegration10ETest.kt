package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.provider.Settings
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.access.DeviceAccessManager
import com.tuempresa.inventariovial.core.*
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.PhotoEntity
import com.tuempresa.inventariovial.sic.*
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

/** Run with the ordinary local Debug configuration; all HTTP is intercepted and fictitious. */
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],application=Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PhotoNamesIntegration10ETest {
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    private fun database()=Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build()
    private suspend fun save(db:InventoryDatabase,id:String):PhotoEntity {
        SicFieldRepository(db).save(id,fajaState(SicFieldFormat.SIC08),"synthetic-engineer",true)
        val file=File(context.cacheDir,"$id.jpg")
        val bitmap=Bitmap.createBitmap(32,24,Bitmap.Config.ARGB_8888)
        file.outputStream().use {assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG,95,it))};bitmap.recycle()
        return PhotoEntity("photo-$id",id,1,file.path,true,null,null,null,"PENDING",1,photoCategory="GENERAL").also {db.inventoryDao().insertPhoto(it)}
    }
    private fun prefix(p:JSONObject)="${p.getString("routeCode")}/${p.getString("familyPath")}/${p.getString("family")} 1"
    private fun receipt(p:JSONObject):JSONObject {
        val files=p.getJSONArray("files");val rows=p.getJSONArray("sicRows")
        return JSONObject().put("ok",true).put("syncProtocol",3).put("pathVersion",4)
            .put("key",p.getString("key")).put("revision",p.getLong("revision")).put("contentHash",p.getString("contentHash"))
            .put("folderId","stable-folder").put("logicalPath",prefix(p))
            .put("technicalDeliveryStatus","DELIVERED").put("georeferencingStatus","PENDING_EXTERNAL").put("externalGeoreferencingStatus","PENDING")
            .put("files",JSONArray((0 until files.length()).map {n->val f=files.getJSONObject(n)
                JSONObject().put("key",f.getString("key")).put("sha256",f.getString("sha256")).put("fileId","stable-file-$n").put("folderId","stable-folder")
                    .put("requestedPath",f.getString("path")).put("logicalPath",prefix(p)+"/"+f.getString("path"))}))
            .put("sicFiles",JSONArray((0 until rows.length()).map {n->val r=rows.getJSONObject(n)
                JSONObject().put("format",r.getString("format")).put("fileId","stable-excel").put("folderId","stable-route-folder").put("sha256","a".repeat(64))
                    .put("logicalPath","${p.getString("routeCode")}/EXCEL_SIC/${r.getString("format")}.xlsx").put("recordIds",JSONArray(listOf(r.getString("recordId"))))}))
    }
    private fun capabilities(version:Int)=JSONObject().put("ok",true).put("syncProtocol",3).put("pathVersion",4)
        .put("stableFamilyNumbering",true).put("photoCategoryVersion",1).put("photoNamingVersion",1)
        .put("readablePhotoNamingVersion",version).put("technicalDeliveryWithoutGps",true)
    private fun http(version:Int=2,post:(JSONObject)->JSONObject={error("Unexpected POST")})=AutomaticDriveHttp(
        OkHttpClient.Builder().addInterceptor {chain->
            val request=chain.request();val response=if(request.method=="GET")capabilities(version) else {
                val buffer=Buffer();request.body!!.writeTo(buffer);post(JSONObject(buffer.readUtf8()))
            }
            Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body(response.toString().toResponseBody("application/json".toMediaType())).build()
        }.build(),"https://example.invalid/10e","synthetic-token")

    @Test fun ordinaryDebugUsesLocalV2AndDiagnosticsReportNegotiatedProtocol() {
        assertTrue("Use ordinary Debug with server.local.properties V2=true",BuildConfig.GBO_PHOTO_NAMES_V2_ENABLED)
        assertEquals("com.tuempresa.inventariovial",BuildConfig.APPLICATION_ID)
        val ready=http().photoNamingDiagnostic();assertTrue(ready.enabled);assertEquals(2,ready.readableVersion)
        assertTrue(ready.nextDelivery.contains("nombres V2 con reserva"))
        val old=http(0).photoNamingDiagnostic();assertEquals(0,old.readableVersion);assertTrue(old.nextDelivery.contains("detenida"))
        assertFalse(ready.text().contains("synthetic-token"));assertFalse(ready.text().contains("https://"))
        assertTrue(PhotoNamingDiagnostic(true,2,false,1,3).nextDelivery.contains("requiere"))
    }
    @Test fun v2ReservationRetryAndCompleteAckKeepUuidNamesAndBinding()=runBlocking(Dispatchers.IO) {database().use {db->
        val photo=save(db,"new-10e");var reserves=0;var commits=0;val stages=mutableMapOf<String,Int>()
        val backend=http {p->when(p.getString("action")) {
            "reservePhotoNamesV2" -> {reserves++;JSONObject().put("ok",true).put("photoNamingVersion",2)
                .put("key",p.getString("key")).put("revision",p.getLong("revision")).put("identityId",p.getString("recordId"))
                .put("folderId","stable-folder").put("logicalPath",prefix(p))
                .put("photos",JSONArray().put(JSONObject().put("id",photo.id).put("category","GENERAL").put("name","001_FAJ1_GEN.jpg").put("historical",false)))}
            "stageRecordV3" -> {val f=p.getJSONObject("file");val key=f.getString("key");stages[key]=(stages[key] ?: 0)+1
                JSONObject().put("ok",true).put("key",p.getString("key")).put("revision",p.getLong("revision"))
                    .put("fileKey",key).put("sha256",f.getString("sha256")).put("stageFileId","stage-$key")}
            else -> {commits++;assertEquals(2,p.getInt("photoNamingVersion"));receipt(p).apply {if(commits==1)getJSONArray("files").getJSONObject(0).remove("fileId")}}
        }}
        assertTrue(AutomaticSyncDrain.run(context,db,{backend},null))
        val pending=db.automaticSyncDao().all().single();assertEquals("ERROR",pending.state);assertEquals(0L,pending.syncedRevision)
        val snapshot=pending.preparedJson!!;assertTrue(snapshot.contains("001_FAJ1_GEN.jpg"))
        assertFalse(AutomaticSyncDrain.run(context,db,{backend},null))
        val confirmed=db.automaticSyncDao().all().single();assertTrue(SyncIntentPresentation.confirmed(confirmed))
        assertEquals(pending.recordId,confirmed.recordId);assertEquals(pending.revision,confirmed.revision)
        assertEquals(snapshot,confirmed.preparedJson);assertEquals(1,reserves);assertEquals(2,commits);assertTrue(stages.values.all {it==1})
        assertEquals("stable-folder",DriveInventorySchema.receipt(context,photo.recordId)!!.getString("folderId"))
        assertEquals("001_FAJ1_GEN.jpg",ReadablePhotoNames.name(context,photo))
    }}
    @Test fun cachedV1DeliveryUsesOriginalBytesAndNamesWithoutV2Reservation()=runBlocking(Dispatchers.IO) {database().use {db->
        val photo=save(db,"pending-v1-10e");val oldName=StablePhotoNames.name(context,photo,true)
        val intent=db.automaticSyncDao().all().single();val prepared=AutomaticDelivery.prepare(context,db,intent,readable=false)
        assertEquals(1,prepared.getInt("photoNamingVersion"));assertTrue(prepared.toString().contains(oldName))
        db.automaticSyncDao().prepared(intent.key,intent.revision,prepared.toString())
        val hashes=(0 until prepared.getJSONArray("files").length()).map {prepared.getJSONArray("files").getJSONObject(it).getString("sha256")}
        var posts=0
        val backend=object:AutomaticDriveTransport {
            override fun supportsV3()=true;override fun supportsFamilyPaths()=true
            override fun supportsTechnicalDeliveryWithoutGps()=true
            override fun supportsReadablePhotoNames():Boolean=error("V1 cache must not negotiate a new reservation")
            override fun post(payload:JSONObject):JSONObject {
                posts++;assertEquals(prepared.toString(),payload.toString());return receipt(payload)
            }
        }
        assertFalse(AutomaticSyncDrain.run(context,db,{backend},null));assertEquals(1,posts)
        val confirmed=db.automaticSyncDao().all().single();assertTrue(SyncIntentPresentation.confirmed(confirmed))
        val cached=JSONObject(confirmed.preparedJson!!)
        assertEquals(1,cached.getInt("photoNamingVersion"));assertTrue(cached.toString().contains(oldName))
        assertEquals(hashes,(0 until cached.getJSONArray("files").length()).map {cached.getJSONArray("files").getJSONObject(it).getString("sha256")})
    }}
    @Test fun historicalReservationKeepsExistingV1Name() {
        val id="historical-10e";val name="F_0123456789abcdef.jpg"
        val p=JSONObject().put("key","SIC/$id").put("kind","SIC").put("recordId",id).put("revision",7).put("pathVersion",4)
            .put("routeCode","R10E").put("familyPath","ALCANTARILLAS").put("family","ALCANTARILLA")
            .put("files",JSONArray().put(JSONObject().put("key","photo/$id").put("path","PANORAMICA/$name").put("photoCategory","CULVERT_PANORAMIC")))
        val ack=JSONObject().put("ok",true).put("photoNamingVersion",2).put("key",p.getString("key")).put("revision",7)
            .put("identityId",id).put("folderId","old-folder").put("logicalPath","R10E/ALCANTARILLAS/ALCANTARILLA 7")
            .put("photos",JSONArray().put(JSONObject().put("id",id).put("category","CULVERT_PANORAMIC").put("name",name).put("historical",true)))
        ReadablePhotoNames.remember(context,p,ack);assertEquals(name,ReadablePhotoNames.name(context,id,"CULVERT_PANORAMIC"))
        assertTrue(runCatching {ReadablePhotoNames.remember(context,p,ack.apply {getJSONArray("photos").getJSONObject(0).put("name","001_ALC7_PAN.jpg")})}.isFailure)
        assertEquals(name,ReadablePhotoNames.name(context,id,"CULVERT_PANORAMIC"))
    }
    @Test fun managerAndRoomRecreationPreserveTabletIdentityAndPendingRecords()=runBlocking(Dispatchers.IO) {
        Settings.Secure.putString(context.contentResolver,Settings.Secure.ANDROID_ID,"synthetic-tablet-10e")
        val first=DeviceAccessManager(context).fingerprint()
        val prefs=context.getSharedPreferences("device_access",Context.MODE_PRIVATE)
        prefs.edit().putString("unrelated-preserved","sentinel").commit()
        val name="identity-update-10e.db"
        Room.databaseBuilder(context,InventoryDatabase::class.java,name).build().use {save(it,"identity-record-10e")}
        assertEquals(first,DeviceAccessManager(context).fingerprint());assertEquals("sentinel",prefs.getString("unrelated-preserved",null))
        assertFalse(prefs.contains("legacy_fingerprint_hash"))
        Room.databaseBuilder(context,InventoryDatabase::class.java,name).build().use {db->
            assertNotNull(db.inventoryDao().recordById("identity-record-10e"));assertEquals("SIC/identity-record-10e",db.automaticSyncDao().all().single().key)
            assertEquals(0L,db.automaticSyncDao().all().single().syncedRevision)
        }
    }
}
