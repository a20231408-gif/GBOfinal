package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
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
import java.io.*

@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],application=Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SyncRecovery10DTest {
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    private fun database()=Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build()
    private suspend fun save(d:InventoryDatabase,id:String) {
        SicFieldRepository(d).save(id,fajaState(SicFieldFormat.SIC08),"synthetic-engineer",true)
        val file=File(context.cacheDir,"$id.jpg");val bitmap=Bitmap.createBitmap(32,24,Bitmap.Config.ARGB_8888)
        file.outputStream().use {assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG,95,it))};bitmap.recycle()
        d.inventoryDao().insertPhoto(PhotoEntity("photo-$id",id,1,file.path,true,null,null,null,"PENDING",1,photoCategory="GENERAL"))
    }
    private fun receipt(p:JSONObject):JSONObject {
        val prefix="${p.getString("routeCode")}/${p.getString("familyPath")}/${p.getString("family")} 1"
        val files=p.getJSONArray("files");val rows=p.getJSONArray("sicRows")
        return JSONObject().put("ok",true).put("syncProtocol",3).put("pathVersion",4)
            .put("key",p.getString("key")).put("revision",p.getLong("revision")).put("contentHash",p.getString("contentHash"))
            .put("folderId","stable-folder").put("logicalPath",prefix)
            .put("technicalDeliveryStatus","DELIVERED").put("georeferencingStatus","PENDING_EXTERNAL").put("externalGeoreferencingStatus","PENDING")
            .put("files",JSONArray((0 until files.length()).map {n->val f=files.getJSONObject(n)
                JSONObject().put("key",f.getString("key")).put("sha256",f.getString("sha256")).put("fileId","stable-file-$n").put("folderId","stable-folder")
                    .put("requestedPath",f.getString("path")).put("logicalPath",prefix+"/"+f.getString("path"))}))
            .put("sicFiles",JSONArray((0 until rows.length()).map {n->val r=rows.getJSONObject(n)
                JSONObject().put("format",r.getString("format")).put("fileId","stable-excel").put("folderId","stable-route-folder").put("sha256","a".repeat(64))
                    .put("logicalPath","${p.getString("routeCode")}/EXCEL_SIC/${r.getString("format")}.xlsx").put("recordIds",JSONArray(listOf(r.getString("recordId"))))}))
    }
    private fun transport(post:(JSONObject)->JSONObject)=object:AutomaticDriveTransport {
        override fun supportsV3()=true
        override fun supportsFamilyPaths()=true
        override fun supportsTechnicalDeliveryWithoutGps()=true
        override fun post(payload:JSONObject)=post(payload)
    }
    @Test fun lostFinalHttpAckReplaysCommitAndNeverRestagesVerifiedFiles()=runBlocking(Dispatchers.IO) {database().use {d->
        save(d,"lost-final-10d");val intent=d.automaticSyncDao().all().single()
        val prepared=AutomaticDelivery.prepare(context,d,intent);val before=prepared.toString()
        val stages=mutableMapOf<String,Int>();var commits=0
        val phases=mutableListOf<DeliveryProgress>()
        val client=OkHttpClient.Builder().addInterceptor {chain->
            val request=chain.request()
            val response=if(request.method=="GET")JSONObject().put("ok",true).put("syncProtocol",3).put("pathVersion",4).put("stableFamilyNumbering",true).put("photoCategoryVersion",1).put("photoNamingVersion",1).put("technicalDeliveryWithoutGps",true)
            else {
                val buffer=Buffer();request.body!!.writeTo(buffer);val p=JSONObject(buffer.readUtf8())
                if(p.getString("action")=="stageRecordV3") {
                    val f=p.getJSONObject("file");val key=f.getString("key");stages[key]=(stages[key] ?: 0)+1
                    JSONObject().put("ok",true).put("key",p.getString("key")).put("revision",p.getLong("revision")).put("fileKey",key).put("sha256",f.getString("sha256")).put("stageFileId","stage-$key")
                } else {commits++;if(commits==1)throw IOException("Synthetic lost final ACK after commit");receipt(p)}
            }
            Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(200).message("OK").body(response.toString().toResponseBody("application/json".toMediaType())).build()
        }.build()
        fun http()=AutomaticDriveHttp(client,"https://example.invalid/10d","synthetic-token").apply {progress {phases.add(it)}}
        assertTrue(runCatching {http().post(prepared)}.isFailure)
        assertEquals(0L,d.automaticSyncDao().get(intent.key)!!.syncedRevision)
        val ack=http().post(prepared)
        assertTrue(AutomaticDelivery.validAck(prepared,ack,null));assertEquals(2,commits)
        assertEquals(prepared.getJSONArray("files").length(),stages.size);assertTrue(stages.values.all {it==1})
        assertEquals(before,prepared.toString());assertTrue(phases.any {it.phase=="PHOTOS"});assertTrue(phases.any {it.phase=="CONFIRMING"})
    }}
    @Test fun validAckConfirmsButIncompleteAckConservesSnapshot()=runBlocking(Dispatchers.IO) {database().use {d->
        save(d,"incomplete-10d")
        assertTrue(AutomaticSyncDrain.run(context,d,{transport {receipt(it).apply {getJSONArray("files").getJSONObject(0).remove("fileId")}}},null))
        val failed=d.automaticSyncDao().all().single();assertEquals("ERROR",failed.state);assertEquals(0L,failed.syncedRevision);assertNotNull(failed.preparedJson)
        assertFalse(AutomaticSyncDrain.run(context,d,{transport(::receipt)},null))
        val synced=d.automaticSyncDao().all().single();assertTrue(SyncIntentPresentation.confirmed(synced));assertEquals(failed.revision,synced.revision)
    }}
    @Test fun editDuringDeliveryKeepsUuidAndUsesLateAckAsBaseOnly()=runBlocking(Dispatchers.IO) {database().use {d->
        save(d,"edit-inflight-10d");val first=d.automaticSyncDao().all().single();var once=true
        val backend=transport {p->
            if(once) {once=false;runBlocking {d.withTransaction {
                val r=d.inventoryDao().recordById(first.recordId)!!;val next=maxOf(r.updatedAt+1,System.currentTimeMillis())
                d.inventoryDao().updateRecord(r.copy(updatedAt=next,observations="Edited during delivery"))
                AutomaticSync.request(d,"SIC",r.id,next)
            }}}
            receipt(p)
        }
        assertTrue(AutomaticSyncDrain.run(context,d,{backend},null))
        val edited=d.automaticSyncDao().all().single();assertEquals(first.recordId,edited.recordId);assertTrue(edited.revision>first.revision)
        assertEquals(first.revision,edited.syncedRevision);assertEquals("UPDATE_PENDING",edited.state);assertFalse(SyncIntentPresentation.confirmed(edited))
        assertFalse(AutomaticSyncDrain.run(context,d,{transport {p->assertEquals(first.revision,p.getLong("baseRevision"));receipt(p)}},null))
        assertTrue(SyncIntentPresentation.confirmed(d.automaticSyncDao().all().single()))
    }}
    @Test fun cancelledWorkerClearsActiveStateAndRetainsExactPreparation()=runBlocking(Dispatchers.IO) {database().use {d->
        save(d,"cancelled-10d")
        try {AutomaticSyncDrain.run(context,d,{transport {throw CancellationException("Synthetic Android cancellation")}},null);fail("Expected cancellation")}catch(_:CancellationException){}
        val intent=d.automaticSyncDao().all().single();assertEquals("ERROR",intent.state);assertNotNull(intent.preparedJson);assertEquals(0L,intent.syncedRevision)
        assertFalse(AutomaticSyncDrain.run(context,d,{transport(::receipt)},null));assertTrue(SyncIntentPresentation.confirmed(d.automaticSyncDao().all().single()))
    }}
    @Test fun inactiveUploadingNeverLooksActiveAndProgressUsesOnlyRealFileCounts() {
        val intent=DriveSyncIntent("SIC/stale","SIC","stale",1,state="UPLOADING")
        assertTrue(SyncIntentPresentation.label(intent,true,true,true).contains("esperando ejecución"))
        assertEquals("Pendiente de conexión",SyncIntentPresentation.label(intent,true,false,true))
        assertEquals("Subiendo",SyncIntentPresentation.label(intent,true,true,true,running=true))
        assertTrue(SyncIntentPresentation.label(intent.copy(state="ERROR"),true,true,true,retrying=true).startsWith("Reintentando"))
        assertFalse(SyncProgressPresentation.running(intent,emptyList()))
        val p=SyncProgress(1,"CONFIRMING",2,4,1000)
        assertTrue(SyncProgressPresentation.files(p).contains("50% de archivos transferidos"));assertTrue(SyncProgressPresentation.files(p).contains("pendientes: 2"))
        assertTrue(SyncProgressPresentation.elapsed(p,61000).contains("1 min 0 s"));assertFalse(SyncIntentPresentation.confirmed(intent))
    }
}
