package com.tuempresa.inventariovial

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.util.ReflectionHelpers
import java.io.OutputStream

/** Windows/JVM has no native PdfDocument handle. Only the PDF container is simulated.
 * PavementPdf.render still draws onto real native bitmap canvases; JPEG/hash/outbox/ACK are real code.
 * Never use the synthetic PDF output as evidence of a real PDF or Drive delivery.
 */
@Implements(PdfDocument::class)
class PdfDocument10CShadow {
    private val pages=mutableListOf<Bitmap>()
    private var current:PdfDocument.Page?=null
    @Implementation fun startPage(info:PdfDocument.PageInfo):PdfDocument.Page {
        check(current==null)
        val bitmap=Bitmap.createBitmap(info.pageWidth,info.pageHeight,Bitmap.Config.ARGB_8888)
        pages+=bitmap
        return ReflectionHelpers.callConstructor(PdfDocument.Page::class.java,
            ReflectionHelpers.ClassParameter.from(Canvas::class.java,Canvas(bitmap)),
            ReflectionHelpers.ClassParameter.from(PdfDocument.PageInfo::class.java,info)).also {current=it}
    }
    @Implementation fun finishPage(page:PdfDocument.Page) {check(current===page);current=null}
    @Implementation fun writeTo(output:OutputStream) {
        check(current==null && pages.isNotEmpty())
        output.write("%PDF-1.4\n% SYNTHETIC JVM CONTAINER FOR PHOTO TRANSPORT TEST ONLY\n".toByteArray())
    }
    @Implementation fun close() {check(current==null);pages.forEach {it.recycle()};pages.clear()}
}
