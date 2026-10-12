package com.tuempresa.inventariovial

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import com.tuempresa.inventariovial.core.*
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.sic.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

/** Two separately invoked phases, with an adb force-stop between them. Only a disposable
 * emulator may change its connectivity; no field database or physical tablet is touched.
 */
class DriveV4Offline8BTest {
    private val instrumentation get()=InstrumentationRegistry.getInstrumentation()
    private val context get()=instrumentation.targetContext
    private val file get()=File(context.getExternalFilesDir(null),"8b-offline-worker.json")
    private fun gate(phase:String) {
        assumeTrue(InstrumentationRegistry.getArguments().getString("gboDrive8B")=="true" &&
            InstrumentationRegistry.getArguments().getString("gboOfflinePhase")==phase)
        require(Build.HARDWARE in setOf("ranchu","goldfish")) {"Disposable emulator required"}
        require(DriveUploadPolicy.configurationError()==null) {"Private configuration required"}
    }
    private fun connected():Boolean {
        val manager=context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return manager.getNetworkCapabilities(manager.activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)==true
    }
    private fun shell(command:String) {
        instrumentation.uiAutomation.executeShellCommand(command).use {descriptor->
            android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).use {it.readBytes()}
        }
    }
    @Test fun createValidatedRecordAndPhotoWithoutNetwork()=runBlocking {
        gate("create")
        // The connected application's actual transport must recognize V4 before disabling Wi-Fi.
        val transport=AutomaticDriveHttp();assertTrue(transport.supportsV3());assertTrue(transport.supportsFamilyPaths())
        shell("svc wifi disable");shell("svc data disable")
        repeat(10) {if(connected())delay(1000)}
        assertFalse("Emulator still has a network",connected())
        val id=UUID.randomUUID().toString()
        val image=File(context.filesDir,"8b-offline-$id.jpg")
        val bitmap=Bitmap.createBitmap(320,240,Bitmap.Config.ARGB_8888)
        Canvas(bitmap).apply {drawColor(Color.GREEN);drawText("PRUEBA 8B OFFLINE",10f,100f,Paint().apply {color=Color.BLACK;textSize=22f})}
        image.outputStream().use {check(bitmap.compress(Bitmap.CompressFormat.JPEG,95,it))};bitmap.recycle()
        val state=SicFieldState(SicFieldFormat.SIC08,mapOf("routeCode" to "PE-22A","roadbedCode" to "CD","faja" to "2",
            "startPrCode" to "0010","startDistanceM" to "45","endPrCode" to "0011","endDistanceM" to "20",
            "surveyDate" to LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),"typeCode" to "1","directionCode" to "C",
            "observations" to "PRUEBA 8B OFFLINE $id"))
        val db=InventoryDatabase.getInstance(context)
        SicFieldRepository(db).save(id,state,"PRUEBA OFFLINE 8B",true,listOf(image.path),mapOf(image.path to "GENERAL"))
        val intent=db.automaticSyncDao().get("SIC/$id")!!
        assertEquals("QUEUED",intent.state);assertFalse(SyncIntentPresentation.confirmed(intent))
        val photo=db.inventoryDao().snapshot(id)!!.photos.single()
        file.writeText(JSONObject().put("recordId",id).put("revision",intent.revision).put("photoId",photo.id)
            .put("originalSha256",hash(image.readBytes())).put("createdWithNetwork",connected())
            .put("initialState",intent.state).put("completed",false).toString(2))
    }
    @Test fun reopenWithoutNetworkThenRecoverThroughExistingWorkManager()=runBlocking {
        gate("recover")
        val evidence=JSONObject(file.readText());val id=evidence.getString("recordId")
        val db=InventoryDatabase.getInstance(context)
        assertFalse("Recovery must start offline after force-stop",connected())
        val restored=db.inventoryDao().snapshot(id)!!
        assertEquals("PRUEBA 8B OFFLINE $id",restored.record.observations)
        assertEquals(evidence.getString("photoId"),restored.photos.single().id)
        assertEquals(evidence.getString("originalSha256"),hash(File(restored.photos.single().originalPath).readBytes()))
        assertEquals(evidence.getLong("revision"),db.automaticSyncDao().get("SIC/$id")!!.revision)
        evidence.put("reopenedWithoutNetwork",true);file.writeText(evidence.toString(2))
        shell("svc wifi enable");shell("svc data enable")
        repeat(20) {if(!connected())delay(1000)}
        assertTrue("Network did not recover",connected())
        // GboApplication already invokes the existing recovery hook on process startup.
        // Restoring the network releases persisted WorkManager constraints without a manual drain.
        val deadline=System.currentTimeMillis()+180_000
        while(System.currentTimeMillis()<deadline && !SyncIntentPresentation.confirmed(db.automaticSyncDao().get("SIC/$id")))delay(2000)
        val result=db.automaticSyncDao().get("SIC/$id")!!
        evidence.put("state",result.state).put("syncedRevision",result.syncedRevision)
            .put("error",result.error ?: JSONObject.NULL).put("worker","AutomaticSyncWorker")
            .put("completed",SyncIntentPresentation.confirmed(result))
        result.receiptJson?.let {evidence.put("ack",JSONObject(it))}
        file.writeText(evidence.toString(2))
        assertTrue("Worker did not confirm delivery within bounded wait; inspect sanitized evidence",SyncIntentPresentation.confirmed(result))
        assertEquals(evidence.getLong("revision"),result.syncedRevision)
    }
    private fun hash(bytes:ByteArray)=java.security.MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it)}
}
