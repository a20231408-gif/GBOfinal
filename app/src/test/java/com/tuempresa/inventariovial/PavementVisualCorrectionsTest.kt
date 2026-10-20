package com.tuempresa.inventariovial

import android.app.Application
import com.tuempresa.inventariovial.pavement.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=Application::class)
class PavementVisualCorrectionsTest {
    @Test fun codeMappingAndCatalogRemainDistinct() {
        assertEquals(PavementSketchKind.DURABILITY_D,PavementSketch.kind(24))
        for(c in listOf(29,30))assertEquals(PavementSketchKind.PATCH,PavementSketch.kind(c))
        assertEquals(PavementSketchKind.CORNER,PavementSketch.kind(22));assertEquals(PavementSketchKind.GLOBAL,PavementSketch.kind(26))
        assertEquals("Grieta de durabilidad D",PavementCatalog.forSurface(PavementSurface.RIGID).first {it.code==24}.name)
    }
    @Test fun dCracksFormOpenBranchedFanConnectedToBothAdjacentEdges() {
        val s=visualSample()
        for(d in listOf(visualDamage(24,1,.05,.05,.6,4.5,1),visualDamage(24,2,.05,.05,3.1,.6,1))) {
            val t=PavementRigidVisualGeometry.traces(s,d)
            assertEquals(9,t.size);assertTrue(t.none{it.closed})
            for(line in t.take(6)) {
                assertEquals(0.0,line.points.first().x,1e-8);assertEquals(0.0,line.points.last().y,1e-8)
                assertTrue(line.points.first().y>0 && line.points.last().x>0)
            }
            for(branch in t.drop(6)) {
                assertTrue(t.take(6).flatMap{it.points}.contains(branch.points.first()))
                assertTrue(t.take(6).flatMap{it.points}.contains(branch.points.last()))
            }
        }
    }
    @Test fun interiorDDoesNotJumpToAnUnobservedJoint() {
        val d=visualDamage(24,3,1.2,1.2,2.1,3.5,1)
        val p=PavementDurabilityGeometry.project(visualSample(),d)
        assertNotNull(p.warning);assertNull(p.corner);assertTrue(p.traces.all{it.points.size==2})
    }
    @Test fun patchesKeepRecordedInteriorAndEdgeExtentAndDifferentAspectRatios() {
        val s=visualSample()
        for(d in listOf(visualDamage(29,1,0.0,.5,2.5,1.5,1),visualDamage(29,2,2.5,.5,3.5,4.5,1),visualDamage(30,3,1.2,2.0,1.8,2.6,1))) {
            val t=PavementRigidVisualGeometry.traces(s,d);val o=t.single {it.closed}
            assertEquals(4,o.points.size);assertTrue(t.size>1)
            assertEquals(d.x1!!,o.points.minOf {it.x},0.0);assertEquals(d.x2!!,o.points.maxOf {it.x},0.0)
            assertTrue(t.flatMap {it.points}.all {it.x in d.x1!!..d.x2!! && it.y in d.y1!!..d.y2!!})
            assertEquals(1.0,PavementRules.quantity(d),0.0)
        }
    }
    @Test fun smallPatchActualAreaStaysSmallWithoutChangingQuantityOrSeverity() {
        val d=visualDamage(30,1,1.2,2.0,1.8,2.6,1);val saved=d.copy()
        val outline=PavementRigidVisualGeometry.traces(visualSample(),d).first()
        val area=(outline.points.maxOf {it.x}-outline.points.minOf {it.x})*(outline.points.maxOf {it.y}-outline.points.minOf {it.y})
        assertTrue(area<=.5);assertEquals(saved,d);assertEquals(1.0,PavementRules.quantity(d),0.0);assertEquals("M",d.severity)
    }
    @Test fun skinnyHistoricalPatchCannotGenerateUnboundedHatching() {
        val d=visualDamage(29,1,0.0,.1,3.5,.10000001,1)
        assertTrue(PavementRigidVisualGeometry.traces(visualSample(),d).size<=35)
    }
    @Test fun historicalNoCoordinatesStaysUnlocalizedAndRetainsIds() {
        val d=visualDamage(24,1,0.0,0.0,0.0,0.0,1).copy(x1=null,x2=null,y1=null,y2=null)
        val before=d.copy();assertTrue(PavementRigidVisualGeometry.traces(visualSample(),d).isNotEmpty())
        assertFalse(PavementSlabGeometry.localized(d));assertEquals(before,d)
    }
    @Test fun horizontalSegmentsFollowLongitudinalRowsAndNeverRestartSlabNumbers() {
        val s=visualSample(100.0);val segments=PavementPdfLayout.segments(s.anchoM,s.largoM,slabLength=5.0)
        assertTrue(segments.size>=3);assertEquals(0.0,segments.first().start,0.0);assertEquals(100.0,segments.last().end,0.0)
        segments.zipWithNext().forEach {(a,b)->assertEquals(a.end,b.start,0.0)}
        assertTrue(segments.dropLast(1).all {it.end%5==0.0})
        assertTrue(segments.all {it.length*PavementPdfLayout.scale(s.anchoM)<=PavementPdfLayout.PLOT_WIDTH+.01})
    }
    @Test fun quarterTurnKeepsOriginAxesAndCoordinatesWithoutInversion() {
        val seg=PavementPdfSegment(2,3,25.0,50.0)
        assertEquals(SketchPoint(0.0,0.0),PavementPdfLayout.point(0.0,25.0,seg,20.0))
        assertEquals(SketchPoint(40.0,60.0),PavementPdfLayout.point(3.0,27.0,seg,20.0))
    }
    @Test fun sectionChainageSupportsBothDirectionsAndDoesNotInventAnInconsistentProjection() {
        val s=visualSample(60.0).copy(prInicial="01+000",prFinal="01+060")
        val seg=PavementPdfSegment(2,3,25.0,50.0)
        assertEquals("01+025 → 01+050",PavementPdfLayout.chainages(s,seg))
        assertEquals("01+035 → 01+010",PavementPdfLayout.chainages(s.copy(prInicial="01+060",prFinal="01+000"),seg))
        assertNull(PavementPdfLayout.chainages(s.copy(prFinal="01+030"),seg))
    }
    @Test fun crossSectionDamageKeepsSameIdentityAndJointSelectionKeepsGlobalMeasurement() {
        val s=visualSample(60.0);val d=visualDamage(29,8,.0,24.0,1.0,26.0,9).copy(slabs="9,11")
        val segs=PavementPdfLayout.segments(7.0,60.0,slabLength=5.0)
        assertEquals(2,segs.count {it.contains(s,d)});assertEquals(8,d.correlativo);assertEquals("TEST_DAMAGE_8",d.id)
        val g=d.copy(code=26,geometry="GLOBAL",slabs="",x1=null,x2=null,y1=null,y2=null)
        val marks=setOf(PavementJointAnnotations.Mark("V",0,1),PavementJointAnnotations.Mark("H",1,0),PavementJointAnnotations.Mark("N",1,1))
        val preview=s.copy(sicDataJson=PavementJointAnnotations.update(s,g,marks));assertEquals(marks,PavementJointAnnotations.marks(preview,g.id));assertNull(s.sicDataJson);assertEquals(0.0,PavementRules.quantity(g),0.0)
    }
}
