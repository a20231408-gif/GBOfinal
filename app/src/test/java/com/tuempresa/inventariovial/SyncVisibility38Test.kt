package com.tuempresa.inventariovial

import android.app.Application
import android.net.NetworkCapabilities
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkInfo
import androidx.work.workDataOf
import com.tuempresa.inventariovial.core.*
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Fictitious receipts only. No HTTP, credentials or field data. */
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],application=Application::class)
class SyncVisibility38Test {
    private val identity=DriveDeliveryIdentity("Puente ficticio","Ruta ficticia",2,true)
    private val pending get()=deliveryFixture().copy(state="QUEUED",syncedRevision=0,receiptJson=null)
    private fun view(i:DriveSyncIntent=pending,connected:Boolean=true,running:Boolean=false,p:SyncProgress?=null)=
        DriveDeliveryPresentation.view(i,identity,running,connected,true,progress=p)

    @Test fun zeroNewRequestsDoesNotMeanExistingQueueIsEmpty()=runBlocking {
        val context=ApplicationProvider.getApplicationContext<Application>()
        Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build().use{db->
            val record=testRecord("FICTITIOUS_QUEUED_38")
            db.inventoryDao().insertRecord(record)
            val queued=DriveSyncIntent("SIC/${record.id}","SIC",record.id,record.updatedAt)
            db.automaticSyncDao().put(queued)
            var callbacks=0
            assertEquals(0,UnifiedSyncAdapter.enqueue(db,false){_,_,_->callbacks++})
            assertEquals(0,callbacks);assertEquals(listOf(queued),db.automaticSyncDao().all())
        }
    }

    @Test fun networkWithInternetCapabilityButNoValidationIsNotPresentedAsOnline() {
        val caps=NetworkCapabilities()
        shadowOf(caps).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        assertFalse(SyncProgressPresentation.validatedInternet(null))
        assertFalse(SyncProgressPresentation.validatedInternet(caps))
        shadowOf(caps).addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        assertTrue(SyncProgressPresentation.validatedInternet(caps))
        shadowOf(caps).removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        assertFalse(SyncProgressPresentation.validatedInternet(caps))
    }
    @Test fun queuedWorkIsNotFileProgressAndOnlyMatchingRunningWorkShowsActivity() {
        fun work(state:WorkInfo.State,key:String=pending.key,revision:Long=2)=WorkInfo(java.util.UUID.randomUUID(),state,emptySet(),
            progress=workDataOf("activeKey" to key,"activeRevision" to revision))
        val queued=listOf(work(WorkInfo.State.ENQUEUED))
        assertFalse(SyncProgressPresentation.running(pending,queued))
        assertTrue(SyncProgressPresentation.workStatus(queued,true).contains("Trabajo en cola"))
        assertTrue(SyncProgressPresentation.workStatus(queued,false).contains("Esperando conexión"))
        assertFalse(SyncProgressPresentation.workStatus(queued,true).contains("%"))
        assertFalse(SyncProgressPresentation.running(pending,listOf(work(WorkInfo.State.RUNNING,key="SCAP/OTHER"))))
        assertFalse(SyncProgressPresentation.running(pending,listOf(work(WorkInfo.State.RUNNING,revision=1))))
        assertTrue(SyncProgressPresentation.running(pending,listOf(work(WorkInfo.State.RUNNING))))
        assertTrue(SyncProgressPresentation.workStatus(listOf(work(WorkInfo.State.RUNNING)),true).contains("procesando la cola"))
    }

    @Test fun offlineQueueAndInterruptedUploadExplicitlyWaitForConnection() {
        for(state in listOf("QUEUED","UPDATE_PENDING","UPLOADING")) {
            val v=view(pending.copy(state=state),connected=false)
            assertEquals(state,"Esperando conexión",v.status.label)
            assertFalse(v.confirmed);assertNull(v.progressText)
        }
        assertEquals("Pendiente de envío",view().status.label)
        assertEquals("Enviando",view(running=true).status.label)
    }
    @Test fun permanentFailureAndIdentityConflictStayActionableOffline() {
        for(code in listOf("HTTP_403","BRIDGE_IDENTITY_CONFLICT")) {
            val v=view(pending.copy(state="BLOCKED",error="Rechazado ($code)"),connected=false)
            assertEquals(DriveDeliveryStatus.ATTENTION,v.status)
            assertFalse(v.confirmed)
            if(code=="BRIDGE_IDENTITY_CONFLICT")assertFalse(v.retryAllowed)
        }
    }
    @Test fun incompleteAckNeverBecomesDeliveredAndCorrectAckStaysDeliveredOffline() {
        val full=deliveryFixture()
        val incomplete=org.json.JSONObject(full.receiptJson!!).apply{getJSONArray("files").getJSONObject(0).remove("fileId")}
        assertFalse(view(full.copy(receiptJson=incomplete.toString())).confirmed)
        val done=view(full,connected=false)
        assertTrue(done.confirmed);assertEquals("Entregado con ACK",done.status.label)
        assertNull(done.progressText)
        assertNull(view(full,running=true,p=SyncProgress(2,"PHOTOS",1,1,1)).progressText)
    }
    @Test fun progressCountsOnlyMeasuredFilesAndHidesInvalidOrOldSamples() {
        val i=pending.copy(state="UPLOADING")
        val measured=view(i,running=true,p=SyncProgress(2,"PHOTOS",2,3,1))
        assertTrue(measured.progressText!!.contains("2 / 3"));assertFalse(measured.confirmed)
        for(p in listOf(SyncProgress(2,"PHOTOS",4,3,1),SyncProgress(2,"PHOTOS",-1,3,1),SyncProgress(1,"PHOTOS",2,3,1)))
            assertNull(view(i,running=true,p=p).progressText)
        assertFalse(SyncProgressPresentation.files(SyncProgress(2,"PHOTOS",4,3,1)).contains("%"))
        assertNull(view(i,p=SyncProgress(2,"PHOTOS",2,3,1)).progressText)
        val confirming=view(i,running=true,p=SyncProgress(2,"CONFIRMING",3,3,1))
        assertEquals("Confirmando archivos",confirming.status.label);assertFalse(confirming.confirmed)
    }
    @Test fun twoConfirmedOnePendingUseSameReadOnlyRowsInQueueAndHistory() {
        val rows=listOf(listEntry("ACK_A",state="SYNCED"),listEntry("ACK_B",state="SYNCED"),listEntry("WAIT",state="QUEUED"))
        val before=rows.toList()
        assertEquals(2,rows.count{it.view.confirmed})
        assertEquals(listOf("FICTICIO_WAIT"),DriveDeliveryListPresentation.select(rows,DriveDeliverySection.QUEUE).map{it.intent.recordId})
        assertEquals(3,DriveDeliveryListPresentation.select(rows,DriveDeliverySection.HISTORY).size)
        assertEquals(2,DriveDeliveryListPresentation.select(rows,DriveDeliverySection.HISTORY,filter=DriveDeliveryFilter.COMPLETE).size)
        assertEquals(before,rows)
    }
}
