package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.core.*
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.delivery.sha256
import com.tuempresa.inventariovial.sic.*
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/** All network calls are intercepted; only synthetic records and memory databases. */
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],application=Application::class)
class RootRoutingEnvelopeTest {
    private fun caps()=JSONObject().put("ok",true).put("syncProtocol",3).put("pathVersion",4)
        .put("stableFamilyNumbering",true).put("photoCategoryVersion",1).put("readablePhotoNamingVersion",2)
        .put("rootRoutingVersion",2).put("rootEpoch","e".repeat(32)).put("rootActivatedAt",100L)
    private fun payload()=JSONObject().put("action","commitRecordV3").put("key","SIC/FICTITIOUS")
        .put("kind","SIC").put("recordId","FICTITIOUS").put("identityId","FICTITIOUS")
        .put("revision",1L).put("baseRevision",0L).put("captureCreatedAt",123L).put("files",JSONArray()).put("sicRows",JSONArray())

    @Test fun originalV21GetsItsOriginalEnvelopeAndTheFrozenInputIsUnchanged() {
        val p=payload().put("rootEpoch","old").put("rootRoutingVersion",1);val before=p.toString()
        val wire=RootRoutingEnvelope.apply(p,JSONObject())
        for(k in listOf("rootEpoch","rootRoutingVersion","captureCreatedAt"))assertFalse(wire.has(k))
        assertEquals(before,p.toString());assertEquals(p.getString("key"),wire.getString("key"))
    }
    @Test fun compatibleBackendReceivesActualCreationTimeAndAnnouncedEpoch() {
        val wire=RootRoutingEnvelope.apply(payload(),caps())
        assertEquals(123L,wire.getLong("captureCreatedAt"));assertEquals(2,wire.getInt("rootRoutingVersion"))
        assertEquals("e".repeat(32),wire.getString("rootEpoch"))
    }
    @Test fun missingFrozenLegacyTimestampIsNeverReplacedWithNow() {
        val p=payload();p.remove("captureCreatedAt")
        assertFalse(RootRoutingEnvelope.apply(p,caps()).has("captureCreatedAt"))
    }
    @Test fun incompatibleOrUnconfiguredRoutingStopsBeforeSending() {
        for(c in listOf(caps().put("rootRoutingVersion",1),caps().put("rootEpoch",""),caps().put("rootActivatedAt",0L))) {
            try {RootRoutingEnvelope.apply(payload(),c);fail("Invalid admission accepted")}
            catch(expected:SyncPending) {assertTrue(expected.message!!.contains("Cola conservada"))}
        }
    }
    @Test fun routingMetadataDoesNotChangeCanonicalHashOrRevision() {
        val p=payload().put("routeCode","PE-22A").put("familyPath","ALCANTARILLAS").put("family","ALCANTARILLA")
        val hash=AutomaticDelivery.contentHash(p);p.put("contentHash",hash)
        for(c in listOf(JSONObject(),caps())) {
            val wire=RootRoutingEnvelope.apply(p,c)
            assertEquals(hash,AutomaticDelivery.contentHash(wire));assertEquals(hash,wire.getString("contentHash"))
            assertEquals(1L,wire.getLong("revision"));assertEquals("FICTITIOUS",wire.getString("recordId"))
        }
    }
    @Test fun reservationStageAndCommitCarryTheSameAdmissionWithoutChangingAck() {
        val context=ApplicationProvider.getApplicationContext<Context>();val file=File(context.cacheDir,"fictitious-root-routing.txt").apply {writeText("synthetic")}
        val p=payload().put("contentHash","a".repeat(64))
            .put("files",JSONArray().put(JSONObject().put("key","metadata").put("path","metadata.json")
                .put("mimeType","application/json").put("localFile",file.path).put("sha256",sha256(file.readBytes()))))
        val seen=mutableListOf<JSONObject>()
        val http=AutomaticDriveHttp(OkHttpClient.Builder().addInterceptor {chain->
            val req=chain.request();val result=if(req.method=="GET")caps() else {
                val b=Buffer();req.body!!.writeTo(b);val request=JSONObject(b.readUtf8());seen.add(request)
                when(request.optString("action")) {
                    "stageRecordV3"->JSONObject().put("ok",true).put("key",p.getString("key")).put("revision",1L)
                        .put("fileKey","metadata").put("sha256",sha256(file.readBytes())).put("stageFileId","FICTITIOUS_STAGE")
                    else->JSONObject().put("ok",true).put("contentHash",p.getString("contentHash"))
                }
            }
            Response.Builder().request(req).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body(result.toString().toResponseBody("application/json".toMediaType())).build()
        }.build(),"https://example.invalid/root-routing","")
        http.reservePhotoNames(p);val result=http.post(p)
        assertEquals(listOf("reservePhotoNamesV2","stageRecordV3","commitRecordV3"),seen.map{it.getString("action")})
        seen.forEach {assertEquals(123L,it.getLong("captureCreatedAt"));assertEquals("e".repeat(32),it.getString("rootEpoch"));assertEquals(2,it.getInt("rootRoutingVersion"))}
        assertEquals(p.getString("contentHash"),result.getString("contentHash"));assertTrue(p.getJSONArray("files").getJSONObject(0).has("localFile"))
    }
    @Test fun reservationUsesPersistedCaptureTimeRatherThanRevisionOrSendTime()=runBlocking(Dispatchers.IO) {
        val c=ApplicationProvider.getApplicationContext<Context>()
        Room.inMemoryDatabaseBuilder(c,InventoryDatabase::class.java).build().use {db->
            SicFieldRepository(db).save("FICTITIOUS_CREATED",fajaState(SicFieldFormat.SIC08),"synthetic",true)
            val i=db.automaticSyncDao().all().single();val record=requireNotNull(db.inventoryDao().recordById(i.recordId))
            assertEquals(record.createdAt,AutomaticDelivery.namingPreview(c,db,i).getLong("captureCreatedAt"))
            assertEquals(record.createdAt,AutomaticDelivery.namingPreview(c,db,i.copy(revision=i.revision+1)).getLong("captureCreatedAt"))
        }
    }
}
