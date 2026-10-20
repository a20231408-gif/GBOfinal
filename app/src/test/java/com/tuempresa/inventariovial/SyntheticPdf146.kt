package com.tuempresa.inventariovial

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import java.io.ByteArrayOutputStream
import java.io.OutputStream

/** TEST ONLY. Runs the actual page renderer on bitmap canvases, packages them as a raster PDF.
 * The installed Robolectric runtime has no PdfDocument native implementation.
 * This verifies preparation/protocol, not Android's native/vector PDF writer or its performance.
 */
@Implements(PdfDocument::class)
class SyntheticPdf146 {
    private val pages=mutableListOf<Pair<PdfDocument.Page,Bitmap>>()
    @Implementation fun __constructor__() {}
    @Implementation fun startPage(info:PdfDocument.PageInfo):PdfDocument.Page {
        val bitmap=Bitmap.createBitmap(info.pageWidth,info.pageHeight,Bitmap.Config.ARGB_8888)
        val canvas=Canvas(bitmap).apply{drawColor(Color.WHITE)}
        val constructor=PdfDocument.Page::class.java.declaredConstructors.first{it.parameterTypes.size==2 && it.parameterTypes.contains(Canvas::class.java)}
        constructor.isAccessible=true
        val page=constructor.newInstance(*constructor.parameterTypes.map{if(it==Canvas::class.java)canvas else info}.toTypedArray()) as PdfDocument.Page
        pages.add(page to bitmap);return page
    }
    @Implementation fun finishPage(page:PdfDocument.Page) {check(pages.any{it.first===page})}
    @Implementation fun writeTo(output:OutputStream) {
        val objects=mutableListOf<ByteArray>()
        fun text(value:String)=value.toByteArray(Charsets.ISO_8859_1)
        fun stream(header:String,bytes:ByteArray)=text("<< $header /Length ${bytes.size} >>\nstream\n")+bytes+text("\nendstream")
        objects.add(text("<< /Type /Catalog /Pages 2 0 R >>"))
        objects.add(text("<< /Type /Pages /Count ${pages.size} /Kids [${pages.indices.joinToString(" "){"${3+it*3} 0 R"}}] >>"))
        for((index,pair) in pages.withIndex()) {
            val b=pair.second;val number=3+index*3
            val image=ByteArrayOutputStream().also{b.compress(Bitmap.CompressFormat.JPEG,85,it)}.toByteArray()
            objects.add(text("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 ${b.width} ${b.height}] /Resources << /XObject << /Image ${number+1} 0 R >> >> /Contents ${number+2} 0 R >>"))
            objects.add(stream("/Type /XObject /Subtype /Image /Width ${b.width} /Height ${b.height} /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode",image))
            objects.add(stream("",text("q ${b.width} 0 0 ${b.height} 0 0 cm /Image Do Q")))
        }
        val out=ByteArrayOutputStream();out.write(text("%PDF-1.4\n%TEST_AUTO_146_RASTER_ONLY\n"));val offsets=mutableListOf<Int>()
        for((index,body) in objects.withIndex()){offsets.add(out.size());out.write(text("${index+1} 0 obj\n"));out.write(body);out.write(text("\nendobj\n"))}
        val xref=out.size();out.write(text("xref\n0 ${objects.size+1}\n0000000000 65535 f \n"))
        offsets.forEach{out.write(text(it.toString().padStart(10,'0')+" 00000 n \n"))}
        out.write(text("trailer\n<< /Size ${objects.size+1} /Root 1 0 R >>\nstartxref\n$xref\n%%EOF\n"));output.write(out.toByteArray())
    }
    @Implementation fun close(){pages.forEach{it.second.recycle()};pages.clear()}
    @Implementation protected fun finalize() {}
}
