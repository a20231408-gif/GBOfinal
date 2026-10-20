package com.tuempresa.inventariovial

import com.tuempresa.inventariovial.pavement.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PavementReportTest {
    @Test fun rendersSampleReportAndExcelWithoutCurves() {
        val s=PavementSampleEntity("demo","Ruta de prueba","R-01",1,"Derecho","","001+000","001+046",5.0,46.0,"ASPHALT","2026-09-28","demo","Inspector de prueba",status="CLOSED",laneWidthM=5.0)
        val d=PavementDamageEntity("d","demo",1,"ASPHALT",1,"Piel de cocodrilo","M","AREA",1.0,10.0,3.0,15.0,null,2.0,5.0,10.0,null,2.0,"Registro sintético para probar el formato; no es una inspección real.")
        val r=PavementResult(listOf(PavementTotal(1,"M",10.0,10.0/230*100,null)),emptyList(),null,null,listOf("PCI no calculable: falta curva asfalto:1:M"))
        val folder=File("build/reports/pavement").apply{mkdirs()}
        val pages=mutableListOf<Bitmap>()
        PavementPdf.render(PavementExportSample(s,listOf(d),r),emptyMap(),{
            val bitmap=Bitmap.createBitmap(PavementPdfLayout.PAGE_WIDTH,PavementPdfLayout.PAGE_HEIGHT,Bitmap.Config.ARGB_8888);pages.add(bitmap)
            Canvas(bitmap).apply{drawColor(Color.WHITE)}
        },{})
        assertTrue("Long samples must be segmented at a readable scale",pages.size>=3)
        pages.forEachIndexed{i,b->File(folder,"reporte-prueba-${i+1}.png").outputStream().use{b.compress(Bitmap.CompressFormat.PNG,100,it)}}
        assertTrue(File(folder,"reporte-prueba-1.png").length()>1000)
        File(folder,"reporte-prueba.xlsx").outputStream().use{PavementExcel.write(it,listOf(PavementExportSample(s,listOf(d),r)))}
    }
}
