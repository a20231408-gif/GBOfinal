package com.tuempresa.inventariovial

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.core.*
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.FieldSession
import com.tuempresa.inventariovial.scap.catalog.ScapCatalog
import com.tuempresa.inventariovial.scap.data.ScapRepository
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],application=Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BridgeIdentityConflictTest {
    private val c get()=ApplicationProvider.getApplicationContext<Application>()
    @Test fun permanentServerRejectionNeverRetriesEvenWithAnIncorrectRetryableFlag()=runBlocking{
        initializeIdleUiWorkers();Room.inMemoryDatabaseBuilder(c,InventoryDatabase::class.java).build().use{db->
            val id=ScapP0Fixture.create(c,db,0);var posts=0
            val transport=object:AutomaticDriveTransport{
                override fun supportsV3()=true
                override fun supportsFamilyPaths()=true
                override fun supportsReadablePhotoNames()=true
                override fun supportsTechnicalDeliveryWithoutGps()=true
                override fun reservePhotoNames(payload:JSONObject)=ReleasePhotoV2Fixture.reservation(payload,"FICTITIOUS_BRIDGE_FOLDER")
                override fun post(payload:JSONObject):JSONObject{posts++;return JSONObject().put("ok",false).put("code","BRIDGE_IDENTITY_CONFLICT").put("retryable",true)}
            }
            assertFalse(AutomaticSyncDrain.run(c,db,{transport},null));val blocked=db.automaticSyncDao().get("SCAP/$id")!!
            assertEquals(1,posts);assertEquals("CONFLICT",blocked.state);assertNull(blocked.receiptJson);val frozen=blocked.preparedJson
            assertFalse(AutomaticSyncDrain.run(c,db,{transport},null));assertEquals(1,posts);assertEquals(frozen,db.automaticSyncDao().get(blocked.key)!!.preparedJson)
        }
    }
    @Test fun historicalBlockedOrErrorPreservesAllEvidenceAndNeverInvokesTransportOrManualRetry()=runBlocking{
        initializeIdleUiWorkers();val db=InventoryDatabase.getInstance(c)
        val id=ScapP0Fixture.create(c,db,0);val before=db.scapDao().snapshot(id)!!;val initial=db.automaticSyncDao().get("SCAP/$id")!!
        val frozen=JSONObject().put("key",initial.key).put("revision",initial.revision).put("identityId",before.inspection.roadRecordId).put("files",org.json.JSONArray((1..9).map{JSONObject().put("key","FICTITIOUS_$it")})).toString()
        for(state in listOf("ERROR","BLOCKED","QUEUED")){
            val old=initial.copy(state=state,error="Servidor: BRIDGE_IDENTITY_CONFLICT",preparedJson=frozen,receiptJson="FICTITIOUS_PREVIOUS_ACK",syncedRevision=1,lastSyncedAt=1)
            db.automaticSyncDao().put(old);assertEquals(1,AutomaticSync.recoverIdentityConflicts(db));assertEquals(old.copy(state="CONFLICT"),db.automaticSyncDao().get(old.key))
            var transports=0;assertFalse(AutomaticSyncDrain.run(c,db,{transports++;error("No transport is allowed")},null));assertEquals(0,transports)
            assertTrue(runCatching{AutomaticSync.retry(c,old.key)}.isFailure)
            assertEquals(old.copy(state="CONFLICT"),db.automaticSyncDao().get(old.key));assertEquals(before,db.scapDao().snapshot(id))
        }
        AutomaticSync.draft(db,initial.kind,id);assertEquals("CONFLICT",db.automaticSyncDao().get(initial.key)!!.state)
        AutomaticSync.request(db,"SCAP",id,initial.revision+1,origin="MANUAL_APP")
        val retained=db.automaticSyncDao().get(initial.key)!!;assertEquals("CONFLICT",retained.state);assertEquals(frozen,retained.preparedJson);assertEquals("FICTITIOUS_PREVIOUS_ACK",retained.receiptJson)
    }
    @Test fun currentFullAcknowledgementIsNeverDowngradedByALegacyError()=runBlocking{
        Room.inMemoryDatabaseBuilder(c,InventoryDatabase::class.java).build().use{db->
            val original=deliveryFixture().copy(state="SYNCED",error="BRIDGE_IDENTITY_CONFLICT");db.automaticSyncDao().put(original)
            assertEquals(0,AutomaticSync.recoverIdentityConflicts(db));assertEquals(original,db.automaticSyncDao().get(original.key))
        }
    }
    @Test fun savedParentUuidAndRouteSurviveAnotherActiveSessionAndLegacyOutbox()=runBlocking{
        initializeIdleUiWorkers();Room.inMemoryDatabaseBuilder(c,InventoryDatabase::class.java).build().use{db->
            val id=ScapP0Fixture.create(c,db,0);val old=db.scapDao().snapshot(id)!!;val i=db.automaticSyncDao().get("SCAP/$id")!!
            val p=AutomaticDelivery.namingPreview(c,db,i)
            db.inventoryDao().insertSession(FieldSession("FICTITIOUS_NEW_SESSION","TEST","TEST","TEST","PE-12A","OTHER","CD",startTime=1))
            val reopened=ScapRepository(db,ScapCatalog.load(c)).dao.snapshot(id)!!
            val q=AutomaticDelivery.namingPreview(c,db,i)
            assertEquals(old,reopened);assertEquals(old.inspection.roadRecordId,q.getString("identityId"));assertEquals(old.inspection.id,q.getString("recordId"))
            for(key in listOf("key","identityId","bridgeCode","routeCode","startPrNumber","startDistanceM","revision"))assertEquals(p.get(key),q.get(key))
            assertEquals(i,db.automaticSyncDao().get(i.key))
            assertTrue((0 until q.getJSONArray("sicRows").length()).all{SicContributionPolicy.enabled(q.getJSONArray("sicRows").getJSONObject(it).getString("format"))})
            assertEquals(old,db.scapDao().snapshot(id))
        }
    }
    @Test fun legacyBridgeErrorIsActionableAndCannotDisplayRetryEvenWhileOfflineOrRetryable(){
        val i=deliveryFixture().copy(state="ERROR",syncedRevision=0,error="El servidor rechazó (BRIDGE_IDENTITY_CONFLICT)")
        val identity=DriveDeliveryIdentity("Puente ficticio","PE-12A",i.revision,true)
        val v=DriveDeliveryPresentation.view(i,identity,false,false,true,retrying=true)
        assertEquals(DriveDeliveryStatus.ATTENTION,v.status);assertFalse(v.retryAllowed);assertFalse(v.confirmed);assertTrue(v.review)
        assertTrue(v.message.contains("otra identidad"));assertTrue(v.message.contains("se conservan"));assertTrue(DeliveryRejectionPolicy.conflict("BRIDGE_IDENTITY_CONFLICT"))
    }
    @Test fun fullyConfirmedCurrentReceiptTakesPriorityOverAStaleErrorLabel(){
        val i=deliveryFixture().copy(error="BRIDGE_IDENTITY_CONFLICT")
        val v=DriveDeliveryPresentation.view(i,DriveDeliveryIdentity("Puente ficticio","PE-12A",i.revision,true),false,false,true)
        assertTrue(v.confirmed);assertEquals(DriveDeliveryStatus.COMPLETE,v.status);assertFalse(v.retryAllowed)
    }
}
