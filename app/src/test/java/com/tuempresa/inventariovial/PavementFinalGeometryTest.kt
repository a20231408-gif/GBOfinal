package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.auth.*
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.pavement.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID
import kotlin.math.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=Application::class)
class PavementFinalGeometryTest {
    @Test fun d24FanQuarterTurnKeepsMetricGeometryAndReversedPoints() {
        val s=visualSample()
        for((i,v) in listOf(listOf(.1,.1,3.2,1.0),listOf(.1,.1,1.0,4.7),listOf(.1,.1,2.0,2.0)).withIndex()) {
            val d=visualDamage(24,i+1,v[0],v[1],v[2],v[3],1)
            PavementRules.validate(s,d,emptyList())
            val lines=PavementRigidVisualGeometry.traces(s,d);assertEquals(9,lines.size)
            assertEquals(lines,PavementRigidVisualGeometry.traces(s,d.copy(x1=d.x2,y1=d.y2,x2=d.x1,y2=d.y1)))
            for(t in lines)for((a,b)in t.points.zipWithNext()) {
                val p=PavementPdfLayout.point(a.x,a.y,PavementPdfSegment(1,1,0.0,5.0),20.0)
                val q=PavementPdfLayout.point(b.x,b.y,PavementPdfSegment(1,1,0.0,5.0),20.0)
                assertEquals(hypot(b.x-a.x,b.y-a.y)*20,hypot(q.x-p.x,q.y-p.y),1e-7)
            }
        }
    }
    @Test fun anchoredAllEdgesSquareTieAndExplicitInteriorKeepMeasuresAndHistoricalBehavior() {
        val s=visualSample()
        for(code in listOf(29,30))for((edge,v) in listOf("L" to listOf(.2,1.0,1.2,2.0),"R" to listOf(2.3,1.0,3.3,2.0),"B" to listOf(1.0,.2,2.0,1.2),"T" to listOf(1.0,3.8,2.0,4.8),"L" to listOf(.25,.25,1.25,1.25))) {
            val d=visualDamage(code,1,v[0],v[1],v[2],v[3],1);val original=d.copy()
            val ps=s.copy(sicDataJson=PavementPatchVisuals.update(s,d,PavementPatchVisuals.ANCHORED))
            val p=PavementRigidVisualGeometry.traces(ps,d).single{it.closed}.points
            if(edge.contains("L"))assertEquals(0.0,p.minOf{it.x},1e-8)
            if(edge.contains("R"))assertEquals(3.5,p.maxOf{it.x},1e-8)
            if(edge.contains("B"))assertEquals(0.0,p.minOf{it.y},1e-8)
            if(edge.contains("T"))assertEquals(5.0,p.maxOf{it.y},1e-8)
            assertEquals(v[2]-v[0],p.maxOf{it.x}-p.minOf{it.x},1e-8)
            assertEquals(v[3]-v[1],p.maxOf{it.y}-p.minOf{it.y},1e-8)
            val interior=s.copy(sicDataJson=PavementPatchVisuals.update(ps,d,PavementPatchVisuals.INTERIOR))
            assertEquals(PavementRigidVisualGeometry.traces(s,d),PavementRigidVisualGeometry.traces(interior,d))
            assertEquals(original,d);assertEquals(1.0,PavementRules.quantity(d),0.0)
        }
    }
    @Test fun projectedPatchRetainsCorrectPdfSection() {
        val s=visualSample(60.0);val d=visualDamage(29,1,1.0,25.3,2.0,26.3,11)
        val p=s.copy(sicDataJson=PavementPatchVisuals.update(s,d,PavementPatchVisuals.ANCHORED))
        assertTrue(PavementPdfSegment(2,3,25.0,50.0).contains(p,d))
        assertFalse(PavementPdfSegment(3,3,50.0,60.0).contains(p,d))
    }
    @Test fun patchModeSurvivesRealRoomCloseReopenAndExportEnvelopeWithoutMigration()=runBlocking {
        val c=ApplicationProvider.getApplicationContext<Context>();val name="patch_${UUID.randomUUID()}.db"
        fun open()=Room.databaseBuilder(c,InventoryDatabase::class.java,name).build()
        val session=AuthSessionManager().apply{login(UserCacheEntity("test","test","FICTICIO","OPERATOR",true,"",0,0,0))}
        val s=visualSample().copy(sicDataJson="{\"HISTORICAL_UNKNOWN\":{\"literal\":\"=1+2\"}}")
        val d=visualDamage(29,1,.2,1.0,1.2,2.0,1)
        open().use {db->
            db.pavementDao().saveSample(s);val repo=PavementRepository(db,session)
            assertTrue(runCatching {repo.saveDamageWithPatchMode(d,"INVALID")}.isFailure)
            assertTrue(db.pavementDao().damages(s.id).isEmpty())
            repo.saveDamageWithPatchMode(d,PavementPatchVisuals.ANCHORED)
        }
        open().use {db->
            val saved=db.pavementDao().sample(s.id)!!;val damage=db.pavementDao().damage(d.id)!!
            assertEquals(20,db.openHelper.writableDatabase.version)
            assertEquals(PavementPatchVisuals.ANCHORED,PavementPatchVisuals.mode(saved,d.id))
            assertEquals("=1+2",JSONObject(saved.sicDataJson!!).getJSONObject("HISTORICAL_UNKNOWN").getString("literal"))
            assertEquals(d.x1,damage.x1);assertEquals(d.y2,damage.y2);assertEquals(1,damage.correlativo)
            val result=PavementCalculator(c).calculate(saved,listOf(damage))
            val native=PavementDriveUploader.nativeJson(saved,listOf(damage),result,emptyMap())
            assertTrue(native.toString().contains("_gboPatchVisuals"))
            val repo=PavementRepository(db,session);repo.saveDamageWithPatchMode(damage,PavementPatchVisuals.INTERIOR)
            assertEquals(PavementPatchVisuals.INTERIOR,PavementPatchVisuals.mode(db.pavementDao().sample(s.id)!!,d.id))
            assertEquals(1,db.pavementDao().damages(s.id).size);assertTrue(db.automaticSyncDao().all().isEmpty())
        }
    }
}
