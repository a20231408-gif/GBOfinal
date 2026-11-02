package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.core.*
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.scap.catalog.ScapCatalog
import com.tuempresa.inventariovial.scap.data.*
import com.tuempresa.inventariovial.scap.domain.*
import com.tuempresa.inventariovial.scap.calculator.*
import com.tuempresa.inventariovial.scap.export.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream
import java.io.File

@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],application=Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScapFinalP0Test {
    private val c get()=ApplicationProvider.getApplicationContext<Context>()
    @Test fun newBothContractsPersistZeroPercentagesButNeverAssertPresenceOrGoodCondition()=runBlocking {
        for(type in ScapBridgeType.entries) {
            val name="zero-${type.name}.db";lateinit var id:String
            Room.databaseBuilder(c,InventoryDatabase::class.java,name).build().use{db->
                val r=ScapRepository(db,ScapCatalog.load(c));id=r.create("TEST","TEST",null,type)
                val s=r.dao.snapshot(id)!!
                assertTrue(s.elements.isNotEmpty())
                for(e in s.elements) {
                    assertEquals(List(6){0.0},ScapPercentages.values(e.condition!!))
                    assertEquals(ScapElementState.UNSET,ScapElementView.state(s,e.element))
                    assertNotNull(ScapPercentages.error(ScapPercentages.values(e.condition)))
                }
                val first=s.elements.first().element
                r.setField(id,first.id,"percent1","75");r.setField(id,first.id,"percent2","25")
                r.selectElement(id,first.elementCode,true);r.saveDraft(id)
                assertFalse(ScapCompletion.assess(r.dao.snapshot(id)!!,r.catalog.forType(type)).pending.any{
                    it.ownerId==first.id && (it.key=="presence"||it.key=="percent0")})
            }
            Room.databaseBuilder(c,InventoryDatabase::class.java,name).build().use{db->
                val r=ScapRepository(db,ScapCatalog.load(c));r.ensureBridgeDefaults(id)
                val s=r.dao.snapshot(id)!!;val first=s.elements.first()
                assertEquals(listOf(0.0,75.0,25.0,0.0,0.0,0.0),ScapPercentages.values(first.condition!!))
                val bytes=ByteArrayOutputStream();ScapDelivery.exporter(c,s).write(bytes,s){File(it).readBytes()}
                val book=TemplateWorkbook(bytes.toByteArray().inputStream())
                assertEquals(0.0,book.cell(5,"G14").textContent.toDouble(),0.0)
                assertEquals(75.0,book.cell(5,"H14").textContent.toDouble(),0.0)
                assertEquals(25.0,book.cell(5,"I14").textContent.toDouble(),0.0)
                assertNotNull(ScapPercentages.error(List(6){null}));assertNotNull(ScapPercentages.error(List(6){0.0}))
                assertNull(ScapPercentages.error(listOf(100.0,0.0,0.0,0.0,0.0,0.0)))
            }
        }
    }
    @Test fun historicalBlanksAndManualRatingsAreNeverFilledOnOpenOrReopen()=runBlocking {
        SicConsolidationFixtures.db(c).use{db->
            val r=ScapRepository(db,ScapCatalog.load(c));val id=r.create("TEST","TEST",null,ScapBridgeType.SLAB)
            val s=r.dao.snapshot(id)!!;val e=s.elements.first().element
            // Model a historical record with missing ratings and no new-creation marker.
            db.openHelper.writableDatabase.execSQL("DELETE FROM scap_conditions WHERE scapElementId=?",arrayOf(e.id))
            for(n in 0..5)r.dao.deleteValue(id,e.id,"percent$n")
            r.dao.deleteValue(id,"inspection","zeroPercentDefaultsVersion")
            val before=r.dao.snapshot(id)!!
            r.ensureBridgeDefaults(id);assertEquals(before,r.dao.snapshot(id))
            r.reopen(id);assertNull(r.dao.snapshot(id)!!.elements.first().condition)
            assertTrue((0..5).all{r.dao.snapshot(id)!!.values(e.id)["percent$it"]==null})
        }
    }
    @Test fun invalidValuesRemainDurableDraftsAndCannotBecomeContractualOrRemoteSuccess()=runBlocking {
        SicConsolidationFixtures.db(c).use{db->
            val r=ScapRepository(db,ScapCatalog.load(c));val id=r.create("TEST","TEST",null,ScapBridgeType.SLAB)
            r.setField(id,"inspection","sidewalkWidthM","INVALIDO");r.saveDraft(id)
            val saved=r.dao.snapshot(id)!!;assertEquals("INVALIDO",saved.values()["sidewalkWidthM"])
            assertEquals("DRAFT",saved.inspection.status)
            assertTrue(runCatching{r.complete(id,true)}.isFailure)
            assertTrue(db.automaticSyncDao().all().isEmpty())
            val block=ScapUploadReadiness.block(saved,r.catalog.forSnapshot(saved),true)!!
            assertEquals("sidewalkWidthM",block.route.fieldKey);assertEquals("B",block.route.subsection)
            assertTrue(block.message.contains("copia local"))
            assertTrue(runCatching{ScapDriveSync.reviewForUpload(saved,r.catalog.forSnapshot(saved),true)}.exceptionOrNull() is SyncPending)
        }
    }
    @Test fun closedIncompleteScapIsNotInstructedToCloseAgainAndCapabilitiesHaveTheirOwnCause()=runBlocking {
        SicConsolidationFixtures.db(c).use{db->
            val id=ScapP0Fixture.create(c,db,0);val i=db.automaticSyncDao().get("SCAP/$id")!!
            val identity=DriveDeliveryPresentation.identity(db,i,c)
            assertTrue(identity.validated);assertTrue(identity.provisional);assertNull(identity.issue)
            val queued=DriveDeliveryPresentation.view(i,identity,false,false,true)
            assertEquals(DriveDeliveryStatus.PENDING,queued.status);assertTrue(queued.message.contains("Internet"))
            val incompatible=i.copy(state="PENDING_VALIDATION",error="Pendiente de compatibilidad del servidor: panel fotográfico SCAP. Evidencia y cola conservadas.")
            val v=DriveDeliveryPresentation.view(incompatible,identity,false,true,true)
            assertTrue(v.message.contains("última consulta"));assertFalse(v.message.contains("finaliza"));assertTrue(v.retryAllowed)
            assertFalse(v.review);assertFalse(v.confirmed)
            val draft=DriveDeliveryPresentation.view(i.copy(state="DRAFT"),identity,false,true,true)
            assertTrue(draft.message.contains("ya está cerrada"));assertTrue(draft.retryAllowed)
        }
    }
    @Test fun actualPreparationFailureIsActionableAndDoesNotLoseTheSnapshot()=runBlocking {
        SicConsolidationFixtures.db(c).use{db->
            val id=ScapP0Fixture.create(c,db,1);val before=db.scapDao().snapshot(id)!!;val i=db.automaticSyncDao().get("SCAP/$id")!!
            var posts=0
            val bad=object:AutomaticDriveTransport {
                override fun supportsV3()=true
                override fun supportsFamilyPaths()=true
                override fun supportsReadablePhotoNames()=true
                override fun reservePhotoNames(payload:JSONObject):JSONObject=throw IllegalArgumentException("TEST_ONLY_INVALID_PREPARATION")
                override fun post(payload:JSONObject):JSONObject {posts++;error("No debe enviar")}
            }
            AutomaticSyncDrain.run(c,db,{bad},configurationError=null)
            val blocked=db.automaticSyncDao().get(i.key)!!
            assertEquals("BLOCKED",blocked.state);assertTrue(blocked.error!!.startsWith("No se pudieron preparar"))
            assertEquals(0,posts);assertEquals(0,blocked.syncedRevision);assertEquals(before,db.scapDao().snapshot(id))
            val v=DriveDeliveryPresentation.view(blocked,DriveDeliveryPresentation.identity(db,blocked,c),false,true,true)
            assertTrue(v.message.contains("preparar"));assertTrue(v.retryAllowed);assertFalse(v.confirmed)
        }
    }
    @Test fun currentAckOnlyAndEditedRevisionRemainDistinctFromLocalAndContractualCompletion() {
        val i=deliveryFixture();val id=DriveDeliveryIdentity("TEST","TEST",i.revision,true,provisional=true)
        val v=DriveDeliveryPresentation.view(i,id,false,true,true)
        assertTrue(v.confirmed);assertTrue(v.message.contains("No acredita finalización contractual"))
        val edit=DriveDeliveryPresentation.view(i,id.copy(currentRevision=i.revision+1),false,true,true)
        assertFalse(edit.confirmed);assertTrue(edit.message.contains("cambios locales"))
        assertTrue(DriveDeliveryListPresentation.select(listOf(DriveDeliveryEntry(i,id,v)),DriveDeliverySection.QUEUE).isEmpty())
        assertEquals(1,DriveDeliveryListPresentation.select(listOf(DriveDeliveryEntry(i,id,v)),DriveDeliverySection.HISTORY).size)
        val unknown=DriveDeliveryPresentation.view(i.copy(state="UPLOADING",syncedRevision=0),id,false,true,true)
        assertEquals(DriveDeliveryStatus.VERIFYING,unknown.status);assertFalse(unknown.confirmed)
    }
    @Test fun waitingTimeNeverClaimsSixtyNineMinutesOfActiveUpload() {
        val p=SyncProgress(2,"PHOTOS",3,6,1)
        val elapsed=SyncProgressPresentation.elapsed(p,69*60*1000L+1)
        assertTrue(elapsed.contains("69 min"));assertTrue(elapsed.contains("no mide la duración de subida"))
        val i=deliveryFixture().copy(state="UPLOADING",syncedRevision=0)
        val id=DriveDeliveryIdentity("TEST","TEST",2,true)
        assertEquals(DriveDeliveryStatus.VERIFYING,DriveDeliveryPresentation.view(i,id,false,true,true,progress=p).status)
        assertEquals("Preparando archivos",DriveDeliveryPresentation.view(i,id,true,true,true,progress=p.copy(phase="PREPARING")).message)
    }
    @Test fun staleDraftRecoveryOnlyRequeuesTheExactClosedDriveRevisionWithoutChangingEvidence()=runBlocking {
        SicConsolidationFixtures.db(c).use{db->
            val id=ScapP0Fixture.create(c,db,0);val old=db.automaticSyncDao().get("SCAP/$id")!!
            val before=db.scapDao().snapshot(id)!!
            db.automaticSyncDao().put(old.copy(state="DRAFT"))
            assertEquals(1,AutomaticSync.recoverClosedScapDrafts(db))
            assertEquals(old,db.automaticSyncDao().get(old.key));assertEquals(before,db.scapDao().snapshot(id))
            assertEquals(0,AutomaticSync.recoverClosedScapDrafts(db))
            db.automaticSyncDao().put(old.copy(state="DRAFT",revision=old.revision-1))
            assertEquals(0,AutomaticSync.recoverClosedScapDrafts(db))
            db.automaticSyncDao().put(old.copy(state="DRAFT"))
            db.localFilesDao().saveChoice(com.tuempresa.inventariovial.localfiles.DeviceDeliveryChoice(old.key,"DEVICE","TEST",1))
            assertEquals(0,AutomaticSync.recoverClosedScapDrafts(db))
            val repo=ScapRepository(db,ScapCatalog.load(c));repo.reopen(id)
            assertEquals(0,AutomaticSync.recoverClosedScapDrafts(db))
        }
    }
    @Test fun explicitClosedRevisionConsentIsRecoveredButNeverInventedOrAppliedToFrozenPayloads()=runBlocking {
        SicConsolidationFixtures.db(c).use{db->
            val id=ScapP0Fixture.create(c,db,0);val key="SCAP/$id"
            val old=db.automaticSyncDao().get(key)!!.copy(warningsConfirmed=false)
            val before=db.scapDao().snapshot(id)!!
            db.automaticSyncDao().put(old)
            assertTrue(AutomaticSync.scapWarningConsent(db,id))
            assertNull(DriveDeliveryPresentation.identity(db,old,c).issue)
            assertEquals(old.copy(warningsConfirmed=true),AutomaticSync.restoreScapConsent(db,old))
            assertEquals(before,db.scapDao().snapshot(id))
            val frozen=old.copy(preparedJson="{\"revision\":"+old.revision+"}")
            db.automaticSyncDao().put(frozen);assertEquals(frozen,AutomaticSync.restoreScapConsent(db,frozen))
            db.scapDao().putValue(ScapFieldValueEntity(id,"completion","uploadedWithWarnings","true","TEMPLATE"))
            db.automaticSyncDao().put(old);assertFalse(AutomaticSync.scapWarningConsent(db,id))
            assertEquals(old,AutomaticSync.restoreScapConsent(db,old))
            val repo=ScapRepository(db,ScapCatalog.load(c));repo.reopen(id)
            assertFalse(AutomaticSync.scapWarningConsent(db,id));assertEquals(db.automaticSyncDao().get(key),AutomaticSync.restoreScapConsent(db,old))
        }
    }
}
