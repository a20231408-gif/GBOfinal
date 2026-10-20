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

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=Application::class,shadows=[SyntheticPdf146::class])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PavementAxisExamplesTest {
    @Test fun realCanvasAndUnmodifiedPdfLayoutExamplesOfTheDefinitiveRule() {
        val out=File(requireNotNull(System.getProperty("gbo.visual.output"))).apply{mkdirs()}
        val specs=listOf(
            Triple("24: b=4 h=1 -> HORIZONTAL",24,listOf(1.0,2.0,5.0,3.0)),
            Triple("24: b=1 h=4 -> VERTICAL",24,listOf(2.0,1.0,3.0,5.0)),
            Triple("24: b=3 h=2 -> HORIZONTAL",24,listOf(1.0,2.0,4.0,4.0)),
            Triple("24: b=2 h=3 -> VERTICAL",24,listOf(2.0,1.0,4.0,4.0)),
            Triple("24: b=h=3 -> HORIZONTAL",24,listOf(1.0,1.0,4.0,4.0)),
            Triple("24: puntos invertidos -> HORIZONTAL",24,listOf(5.0,3.0,1.0,2.0)),
            Triple("29: horizontal -> SUPERIOR",29,listOf(.1,4.5,4.1,5.5)),
            Triple("30: horizontal -> INFERIOR",30,listOf(1.5,.1,2.5,.35)),
            Triple("29: vertical -> IZQUIERDO",29,listOf(.2,1.0,1.2,5.0)),
            Triple("30: vertical -> DERECHO",30,listOf(5.65,.02,5.9,1.02)),
            Triple("29: inferior mas cerca; elige IZQ.",29,listOf(1.5,.1,2.5,4.1)),
            Triple("30: derecho mas cerca; elige INF.",30,listOf(4.9,1.5,5.9,1.75)),
            Triple("30: INTERIOR explicito",30,listOf(2.6,2.0,3.4,2.4)))
        val sheet=Bitmap.createBitmap(1600,1800,Bitmap.Config.ARGB_8888);val canvas=Canvas(sheet);canvas.drawColor(Color.WHITE)
        val text=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.BLACK;textSize=16f}
        val full=axisSample().copy(anchoM=12.0,largoM=42.0,slabColumns=2,slabRows=7,prFinal="00+042")
        var pdfSample=full;val damages=mutableListOf<PavementDamageEntity>()
        specs.forEachIndexed {i,(title,code,v)->
            val s=axisSample();val d=visualDamage(code,i+1,v[0],v[1],v[2],v[3],1)
            val mode=if(i==12)PavementPatchVisuals.INTERIOR else PavementPatchVisuals.ANCHORED
            val ps=if(code in listOf(29,30))s.copy(sicDataJson=PavementPatchVisuals.update(s,d,mode))else s
            val x=(i%4)*400f;val y=(i/4)*430f
            canvas.drawText(title,x+12,y+28,text);canvas.drawText("Metros originales: X derecha, Y arriba",x+12,y+52,text)
            canvas.save();canvas.translate(x+50,y+75);PavementDrawing.draw(canvas,ps,listOf(d),50f,labels=false,slabLabels=false);canvas.restore()
            canvas.drawText("Datos ficticios; dibujante real Android",x+12,y+400,text)
            val ox=(i%2)*6.0;val oy=(i/2)*6.0
            val pd=d.copy(x1=d.x1!!+ox,x2=d.x2!!+ox,y1=d.y1!!+oy,y2=d.y2!!+oy,slabs=(i+1).toString())
            damages+=pd
            if(code in listOf(29,30))pdfSample=pdfSample.copy(sicDataJson=PavementPatchVisuals.update(pdfSample,pd,mode))
        }
        canvas.drawText("REGLA DEFINITIVA: eje predominante; sin direccion diagonal. No son capturas de tablet.",12f,1775f,text)
        File(out,"regla-definitiva-Canvas-real.png").outputStream().use{sheet.compress(Bitmap.CompressFormat.PNG,100,it)};sheet.recycle()
        val c=ApplicationProvider.getApplicationContext<Context>()
        val data=PavementExportSample(pdfSample,damages,PavementCalculator(c).calculate(pdfSample,damages))
        File(out,"regla-definitiva-PDF-horizontal-SIMULADO.pdf").outputStream().use{PavementPdf.write(it,data,emptyMap())}
        assertEquals(13,damages.size)
    }
}
