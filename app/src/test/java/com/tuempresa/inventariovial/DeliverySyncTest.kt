package com.tuempresa.inventariovial

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.delivery.*
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class DeliverySyncTest {
    @get:Rule val temp = TemporaryFolder()
    private fun database() = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), InventoryDatabase::class.java).build()
    private suspend fun batch(db: InventoryDatabase): DeliveryBatchEntity {
        db.inventoryDao().insertRecord(testRecord("record", sic = "SIC-18"))
        val folder = temp.newFolder()
        val entries = JSONArray()
        repeat(2) { i ->
            val file = File(folder, "photo-$i.jpg").apply { writeText("synthetic immutable photo bytes $i") }
            db.inventoryDao().insertPhoto(PhotoEntity("photo-$i", "record", i + 1, file.absolutePath, i == 0, null, null, null, "PENDING", 1))
            entries.put(json("localFile" to file.name, "payload" to json("kind" to "photo", "photoId" to "photo-$i",
                "fileName" to "00${i + 1}_F0${i + 1}.jpg", "sha256" to sha256(file.readBytes()))))
        }
        val fingerprint=RouteDelivery.fingerprint(db.inventoryDao().activeSnapshots(),DeliveryNamingPolicy.FOLDER_COMPACT)
        val manifest = File(folder, "manifest.local.json").apply {
            writeText(json("deliveryId" to "batch", "fingerprint" to fingerprint, "files" to entries).toString())
        }
        return DeliveryBatchEntity("batch", "R", fingerprint, manifest.absolutePath, "FOLDER_COMPACT", "QUEUED", 1).also {
            db.inventoryDao().insertDelivery(it)
        }
    }
    private class FakeDrive(var supported: Boolean = true, var failSecond: Boolean = false, var badReceipt: Boolean = false) : DeliveryTransport {
        val requests = mutableListOf<String>()
        val stored = mutableMapOf<String, String>()
        override fun supportsDelivery() = supported
        override fun post(payload: JSONObject): JSONObject {
            requests += payload.toString()
            val id = payload.getString("photoId")
            val hash = payload.getString("sha256")
            assertFalse(payload.has("token"))
            stored[id]?.let { assertEquals(it, hash) }
            stored[id] = hash // Simulate a lost response after Drive saved the bytes.
            if (failSecond && id == "photo-1") { failSecond = false; throw java.io.IOException("lost response") }
            return json("ok" to true, "protocol" to 1, "deliveryId" to payload.getString("deliveryId"), "folderId" to "folder", "fileName" to payload.getString("fileName"), "fileId" to "drive-$id", "sha256" to if (badReceipt) "wrong" else hash)
        }
    }
    @Test fun lostResponseRetriesIdenticalSnapshotWithoutDuplicatingOrChangingIds() = runBlocking {
        val db = database()
        try {
            val b = batch(db); val drive = FakeDrive(failSecond = true)
            assertEquals(ListenableWorker.Result.retry(), DeliverySync.sync(db, b.id, drive))
            assertEquals("ERROR", db.inventoryDao().delivery(b.id)!!.status)
            withContext(Dispatchers.IO) {
                assertEquals("SYNCED", db.inventoryDao().photoById("photo-0")!!.syncStatus)
                assertEquals("ERROR", db.inventoryDao().photoById("photo-1")!!.syncStatus)
            }
            assertEquals(ListenableWorker.Result.success(), DeliverySync.sync(db, b.id, drive))
            assertEquals("SYNCED", db.inventoryDao().delivery(b.id)!!.status)
            assertEquals(2, drive.stored.size)
            assertEquals(drive.requests[0], drive.requests[2]); assertEquals(drive.requests[1], drive.requests[3])
            withContext(Dispatchers.IO) {
                assertEquals("drive-photo-1", db.inventoryDao().photoById("photo-1")!!.driveFileId)
                assertEquals(2, db.inventoryDao().photoById("photo-1")!!.photoIndex)
            }
        } finally { db.close() }
    }
    @Test fun unsupportedServerAndTamperedFileAreBlockedBeforePosting() = runBlocking {
        val db = database()
        try {
            val b = batch(db); val drive = FakeDrive(supported = false)
            DeliverySync.sync(db, b.id, drive)
            assertEquals("BLOCKED", db.inventoryDao().delivery(b.id)!!.status); assertTrue(drive.requests.isEmpty())
            drive.supported = true
            db.inventoryDao().deliveryStatus(b.id,"QUEUED",null)
            File(File(b.manifestPath).parentFile, "photo-0.jpg").appendText("tampered")
            DeliverySync.sync(db, b.id, drive)
            assertEquals("BLOCKED", db.inventoryDao().delivery(b.id)!!.status); assertTrue(drive.requests.isEmpty())
        } finally { db.close() }
    }
    @Test fun annulledRecordIsBlockedBeforeAnyUpload() = runBlocking {
        val db=database()
        try {
            val b=batch(db);val drive=FakeDrive()
            db.inventoryDao().setRecordStatus("record","ANNULLED",2)
            DeliverySync.sync(db,b.id,drive)
            assertTrue(drive.requests.isEmpty())
            assertEquals("BLOCKED",db.inventoryDao().delivery(b.id)!!.status)
        } finally { db.close() }
    }
    @Test fun wrongReceiptNeverMarksPhotoSynced() = runBlocking {
        val db = database()
        try {
            val b = batch(db); val drive = FakeDrive(badReceipt = true)
            DeliverySync.sync(db, b.id, drive)
            assertEquals("BLOCKED", db.inventoryDao().delivery(b.id)!!.status)
            withContext(Dispatchers.IO) {
                assertEquals("ERROR", db.inventoryDao().photoById("photo-0")!!.syncStatus)
                assertNull(db.inventoryDao().photoById("photo-0")!!.driveFileId)
                assertEquals("PENDING", db.inventoryDao().photoById("photo-1")!!.syncStatus)
            }
        } finally { db.close() }
    }
}
