package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.core.*
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.pavement.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=Application::class)
class LocalDraftDiscard10CTest {
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    @org.junit.Before fun tabletIdentity() {android.provider.Settings.Secure.putString(context.contentResolver,android.provider.Settings.Secure.ANDROID_ID,"synthetic-tablet-10c")}
    private fun db()=Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build()
    private fun record(id:String="draft",status:String="DRAFT")=InventoryRecordEntity(id,"SIC-18","ALCANTARILLA","R10C","UC","0000",10.0,null,null,null,null,null,null,null,"2026-10-12",null,status,"PENDING","PENDING",1,1)
    @Test fun localDiscardAndRestorePreserveUuidPhotosAndNeverEnterLegacyOrDriveQueues()=runBlocking {db().use {d->
        val file=File(context.cacheDir,"draft-original10c.jpg").apply {writeText("synthetic preserved bytes")}
        try {
            d.inventoryDao().insertRecord(record());d.inventoryDao().insertPhoto(PhotoEntity("photo","draft",1,file.path,true,null,null,null,"PENDING",1))
            val operation=LocalDraftDiscard(d,context);operation.change("SIC","draft",userId="operator")
            assertEquals("DISCARDED_LOCAL",d.inventoryDao().recordById("draft")!!.status)
            assertTrue(d.inventoryDao().pendingServerIds().isEmpty());assertTrue(d.inventoryDao().recordsForAxis().isEmpty());assertTrue(d.automaticSyncDao().all().isEmpty())
            assertEquals("photo",d.inventoryDao().snapshot("draft")!!.photos.single().id);assertTrue(file.exists())
            operation.change("SIC","draft",restore=true,userId="operator")
            assertEquals("DRAFT",d.inventoryDao().recordById("draft")!!.status);assertEquals(2,d.auditDao().recent().size)
            assertTrue(d.automaticSyncDao().all().isEmpty())
        }finally {file.delete()}
    }}
    @Test fun anyOutboxIncludingReopenedSyncedOrLostAckBlocksDiscard()=runBlocking {
        for(state in listOf("QUEUED","UPLOADING","ERROR","BLOCKED","CONFLICT","SYNCED","DRAFT")) db().use {d->
            d.inventoryDao().insertRecord(record());d.automaticSyncDao().put(DriveSyncIntent("SIC/draft","SIC","draft",3,syncedRevision=if(state=="SYNCED" || state=="DRAFT")2 else 0,state=state))
            assertTrue(state,runCatching {LocalDraftDiscard(d,context).change("SIC","draft")}.isFailure)
            assertEquals("DRAFT",d.inventoryDao().recordById("draft")!!.status);assertEquals(state,d.automaticSyncDao().get("SIC/draft")!!.state)
        }
    }
    @Test fun activeRecordReservedIdentityAndSyncedPhotoCannotBeDiscardedLocally()=runBlocking {
        db().use {d->d.inventoryDao().insertRecord(record(status="ACTIVE"));assertTrue(runCatching {LocalDraftDiscard(d,context).change("SIC","draft")}.isFailure)}
        db().use {d->d.inventoryDao().insertRecord(record("bound"));context.getSharedPreferences("drive_bindings_v4",0).edit().putString("bound","{}").commit()
            try {assertTrue(runCatching {LocalDraftDiscard(d,context).change("SIC","bound")}.isFailure)} finally {context.getSharedPreferences("drive_bindings_v4",0).edit().remove("bound").commit()}}
        db().use {d->d.inventoryDao().insertRecord(record());d.inventoryDao().insertPhoto(PhotoEntity("synced","draft",1,"/old.jpg",true,null,"folder","file","SYNCED",1))
            assertTrue(runCatching {LocalDraftDiscard(d,context).change("SIC","draft")}.isFailure)}
    }
    @Test fun legacyRouteManifestAndUnreadableDeliveryBlockDiscard()=runBlocking {db().use {d->
        d.inventoryDao().insertRecord(record());val file=File(context.cacheDir,"manifest10c.json").apply {writeText("{\"files\":[{\"recordId\":\"draft\"}]}")}
        try {d.inventoryDao().insertDelivery(DeliveryBatchEntity("batch","R10C","hash",file.path,"V4","PREPARED",1))
            assertTrue(runCatching {LocalDraftDiscard(d,context).change("SIC","draft")}.isFailure)
            file.delete();assertTrue(runCatching {LocalDraftDiscard(d,context).change("SIC","draft")}.isFailure)
            assertEquals("DRAFT",d.inventoryDao().recordById("draft")!!.status)
        }finally {file.delete()}
    }}
    @Test fun pavementOwnerMayDiscardUntouchedLocalSampleButNotAnotherOperators()=runBlocking {db().use {d->
        val sample=PavementSampleEntity("pavdraft","T","R10C",1,"Derecho","UC","00+000","00+040",5.0,40.0,"ASPHALT","2026-10-12","owner","Inspector")
        d.pavementDao().saveSample(sample);val operation=LocalDraftDiscard(d,context)
        assertTrue(runCatching {operation.change("PAVEMENT",sample.id,userId="other")}.isFailure)
        operation.change("PAVEMENT",sample.id,userId="owner");assertEquals("DISCARDED_LOCAL",d.pavementDao().sample(sample.id)!!.status)
        operation.change("PAVEMENT",sample.id,restore=true,userId="owner");assertEquals("DRAFT",d.pavementDao().sample(sample.id)!!.status)
    }}
}
