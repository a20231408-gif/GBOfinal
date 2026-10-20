package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import android.graphics.*
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.pavement.*
import com.tuempresa.inventariovial.pavement.ui.PavementDrawing
import com.tuempresa.inventariovial.pavement.ui.RecoveredD24Before19Drawing
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=Application::class,shadows=[SyntheticPdf146::class])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PavementDurabilityExamplesTest {
    @Test fun actualRendererFourCornersWideTallBalancedReversedAndHistoricalComparison() {
        val out=File(requireNotNull(System.getProperty("gbo.visual.output"))).apply{mkdirs()}
        val text=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.BLACK;textSize=17f}
        val sheet=Bitmap.createBitmap(1600,1840,Bitmap.Config.ARGB_8888);val canvas=Canvas(sheet);canvas.drawColor(Color.WHITE)
        val headings=listOf("Inferior izquierda","Inferior derecha","Superior izquierda","Superior derecha")
        val pdfSample=axisSample().copy(anchoM=12.0,largoM=42.0,slabColumns=2,slabRows=7,prFinal="00+042")
        val damages=mutableListOf<PavementDamageEntity>()
        for((row,ext)in listOf(4.0 to 1.0,1.0 to 4.0,3.0 to 3.0).withIndex())for(col in 0..3) {
            val (b,h)=ext;val right=col%2==1;val top=col>=2
            fun x(v:Double)=if(right)6-v else v
            fun y(v:Double)=if(top)6-v else v
            val d=visualDamage(24,row*4+col+1,x(.1),y(.1),x(.1+b),y(.1+h),1)
            assertNull(PavementDurabilityGeometry.project(axisSample(),d).warning)
            val ox=col*400f;val oy=row*430f
            canvas.drawText(headings[col],ox+12,oy+27,text)
            canvas.drawText("b=$b m; h=$h m; X derecha / Y arriba",ox+12,oy+51,text)
            canvas.save();canvas.translate(ox+50,oy+77);PavementDrawing.draw(canvas,axisSample(),listOf(d),50f,labels=false,slabLabels=false);canvas.restore()
            canvas.drawText("D24 sin relleno ni contorno de parche",ox+12,oy+405,text)
            val i=row*4+col;val dx=i%2*6.0;val dy=i/2*6.0
            damages+=d.copy(x1=d.x1!!+dx,x2=d.x2!!+dx,y1=d.y1!!+dy,y2=d.y2!!+dy,slabs=(i+1).toString())
        }
        canvas.drawText("Respaldo previo: malla NETWORK",12f,1317f,text)
        canvas.drawText("No es la referencia del propietario",12f,1342f,text)
        val old=visualDamage(24,50,.0,.0,.0,.0,1).copy(x1=null,y1=null,x2=null,y2=null)
        canvas.save();canvas.translate(50f,1365f);RecoveredD24Before19Drawing.draw(canvas,axisSample(),listOf(old),50f,labels=false);canvas.restore()
        canvas.drawText("Imagen de referencia: no recibida",412f,1317f,text)
        canvas.drawText("No se inventa ni se sustituye por",412f,1342f,text)
        canvas.drawText("una imagen generada.",412f,1367f,text)
        val d=visualDamage(24,13,.1,.1,4.1,1.1,1);val reverse=d.copy(x1=d.x2,x2=d.x1,y1=d.y2,y2=d.y1)
        canvas.drawText("Puntos invertidos: mismo abanico",812f,1317f,text)
        canvas.save();canvas.translate(850f,1365f);PavementDrawing.draw(canvas,axisSample(),listOf(reverse),50f,labels=false,slabLabels=false);canvas.restore()
        assertEquals(PavementRigidVisualGeometry.traces(axisSample(),d),PavementRigidVisualGeometry.traces(axisSample(),reverse))
        canvas.drawText("Interior ambiguo: revisar los puntos",1212f,1317f,text)
        canvas.save();canvas.translate(1250f,1365f);PavementDrawing.draw(canvas,axisSample(),listOf(d.copy(x1=2.0,y1=2.0,x2=3.0,y2=3.0)),50f,labels=false,slabLabels=false);canvas.restore()
        canvas.drawText("Datos ficticios. Canvas Android real bajo Robolectric; no son capturas de tablet. PDF raster de ensayo, no PDF nativo.",12f,1790f,text)
        canvas.drawText("Abanico reconstruido segun texto; falta cotejar la imagen original. PDF conserva el layout aprobado.",12f,1818f,text)
        File(out,"D24-cuatro-esquinas-Canvas.png").outputStream().use{sheet.compress(Bitmap.CompressFormat.PNG,100,it)};sheet.recycle()
        val rectangular=Bitmap.createBitmap(1400,1000,Bitmap.Config.ARGB_8888);val rc=Canvas(rectangular);rc.drawColor(Color.WHITE)
        val samples=listOf(axisSample().copy(anchoM=3.5,largoM=5.0),axisSample().copy(anchoM=7.0,largoM=3.0),
            axisSample().copy(anchoM=2.0,largoM=8.0),axisSample().copy(anchoM=12.0,largoM=12.0,slabRows=2,slabColumns=2))
        samples.forEachIndexed{i,s->
            val w=s.anchoM/s.slabColumns;val h=s.largoM/s.slabRows
            val d=if(i==3)visualDamage(24,14,6.01,6.01,10.01,7.01,4)
                else visualDamage(24,i+20,.02*w,.02*h,.72*w,.42*h,1)
            val x=i%2*700f;val y=i/2*480f
            rc.drawText(if(i==3)"Cerca de junta: losa 4; sin invadir vecinas"else "Losa ${s.anchoM} x ${s.largoM} m",x+14,y+26,text)
            val scale=minOf(630/s.anchoM,390/s.largoM).toFloat()
            rc.save();rc.translate(x+40,y+55);PavementDrawing.draw(rc,s,listOf(d),scale,labels=false,slabLabels=true);rc.restore()
        }
        rc.drawText("Datos ficticios; geometria comun Android/PDF. No son capturas de tablet.",14f,985f,text)
        File(out,"D24-losas-rectangulares-y-junta.png").outputStream().use{rectangular.compress(Bitmap.CompressFormat.PNG,100,it)};rectangular.recycle()
        // Non-square slab and near the shared boundary, in the last row of the PDF specimen.
        damages+=visualDamage(24,13,6.01,36.01,10.01,37.01,14)
        val c=ApplicationProvider.getApplicationContext<Context>();val data=PavementExportSample(pdfSample,damages,PavementCalculator(c).calculate(pdfSample,damages))
        File(out,"D24-PDF-horizontal-SIMULADO.pdf").outputStream().use{PavementPdf.write(it,data,emptyMap())}
        assertEquals(13,damages.size)
    }
}
