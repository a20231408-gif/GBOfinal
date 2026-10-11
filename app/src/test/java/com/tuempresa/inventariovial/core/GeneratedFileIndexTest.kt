package com.tuempresa.inventariovial.core

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk=[34])
class GeneratedFileIndexTest {
    @Test fun existingReportsAppearAndOnlyExpectedTypesAreOpenable() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val dir=File(context.cacheDir,"sic_exports").apply {mkdirs()}
        val report=File(dir,"SIC-22.xlsx")
        val secret=File(dir,"server.local.properties")
        try {
            report.writeBytes(byteArrayOf(1,2,3))
            secret.writeText("SECRET")
            val files=GeneratedFileIndex.discover(context)
            val selected=files.first {it.localPath==report.canonicalPath}
            assertEquals("XLSX",selected.fileType)
            val uri=GeneratedFileIndex.uri(context,selected)
            assertEquals("content",uri.scheme);assertEquals("${context.packageName}.fileprovider",uri.authority)
            assertEquals("/generated_cache_files/sic_exports/SIC-22.xlsx",uri.path)
            assertFalse(files.any {it.localPath==secret.canonicalPath})
        } finally {report.delete();secret.delete()}
    }
}
