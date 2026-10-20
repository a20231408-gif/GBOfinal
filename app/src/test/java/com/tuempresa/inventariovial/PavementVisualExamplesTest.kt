package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import android.graphics.*
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.pavement.*
import com.tuempresa.inventariovial.pavement.ui.PavementDrawing
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
import java.io.File

internal fun visualSample(length:Double=30.0)=PavementSampleEntity("TEST_VISUAL_20261019","TEST FICTICIO","TEST",1,"D","CD","00+000",PavementChainage.format(length),7.0,length,"RIGID","2026-10-19","test","FICTICIO",slabRows=(length/5).toInt(),slabColumns=2)
internal fun visualDamage(code:Int,id:Int,x:Double,y:Double,x2:Double,y2:Double,slab:Int)=PavementDamageEntity("TEST_DAMAGE_$id","TEST_VISUAL_20261019",id,"RIGID",code,PavementCatalog.forSurface(PavementSurface.RIGID).first {it.code==code}.name,"M","SLABS",x,y,x2,y2,null,kotlin.math.abs(x2-x),kotlin.math.abs(y2-y),null,null,null,"FICTICIO",slabs=slab.toString())

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=Application::class,shadows=[SyntheticPdf146::class])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PavementVisualExamplesTest {
    @Test fun actualSharedRendererShortLongAndMultiSectionExamples() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val output=File(System.getProperty("gbo.visual.output"));output.mkdirs()
        val phase=System.getProperty("gbo.visual.phase","after")
        for((name,length) in listOf("corto" to 10.0,"largo" to 60.0,"multiseccion" to 100.0)) {
            var s=visualSample(length)
            val ds=mutableListOf(
                visualDamage(22,1,.25,1.2,1.1,.2,1),
                visualDamage(24,2,3.55,.5,4.1,4.4,2),
                visualDamage(24,3,.4,5.05,2.8,5.6,3),
                visualDamage(29,4,3.5,6.0,6.6,8.5,4),
                visualDamage(30,5,1.2,2.0,1.8,2.6,1)
            )
            if(length>=60) {
                ds+=visualDamage(29,6,.0,25.0,1.2,29.0,11)
                ds+=visualDamage(24,7,3.55,55.5,4.2,59.0,24)
                ds+=visualDamage(22,8,6.8,51.0,5.8,50.2,22)
            }
            val g=visualDamage(26,9,0.0,0.0,0.0,0.0,1).copy(geometry="GLOBAL",x1=null,y1=null,x2=null,y2=null,slabs="")
            ds+=g
            s=s.copy(sicDataJson=PavementJointAnnotations.update(s,g,setOf(PavementJointAnnotations.Mark("V",0,1),PavementJointAnnotations.Mark("H",1,0),PavementJointAnnotations.Mark("N",1,1))))
            val data=PavementExportSample(s,ds,PavementCalculator(context).calculate(s,ds))
            File(output,"$phase-$name-SIMULADO.pdf").outputStream().use {PavementPdf.write(it,data,emptyMap())}
            if(name=="corto") {
                val bitmap=Bitmap.createBitmap(420,600,Bitmap.Config.ARGB_8888)
                PavementDrawing.draw(Canvas(bitmap),s,ds,55f)
                File(output,"$phase-croquis-Canvas.png").outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
            }
            assertTrue(File(output,"$phase-$name-SIMULADO.pdf").length()>1000)
        }
    }
}
