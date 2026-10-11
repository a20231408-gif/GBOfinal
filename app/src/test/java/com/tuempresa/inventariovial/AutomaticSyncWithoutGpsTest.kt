package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.core.*
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.PhotoEntity
import com.tuempresa.inventariovial.sic.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Dispatchers
import org.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AutomaticSyncWithoutGpsTest {
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    private fun database()=Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build()
    private suspend fun save(d:InventoryDatabase,id:String,format:SicFieldFormat=SicFieldFormat.SIC08,finalize:Boolean=true) {
        SicFieldRepository(d).save(id,fajaState(format).set("observations","Campo sin GPS"),"test-engineer",finalize)
    }
    private suspend fun photo(d:InventoryDatabase,id:String) {
        val file=File(context.cacheDir,"$id.jpg")
        val bitmap=Bitmap.createBitmap(32,24,Bitmap.Config.ARGB_8888)
        file.outputStream().use {assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG,95,it))};bitmap.recycle()
        d.inventoryDao().insertPhoto(PhotoEntity("photo-$id",id,1,file.path,true,null,null,null,"PENDING",1,photoCategory="GENERAL"))
    }
    private class Backend:AutomaticDriveTransport {
        var posts=0;var mutations=0;var loseFirstAck=false;var invalidAck=false;var capable=true
        val receipts=mutableMapOf<String,JSONObject>()
        override fun supportsV3()=true
        override fun supportsTechnicalDeliveryWithoutGps()=capable
        override fun post(payload:JSONObject):JSONObject {
            posts++
            val key=payload.getString("key")+"/"+payload.getLong("revision")
            val receipt=receipts.getOrPut(key) {
                mutations++
                val prefix="${payload.getString("routeCode")}/${payload.getString("sibCode")}/FAJA $mutations"
                val files=JSONArray();val rows=payload.getJSONArray("sicRows");val aggregates=JSONArray()
                val expected=payload.getJSONArray("files")
                for(n in 0 until expected.length()) {
                    val f=expected.getJSONObject(n)
                    files.put(JSONObject().put("key",f.getString("key")).put("sha256",f.getString("sha256"))
                        .put("fileId","file-$mutations-$n").put("folderId","folder-$mutations")
                        .put("requestedPath",f.getString("path")).put("logicalPath",prefix+"/"+f.getString("path")))
                }
                for(n in 0 until rows.length()) {
                    val r=rows.getJSONObject(n)
                    aggregates.put(JSONObject().put("format",r.getString("format")).put("fileId","excel-${r.getString("format")}")
                        .put("folderId","sib-folder").put("sha256","a".repeat(64))
                        .put("logicalPath","${payload.getString("routeCode")}/${payload.getString("sibCode")}/${r.getString("format")}.xlsx")
                        .put("recordIds",JSONArray(listOf(r.getString("recordId")))))
                }
                JSONObject().put("ok",true).put("syncProtocol",3).put("pathVersion",3)
                    .put("key",payload.getString("key")).put("revision",payload.getLong("revision"))
                    .put("contentHash",payload.getString("contentHash")).put("folderId","folder-$mutations")
                    .put("logicalPath",prefix).put("files",files).put("sicFiles",aggregates)
                    .put("technicalDeliveryStatus","DELIVERED").put("georeferencingStatus","PENDING_EXTERNAL")
                    .put("externalGeoreferencingStatus","PENDING")
            }
            if(loseFirstAck) {loseFirstAck=false;throw IOException("Response lost after server commit")}
            return JSONObject(receipt.toString()).apply {if(invalidAck)getJSONArray("files").getJSONObject(0).remove("fileId")}
        }
    }
    private suspend fun drain(d:InventoryDatabase,b:Backend)=AutomaticSyncDrain.run(context,d,{b},null)

    @Test fun offlineQueueAndNullCoordinatesSurviveReopenThenBothFormatsDeliverPhotoAndExcel()=runBlocking(Dispatchers.IO) {
        val name="gps-offline-${System.nanoTime()}"
        fun open()=Room.databaseBuilder(context,InventoryDatabase::class.java,name).build()
        try {
            open().use {d->for(f in listOf(SicFieldFormat.SIC08,SicFieldFormat.SIC09)) {save(d,f.code,f);photo(d,f.code)}
                assertEquals(2,d.automaticSyncDao().all().count {it.state=="QUEUED"})}
            open().use {d->val b=Backend();assertFalse(drain(d,b));assertEquals(2,b.posts)
                for(i in d.automaticSyncDao().all()) {
                    assertEquals("SYNCED",i.state);assertEquals(i.revision,i.syncedRevision)
                    val r=d.inventoryDao().recordById(i.recordId)!!
                    assertNull(r.latitude);assertNull(r.longitude);assertNull(r.endLatitude);assertNull(r.endLongitude)
                    assertEquals("SYNCED",r.excelSyncStatus);assertEquals("SYNCED",r.photoSyncStatus)
                    val p=JSONObject(i.preparedJson!!)
                    assertEquals("CONTRACTUAL_PR",p.getString("orderSource"))
                    assertEquals("PENDING",p.getJSONObject("georeferencing").getString("externalSubmetricStatus"))
                    val fs=p.getJSONArray("files")
                    val metadata=(0 until fs.length()).map {fs.getJSONObject(it)}.single {it.getString("key")=="metadata"}
                    val m=JSONObject(File(metadata.getString("localFile")).readText())
                    assertTrue(m.isNull("latitude"));assertTrue(m.isNull("longitude"))
                    val stored=d.inventoryDao().photoById("photo-${i.recordId}")!!
                    assertEquals("SYNCED",stored.syncStatus);assertNotNull(stored.driveFileId);assertNull(stored.photoLatitude)
                    val root=File("../docs/sync-sin-gps-2026-10-11/examples/${i.recordId}").apply {mkdirs()}
                    val exported=JSONObject(p.toString());val outFiles=exported.getJSONArray("files")
                    for(n in 0 until outFiles.length()) {val f=outFiles.getJSONObject(n);f.put("base64",android.util.Base64.encodeToString(File(f.getString("localFile")).readBytes(),android.util.Base64.NO_WRAP));f.remove("localFile")}
                    File(root,"payload.json").writeText(exported.toString(2))
                    File(root,"simulated-receipt.json").writeText(i.receiptJson!!)
                }
            }
        }finally {context.deleteDatabase(name)}
    }
    @Test fun lostResponseRetriesSameRevisionAndIdsWithoutCreatingAnotherDelivery()=runBlocking(Dispatchers.IO) {database().use {d->
        save(d,"retry");photo(d,"retry");val b=Backend().apply {loseFirstAck=true}
        assertTrue(drain(d,b));assertEquals("ERROR",d.automaticSyncDao().all().single().state)
        assertNull(d.inventoryDao().photoById("photo-retry")!!.driveFileId)
        val first=b.receipts.values.single().toString()
        assertFalse(drain(d,b));assertEquals(2,b.posts);assertEquals(1,b.mutations)
        assertEquals(first,d.automaticSyncDao().all().single().receiptJson)
        assertFalse(drain(d,b));assertEquals(2,b.posts)
    }}
    @Test fun incompleteDraftsAreNeverDeliveredEvenWithOldGpsError()=runBlocking(Dispatchers.IO) {database().use {d->
        SicFieldRepository(d).save("draft",fajaState(SicFieldFormat.SIC09).set("endPrCode",""),"test",false)
        d.automaticSyncDao().put(DriveSyncIntent("SIC/draft","SIC","draft",1,state="PENDING_VALIDATION",error="Entrega pendiente de georreferenciación externa."))
        val b=Backend();assertFalse(drain(d,b));assertEquals(0,b.posts);assertEquals("DRAFT",d.inventoryDao().recordById("draft")!!.status)
        assertEquals("PENDING_VALIDATION",d.automaticSyncDao().all().single().state)
    }}
    @Test fun recoverFourGpsOnlyBlocksPreservesRevisionAndPreviousReceipts()=runBlocking(Dispatchers.IO) {database().use {d->
        for(n in 1..4) {save(d,"old$n");val i=d.automaticSyncDao().get("SIC/old$n")!!
            d.automaticSyncDao().put(i.copy(state="PENDING_VALIDATION",syncedRevision=if(n==4)1 else 0,receiptJson="{\"historical\":true}",error="Entrega pendiente de georreferenciación externa. Ficha y observaciones conservadas."))}
        save(d,"classification");val other=d.automaticSyncDao().get("SIC/classification")!!
        d.automaticSyncDao().put(other.copy(state="PENDING_VALIDATION",error="Pendiente de clasificación SIB."))
        val before=d.automaticSyncDao().all().associateBy {it.key}
        assertEquals(4,AutomaticSync.recoverGpsBlocked(d));assertEquals(0,AutomaticSync.recoverGpsBlocked(d))
        for(i in d.automaticSyncDao().all().filter {it.recordId.startsWith("old")}) {
            assertEquals(before[i.key]!!.revision,i.revision);assertEquals(before[i.key]!!.receiptJson,i.receiptJson)
            assertEquals(if(i.syncedRevision>0)"UPDATE_PENDING" else "QUEUED",i.state);assertNull(i.error)
        }
        assertEquals("PENDING_VALIDATION",d.automaticSyncDao().get(other.key)!!.state)
    }}
    @Test fun oldBackendCannotReceiveNoGpsFilesAndQueueIsConserved()=runBlocking(Dispatchers.IO) {database().use {d->
        save(d,"unsupported");photo(d,"unsupported");val b=Backend().apply {capable=false}
        assertFalse(drain(d,b));val i=d.automaticSyncDao().all().single()
        assertEquals("UNCONFIGURED",i.state);assertEquals(0,b.posts);assertEquals(0L,i.syncedRevision)
        b.capable=true;assertFalse(drain(d,b));assertEquals("SYNCED",d.automaticSyncDao().all().single().state)
    }}
    @Test fun incompleteBackendReceiptKeepsQueueAndPhotosUnconfirmed()=runBlocking(Dispatchers.IO) {database().use {d->
        save(d,"bad-ack");photo(d,"bad-ack");val b=Backend().apply {invalidAck=true}
        assertTrue(drain(d,b));val i=d.automaticSyncDao().all().single()
        assertEquals("ERROR",i.state);assertEquals(0L,i.syncedRevision);assertNull(i.receiptJson)
        assertNull(d.inventoryDao().photoById("photo-bad-ack")!!.driveFileId)
        b.invalidAck=false;assertFalse(drain(d,b));assertEquals(1,b.mutations)
    }}
    @Test fun workManagerKeepsOfflineDrainEnqueuedWithNetworkConstraintAndSafeBackoff()=runBlocking(Dispatchers.IO) {
        org.robolectric.Shadows.shadowOf(context.getSystemService(android.net.ConnectivityManager::class.java)).setActiveNetworkInfo(null)
        try {androidx.work.WorkManager.getInstance(context)}catch(_:IllegalStateException) {
            androidx.work.WorkManager.initialize(context,androidx.work.Configuration.Builder().build())
        }
        AutomaticSync.enqueue(context)
        val manager=androidx.work.WorkManager.getInstance(context) as androidx.work.impl.WorkManagerImpl
        val work=manager.getWorkInfosForUniqueWork("automatic-drive-drain").get(20,java.util.concurrent.TimeUnit.SECONDS).last()
        val spec=manager.workDatabase.workSpecDao().getWorkSpec(work.id.toString())!!
        assertEquals(androidx.work.NetworkType.CONNECTED,spec.constraints.requiredNetworkType)
        assertEquals(androidx.work.BackoffPolicy.EXPONENTIAL,spec.backoffPolicy)
        assertEquals(30_000L,spec.backoffDelayDuration)
        assertEquals(androidx.work.WorkInfo.State.ENQUEUED,work.state)
    }
}
