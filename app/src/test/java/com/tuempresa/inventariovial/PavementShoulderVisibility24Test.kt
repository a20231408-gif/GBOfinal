package com.tuempresa.inventariovial

import android.app.Application
import android.graphics.*
import com.tuempresa.inventariovial.pavement.*
import com.tuempresa.inventariovial.pavement.ui.PavementDrawing
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
import java.io.File
import kotlin.math.roundToInt

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PavementShoulderVisibility24Test {
    @Test fun bothExteriorTicksStayRedAboveFramesAndPdfLeadersAtUnchangedCoordinates() {
        for(right in listOf(false,true)) {
            var s=axisSample().copy(largoM=12.5,slabColumns=2,slabRows=2)
            val d=visualDamage(27,1,if(right)5.9 else .1,1.0,if(right)5.9 else .1,4.0,if(right)2 else 1)
            val original=d.copy()
            s=s.copy(sicDataJson=PavementBoundaryGeometry.update(s,d,if(right)"EXTERIOR_XW"else"EXTERIOR_X0"))
            val projection=PavementBoundaryGeometry.resolve(s,d)
            val plan=PavementRigidPdfScheme.plans(s,listOf(d)).single()
            val scale=plan.region.scale.toFloat()
            val x=if(right)s.anchoM*scale-1 else 1.0
            val y=(projection.a.y+projection.b.y)/2
            val canvasImage=Bitmap.createBitmap(420,770,Bitmap.Config.ARGB_8888)
            val c=Canvas(canvasImage);c.drawColor(Color.WHITE);c.translate(20f,20f)
            PavementDrawing.draw(c,s,listOf(d),scale,labels=false,slabLabels=false)
            assertEquals("Canvas exterior tick remains red",Color.RED,canvasImage.getPixel((20+x).roundToInt(),(20+(s.largoM-y)*scale).roundToInt()))
            val pdfImage=Bitmap.createBitmap(842,595,Bitmap.Config.ARGB_8888)
            Canvas(pdfImage).also{it.drawColor(Color.WHITE);PavementRigidPdfScheme.draw(it,s,plan,56f,105f)}
            assertEquals("PDF frame/leader must not hide exterior tick",Color.RED,pdfImage.getPixel((56+y*scale).roundToInt(),(135+x).roundToInt()))
            assertEquals(original,d);assertEquals(projection,PavementBoundaryGeometry.resolve(s,d))
            assertEquals(1.0,PavementRules.quantity(d),0.0)
            System.getProperty("gbo.visual.output")?.let{path->
                val out=File(path).apply{mkdirs()};val side=if(right)"XW"else"X0"
                File(out,"27-$side-Canvas-ROBOLECTRIC.png").outputStream().use{canvasImage.compress(Bitmap.CompressFormat.PNG,100,it)}
                File(out,"27-$side-PDF-ROBOLECTRIC.png").outputStream().use{pdfImage.compress(Bitmap.CompressFormat.PNG,100,it)}
            }
            canvasImage.recycle();pdfImage.recycle()
        }
    }
}
