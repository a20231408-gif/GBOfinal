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
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=Application::class,shadows=[SyntheticPdf146::class])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PavementBoundaryExamples20261019Test {
    @Test fun realDrawingSharedJointsAndConfirmedShouldersKeepExistingPdfLayout() {
        val out=File(requireNotNull(System.getProperty("gbo.visual.output"))).apply{mkdirs()}
        val s=axisSample().copy(anchoM=18.0,largoM=18.0,slabRows=3,slabColumns=3,prFinal="00+018")
        val specs=listOf(
            "25: junta izquierda" to visualDamage(25,1,6.1,7.0,6.1,10.0,5),
            "25: junta derecha" to visualDamage(25,2,11.9,7.0,11.9,10.0,5),
            "25: junta inferior" to visualDamage(25,3,7.0,6.1,10.0,6.1,5),
            "25: junta superior" to visualDamage(25,4,7.0,11.9,10.0,11.9,5),
            "27: berma X=0 confirmada" to visualDamage(27,5,.1,7.0,.1,10.0,4),
            "27: berma X=18 confirmada" to visualDamage(27,6,17.9,7.0,17.9,10.0,6))
        val bitmap=Bitmap.createBitmap(1500,1100,Bitmap.Config.ARGB_8888);val canvas=Canvas(bitmap);canvas.drawColor(Color.WHITE)
        val text=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.BLACK;textSize=20f}
        var ps=s
        for((i,spec)in specs.withIndex()) {
            val(title,d)=spec;val choice=if(d.code==27)if(i==4)"EXTERIOR_X0"else"EXTERIOR_XW"else PavementBoundaryGeometry.resolve(s,d).joint.key
            ps=ps.copy(sicDataJson=PavementBoundaryGeometry.update(ps,d,choice))
            val x=i%3*500f;val y=i/3*500f
            canvas.drawText(title,x+14,y+28,text);canvas.drawText("X derecha / Y arriba; metros originales",x+14,y+55,text)
            canvas.save();canvas.translate(x+35,y+78);PavementDrawing.draw(canvas,ps,listOf(d),22f,labels=false,slabLabels=true);canvas.restore()
            canvas.drawText("Segmento fino + marca roja perpendicular",x+14,y+495,text)
            assertEquals(1.0,PavementRules.quantity(d),0.0)
        }
        canvas.drawText("Datos ficticios: dibujante real Android bajo Robolectric. No son capturas de tablet.",14f,1040f,text)
        canvas.drawText("Ejemplos segun especificacion escrita: no se recibieron las imagenes de referencia. PDF raster de ensayo.",14f,1075f,text)
        File(out,"25-27-juntas-y-bermas-Canvas.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
        ps=ps.copy(largoM=60.0,slabRows=10,prFinal="00+060",sicDataJson=null)
        val damages=specs.map{it.second}.toMutableList()
        damages+=visualDamage(25,7,7.0,29.9,10.0,29.9,14)
        damages+=visualDamage(27,8,.1,28.0,.1,32.0,13).copy(slabs="13,16")
        for(d in damages)ps=ps.copy(sicDataJson=PavementBoundaryGeometry.update(ps,d,if(d.code==27)if(d.id=="TEST_DAMAGE_6")"EXTERIOR_XW"else"EXTERIOR_X0"else PavementBoundaryGeometry.resolve(ps,d).joint.key))
        val c=ApplicationProvider.getApplicationContext<Context>();val data=PavementExportSample(ps,damages,PavementCalculator(c).calculate(ps,damages))
        File(out,"25-27-PDF-horizontal-SIMULADO.pdf").outputStream().use{PavementPdf.write(it,data,emptyMap())}
        assertEquals(8,damages.size)
    }
}
