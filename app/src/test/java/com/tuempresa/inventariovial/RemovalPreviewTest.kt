package com.tuempresa.inventariovial

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.localfiles.*
import com.tuempresa.inventariovial.data.entity.SicExtensionEntity
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*

@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],application=Application::class)
class RemovalPreviewTest {
    private fun item(id:String,revision:Long=1)=LocalInventoryItem("SIC",id,"SIC-19","Ficha ficticia","FICTITIOUS_ROAD","0010",null,"CD","INCREASING","DRAFT",revision,"2026-11-02")
    private fun db()=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Application>(),InventoryDatabase::class.java).build()
    @Test fun exactTextAndSecondConfirmationNeverImplyServerAuthorization() {
        for(text in listOf("","eliminar"," ELIMINAR","ELIMINAR "))assertFalse(RemovalPreview.confirmationMatches(text,1,true))
        assertFalse(RemovalPreview.confirmationMatches("ELIMINAR",2,false))
        assertTrue(RemovalPreview.confirmationMatches("ELIMINAR",2,true))
        assertFalse(RemovalPreview.confirmationMatches("ELIMINAR",0,true))
    }
    @Test fun previewPreservesRecordsOutboxAndAuditAndFlagsCriticalLinks()=runBlocking {db().use {d->
        val first=testRecord("FICTITIOUS_SOURCE",status="DRAFT");d.inventoryDao().insertRecord(first)
        d.inventoryDao().insertRecord(testRecord("FICTITIOUS_CHILD",status="DRAFT"))
        d.inventoryDao().saveSicExtension(SicExtensionEntity("FICTITIOUS_CHILD","SIC-18A","{}",first.id,"FICTITIOUS_OWNER","DRAFT"))
        val result=RemovalPreview.inspect(d,listOf(item(first.id))).single()
        assertTrue(result.sharedOrLinked);assertNull(result.remoteIds);assertFalse(result.validCurrentAck)
        assertEquals(first,d.inventoryDao().recordById(first.id));assertTrue(d.automaticSyncDao().all().isEmpty());assertTrue(d.auditDao().recent().isEmpty())
    }}
    @Test fun remoteIdsRequireStrictCurrentAckAndChangedRevisionIsVisible()=runBlocking {db().use {d->
        val intent=deliveryFixture();d.inventoryDao().insertRecord(testRecord(intent.recordId).copy(updatedAt=intent.revision))
        d.automaticSyncDao().put(intent)
        val confirmed=RemovalPreview.inspect(d,listOf(item(intent.recordId,intent.revision))).single()
        assertTrue(confirmed.validCurrentAck);assertTrue(confirmed.remoteIds!!>0)
        val stale=RemovalPreview.inspect(d,listOf(item(intent.recordId,intent.revision-1))).single()
        assertTrue(stale.revisionChanged);assertFalse(stale.validCurrentAck);assertNull(stale.remoteIds)
        d.automaticSyncDao().put(intent.copy(receiptJson="{\"ok\":true}"))
        assertNull(RemovalPreview.inspect(d,listOf(item(intent.recordId,intent.revision))).single().remoteIds)
    }}
    @Test fun duplicateOrEmptySelectionCannotBePreviewed()=runBlocking {db().use{d->
        assertTrue(runCatching{RemovalPreview.inspect(d,emptyList())}.isFailure)
        assertTrue(runCatching{RemovalPreview.inspect(d,listOf(item("x"),item("x")))}.isFailure)
    }}
}
