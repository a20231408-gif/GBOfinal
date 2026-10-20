package com.tuempresa.inventariovial

import android.app.Application
import android.graphics.pdf.PdfDocument
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
import org.robolectric.util.ReflectionHelpers
import java.io.File

/** Availability probe without any custom PdfDocument shadow. Not an application acceptance test. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PdfRuntime146Test {
    @Test fun nativeAvailability() {
        val doc=PdfDocument()
        try {
            val handle:Long=ReflectionHelpers.getField(doc,"mNativeDocument")
            assertEquals("Installed Robolectric PdfDocument native handle",0L,handle)
            val failure=runCatching{doc.startPage(PdfDocument.PageInfo.Builder(595,842,1).create())}.exceptionOrNull()
            assertTrue(failure is IllegalStateException);assertEquals("document is closed!",failure!!.message)
            val out=File("build/outputs/hotfix-1.4.6/pdf-runtime-probe.json");out.parentFile!!.mkdirs()
            out.writeText(JSONObject().put("environment","Robolectric 4.16.1 / SDK34 / Windows, no custom PDF shadow")
                .put("freshDocumentNativeHandle",handle).put("nativePdfWriterVerified",false)
                .put("status","ENVIRONMENT_LIMITATION: fresh document is already closed before application rendering").toString(2))
        } finally {doc.close()}
    }
}
