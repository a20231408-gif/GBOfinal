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

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PavementBoundary20261019Test {
    private fun sample()=axisSample().copy(anchoM=18.0,largoM=18.0,slabRows=3,slabColumns=3,prFinal="00+018")
    @Test fun stepAllFourSharedSidesArePerpendicularCenteredAndPointOrderIndependent() {
        val s=sample()
        val cases=listOf("V:1:1" to listOf(6.1,7.0,6.2,10.0),"V:1:2" to listOf(11.8,7.0,11.9,10.0),
            "H:1:1" to listOf(7.0,6.1,10.0,6.2),"H:2:1" to listOf(7.0,11.8,10.0,11.9))
        for((key,v)in cases) {
            val d=visualDamage(25,1,v[0],v[1],v[2],v[3],5);val before=d.copy()
            val p=PavementBoundaryGeometry.resolve(s,d)
            assertEquals(key,p.joint.key);assertEquals(p,PavementBoundaryGeometry.resolve(s,d.copy(x1=d.x2,x2=d.x1,y1=d.y2,y2=d.y1)))
            assertEquals(0.0,(p.b.x-p.a.x)*(p.markerB.x-p.markerA.x)+(p.b.y-p.a.y)*(p.markerB.y-p.markerA.y),1e-8)
            assertEquals((p.a.x+p.b.x)/2,(p.markerA.x+p.markerB.x)/2,1e-8)
            assertEquals((p.a.y+p.b.y)/2,(p.markerA.y+p.markerB.y)/2,1e-8)
            assertEquals(3.0,hypot(p.b.x-p.a.x,p.b.y-p.a.y),1e-8)
            assertEquals(1.0,PavementRules.quantity(d),0.0);assertEquals(before,d)
            PavementRules.validate(s,d,emptyList())
        }
    }
    @Test fun intersectionRequiresChoiceAndOuterEdgeWithoutNeighborIsRejected() {
        val s=sample();val d=visualDamage(25,1,6.1,6.1,6.2,6.2,5)
        assertEquals(2,PavementBoundaryGeometry.options(s,d).size)
        assertTrue(runCatching{PavementBoundaryGeometry.resolve(s,d)}.isFailure)
        assertEquals("V:1:1",PavementBoundaryGeometry.resolve(s,d,"V:1:1").joint.key)
        assertEquals("H:1:1",PavementBoundaryGeometry.resolve(s,d,"H:1:1").joint.key)
        assertTrue(runCatching{PavementBoundaryGeometry.resolve(s,visualDamage(25,2,.1,1.0,.1,4.0,1))}.isFailure)
        assertTrue(runCatching{PavementBoundaryGeometry.resolve(s,visualDamage(25,3,6.1,5.0,6.1,8.0,5))}.isFailure)
    }
    @Test fun shoulderNeedsExplicitExteriorChoiceForEitherLaneAndEitherChainageDirection() {
        for(lane in listOf("Derecho","Izquierdo"))for(decreasing in listOf(false,true)) {
            val s=sample().copy(carril=lane,prInicial=if(decreasing)"00+018"else"00+000",prFinal=if(decreasing)"00+000"else"00+018")
            for(right in listOf(false,true)) {
                val x=if(right)17.9 else .1;val slab=if(right)3 else 1
                val d=visualDamage(27,1,x,1.0,x,4.0,slab);val key=if(right)"EXTERIOR_XW"else"EXTERIOR_X0"
                assertTrue(runCatching{PavementBoundaryGeometry.resolve(s,d)}.isFailure)
                val ps=s.copy(sicDataJson=PavementBoundaryGeometry.update(s,d,key));val p=PavementBoundaryGeometry.resolve(ps,d)
                assertEquals(if(right)18.0 else 0.0,p.a.x,0.0);assertEquals(p.a.x,p.b.x,0.0)
                assertEquals(p,PavementBoundaryGeometry.resolve(ps,d.copy(x1=d.x2,x2=d.x1,y1=d.y2,y2=d.y1)))
                assertTrue(runCatching{PavementBoundaryGeometry.resolve(ps,d,if(right)"EXTERIOR_X0"else"EXTERIOR_XW")}.isFailure)
                assertTrue(p.traces().flatMap{it.points}.all{it.x in 0.0..18.0 && it.y in 0.0..18.0})
                PavementRules.validate(ps,d,emptyList())
            }
        }
        val s=sample();val internal=visualDamage(27,1,6.1,7.0,6.1,10.0,5)
        assertTrue(PavementBoundaryGeometry.options(s,internal).isEmpty())
        assertTrue(runCatching{PavementBoundaryGeometry.resolve(s,internal,"EXTERIOR_X0")}.isFailure)
        assertTrue(runCatching{PavementBoundaryGeometry.resolve(s,visualDamage(27,1,1.0,.1,4.0,.1,1),"EXTERIOR_X0")}.isFailure)
    }
    @Test fun atomicChoicePersistsWithSameUuidPhotosQuantityAndOtherVisualMetadata()=runBlocking {
        val c=ApplicationProvider.getApplicationContext<Context>();val name="TEST_BOUNDARY_${UUID.randomUUID()}.db"
        fun open()=Room.databaseBuilder(c,InventoryDatabase::class.java,name).build()
        val s=sample().copy(sicDataJson="{\"UNKNOWN\":{\"literal\":\"=1+2\"}}")
        val d=visualDamage(27,1,.1,1.0,.1,4.0,1)
        val sessions=AuthSessionManager().apply{login(UserCacheEntity("test","test","FICTICIO","OPERATOR",true,"",1,1,1))}
        open().use{db->
            db.pavementDao().saveSample(s);val repo=PavementRepository(db,sessions)
            assertTrue(runCatching{repo.saveDamageWithBoundary(d,"EXTERIOR_XW")}.isFailure)
            assertNull(db.pavementDao().damage(d.id));assertEquals(s,db.pavementDao().sample(s.id))
            repo.saveDamageWithBoundary(d,"EXTERIOR_X0")
            repo.savePhoto(PavementDamagePhotoEntity("TEST_PHOTO",d.id,"FICTICIO.jpg"),s.id)
        }
        open().use{db->
            val ps=db.pavementDao().sample(s.id)!!;val saved=db.pavementDao().damage(d.id)!!
            assertEquals(20,db.openHelper.writableDatabase.version);assertEquals(1,db.pavementDao().damages(s.id).size)
            assertEquals(d.id,saved.id);assertEquals(d.x1,saved.x1);assertEquals(d.y2,saved.y2);assertEquals(1.0,PavementRules.quantity(saved),0.0)
            assertEquals("EXTERIOR_X0",PavementBoundaryGeometry.choice(ps,saved));assertEquals(1,db.pavementDao().photos(d.id).size)
            assertEquals("=1+2",JSONObject(ps.sicDataJson!!).getJSONObject("UNKNOWN").getString("literal"))
            assertTrue(runCatching{PavementBoundaryGeometry.resolve(ps.copy(carril="Izquierdo"),saved)}.isFailure)
            assertTrue(runCatching{PavementBoundaryGeometry.resolve(ps,saved.copy(y2=5.0))}.isFailure)
        }
    }
    @Test fun canvasAndPdfUseSameRedSymbolForHorizontalAndVerticalJointsAndShoulders() {
        var s=axisSample().copy(largoM=12.5,slabColumns=2,slabRows=2)
        val ds=listOf(visualDamage(25,1,2.9,1.0,2.9,4.0,1),visualDamage(25,2,.5,6.15,2.0,6.15,1),visualDamage(27,3,.1,1.0,.1,4.0,1))
        s=s.copy(sicDataJson=PavementBoundaryGeometry.update(s,ds.last(),"EXTERIOR_X0"))
        for(d in ds) {
            val plan=PavementRigidPdfScheme.plans(s,listOf(d)).single();val scale=plan.region.scale.toFloat()
            val normal=Bitmap.createBitmap(ceil(s.anchoM*scale).toInt(),ceil(s.largoM*scale).toInt(),Bitmap.Config.ARGB_8888)
            PavementDrawing.draw(Canvas(normal),s,listOf(d),scale,labels=false,slabLabels=false)
            val pdf=Bitmap.createBitmap(842,595,Bitmap.Config.ARGB_8888);PavementRigidPdfScheme.draw(Canvas(pdf),s,plan,56f,105f)
            var total=0;var matched=0
            for(x in 0 until normal.width)for(y in 0 until normal.height)if(normal.getPixel(x,y)==Color.RED) {
                total++;val px=(56+s.largoM*scale-y-1).roundToInt();val py=135+x
                if((-2..2).any{dx->(-2..2).any{dy->val xx=px+dx;val yy=py+dy;xx in 0 until pdf.width && yy in 0 until pdf.height && pdf.getPixel(xx,yy)==Color.RED}})matched++
            }
            println("TEST_BOUNDARY code=${d.code} item=${d.correlativo} redPixels=$total matched=$matched")
            assertTrue("Visible perpendicular symbol: $total red pixels",total>8)
            assertTrue("Canvas/PDF red symbol $matched/$total",matched.toDouble()/total>.85)
            normal.recycle();pdf.recycle()
        }
    }
    @Test fun confirmationDoesNotChangePciExcelFieldsOrNativeDamageContractAndSegmentKeepsIdentity() {
        val c=ApplicationProvider.getApplicationContext<Context>();val s=sample().copy(largoM=60.0,slabRows=10)
        val d=visualDamage(25,17,7.0,29.9,10.0,29.9,14)
        val ps=s.copy(sicDataJson=PavementBoundaryGeometry.update(s,d,"H:5:1"))
        val calculator=PavementCalculator(c);val result=calculator.calculate(s,listOf(d))
        assertEquals(result,calculator.calculate(ps,listOf(d)))
        fun excel(sample:PavementSampleEntity):Map<String,String> {
            val bytes=java.io.ByteArrayOutputStream().also{PavementExcel.write(it,listOf(PavementExportSample(sample,listOf(d),result)))}.toByteArray()
            return buildMap{java.util.zip.ZipInputStream(bytes.inputStream()).use{z->while(true){val e=z.nextEntry?:break;put(e.name,z.readBytes().toString(Charsets.UTF_8))}}}
        }
        assertEquals(excel(s),excel(ps))
        val a=PavementDriveUploader.nativeJson(s,listOf(d),result,emptyMap())
        val b=PavementDriveUploader.nativeJson(ps,listOf(d),result,emptyMap())
        assertEquals(a.getJSONArray("damages").toString(),b.getJSONArray("damages").toString())
        assertTrue(PavementPdfSegment(1,2,0.0,30.0).contains(ps,d));assertTrue(PavementPdfSegment(2,2,30.0,60.0).contains(ps,d))
        assertEquals(17,d.correlativo);assertEquals(1.0,PavementRules.quantity(d),0.0)
    }
}
