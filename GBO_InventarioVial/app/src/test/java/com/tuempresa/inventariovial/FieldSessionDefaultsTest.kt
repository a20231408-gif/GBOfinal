package com.tuempresa.inventariovial

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.auth.*
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.field.*
import com.tuempresa.inventariovial.pavement.*
import com.tuempresa.inventariovial.road.ProjectRouteCatalog
import com.tuempresa.inventariovial.scap.catalog.ScapCatalog
import com.tuempresa.inventariovial.scap.data.ScapRepository
import com.tuempresa.inventariovial.scap.domain.ScapBridgeType
import com.tuempresa.inventariovial.sic.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class FieldSessionDefaultsTest {
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    private val routes get()=ProjectRouteCatalog.load(context)
    private fun session(route:String="PE-22A",bed:String="CD")=FieldSession("field","Project","Engineer","device",route,
        routes.requireRoute(route).segmentCode,bed,startTime=1)
    private fun record(route:String="HISTORICAL",bed:String="OLD")=InventoryRecordEntity("old","SIC-21","HORIZONTAL_MARKS",route,bed,
        "0042",125.5,null,null,null,-12.0,-77.0,null,1.0,"09/10/2026",null,"DRAFT","PENDING","PENDING",1,1,
        sessionId="old-session",segment="Historical segment",surveyDirection="DECREASING")
    private fun sessions()=AuthSessionManager().apply {login(UserCacheEntity("engineer","engineer","Engineer","ENGINEER",true,"",1,1,1))}

    @Test fun aAndBPreserveBothSelectedValuesAndTheRouteCatalogIdentity() {
        for((route,bed) in listOf("PE-22A" to "CD","PE-28H" to "A2")) {
            val choice=session(route,bed)
            val defaults=FieldSessionDefaults.location(choice,routes=routes)
            assertEquals(route,defaults.route);assertEquals(bed,defaults.roadbed)
            assertEquals(routes.requireRoute(route).segmentCode,defaults.segment)
            assertEquals(routes.requireRoute(route),routes.find(defaults.route))
        }
        // No interpretation of a roadbed as a lane, axis or route identifier.
        for(bed in listOf("Derecha","Izquierda")) assertEquals(bed,FieldSessionDefaults.location(session(bed=bed)).roadbed)
    }

    @Test fun recoveredDraftAndHistoricalBlanksAlwaysWinOverSessionDefaults() {
        val old=record()
        assertEquals(FieldFormLocation(old.routeCode,old.roadbedCode,old.segment!!,old.surveyDirection!!),
            FieldSessionDefaults.location(session(),old,routes))
        val blank=old.copy(routeCode="",roadbedCode="",segment=null,surveyDirection=null)
        assertEquals(FieldFormLocation(),FieldSessionDefaults.location(session(),blank,routes))
        assertEquals("HISTORICAL",old.routeCode);assertEquals("OLD",old.roadbedCode)
    }

    @Test fun manualRecordAndPreviousSurveyPositionCannotReplaceTheSession() {
        val active=session()
        val first=FieldSessionDefaults.location(active,routes=routes)
        val edited=first.copy(route="PE-3NG",roadbed="UC",segment="Tramo 2")
        assertEquals("PE-3NG",edited.route)
        repeat(4) {assertEquals(first,FieldSessionDefaults.location(active,routes=routes))}
        val prior=SurveyPosition("Tramo 2","PE-3NG","UC","0040",50.0,"INCREASING",active.sessionId)
        assertNull(FieldSessionDefaults.matchingPosition(active,prior))
        val matching=prior.copy(route=active.road!!,roadbed=active.roadbed!!)
        assertEquals(matching,FieldSessionDefaults.matchingPosition(active,matching))
        assertNull(FieldSessionDefaults.matchingPosition(active,matching.copy(sessionId="other")))
        assertNull(FieldSessionDefaults.matchingPosition(active,matching.copy(roadbed="UC")))
        assertEquals("PE-22A",active.road)
    }

    @Test fun absentOrEndedSessionDoesNotProvideNewDefaults() {
        assertEquals(FieldFormLocation(),FieldSessionDefaults.location(null,routes=routes))
        assertEquals(FieldFormLocation(),FieldSessionDefaults.location(session().copy(endTime=2),routes=routes))
        assertEquals("HISTORICAL",FieldSessionDefaults.location(null,record(),routes).route)
    }

    @Test fun switchingModulesAndCreatingConsecutiveRecordsUsesRoomSessionAndPreservesManualEdits()=runBlocking {
        val db=Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build()
        try {
            val active=session();db.inventoryDao().insertSession(active)
            val scap=ScapRepository(db,ScapCatalog.load(context),routes)
            val pavement=PavementRepository(db,sessions())
            val sic=SicFieldRepository(db)
            val created=mutableListOf<String>()
            for(type in ScapBridgeType.entries) {
                val id=scap.create("Engineer","device",active.sessionId,type)
                created+=id
                val snapshot=scap.dao.snapshot(id)!!
                assertEquals(active.road,snapshot.values()["route"])
                assertEquals(active.roadbed,snapshot.values()["sic17.roadbedCode"])
                assertEquals(active.road,snapshot.roadRecord!!.routeCode)
                assertEquals(active.roadbed,snapshot.roadRecord!!.roadbedCode)
                assertTrue(snapshot.roadRecord!!.startPrCode.isEmpty())
            }
            for(surface in PavementSurface.entries) {
                repeat(2) {
                    val id=pavement.newSample(surface);val sample=db.pavementDao().sample(id)!!
                    assertEquals(active.road,sample.ruta);assertEquals(active.roadbed,sample.calzada)
                    assertEquals(active.segment,sample.tramo)
                }
            }
            for(format in listOf(SicFieldFormat.SIC08,SicFieldFormat.SIC09,SicFieldFormat.SIC13,SicFieldFormat.SIC34)) {
                val defaults=FieldSessionDefaults.location(db.inventoryDao().currentSession(),routes=routes)
                val state=SicFieldState(format,mapOf("routeCode" to defaults.route,"roadbedCode" to defaults.roadbed,"_segment" to defaults.segment))
                val id="new-${format.code}";sic.save(id,state,"engineer",false,sessionId=active.sessionId)
                assertEquals(active.road,sic.load(id,format)!!.text("routeCode"))
                assertEquals(active.roadbed,sic.load(id,format)!!.text("roadbedCode"))
            }
            val first=created.first();val otherBefore=scap.dao.snapshot(created.last())!!
            scap.setField(first,"inspection","route","PE-3NG")
            scap.setField(first,"inspection","sic17.roadbedCode","UC")
            val manual=scap.dao.snapshot(first)!!
            assertEquals("PE-3NG",manual.roadRecord!!.routeCode)
            assertEquals("UC",manual.roadRecord!!.roadbedCode)
            assertEquals(otherBefore,scap.dao.snapshot(created.last()))
            assertEquals(active,db.inventoryDao().currentSession())
            val next=scap.dao.snapshot(scap.create("Engineer","device",active.sessionId,ScapBridgeType.SLAB))!!
            assertEquals(active.road,next.roadRecord!!.routeCode);assertEquals(active.roadbed,next.roadRecord!!.roadbedCode)
            db.inventoryDao().endSessions(2)
            val after=scap.dao.snapshot(scap.create("Engineer","device",active.sessionId))!!
            assertTrue(after.roadRecord!!.routeCode.isEmpty());assertTrue(after.roadRecord!!.roadbedCode.isEmpty())
            assertEquals(manual,scap.dao.snapshot(first))
        }finally{db.close()}
    }

    @Test fun pavementManualRoutePersistsWithoutChangingOtherSamplesOrNextDefaults()=runBlocking {
        val db=Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build()
        try {
            val active=session();db.inventoryDao().insertSession(active)
            val repo=PavementRepository(db,sessions())
            val id=repo.newSample(PavementSurface.ASPHALT);val initial=db.pavementDao().sample(id)!!
            repo.saveSample(initial.copy(ruta="PE-3NG",tramo="Tramo 2",calzada="UC",anchoM=5.0,largoM=40.0))
            val manual=db.pavementDao().sample(id)!!
            val next=db.pavementDao().sample(repo.newSample(PavementSurface.ASPHALT))!!
            assertEquals("PE-3NG",manual.ruta);assertEquals("UC",manual.calzada)
            assertEquals(active.road,next.ruta);assertEquals(active.roadbed,next.calzada)
            assertEquals(manual,db.pavementDao().sample(id))
            assertEquals(active,db.inventoryDao().currentSession())
        }finally{db.close()}
    }

    @Test fun completedScapContractualLocationAndRawDraftRemainUntouchedWhenSessionChanges()=runBlocking {
        val db=Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build()
        try {
            val repo=ScapRepository(db,ScapCatalog.load(context),routes)
            val id=repo.create("Engineer","device",null,ScapBridgeType.MODULAR)
            repo.saveSicLocation(id,"PE-3NG","UC","0042","125.5")
            repo.setSicLocationDraft(id,"routeCode","PE-28C")
            val old=repo.dao.snapshot(id)!!
            db.inventoryDao().insertSession(session())
            repo.create("Engineer","device","field",ScapBridgeType.SLAB)
            assertEquals(old,repo.dao.snapshot(id))
            assertEquals("PE-3NG",old.roadRecord!!.routeCode)
            assertEquals("PE-28C",old.values()["sicLocation.routeCode"])
        }finally{db.close()}
    }
}
