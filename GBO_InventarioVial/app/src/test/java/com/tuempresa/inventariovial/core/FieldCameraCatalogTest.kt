package com.tuempresa.inventariovial.core

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk=[34])
class FieldCameraCatalogTest {
    @Test fun enumerationIsSafeWithoutAnAttachedExternalCamera() {
        val cameras = FieldCameraCatalog.cameras(ApplicationProvider.getApplicationContext<Context>())
        assertEquals(cameras.size, cameras.map {it.cameraId}.distinct().size)
    }
}
