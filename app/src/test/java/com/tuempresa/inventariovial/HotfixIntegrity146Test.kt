package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.repository.InventoryRepository
import com.tuempresa.inventariovial.repository.EngineeringEdits
import com.tuempresa.inventariovial.core.*
import com.tuempresa.inventariovial.field.draftDestination
import com.tuempresa.inventariovial.model.*
import com.tuempresa.inventariovial.model.form.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import android.graphics.Bitmap

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],application=Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HotfixIntegrity146Test {
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    private fun db()=Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build()
    private fun record()=InventoryRecordEntity("TEST_AUTO_146","SIC-22","VERTICAL","PE-22A","CD","0001",20.0,
        null,null,"D",null,null,null,null,"15/10/2026","FICTICIO","DRAFT","PENDING","PENDING",1,1)
    private fun detail()=Sic22Entity("TEST_AUTO_146","20","1","2","R-1",null,"1",null,null,null)
    @Test fun existingSic22DraftFinalizesWithoutPrimaryKeyCollision()=runBlocking {db().use { d->
        val repo=InventoryRepository(d);repo.saveSic22(record(),detail(),emptyList())
        repo.saveSic22(record().copy(status="ACTIVE",updatedAt=2),detail().copy(signalObservation="EDITADA"),emptyList())
        assertEquals("ACTIVE",d.inventoryDao().snapshot(record().id)!!.record.status)
        assertEquals("EDITADA",d.inventoryDao().snapshot(record().id)!!.sic22!!.signalObservation)
    }}
    @Test fun recoveredUuidCannotBecomeAnotherSic()=runBlocking {db().use {d->
        d.inventoryDao().insertRecord(record())
        try {d.inventoryDao().insertOrCompleteDraft(record().copy(sicCode="SIC-20",assetType="FORD"));fail("Debe conservar SIC-22")}
        catch(_:IllegalArgumentException) { }
        assertEquals("SIC-22",d.inventoryDao().recordById(record().id)!!.sicCode)
    }}
    @Test fun originalAbortInsertionReproducesReportedUniqueConstraint()=runBlocking {db().use {d->
        d.inventoryDao().insertRecord(record());d.inventoryDao().insertSic22(detail())
        val failure=runCatching {d.inventoryDao().insertSic22(detail())}.exceptionOrNull()
        assertNotNull(failure);assertTrue(failure!!.message.orEmpty().contains("sic22_details.recordId"))
        assertNotNull(d.inventoryDao().snapshot(record().id)!!.sic22)
    }}
    @Test fun verticalDraftWithPhotoRoutesToVerticalAndNeverFord()=runBlocking {db().use {d->
        val p=PhotoEntity("photo-test",record().id,1,"fictitious.jpg",true,null,null,null,"PENDING",1)
        InventoryRepository(d).saveSic22(record(),detail(),listOf(p))
        val snapshot=d.inventoryDao().snapshot(record().id)!!
        val destination=draftDestination(snapshot.record)
        assertEquals(RoadAssetType.SIGNALIZATION,destination.asset);assertEquals(SignalizationType.VERTICAL,destination.signal)
        assertEquals(p,snapshot.photos.single());assertNull(snapshot.sic20)
        assertTrue(runCatching {requireSicDetail("SIC-22",SicFormDetail.Sic20(Sic20FormState()))}.isFailure)
    }}
    @Test fun allKnownSubtypesResumeAndUnknownTypesFailClosed() {
        for(s in SignalizationType.entries)assertEquals(s,draftDestination(record().copy(sicCode=s.sicCode,assetType=s.name)).signal)
        for((name,code,asset) in listOf(Triple("FORD","12",RoadAssetType.FORD),Triple("WALL","14",RoadAssetType.WALL),Triple("TUNNEL","13",RoadAssetType.TUNNEL))) {
            assertEquals(asset,draftDestination(record().copy(sicCode="SIC-20",assetType=name),code).asset)
            assertEquals(asset,draftDestination(record().copy(sicCode="SIC-20",assetType="LEGACY"),code).asset)
        }
        assertEquals(SignalizationType.HORIZONTAL_STUDS,draftDestination(record().copy(sicCode="SIC-21",assetType="LEGACY"),"20").signal)
        assertTrue(runCatching{draftDestination(record().copy(sicCode="SIC-99"))}.isFailure)
        assertTrue(runCatching{draftDestination(record().copy(sicCode="SIC-20",assetType="UNKNOWN"))}.isFailure)
    }
    @Test fun existingPhotosKeepIdentityIndexAndAckWhileNewPhotosAreAdded()=runBlocking {db().use {d->
        val repo=InventoryRepository(d)
        val p=PhotoEntity("photo-old",record().id,7,"old.jpg",true,"old-name.jpg","folder","file","SYNCED",1)
        d.inventoryDao().insertRecord(record());d.inventoryDao().insertSic22(detail());d.inventoryDao().insertPhoto(p)
        val added=p.copy(id="photo-new",localPath="new.jpg",originalPath="new.jpg",photoIndex=1,syncStatus="PENDING",driveFileId=null)
        repo.saveSic22(record().copy(updatedAt=2),detail(),listOf(p.copy(photoIndex=1,syncStatus="PENDING",driveFileId=null),added))
        repo.saveSic22(record().copy(status="ACTIVE",updatedAt=3),detail(),listOf(p,added))
        val photos=d.inventoryDao().snapshot(record().id)!!.photos
        assertEquals(2,photos.size);assertEquals(p,photos.single{it.id==p.id});assertEquals(8,photos.single{it.id==added.id}.photoIndex)
        assertEquals(1,d.automaticSyncDao().all().size)
    }}
    @Test fun rawDraftWrongFormCannotOverwriteTextOrPhotos()=runBlocking {db().use {d->
        initializeIdleUiWorkers()
        val auth=com.tuempresa.inventariovial.auth.AuthGraph.get(context).sessions
        auth.logout();auth.login(com.tuempresa.inventariovial.auth.UserCacheEntity("fictitious","fictitious","Ficticio","ENGINEER",true,"",1,1,1))
        try {
            val request=InventorySaveRequest("SIC-22","VERTICAL","PE-22A","CD","1","20",null,null,"D",Double.NaN,Double.NaN,null,
                "15/10/2026","FICTICIO","",SicFormDetail.Sic22(Sic22FormState(signalCode="R-1")),recordId=record().id)
            InventoryDraftStore.save(context,d,request)
            val before=InventoryDraftStore.read(context,record().id)!!.toString()
            assertTrue(runCatching {InventoryDraftStore.save(context,d,request.copy(sicCode="SIC-20",assetType="FORD",detail=SicFormDetail.Sic20(Sic20FormState())))}.isFailure)
            assertEquals(before,InventoryDraftStore.read(context,record().id)!!.toString())
            assertEquals("SIC-22",d.inventoryDao().recordById(record().id)!!.sicCode)
        }finally {auth.logout()}
    }}
    @Test fun draftReopenAddPhotoFinalizeAndAuthorizedEditKeepOneIdentity()=runBlocking {db().use {d->
        val repo=InventoryRepository(d);repo.saveSic22(record(),detail(),emptyList())
        val f=File(context.cacheDir,"TEST_AUTO_146.jpg")
        val bitmap=Bitmap.createBitmap(24,24,Bitmap.Config.ARGB_8888)
        f.outputStream().use{bitmap.compress(Bitmap.CompressFormat.JPEG,90,it)};bitmap.recycle()
        val bytes=f.readBytes()
        val p=PhotoEntity("photo-146",record().id,1,f.path,true,null,null,null,"PENDING",1,photoCategory="GENERAL")
        repo.saveSic22(record().copy(updatedAt=2),detail().copy(signalObservation="BORRADOR EDITADO"),listOf(p))
        assertEquals(SignalizationType.VERTICAL,draftDestination(d.inventoryDao().recordById(record().id)!!).signal)
        repo.saveSic22(record().copy(status="ACTIVE",updatedAt=3),detail(),listOf(p))
        val s=d.inventoryDao().snapshot(record().id)!!
        val form=EngineeringEdits.form(s) as SicFormDetail.Sic22
        EngineeringEdits.save(d,record().id,form.copy(state=form.state.copy(conditionCode="2")),listOf(f.path),mapOf(f.path to "GENERAL"),expectedUpdatedAt=s.record.updatedAt)
        val final=d.inventoryDao().snapshot(record().id)!!
        assertEquals("SIC-22",final.record.sicCode);assertNull(final.sic20);assertEquals("2",final.sic22!!.conditionCode)
        assertEquals("photo-146",final.photos.single().id);assertArrayEquals(bytes,f.readBytes())
        assertTrue(final.record.updatedAt>s.record.updatedAt);assertEquals(1,d.automaticSyncDao().all().size)
        assertEquals(final.record.updatedAt,d.automaticSyncDao().all().single().revision)
    }}
    @Test fun repeatedRequestForBusyRevisionPreservesPreparedSnapshotAndReceipt()=runBlocking {db().use {d->
        InventoryRepository(d).saveSic22(record().copy(status="ACTIVE"),detail(),emptyList())
        val old=d.automaticSyncDao().all().single().copy(state="ERROR",error="Servidor ocupado (BUSY)",receiptJson="previous ACK",syncedRevision=0,preparedJson="{\"revision\":1}")
        d.automaticSyncDao().put(old);AutomaticSync.request(d,"SIC",record().id,1)
        assertEquals(old,d.automaticSyncDao().all().single())
    }}
    @Test fun legacyBusyBlockRecoversWithoutTouchingPreviousAckOrPreparedRevision()=runBlocking {db().use {d->
        InventoryRepository(d).saveSic22(record().copy(status="ACTIVE",updatedAt=2),detail(),emptyList())
        val old=d.automaticSyncDao().all().single().copy(state="BLOCKED",error="El servidor rechazó la revisión (BUSY)",receiptJson="previous ACK",syncedRevision=1,preparedJson="{\"revision\":2}")
        d.automaticSyncDao().put(old);assertEquals(1,AutomaticSync.recoverBusyBlocked(d))
        val recovered=d.automaticSyncDao().all().single()
        assertEquals("ERROR",recovered.state);assertEquals(old.preparedJson,recovered.preparedJson);assertEquals(old.receiptJson,recovered.receiptJson)
        assertEquals(old.revision,recovered.revision);assertEquals(1L,recovered.syncedRevision)
        assertEquals("Esperando para reintentar",SyncIntentPresentation.label(recovered,true,true,true))
    }}
    @Test fun otherEditableSicDetailsUpdateInPlaceWithoutAbortOrPhotoCascade()=runBlocking {db().use {d->
        val repo=InventoryRepository(d)
        for(code in listOf("SIC-17","SIC-18","SIC-19","SIC-20","SIC-21","SIC-23")) {
            val id="TEST_AUTO_146_"+code;val first=record().copy(id=id,sicCode=code,assetType=if(code=="SIC-20")"BADEN"else code)
            val photo=PhotoEntity("photo-$id",id,1,"$id.jpg",true,null,null,null,"PENDING",1)
            suspend fun save(r:InventoryRecordEntity)=when(code) {
                "SIC-17"->repo.saveSic17(r,Sic17Entity(id,"01","2","TEST","S",1,10.0,2.0,"1","1","1","1","FICTICIO",0.0),listOf(photo))
                "SIC-18"->repo.saveSic18(r,Sic18Entity(id,"06","1",1,"1",1.0,1.0,"1","1"),listOf(photo))
                "SIC-19"->repo.saveSic19(r,Sic19Entity(id,"09","1","1","1","1"),listOf(photo))
                "SIC-20"->repo.saveSic20(r,Sic20Entity(id,"12","2",1.0,2.0,"1","1"),listOf(photo))
                "SIC-21"->repo.saveSic21(r,Sic21Entity(id,"18","1","5","1"),listOf(photo))
                else->repo.saveSic23(r,Sic23Entity(id,"23","1",1.0,"FICTICIO"),listOf(photo))
            }
            save(first);save(first.copy(updatedAt=2));save(first.copy(status="ACTIVE",updatedAt=3))
            val s=d.inventoryDao().snapshot(id)!!;assertEquals("ACTIVE",s.record.status);assertEquals(photo,s.photos.single())
        }
        assertEquals(6,d.automaticSyncDao().all().size)
    }}
    @Test fun previouslyMisclassifiedRowIsBlockedAndPreservedForAssistedRecovery()=runBlocking {db().use {d->
        val r=record().copy(sicCode="SIC-20",assetType="FORD")
        d.inventoryDao().insertRecord(r);d.inventoryDao().insertSic22(detail())
        assertTrue(runCatching{requireSicSnapshot(d.inventoryDao().snapshot(r.id)!!)}.isFailure)
        assertTrue(runCatching{d.inventoryDao().insertOrCompleteDraft(r.copy(updatedAt=2))}.isFailure)
        assertEquals(r,d.inventoryDao().recordById(r.id));assertEquals(detail(),d.inventoryDao().snapshot(r.id)!!.sic22)
        assertTrue(runCatching{requireSicRawDetail("SIC-20","Sic22FormState")}.isFailure)
        requireSicRawDetail("SIC-22","Sic22FormState")
    }}
    @Test fun blockedAndConflictRecordsCannotAbortBulkSchedulingOfIndependentRecords()=runBlocking {db().use {d->
        for((n,state) in listOf("CONFLICT","BLOCKED","PENDING_VALIDATION","ERROR","QUEUED").withIndex()) {
            val r=record().copy(id="TEST_AUTO_146_BULK_$n",status="ACTIVE")
            d.inventoryDao().insertRecord(r)
            d.automaticSyncDao().put(DriveSyncIntent("SIC/${r.id}","SIC",r.id,1,state=state))
        }
        val requested=mutableListOf<String>()
        val count=UnifiedSyncAdapter.enqueue(d,false){_,id,_->
            require(d.automaticSyncDao().get("SIC/$id")!!.state!="CONFLICT")
            requested.add(id)
        }
        assertEquals(2,count);assertEquals(setOf("TEST_AUTO_146_BULK_3","TEST_AUTO_146_BULK_4"),requested.toSet())
        assertEquals("CONFLICT",d.automaticSyncDao().get("SIC/TEST_AUTO_146_BULK_0")!!.state)
        assertEquals(1,UnifiedSyncAdapter.enqueue(d,true){_,_,_->})
    }}
}
