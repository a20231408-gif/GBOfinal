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
class PavementFinalExamplesTest {
    @Test fun realSharedCanvasAndLandscapePageRendererExamples() {
        val out=File(System.getProperty("gbo.visual.output")).apply{mkdirs()}
        val specs=listOf(
            Triple("24 - Horizontal",24,listOf(.2,2.0,3.3,2.0)),Triple("24 - Vertical",24,listOf(1.5,.2,1.5,4.8)),
            Triple("24 - Vertical (h > b)",24,listOf(.2,.3,3.3,4.7)),Triple("24 - Vertical invertida",24,listOf(.2,4.7,3.3,.3)),
            Triple("24 - Puntos invertidos",24,listOf(1.5,4.8,1.5,.2)),Triple("24 - En borde",24,listOf(0.0,.1,0.0,4.9)),
            Triple("29 - Adosado izquierda",29,listOf(.2,1.5,1.2,3.0)),Triple("29 - Adosado derecha",29,listOf(2.3,1.5,3.3,3.0)),
            Triple("30 - Adosado abajo",30,listOf(1.2,.2,2.2,1.2)),Triple("30 - Adosado arriba",30,listOf(1.2,3.8,2.2,4.8)),
            Triple("29 - Cuadrado: empate estable",29,listOf(.2,.2,1.2,1.2)),Triple("30 - Interior explicito",30,listOf(1.2,2.0,1.8,2.6)))
        val bitmap=Bitmap.createBitmap(1600,1590,Bitmap.Config.ARGB_8888);val canvas=Canvas(bitmap);canvas.drawColor(Color.WHITE)
        val text=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.BLACK;textSize=19f}
        val full=visualSample();var pdfSample=full;val pdfDamages=mutableListOf<PavementDamageEntity>()
        specs.forEachIndexed {i,(label,code,v)->
            val s=visualSample(5.0).copy(anchoM=3.5,slabColumns=1)
            val d=visualDamage(code,i+1,v[0],v[1],v[2],v[3],1)
            val mode=if(i==11)PavementPatchVisuals.INTERIOR else PavementPatchVisuals.ANCHORED
            val ps=if(code in listOf(29,30))s.copy(sicDataJson=PavementPatchVisuals.update(s,d,mode))else s
            val x=(i%4)*400f;val y=(i/4)*510f
            canvas.drawText(label,x+16,y+30,text)
            canvas.drawText("Canvas real - datos ficticios",x+16,y+57,text.apply{textSize=16f})
            text.textSize=19f
            canvas.save();canvas.translate(x+65,y+80);PavementDrawing.draw(canvas,ps,listOf(d),76f,labels=false);canvas.restore()
            canvas.drawText(if(code==24)"P1 -> P2; cantidad: 1 losa"else if(i==11)"Modo INTERIOR guardado"else"Modo ADOSADO guardado",x+16,y+485,text.apply{textSize=16f});text.textSize=19f
            val ox=(i%2)*3.5;val oy=(i/2)*5.0
            val pd=d.copy(x1=d.x1!!+ox,x2=d.x2!!+ox,y1=d.y1!!+oy,y2=d.y2!!+oy,slabs=(i+1).toString())
            pdfDamages+=pd
            if(code in listOf(29,30))pdfSample=pdfSample.copy(sicDataJson=PavementPatchVisuals.update(pdfSample,pd,mode))
        }
        canvas.drawText("DIBUJANTE DE LA APP / ROBOLECTRIC. No son capturas de tablet.",16f,1570f,text)
        File(out,"ejemplos-Canvas-real.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
        val c=ApplicationProvider.getApplicationContext<Context>()
        val data=PavementExportSample(pdfSample,pdfDamages,PavementCalculator(c).calculate(pdfSample,pdfDamages))
        File(out,"ejemplos-PDF-horizontal-SIMULADO.pdf").outputStream().use{PavementPdf.write(it,data,emptyMap())}
        assertTrue(File(out,"ejemplos-Canvas-real.png").length()>1000)
    }
}
