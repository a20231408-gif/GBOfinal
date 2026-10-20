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
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.*
import java.util.UUID
import java.util.zip.ZipInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=Application::class,shadows=[SyntheticPdf146::class])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PavementRigid147Test {
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    private fun sample()=PavementSampleEntity("TEST_AUTO_147","T","PE-22A",1,"Derecho","CD","00+000","00+020",10.0,20.0,"RIGID","2026-10-15","u","FICTICIO",slabRows=2,slabColumns=2)
    private fun damage(code:Int=26)=PavementDamageEntity("damage-$code","TEST_AUTO_147",1,"RIGID",code,"FICTICIO","M",if(code==26)"GLOBAL" else "SLABS",null,null,null,null,null,null,null,null,null,null,"TEXTO LITERAL",slabs=if(code==26)"" else "1")
    private fun measured()=damage(22).copy(x1=.5,y1=2.0,x2=2.0,y2=.5,widthM=1.5,lengthM=1.5)
    private fun fail(block:()->Unit){assertTrue(runCatching(block).isFailure)}
    @Test fun historicalCornerWithoutCoordinatesNeverInventsACrack() {
        val s=sample();val old=damage(22)
        val plain=Bitmap.createBitmap(220,420,Bitmap.Config.ARGB_8888)
        val historical=Bitmap.createBitmap(220,420,Bitmap.Config.ARGB_8888)
        try {
            PavementDrawing.draw(Canvas(plain),s,emptyList(),20f,labels=false)
            PavementDrawing.draw(Canvas(historical),s,listOf(old),20f,labels=false)
            for(x in 12..85)for(y in 220..380)assertEquals("Unknown corner must not create an interior crack",plain.getPixel(x,y),historical.getPixel(x,y))
            assertEquals(1.0,PavementRules.quantity(old),0.0);assertFalse(PavementSlabGeometry.localized(old))
        }finally {plain.recycle();historical.recycle()}
    }
    @Test fun allCornersBothTouchOrdersSquareAndRectangular() {
        for((w,h) in listOf(10.0 to 10.0,6.0 to 12.0,12.0 to 6.0))for(right in listOf(false,true))for(top in listOf(false,true)) {
            val s=sample().copy(anchoM=w,largoM=h,slabRows=1,slabColumns=1)
            val u=minOf(w,h)
            fun p(x:Double,y:Double)=SketchPoint(if(right)w-x else x,if(top)h-y else y)
            val a=p(u*.1,u*.2);val b=p(u*.3,u*.1)
            val expected=listOf(p(0.0,u*.2),p(u*.3,0.0))
            for(reverse in listOf(false,true)) {
                val c=if(reverse)PavementCornerGeometry.resolve(s,b,a)else PavementCornerGeometry.resolve(s,a,b)
                assertEquals(if(reverse)expected.reversed() else expected,listOf(c.a,c.b))
                val crack=c.crack("same-id");assertEquals(c.a,crack.first());assertEquals(c.b,crack.last())
                assertTrue(crack.all {it.x in 0.0..w && it.y in 0.0..h})
            }
        }
    }
    @Test fun mandatoryTenMetreExampleAndInvalidEdges() {
        val s=sample().copy(anchoM=10.0,largoM=10.0,slabRows=1,slabColumns=1)
        val c=PavementCornerGeometry.resolve(s,SketchPoint(1.0,2.0),SketchPoint(3.0,1.0))
        assertEquals(SketchPoint(0.0,2.0),c.a);assertEquals(SketchPoint(3.0,0.0),c.b)
        for((a,b) in listOf(SketchPoint(1.0,2.0) to SketchPoint(1.0,4.0),SketchPoint(1.0,3.0) to SketchPoint(9.0,3.0),
            SketchPoint(5.0,5.0) to SketchPoint(3.0,1.0),SketchPoint(1.0,2.0) to SketchPoint(1.001,2.001)))fail {PavementCornerGeometry.resolve(s,a,b)}
    }
    @Test fun severalSlabsBoundaryAmbiguityAndViewportRoundTrip() {
        val s=sample();val a=SketchPoint(5.5,12.0);val b=SketchPoint(7.0,10.5)
        val expected=PavementCornerGeometry.resolve(s,a,b)
        assertEquals(4,expected.slab);assertEquals(SketchPoint(5.0,12.0),expected.a);assertEquals(SketchPoint(7.0,10.0),expected.b)
        fail {PavementCornerGeometry.resolve(s,SketchPoint(4.5,2.0),SketchPoint(5.5,1.0))}
        fail {PavementCornerGeometry.resolve(s,SketchPoint(5.01,2.0),SketchPoint(7.0,.5),boundaryTolerance=.1)}
        for(zoom in listOf(1f,2f,8f)) {
            val v=PavementViewport.fit(s.anchoM,s.largoM,800f,600f,64f,zoom,-120f,75f)
            fun round(p:SketchPoint)=SketchPoint(v.metresX(v.originX+(p.x*v.scale).toFloat()),v.metresY(v.canvasY(p.y)))
            val c=PavementCornerGeometry.resolve(s,round(a),round(b))
            assertEquals(expected.a.x,c.a.x,1e-5);assertEquals(expected.b.y,c.b.y,1e-5)
        }
    }
    @Test fun jointHitsSegmentsAndNodesWithNoInventedLongSections() {
        val s=sample()
        assertEquals(PavementJointAnnotations.Mark("V",0,1),PavementJointAnnotations.hit(s,5.05,2.0,.25))
        assertEquals(PavementJointAnnotations.Mark("H",1,0),PavementJointAnnotations.hit(s,2.0,10.05,.25))
        assertEquals(PavementJointAnnotations.Mark("N",1,1),PavementJointAnnotations.hit(s,5.05,10.05,.25))
        assertNull(PavementJointAnnotations.hit(s,2.5,5.0,.25));assertNull(PavementJointAnnotations.hit(s,0.0,2.0,.25))
        assertEquals(SketchPoint(5.0,0.0) to SketchPoint(5.0,10.0),PavementJointAnnotations.endpoints(s,PavementJointAnnotations.Mark("V",0,1)))
        val v=PavementViewport.fit(10.0,20.0,800f,600f,64f,4f,-100f,80f)
        val x=v.originX+(5.0*v.scale).toFloat()+2f;val y=v.canvasY(2.0)
        assertEquals(PavementJointAnnotations.Mark("V",0,1),PavementJointAnnotations.hit(s,v.metresX(x),v.metresY(y),18.0/v.scale))
    }
    @Test fun annotationVersionGridReviewAndSicSupplementPreservation() {
        val s=sample();val d=damage();assertTrue(PavementJointAnnotations.marks(s,d.id).isEmpty())
        val marks=setOf(PavementJointAnnotations.Mark("V",0,1),PavementJointAnnotations.Mark("H",1,0),PavementJointAnnotations.Mark("N",1,1))
        val saved=s.copy(sicDataJson=PavementJointAnnotations.update(s,d,marks))
        assertEquals(marks,PavementJointAnnotations.marks(saved,d.id));assertFalse(PavementJointAnnotations.needsReview(saved,d.id))
        val changed=saved.copy(anchoM=11.0);assertTrue(PavementJointAnnotations.needsReview(changed,d.id));assertTrue(PavementJointAnnotations.marks(changed,d.id).isEmpty())
        val withSic=saved.copy(sicDataJson=PavementSic.update(saved,PavementSicFormat.SIC15,d.id,mapOf("faja" to "1")))
        assertEquals(marks,PavementJointAnnotations.marks(withSic,d.id));assertEquals("1",PavementSic.value(withSic,PavementSicFormat.SIC15,d.id,"faja"))
        fail {PavementJointAnnotations.update(s,measured(),marks)}
    }
    @Test fun diskCloseReopenKeepsRawCornerGlobalMarksUuidAndOutboxRevision()=runBlocking {
        val name="p147.db"
        fun db()=Room.databaseBuilder(context,InventoryDatabase::class.java,name).build()
        val sessions=AuthSessionManager();sessions.login(UserCacheEntity("u","u","FICTICIO","OPERATOR",true,"",0,0,0))
        var d=db();val s=sample();d.pavementDao().saveSample(s)
        var repo=PavementRepository(d,sessions);repo.saveDamage(damage());repo.saveDamage(measured())
        val mark=PavementJointAnnotations.Mark("V",0,1)
        repo.toggleJointMark(s.id,damage().id,mark);repo.toggleJointMark(s.id,damage().id,mark)
        assertTrue(PavementJointAnnotations.marks(d.pavementDao().sample(s.id)!!,damage().id).isEmpty())
        repo.toggleJointMark(s.id,damage().id,mark)
        assertTrue(runCatching {repo.saveDamage(damage().copy(id="second-global"))}.isFailure)
        assertTrue(runCatching {repo.saveSample(d.pavementDao().sample(s.id)!!.copy(slabRows=3))}.isFailure)
        repo.closeSample(s.id);val queue=d.automaticSyncDao().get("PAVEMENT/${s.id}")!!
        d.close();d=db();repo=PavementRepository(d,sessions)
        val reopened=d.pavementDao().sample(s.id)!!
        assertEquals("CLOSED",reopened.status);assertEquals(setOf(mark),PavementJointAnnotations.marks(reopened,damage().id))
        assertEquals(measured().x1,d.pavementDao().damage(measured().id)!!.x1);assertEquals(queue,d.automaticSyncDao().get(queue.key))
        repo.reopenSample(s.id);repo.saveJointMarks(s.id,damage().id,emptySet());repo.closeSample(s.id)
        assertEquals(2,d.pavementDao().damages(s.id).size);assertTrue(d.automaticSyncDao().get(queue.key)!!.revision>queue.revision)
        d.close()
    }
    private fun unzip(bytes:ByteArray)=buildMap<String,String> {ZipInputStream(ByteArrayInputStream(bytes)).use {z->while(true){val e=z.nextEntry?:break;put(e.name,z.readBytes().toString(Charsets.UTF_8))}}}
    @Test fun pciAndExcelUnchangedWhileNativeJsonCarriesOpaqueVisualMetadata() {
        val s=sample();val ds=listOf(damage(),measured());val calculator=PavementCalculator(context)
        val saved=s.copy(sicDataJson=PavementJointAnnotations.update(s,damage(),setOf(PavementJointAnnotations.Mark("N",1,1))))
        val result=calculator.calculate(s,ds)
        assertEquals(result,calculator.calculate(saved,ds));assertEquals(1.0,PavementRules.quantity(measured()),0.0);assertEquals(0.0,PavementRules.quantity(damage()),0.0)
        fun excel(s:PavementSampleEntity)=ByteArrayOutputStream().also {PavementExcel.write(it,listOf(PavementExportSample(s,ds,result)))}.toByteArray()
        assertEquals(unzip(excel(s)),unzip(excel(saved)))
        val native=PavementDriveUploader.nativeJson(saved,ds,result,emptyMap())
        val envelope=native.getJSONObject("sample").getJSONObject("sicData")
        assertTrue(envelope.has("_gboJointAnnotations"));assertEquals(2,native.getJSONArray("damages").length())
    }
    @Test fun sharedDrawingAndPdfWithAndWithoutMarksAndMultipleSegments() {
        val s=sample().copy(largoM=60.0,slabRows=6);val global=damage()
        val saved=s.copy(sicDataJson=PavementJointAnnotations.update(s,global,setOf(PavementJointAnnotations.Mark("H",4,0),PavementJointAnnotations.Mark("N",4,1))))
        val segments=PavementPdfLayout.segments(saved.anchoM,saved.largoM)
        assertTrue(segments.size>1);assertTrue(segments.any {it.start>=20 && it.contains(saved,global)})
        val target=File("D:/Downloads/GBO_PRUEBAS_8B/tmp/rigidos-1.4.7-20261015/visual").apply {mkdirs()}
        for((name,head) in listOf("sin-marcas" to s,"con-marcas" to saved)) {
            val bitmap=Bitmap.createBitmap(420,1260,Bitmap.Config.ARGB_8888);val canvas=Canvas(bitmap)
            PavementDrawing.draw(canvas,head,listOf(global),20f,labels=false)
            File(target,"croquis-$name.png").outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
            val out=ByteArrayOutputStream();PavementPdf.write(out,PavementExportSample(head,listOf(global),PavementCalculator(context).calculate(head,listOf(global))),emptyMap())
            assertTrue(out.toByteArray().toString(Charsets.ISO_8859_1).startsWith("%PDF"));File(target,"reporte-$name-TEST_WRAPPER.pdf").writeBytes(out.toByteArray())
        }
        val corner=measured();val bitmap=Bitmap.createBitmap(220,420,Bitmap.Config.ARGB_8888)
        PavementDrawing.draw(Canvas(bitmap),sample(),listOf(corner),20f)
        File(target,"esquina.png").outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
    }
}
