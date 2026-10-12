package com.tuempresa.inventariovial

import com.tuempresa.inventariovial.dashboard.*
import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.core.DriveSyncIntent
import com.tuempresa.inventariovial.road.RoadPr
import org.junit.Test
import org.junit.Assert.*
import java.time.*

class Dashboard6PresentationTest {
    private val day=LocalDate.of(2026,10,11)
    private val time=day.atTime(8,0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    private fun session(id:String="session",direction:String="INCREASING",start:Long=time)=
        FieldSession(id,"Proyecto ficticio","Operador ficticio","test","R",null,"CD",direction,start)
    private fun draft(id:String="id",pr:String="0010",distance:Double=50.0)=
        testRecord(id=id,pr=pr,distance=distance,status="DRAFT",created=time).copy(sessionId="session",endPrCode=null,endDistanceM=null)
    private fun ids(rows:List<InventoryRecordEntity>,sessions:List<FieldSession> =listOf(session()),prs:List<RoadPr> =emptyList())=
        DashboardDrafts.order(rows,sessions,prs).map {it.id}
    @Test fun todayUsesRealSessionDate() {assertEquals(day,DashboardDrafts.date(draft().copy(createdAt=time-86400000),listOf(session())))}
    @Test fun historicalDraftUsesOriginalCreationNotModificationOrSurveyText() {
        val old=draft().copy(sessionId=null,createdAt=time-86400000,updatedAt=time,surveyDate="11/10/2026")
        assertEquals(day.minusDays(1),DashboardDrafts.date(old,emptyList()))
    }
    @Test fun dateRemainsStableAfterEditingOldDraft() {assertEquals(day.minusDays(1),DashboardDrafts.date(draft().copy(updatedAt=time+86400000),listOf(session(start=time-86400000))))}
    @Test fun dayFilterKeepsOldInFullList() {
        val old=draft("old").copy(sessionId=null,createdAt=time-86400000)
        assertEquals(listOf("id"),DashboardDrafts.filter(listOf(old,draft()),listOf(session()),day).map {it.id})
        assertEquals(2,DashboardDrafts.filter(listOf(old,draft()),listOf(session())).size)
    }
    @Test fun localZoneSetsDayWithoutUtcAssumption() {
        val r=draft().copy(sessionId=null,createdAt=Instant.parse("2026-10-11T02:00:00Z").toEpochMilli())
        assertEquals(day.minusDays(1),DashboardDrafts.date(r,emptyList(),ZoneId.of("America/Lima")))
    }
    @Test fun progressiveDisplaysPaddedCodeAndDistanceWithoutUuid() {assertEquals("PR 0041+325.50",DashboardDrafts.progressive(draft("uuid-secret","41",325.5)))}
    @Test fun linearProgressiveIncludesEnd() {assertEquals("PR 0010+050.00 → 0011+100.00",DashboardDrafts.progressive(draft().copy(endPrCode="0011",endDistanceM=100.0)))}
    @Test fun missingProgressiveNeverFallsBackToIdentity() {assertEquals("PROGRESIVA PENDIENTE",DashboardDrafts.progressive(draft("uuid-secret","")))}
    @Test fun increasingOrderIsNumeric() {assertEquals(listOf("9","10a","10b"),ids(listOf(draft("10b","10",125.5),draft("9","9",950.0),draft("10a","10",50.0))))}
    @Test fun decreasingOrderReversesPrAndDistance() {assertEquals(listOf("10b","10a","9"),ids(listOf(draft("9","9",950.0),draft("10a","10",50.0),draft("10b","10",125.5)),listOf(session(direction="DECREASING"))))}
    @Test fun equalProgressiveRetainsBothWithStableTie() {assertEquals(listOf("a","b","c"),ids(listOf(draft("c").copy(createdAt=time+1),draft("b"),draft("a"))))}
    @Test fun missingOrUnknownPrIsLastAcrossRoutes() {
        assertEquals(listOf("z","empty"),ids(listOf(draft("empty","").copy(routeCode="A"),draft("z").copy(routeCode="Z"))))
    }
    @Test fun catalogueSpecialOrderIsRespectedWithoutKmConversion() {
        val prs=listOf(RoadPr("R","CD","99",0.0,0.0,10.0),RoadPr("R","CD","2",0.0,0.0,20.0))
        assertEquals(listOf("99","2","unknown"),ids(listOf(draft("2","2"),draft("unknown","3"),draft("99","99")),prs=prs))
    }
    @Test fun routesBedsSessionsAndDirectionsStayDistinct() {
        val rows=listOf(draft("z").copy(routeCode="Z"),draft("bed").copy(roadbedCode="UD"),draft("next").copy(sessionId="next"),draft("first"))
        assertEquals(listOf("first","next","bed","z"),ids(rows,listOf(session(),session("next",start=time+1000))))
        assertTrue(DashboardDrafts.context(rows[2],listOf(session("next"))).contains("sesión"))
    }
    @Test fun filtersSelectDateRouteBedAndTypeTogether() {
        val rows=listOf(draft("culvert").copy(sicCode="SIC-18"),draft("ditch"),draft("other").copy(routeCode="Z"))
        assertEquals(listOf("culvert"),DashboardDrafts.filter(rows,listOf(session()),day,"R","CD","alcantarilla").map {it.id})
    }
    @Test fun savedPrReordersImmediatelyFromNewSnapshot() {
        val a=draft("a","10");val b=draft("b","20")
        assertEquals(listOf("a","b"),ids(listOf(a,b)))
        assertEquals(listOf("b","a"),ids(listOf(a.copy(startPrCode="30"),b)))
    }
    @Test fun syncCountsRequireAckAndExcludeIncompleteDrafts() {
        fun intent(id:String,state:String,revision:Long=1,synced:Long=0)=DriveSyncIntent("SIC/$id","SIC",id,revision,synced,state)
        val acknowledged=intent("confirmed","SYNCED",1,1).copy(receiptJson="{}",lastSyncedAt=1)
        val rows=listOf(acknowledged,intent("fake","SYNCED",1,1),intent("queue","QUEUED"),
            intent("edit","UPDATE_PENDING",2,1),intent("error","ERROR"),intent("conflict","CONFLICT"),intent("draft","DRAFT"))
        assertEquals(DashboardDeliveryCounts(1,2,2,1),DashboardDeliveryCounts.from(rows))
    }
}
