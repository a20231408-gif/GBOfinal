package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import android.graphics.*
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.pavement.*
import com.tuempresa.inventariovial.pavement.ui.PavementDrawing
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream
import kotlin.math.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PavementDurabilityFanTest {
    @Test fun rectangularSlabsHaveExactIndependentScalingAndFourReflections() {
        for((w,h)in listOf(3.5 to 5.0,7.0 to 3.0,2.0 to 8.0)) {
            val s=axisSample().copy(anchoM=w,largoM=h)
            for(right in listOf(false,true))for(top in listOf(false,true)) {
                fun x(v:Double)=if(right)w-v else v
                fun y(v:Double)=if(top)h-v else v
                val d=visualDamage(24,1,x(.02*w),y(.02*h),x(.72*w),y(.42*h),1)
                val p=PavementDurabilityGeometry.project(s,d);assertNull(p.warning)
                val all=p.traces.flatMap{it.points}
                assertEquals(.70*w,all.maxOf{it.x}-all.minOf{it.x},1e-8)
                assertEquals(.40*h,all.maxOf{it.y}-all.minOf{it.y},1e-8)
                assertTrue(all.all{it.x in 0.0..w && it.y in 0.0..h})
                assertEquals(p,PavementDurabilityGeometry.project(s,d.copy(x1=d.x2,x2=d.x1,y1=d.y2,y2=d.y1)))
            }
        }
    }
    @Test fun explicitSlabAtSharedBoundaryDoesNotLeakOrSilentlyChooseAnotherSlab() {
        val s=axisSample().copy(anchoM=12.0,largoM=12.0,slabRows=2,slabColumns=2)
        val d=visualDamage(24,1,6.01,6.01,10.01,7.01,4)
        val p=PavementDurabilityGeometry.project(s,d)
        assertNull(p.warning);assertEquals(SketchPoint(6.0,6.0),p.corner)
        assertTrue(p.traces.flatMap{it.points}.all{it.x in 6.0..12.0 && it.y in 6.0..12.0})
        assertNotNull(PavementDurabilityGeometry.project(s,d.copy(slabs="1")).warning)
        assertNotNull(PavementDurabilityGeometry.project(s,d.copy(slabs="1,4")).warning)
        assertEquals(SketchPoint(6.0,6.0),PavementDurabilityGeometry.project(s,d.copy(x1=6.0,y1=6.0)).corner)
    }
    @Test fun fanHasIrregularInclinedPathsConnectedBranchesAndDenserCornerSpacing() {
        val s=axisSample();val d=visualDamage(24,1,.1,.1,4.1,2.1,1)
        val p=PavementDurabilityGeometry.project(s,d)
        val endpoints=p.traces.take(6).map{it.points.last().x}
        val gaps=endpoints.zipWithNext().map{(a,b)->b-a}
        assertTrue(gaps.zipWithNext().all{(a,b)->a<b})
        for(line in p.traces.take(6)) {
            assertTrue(line.points.first().y>line.points.last().y)
            assertTrue(line.points.last().x>line.points.first().x)
            assertTrue(line.points.drop(1).dropLast(1).any{abs(it.x/4+it.y/2-line.points.first().y/2)>1e-5})
        }
        assertNotEquals(PavementCornerGeometry.resolve(s,d.copy(code=22,x1=.1,y1=1.5,x2=3.0,y2=.1)).crack(d.id),p.traces.first().points)
    }
    @Test fun drawingDoesNotMutateCoordinatesPciExcelNativeJsonOrOtherAnnotations() {
        val c=ApplicationProvider.getApplicationContext<Context>();val s=axisSample().copy(sicDataJson="{\"UNKNOWN\":\"preservar\"}")
        val d=visualDamage(24,1,.1,.1,4.1,1.1,1);val saved=d.copy();val savedSample=s.copy()
        val calc=PavementCalculator(c);val before=calc.calculate(s,listOf(d))
        fun xlsx():Map<String,String> {
            val bytes=ByteArrayOutputStream().also{PavementExcel.write(it,listOf(PavementExportSample(s,listOf(d),before)))}.toByteArray()
            return buildMap{ZipInputStream(bytes.inputStream()).use{z->while(true){val e=z.nextEntry?:break;put(e.name,z.readBytes().toString(Charsets.UTF_8))}}}
        }
        val excel=xlsx();val json=PavementDriveUploader.nativeJson(s,listOf(d),before,emptyMap()).toString()
        repeat(5){PavementRigidVisualGeometry.traces(s,d)}
        assertEquals(saved,d);assertEquals(savedSample,s);assertEquals(before,calc.calculate(s,listOf(d)))
        assertEquals(excel,xlsx());assertEquals(json,PavementDriveUploader.nativeJson(s,listOf(d),before,emptyMap()).toString())
    }
    @Test fun allCornersAndProportionsMatchActualCanvasAndQuarterTurnPdfPixels() {
        val s=axisSample().copy(largoM=12.5)
        for((b,h)in listOf(4.0 to 1.0,1.0 to 4.0,3.0 to 3.0))for(right in listOf(false,true))for(top in listOf(false,true)) {
            fun x(v:Double)=if(right)6-v else v
            fun y(v:Double)=if(top)12.5-v else v
            val d=visualDamage(24,1,x(.1),y(.1),x(.1+b),y(.1+h),1)
            assertNull(PavementDurabilityGeometry.project(s,d).warning)
            val p=PavementRigidPdfScheme.plans(s,listOf(d)).single();val scale=p.region.scale.toFloat()
            val normal=Bitmap.createBitmap(ceil(s.anchoM*scale).toInt(),ceil(s.largoM*scale).toInt(),Bitmap.Config.ARGB_8888)
            PavementDrawing.draw(Canvas(normal),s,listOf(d),scale,labels=false,slabLabels=false)
            val pdf=Bitmap.createBitmap(842,595,Bitmap.Config.ARGB_8888)
            PavementRigidPdfScheme.draw(Canvas(pdf),s,p,56f,105f)
            val color=PavementSeverity.color(d.severity);var total=0;var matched=0
            for(x in 0 until normal.width)for(y in 0 until normal.height)if(normal.getPixel(x,y)==color){
                total++;val px=(56+s.largoM*scale-y-1).roundToInt();val py=135+x
                if((-2..2).any{dx->(-2..2).any{dy->val xx=px+dx;val yy=py+dy;xx in 0 until pdf.width && yy in 0 until pdf.height && pdf.getPixel(xx,yy)==color}})matched++
            }
            assertTrue(total>100);assertTrue("corner/proportion PDF pixels: $matched/$total",matched.toDouble()/total>.85)
            normal.recycle();pdf.recycle()
        }
    }
    @Test fun longSampleUsesSharedFanForSegmentClippingWithoutRenumbering() {
        val s=axisSample().copy(largoM=60.0,slabRows=6)
        val d=visualDamage(24,71,.1,24.9,2.1,29.9,3)
        val p=PavementDurabilityGeometry.project(s,d);assertEquals(SketchPoint(0.0,30.0),p.corner)
        assertFalse(PavementPdfSegment(1,3,0.0,25.0).contains(s,d))
        assertTrue(PavementPdfSegment(2,3,25.0,50.0).contains(s,d))
        for(r in PavementRigidPdfLayout.regions(s))for(point in PavementRigidPdfLayout.visiblePoints(s,d,r)) {
            assertTrue(point.x in -1e-7..(r.width+1e-7) && point.y in -1e-7..(r.height+1e-7))
        }
        assertEquals(71,d.correlativo);assertEquals("TEST_DAMAGE_71",d.id)
    }
}
