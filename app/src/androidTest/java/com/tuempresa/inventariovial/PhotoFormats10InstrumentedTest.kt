package com.tuempresa.inventariovial

import android.os.Build
import android.graphics.BitmapFactory
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tuempresa.inventariovial.core.DeliveryJpeg
import com.tuempresa.inventariovial.core.JpegEnvelope
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

/** Synthetic fixture only; no database, inventory or network access. */
@RunWith(AndroidJUnit4::class)
class PhotoFormats10InstrumentedTest {
    @Test fun heicProducesJpegAndPreservesOriginalBytes() {
        assumeTrue("HEIF requires Android 9 or later", Build.VERSION.SDK_INT >= 28)
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val directory=File(instrumentation.targetContext.cacheDir,"formats10-${UUID.randomUUID()}").apply {mkdirs()}
        try {
            val original=File(directory,"synthetic.heic")
            instrumentation.context.assets.open("photo10.heic").use {input->original.outputStream().use {input.copyTo(it)}}
            val before=original.readBytes()
            val target=File(directory,"delivery.jpg")
            DeliveryJpeg.copy(original,target)
            assertArrayEquals(before,original.readBytes())
            assertTrue(JpegEnvelope.valid(target.readBytes()))
            val bitmap=requireNotNull(BitmapFactory.decodeFile(target.absolutePath))
            assertEquals(96,bitmap.width);assertEquals(64,bitmap.height);bitmap.recycle()
        } finally {directory.deleteRecursively()}
    }
}
