package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import java.io.File
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.core.*
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.model.form.*
import com.tuempresa.inventariovial.repository.*
import kotlinx.coroutines.*
import org.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HotfixBusy146Test {
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    private fun db()=Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build()
    private suspend fun save(d:InventoryDatabase,id:String,pr:String="1") {
        val file=File(context.cacheDir,"$id.jpg");val bitmap=Bitmap.createBitmap(24,24,Bitmap.Config.ARGB_8888)
        file.outputStream().use {bitmap.compress(Bitmap.CompressFormat.JPEG,90,it)};bitmap.recycle()
        InventoryRepository(d).saveSic22(InventoryRecordEntity(id,"SIC-22","VERTICAL","PE-22A","CD",pr,20.0,null,null,"D",
            null,null,null,null,"15/10/2026","FICTICIO","ACTIVE","SYNCED","PENDING",1,1),
            Sic22Entity(id,"20","1","2","R-1",null,"1",null,null,null),
            listOf(PhotoEntity("photo-$id",id,1,file.path,true,null,null,null,"PENDING",1,photoCategory="GENERAL")))
    }
    private class Backend:AutomaticDriveTransport {
        var busy=true;var posts=0
        val records=mutableMapOf<String,JSONObject>();val receipts=mutableMapOf<String,JSONObject>()
        override fun supportsV3()=true
        override fun supportsFamilyPaths()=true
        override fun supportsTechnicalDeliveryWithoutGps()=true
        override fun supportsReadablePhotoNames()=true
        // These cases force BUSY on commit, after a successful V2 reservation.
        override fun reservePhotoNames(payload:JSONObject)=ReleasePhotoV2Fixture.reservation(payload,"folder-${payload.getString("key")}",if(payload.getString("key").endsWith("B"))2 else 1)
        override fun post(payload:JSONObject):JSONObject {
            posts++
            if(busy)return JSONObject().put("ok",false).put("code","BUSY") // Reproduce a missing retryable flag too.
            val p=JSONObject(payload.toString());val key=p.getString("key");val old=records[key]
            if(old!=null && old.getLong("revision")==p.getLong("revision")) {
                check(old.getString("contentHash")==p.getString("contentHash"));return JSONObject(receipts.getValue(key).toString())
            }
            assertEquals("Base de la revisión ficticia",old?.getLong("revision") ?: 0L,p.getLong("baseRevision"))
            val prefix="PE-22A/SENALIZACION_VERTICAL/SENAL ${if(key.endsWith("B"))2 else 1}"
            val ack=JSONObject().put("ok",true).put("syncProtocol",3).put("pathVersion",4).put("key",key)
                .put("revision",p.getLong("revision")).put("contentHash",p.getString("contentHash"))
                .put("folderId","folder-$key").put("logicalPath",prefix)
                .put("technicalDeliveryStatus","DELIVERED").put("georeferencingStatus","PENDING_EXTERNAL").put("externalGeoreferencingStatus","PENDING")
                .put("files",JSONArray((0 until p.getJSONArray("files").length()).map {n->val f=p.getJSONArray("files").getJSONObject(n)
                    JSONObject().put("key",f.getString("key")).put("sha256",f.getString("sha256")).put("fileId","file-$key-$n")
                        .put("folderId","folder-$key").put("requestedPath",f.getString("path")).put("logicalPath",prefix+"/"+f.getString("path"))}))
                .put("sicFiles",JSONArray((0 until p.getJSONArray("sicRows").length()).map {n->val row=p.getJSONArray("sicRows").getJSONObject(n)
                    JSONObject().put("format",row.getString("format")).put("sha256",row.getString("sha256")).put("fileId","excel-sic22").put("folderId","excel-folder")
                        .put("logicalPath","PE-22A/EXCEL_SIC/SIC-22.xlsx").put("recordIds",JSONArray(listOf(p.getString("recordId"))))}))
            assertTrue("ACK ficticio incompleto: $ack",AutomaticDelivery.validAck(p,ack))
            records[key]=p;receipts[key]=ack;return ack
        }
    }
    private suspend fun drain(d:InventoryDatabase,b:Backend,attempt:Int=0)=AutomaticSyncDrain.run(context,d,{b},null,attempt).also {retry->
        if(retry && !b.busy)println("FICTITIOUS_ONLY: ${d.automaticSyncDao().all().map {Triple(it.state,it.error,it.syncedRevision)}}; remote=${b.records.keys}; posts=${b.posts}")
    }
    private suspend fun edit(d:InventoryDatabase,id:String) {
        val s=d.inventoryDao().snapshot(id)!!;val form=EngineeringEdits.form(s) as SicFormDetail.Sic22
        EngineeringEdits.save(d,id,form.copy(state=form.state.copy(conditionCode="2")),s.photos.map{it.localPath},s.photos.associate{it.localPath to "GENERAL"},expectedUpdatedAt=s.record.updatedAt)
    }
    @Test fun busyThenEditReplaysExactPredecessorAndConfirmsOnlyCurrentRevision()=runBlocking {db().use {d->
        save(d,"TEST_AUTO_146_BUSY_A");val b=Backend();assertTrue(drain(d,b))
        val before=d.automaticSyncDao().all().single();assertEquals("ERROR",before.state);assertTrue(before.error!!.contains("(BUSY)"));assertEquals(0L,before.syncedRevision)
        val snapshot=before.preparedJson!!;edit(d,before.recordId)
        assertEquals(snapshot,d.automaticSyncDao().all().single().preparedJson)
        b.busy=false;assertFalse(drain(d,b))
        val final=d.automaticSyncDao().all().single();assertEquals("SYNCED",final.state);assertEquals(final.revision,final.syncedRevision)
        assertEquals(1,b.records.size);assertEquals(3,b.posts);assertEquals("2",d.inventoryDao().snapshot(before.recordId)!!.sic22!!.conditionCode)
        val photo=d.inventoryDao().snapshot(before.recordId)!!.photos.single()
        assertEquals("photo-${before.recordId}",photo.id);assertTrue(File(photo.localPath).isFile);assertEquals("SYNCED",photo.syncStatus)
    }}
    @Test fun busyOnAnUpdatePreservesPreviouslyVerifiedAck()=runBlocking {db().use {d->
        save(d,"TEST_AUTO_146_ACK_A");val b=Backend();b.busy=false;assertFalse(drain(d,b));val old=d.automaticSyncDao().all().single()
        edit(d,old.recordId);b.busy=true;assertTrue(drain(d,b));val waiting=d.automaticSyncDao().all().single()
        assertEquals(old.receiptJson,waiting.receiptJson);assertEquals(old.syncedRevision,waiting.syncedRevision);assertFalse(SyncIntentPresentation.confirmed(waiting))
        b.busy=false;assertFalse(drain(d,b));val next=d.automaticSyncDao().all().single()
        assertTrue(SyncIntentPresentation.confirmed(next));assertEquals(JSONObject(old.receiptJson!!).getString("folderId"),JSONObject(next.receiptJson!!).getString("folderId"))
    }}
    @Test fun independentDeviceDatabasesKeepBothUuidsAfterServerWait()=runBlocking {db().use {a->db().use {b->
        save(a,"TEST_AUTO_146_DEVICE_A","10");save(b,"TEST_AUTO_146_DEVICE_B","2");val server=Backend()
        assertTrue(drain(a,server));assertTrue(drain(b,server));server.busy=false
        assertFalse(drain(b,server));assertFalse(drain(a,server))
        assertEquals(2,server.records.size);assertEquals(1,a.automaticSyncDao().all().size);assertEquals(1,b.automaticSyncDao().all().size)
        assertTrue(SyncIntentPresentation.confirmed(a.automaticSyncDao().all().single()));assertTrue(SyncIntentPresentation.confirmed(b.automaticSyncDao().all().single()))
        assertNull(a.inventoryDao().recordById("TEST_AUTO_146_DEVICE_B"));assertNull(b.inventoryDao().recordById("TEST_AUTO_146_DEVICE_A"))
    }}}
    @Test fun boundedImmediateRetriesPreserveOutboxForPeriodicRecovery()=runBlocking {db().use {d->
        save(d,"TEST_AUTO_146_LIMIT_A");assertFalse(drain(d,Backend(),4))
        assertEquals("ERROR",d.automaticSyncDao().all().single().state);assertEquals(0L,d.automaticSyncDao().all().single().syncedRevision)
    }}
}
