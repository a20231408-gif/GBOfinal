package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import android.graphics.*
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.auth.*
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.pavement.*
import com.tuempresa.inventariovial.pavement.ui.PavementDrawing
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
import java.util.UUID
import kotlin.math.*

internal fun axisSample()=visualSample().copy(anchoM=6.0,largoM=6.0,slabRows=1,slabColumns=1,prFinal="00+006")
internal fun legacyAnchor(s:PavementSampleEntity,d:PavementDamageEntity):PavementSampleEntity {
    val e=JSONObject().put("version",1).put("sampleId",s.id).put("damageId",d.id).put("mode","ANCHORED")
        .put("rows",s.slabRows).put("columns",s.slabColumns).put("width",s.anchoM).put("length",s.largoM)
    return s.copy(sicDataJson=JSONObject().put("_gboPatchVisuals",JSONObject().put(d.id,e)).toString())
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PavementAxisAnchoringTest {
    @Test fun d24FourReflectionsPreserveMeasuredExtentsAndPointOrder() {
        val s=axisSample()
        for((b,h) in listOf(4.0 to 1.0,1.0 to 4.0,3.0 to 2.0,2.0 to 3.0,3.0 to 3.0)) {
            val base=visualDamage(24,1,.1,.1,.1+b,.1+h,1)
            val canonical=PavementDurabilityGeometry.project(s,base)
            assertNull(canonical.warning);assertEquals(9,canonical.traces.size)
            for(right in listOf(false,true))for(top in listOf(false,true)) {
                fun x(v:Double)=if(right)6-v else v
                fun y(v:Double)=if(top)6-v else v
                val d=base.copy(x1=x(base.x1!!),x2=x(base.x2!!),y1=y(base.y1!!),y2=y(base.y2!!));val before=d.copy()
                val p=PavementDurabilityGeometry.project(s,d);assertNull(p.warning)
                assertEquals(SketchPoint(if(right)6.0 else 0.0,if(top)6.0 else 0.0),p.corner)
                assertEquals(p,PavementDurabilityGeometry.project(s,d.copy(x1=d.x2,x2=d.x1,y1=d.y2,y2=d.y1)))
                for((original,reflected) in canonical.traces.zip(p.traces))for((a,z)in original.points.zip(reflected.points)) {
                    assertEquals(x(a.x),z.x,1e-8);assertEquals(y(a.y),z.y,1e-8)
                    assertTrue(z.x in 0.0..6.0 && z.y in 0.0..6.0)
                }
                val points=p.traces.flatMap{it.points}
                assertEquals(b,points.maxOf{it.x}-points.minOf{it.x},1e-8)
                assertEquals(h,points.maxOf{it.y}-points.minOf{it.y},1e-8)
                assertTrue(p.traces.none{it.closed});assertEquals(before,d);assertEquals(1.0,PavementRules.quantity(d),0.0)
            }
        }
    }
    @Test fun d24AmbiguousInteriorZeroAxisMultipleSlabsAndHistoricalDataNeverInventACorner() {
        val s=axisSample()
        for(d in listOf(visualDamage(24,1,2.0,2.0,3.0,3.0,1),visualDamage(24,1,.1,.1,5.9,5.9,1),
            visualDamage(24,1,0.0,0.0,6.0,0.0,1),visualDamage(24,1,2.0,2.0,2.0,2.0,1),
            visualDamage(24,1,.1,.1,2.0,2.0,1).copy(x1=null,y1=null,x2=null,y2=null),
            visualDamage(24,1,.1,.1,2.0,2.0,1).copy(slabs="1,2"))) {
            val ps=if(d.slabs=="1,2")s.copy(slabColumns=2,anchoM=12.0)else s
            val p=PavementDurabilityGeometry.project(ps,d);assertNotNull(p.warning);assertNull(p.corner)
            assertTrue(p.traces.all{it.points.size==2 && !it.closed})
        }
    }
    @Test fun bothPatchCodesAnchorToParallelEdgesEvenWhenThePerpendicularEdgeIsCloser() {
        val s=axisSample()
        val cases=listOf("T" to listOf(.1,4.5,4.1,5.5),"B" to listOf(1.5,.2,5.5,1.2),
            "L" to listOf(.2,1.0,1.2,5.0),"R" to listOf(4.8,.1,5.8,4.1),
            "L" to listOf(1.5,.1,2.5,4.1),"B" to listOf(1.9,1.5,5.9,2.5))
        for(code in listOf(29,30))for((i,case)in cases.withIndex()) {
            val(edge,v)=case;val d=visualDamage(code,i+1,v[0],v[1],v[2],v[3],1)
            val p=s.copy(sicDataJson=PavementPatchVisuals.update(s,d,PavementPatchVisuals.ANCHORED))
            val traces=PavementRigidVisualGeometry.traces(p,d);val outline=traces.single{it.closed}.points
            val left=outline.minOf{it.x};val right=outline.maxOf{it.x};val bottom=outline.minOf{it.y};val top=outline.maxOf{it.y}
            when(edge){"T"->assertEquals(6.0,top,1e-8);"B"->assertEquals(0.0,bottom,1e-8);"L"->assertEquals(0.0,left,1e-8);"R"->assertEquals(6.0,right,1e-8)}
            assertEquals(v[2]-v[0],right-left,1e-8);assertEquals(v[3]-v[1],top-bottom,1e-8)
            if(edge in listOf("T","B")){assertEquals(v[0],left,1e-8);assertEquals(v[2],right,1e-8)}
            else{assertEquals(v[1],bottom,1e-8);assertEquals(v[3],top,1e-8)}
            assertEquals(traces,PavementRigidVisualGeometry.traces(p,d.copy(x1=d.x2,x2=d.x1,y1=d.y2,y2=d.y1)))
            assertTrue(traces.flatMap{it.points}.all{it.x in -1e-8..6.00000001&&it.y in -1e-8..6.00000001})
        }
    }
    @Test fun squarePatchTieIsDeterministicAndExplicitInteriorDoesNotMove() {
        val s=axisSample();val d=visualDamage(29,1,1.0,1.0,3.0,3.0,1)
        val p=s.copy(sicDataJson=PavementPatchVisuals.update(s,d,PavementPatchVisuals.ANCHORED))
        val points=PavementRigidVisualGeometry.traces(p,d).single{it.closed}.points
        assertEquals(0.0,points.minOf{it.x},0.0);assertEquals(1.0,points.minOf{it.y},0.0) // L wins equal L/B; no automatic corner.
        val inside=p.copy(sicDataJson=PavementPatchVisuals.update(p,d,PavementPatchVisuals.INTERIOR))
        assertEquals(PavementRigidVisualGeometry.traces(s,d),PavementRigidVisualGeometry.traces(inside,d))
    }
    @Test fun historicalAnchorAndInteriorKeepTheirPreviousLocationAndMetadataOnReopen() {
        val s=axisSample();val d=visualDamage(29,1,.1,1.0,4.1,2.0,1)
        val historic=legacyAnchor(s,d);val expected=PavementRigidVisualGeometry.traces(historic,d)
        assertFalse(PavementPatchVisuals.parallelAnchoring(historic,d.id))
        val points=expected.single{it.closed}.points;assertEquals(0.0,points.minOf{it.x},1e-8);assertEquals(1.0,points.minOf{it.y},1e-8)
        val reopened=historic.copy(sicDataJson=PavementPatchVisuals.update(historic,d,PavementPatchVisuals.mode(historic,d.id)))
        assertEquals(expected,PavementRigidVisualGeometry.traces(reopened,d))
        assertFalse(JSONObject(reopened.sicDataJson!!).getJSONObject("_gboPatchVisuals").getJSONObject(d.id).has("anchorRule"))
        assertEquals(PavementPatchVisuals.INTERIOR,PavementPatchVisuals.mode(s,d.id))
        val current=s.copy(sicDataJson=PavementPatchVisuals.update(s,d,PavementPatchVisuals.ANCHORED))
        assertTrue(PavementPatchVisuals.parallelAnchoring(current,d.id));assertEquals(0.0,PavementRigidVisualGeometry.traces(current,d).first().points.minOf{it.y},1e-8)
    }
    @Test fun oversizedAnchorIsReportedAndTransactionRollsBackWithoutChangingMeasurements()=runBlocking {
        val c=ApplicationProvider.getApplicationContext<Context>();val s=axisSample().copy(anchoM=12.0,slabColumns=2)
        val d=visualDamage(29,1,.5,1.0,7.5,2.0,1).copy(slabs="1,2")
        Room.inMemoryDatabaseBuilder(c,InventoryDatabase::class.java).build().use{db->
            db.pavementDao().saveSample(s)
            val sessions=AuthSessionManager().apply{login(UserCacheEntity("test","test","FICTICIO","OPERATOR",true,"",0,0,0))}
            val repo=PavementRepository(db,sessions)
            val error=runCatching{repo.saveDamageWithPatchMode(d,PavementPatchVisuals.ANCHORED)}.exceptionOrNull()
            assertNotNull(error);assertTrue(error!!.message!!.contains("no cabe"))
            assertNull(db.pavementDao().damage(d.id));assertEquals(s,db.pavementDao().sample(s.id));assertTrue(db.automaticSyncDao().all().isEmpty())
            repo.saveDamageWithPatchMode(d,PavementPatchVisuals.INTERIOR)
            val saved=db.pavementDao().damage(d.id)!!;assertEquals(d.x1,saved.x1);assertEquals(d.x2,saved.x2);assertEquals(2.0,PavementRules.quantity(saved),0.0)
        }
    }
    @Test fun newRuleAndInteriorSelectionSurviveRealRoomCloseAndReopenWithSameUuid()=runBlocking {
        val c=ApplicationProvider.getApplicationContext<Context>();val name="axis_${UUID.randomUUID()}.db"
        fun open()=Room.databaseBuilder(c,InventoryDatabase::class.java,name).build()
        val s=axisSample();val d=visualDamage(30,1,1.5,.1,2.5,4.1,1)
        val sessions=AuthSessionManager().apply{login(UserCacheEntity("test","test","FICTICIO","OPERATOR",true,"",0,0,0))}
        open().use{db->db.pavementDao().saveSample(s);PavementRepository(db,sessions).saveDamageWithPatchMode(d,PavementPatchVisuals.ANCHORED)}
        open().use{db->
            val saved=db.pavementDao().sample(s.id)!!;assertEquals(20,db.openHelper.writableDatabase.version)
            assertTrue(PavementPatchVisuals.parallelAnchoring(saved,d.id));assertEquals(d.id,db.pavementDao().damage(d.id)!!.id)
            val points=PavementRigidVisualGeometry.traces(saved,db.pavementDao().damage(d.id)!!).first().points
            assertEquals(0.0,points.minOf{it.x},1e-8)
            PavementRepository(db,sessions).saveDamageWithPatchMode(db.pavementDao().damage(d.id)!!,PavementPatchVisuals.INTERIOR)
        }
        open().use{db->assertEquals(PavementPatchVisuals.INTERIOR,PavementPatchVisuals.mode(db.pavementDao().sample(s.id)!!,d.id));assertEquals(1,db.pavementDao().damages(s.id).size)}
    }
    @Test fun originalAxisGeometryMatchesActualCanvasAndRotatedPdfRendererByColorPixels() {
        val s=axisSample().copy(largoM=12.5,prFinal="00+012.5")
        for(d in listOf(visualDamage(24,1,.1,.1,4.1,1.1,1),visualDamage(24,2,.1,.1,1.1,4.1,1),visualDamage(29,3,.1,10.5,4.1,11.5,1),visualDamage(30,4,4.8,.1,5.8,4.1,1))) {
            val ps=if(d.code in listOf(29,30))s.copy(sicDataJson=PavementPatchVisuals.update(s,d,PavementPatchVisuals.ANCHORED))else s
            val p=PavementRigidPdfScheme.plans(ps,listOf(d)).single();val scale=p.region.scale.toFloat()
            val normal=Bitmap.createBitmap(ceil(s.anchoM*scale).toInt(),ceil(s.largoM*scale).toInt(),Bitmap.Config.ARGB_8888)
            PavementDrawing.draw(Canvas(normal),ps,listOf(d),scale,labels=false,slabLabels=false)
            val pdf=Bitmap.createBitmap(842,595,Bitmap.Config.ARGB_8888)
            PavementRigidPdfScheme.draw(Canvas(pdf),ps,p,56f,105f)
            val color=PavementSeverity.color(d.severity);var colored=0;var matched=0
            for(x in 0 until normal.width)for(y in 0 until normal.height)if(normal.getPixel(x,y)==color){
                colored++;val px=(56+s.largoM*scale-y-1).roundToInt();val py=135+x
                if((-2..2).any{dx->(-2..2).any{dy->val xx=px+dx;val yy=py+dy;xx in 0 until pdf.width&&yy in 0 until pdf.height&&pdf.getPixel(xx,yy)==color}})matched++
            }
            assertTrue(colored>100);assertTrue("same geometry after PDF rotation: $matched/$colored",matched.toDouble()/colored>.85)
            normal.recycle();pdf.recycle()
        }
    }
    @Test fun pdfLayoutAndCalloutRegressionsStayApprovedWithoutChangingTheirSource() {
        val t=PavementPdfLegibilityTest()
        t.largeRegionsUseAtLeastEightyFivePercentWidthWithoutAnisotropicStretchOrMissingCoverage()
        t.crowdedBandsNeverOverlapAndNoItemIsLostOrRenumbered()
        t.emptyLongViewsDoNotCreateStandaloneNoticePages()
    }
}
