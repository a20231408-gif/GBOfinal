package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.catalog.SicCatalogRepository
import com.tuempresa.inventariovial.field.*
import com.tuempresa.inventariovial.road.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],application=Application::class)
class FieldContinuation9Test {
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    @Test fun everyOfficialConditionKeepsItsCodeAndText() {
        val keys=listOf("sic17.structural.0","sic17.functional.0","sic18.structural.0","sic18.functional.0","sic19.structural.0","sic19.functional.0","sic20.structural.0","sic20.functional.0","sic21.condition.0","sic21.condition.1","sic21.condition.2","sic21.condition.3","sic22.condition.0")
        keys.forEach {key->val labels=SicCatalogRepository.options(key);val options=ContractConditionCatalog.options(labels)
            assertEquals(labels.map {it.substringBefore(" - ")},options.map {it.code})
            assertEquals(labels.map {it.substringAfter(" - ")},options.map {it.label})
            assertEquals(listOf(ConditionTone.FAVORABLE,ConditionTone.INTERMEDIATE,ConditionTone.UNFAVORABLE),options.map {it.tone})
        }
        assertEquals(ConditionTone.UNSPECIFIED,ContractConditionCatalog.options(listOf("1 - Ausente sin escala definida")).single().tone)
        assertEquals(ConditionTone.UNFAVORABLE,ContractConditionCatalog.options(listOf("0 - Mala")).single().tone)
    }
    @Test fun displayNumbersSurviveReorderingAndNewElements() {
        val a=testRecord("number9a").copy(createdAt=1);val b=testRecord("number9b").copy(createdAt=2)
        val numbers=ElementNumbers(context);val before=numbers.assign(listOf(b,a))
        assertEquals(before,numbers.assign(listOf(a,b)))
        val after=ElementNumbers(context).assign(listOf(testRecord("number9c").copy(createdAt=0),b,a))
        assertEquals(before[a.id],after[a.id]);assertEquals(before[b.id],after[b.id]);assertTrue(after.values.toSet().size==3)
    }
    @Test fun localGeometryAccumulatesSegmentsWithoutStraightLineGapsAndReverses() {
        val p=KmzRouteParser.parseKml("""<kml xmlns="http://www.opengis.net/kml/2.2"><Document><Placemark><LineString><coordinates>0,0 0.001,0</coordinates></LineString></Placemark><Placemark><LineString><coordinates>1,0 1.001,0</coordinates></LineString></Placemark></Document></kml>""".toByteArray(),"R9")
        val cumulative=LocalVisualOrder.cumulative(p.segments)
        assertEquals(cumulative[0].points.last().chainageM,cumulative[1].points.first().chainageM,0.0)
        assertEquals(222.39,cumulative.last().points.last().chainageM,0.2)
        assertEquals(1.001,LocalVisualOrder.cumulative(p.segments,true).first().points.first().longitude,0.0)
        val store=LocalVisualOrder(context);store.save(p)
        val r=testRecord("visual9",route="R9").copy(latitude=0.0,longitude=1.0005,gpsAccuracyM=1.0)
        val before=r.copy();assertEquals(166.79,store.position(r).meters!!,0.2)
        store.manual(r.id,25.0);assertEquals(25.0,LocalVisualOrder(context).position(r).meters!!,0.0)
        assertEquals(before,r);store.manual(r.id,null)
        assertNull(store.position(r.copy(gpsAccuracyM=100.0)).meters)
        assertNull(store.position(r.copy(latitude=null)).meters)
    }
    @Test fun selfCrossingAndParallelGeometryRequireManualReview() {
        val p=KmzRouteParser.parseKml("""<kml xmlns="http://www.opengis.net/kml/2.2"><LineString><coordinates>-0.001,-0.001 0.001,0.001 -0.001,0.001 0.001,-0.001</coordinates></LineString></kml>""".toByteArray(),"CROSS9")
        val position=LocalVisualOrder.locate(LocalVisualOrder.cumulative(p.segments),0.0,0.0,1.0,MatchConfig())
        assertNull(position.meters);assertTrue(position.warning.contains("ambiguo"))
    }
    @Test fun historicalEditDraftDoesNotDeliverAndIncompleteFinalizationRollsBack()=kotlinx.coroutines.runBlocking {
        androidx.room.Room.inMemoryDatabaseBuilder(context,com.tuempresa.inventariovial.data.database.InventoryDatabase::class.java).build().use {db->
            val r=testRecord("review9",sic="SIC-21").copy(latitude=null,longitude=null)
            db.inventoryDao().insertRecord(r)
            db.inventoryDao().insertSic21(com.tuempresa.inventariovial.data.entity.Sic21Entity(r.id,"18","1","1","1"))
            val s=db.inventoryDao().snapshot(r.id)!!
            val detail=com.tuempresa.inventariovial.repository.EngineeringEdits.form(s) as com.tuempresa.inventariovial.model.form.SicFormDetail.Sic21
            val incomplete=detail.copy(state=detail.state.copy(conditionCode=""))
            com.tuempresa.inventariovial.repository.EngineeringEdits.save(db,r.id,incomplete,emptyList(),emptyMap(),finalize=false)
            assertEquals("DRAFT",db.inventoryDao().recordById(r.id)!!.status)
            assertNull(db.automaticSyncDao().get("SIC/${r.id}"))
            assertTrue(runCatching {com.tuempresa.inventariovial.repository.EngineeringEdits.save(db,r.id,incomplete,emptyList(),emptyMap())}.isFailure)
            assertEquals("DRAFT",db.inventoryDao().recordById(r.id)!!.status)
            assertEquals(r.id,db.inventoryDao().snapshot(r.id)!!.record.id)
            assertNull(db.automaticSyncDao().get("SIC/${r.id}"))
        }
    }
    @Test fun allConditionCodesRoundTripRoomForEveryCoreFormatWithStableIdentity()=kotlinx.coroutines.runBlocking {
        androidx.room.Room.inMemoryDatabaseBuilder(context,com.tuempresa.inventariovial.data.database.InventoryDatabase::class.java).build().use {db->
            val dao=db.inventoryDao()
            for(sic in listOf("SIC-17","SIC-18","SIC-19","SIC-20","SIC-21","SIC-22")) {
                val id="condition9-$sic";dao.insertRecord(testRecord(id,sic=sic))
                when(sic) {
                    "SIC-17"->dao.insertSic17(com.tuempresa.inventariovial.data.entity.Sic17Entity(id,"01","1","P", "S",1,10.0,0.0,"1","1","1","1","Río",0.0))
                    "SIC-18"->dao.insertSic18(com.tuempresa.inventariovial.data.entity.Sic18Entity(id,"06","1",1,"1",1.0,1.0,"1","1"))
                    "SIC-19"->dao.insertSic19(com.tuempresa.inventariovial.data.entity.Sic19Entity(id,"10","1","1","1","1"))
                    "SIC-20"->dao.insertSic20(com.tuempresa.inventariovial.data.entity.Sic20Entity(id,"12","2",1.0,2.0,"1","1"))
                    "SIC-21"->dao.insertSic21(com.tuempresa.inventariovial.data.entity.Sic21Entity(id,"18","1","5","1"))
                    "SIC-22"->dao.insertSic22(com.tuempresa.inventariovial.data.entity.Sic22Entity(id,"21","1","1","R-1",null,"1",1.0,1.0,1.0))
                }
                for(code in listOf("1","2","3")) {
                    val d=com.tuempresa.inventariovial.repository.EngineeringEdits.form(dao.snapshot(id)!!)
                    val changed=when(d) {
                        is com.tuempresa.inventariovial.model.form.SicFormDetail.Sic17->d.copy(state=d.state.copy(structuralConditionCode=code,functionalConditionCode=code))
                        is com.tuempresa.inventariovial.model.form.SicFormDetail.Sic18->d.copy(state=d.state.copy(structuralConditionCode=code,functionalConditionCode=code))
                        is com.tuempresa.inventariovial.model.form.SicFormDetail.Sic19->d.copy(state=d.state.copy(structuralConditionCode=code,functionalConditionCode=code))
                        is com.tuempresa.inventariovial.model.form.SicFormDetail.Sic20->d.copy(state=d.state.copy(structuralConditionCode=code,functionalConditionCode=code))
                        is com.tuempresa.inventariovial.model.form.SicFormDetail.Sic21->d.copy(state=d.state.copy(conditionCode=code))
                        is com.tuempresa.inventariovial.model.form.SicFormDetail.Sic22->d.copy(state=d.state.copy(conditionCode=code))
                        else->error("Unexpected format")
                    }
                    com.tuempresa.inventariovial.repository.EngineeringEdits.save(db,id,changed,emptyList(),emptyMap(),finalize=false)
                    assertEquals(changed,com.tuempresa.inventariovial.repository.EngineeringEdits.form(dao.snapshot(id)!!))
                    assertEquals(id,dao.recordById(id)!!.id);assertNull(db.automaticSyncDao().get("SIC/$id"))
                }
            }
        }
    }
}
