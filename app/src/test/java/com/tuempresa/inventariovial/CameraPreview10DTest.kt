package com.tuempresa.inventariovial

import android.graphics.RectF
import com.tuempresa.inventariovial.core.cameraPreviewTransform
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.hypot

@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],application=android.app.Application::class)
class CameraPreview10DTest {
    @Test fun previewHasUniformPixelScaleAndFitsAfterAllSensorAndDisplayRotations() {
        for((w,h) in listOf(800 to 1200,1200 to 800))for(sensor in listOf(0,90,180,270))for(display in listOf(0,90,180,270)) {
            val m=cameraPreviewTransform(w,h,1920,1080,sensor,display)
            val naturalW=if(sensor%180!=0)1080f else 1920f
            val naturalH=if(sensor%180!=0)1920f else 1080f
            val points=floatArrayOf(w/2f,h/2f,w/2f+w/naturalW,h/2f,w/2f,h/2f+h/naturalH)
            m.mapPoints(points)
            assertEquals(w/2f,points[0],.01f);assertEquals(h/2f,points[1],.01f)
            assertEquals(hypot(points[2]-points[0],points[3]-points[1]),hypot(points[4]-points[0],points[5]-points[1]),.01f)
            val bounds=RectF(0f,0f,w.toFloat(),h.toFloat());m.mapRect(bounds)
            assertTrue(bounds.left>=-.01f && bounds.top>=-.01f && bounds.right<=w+.01f && bounds.bottom<=h+.01f)
            val expected=if((sensor+display)%180!=0)1080f/1920 else 1920f/1080
            assertEquals(expected,bounds.width()/bounds.height(),.001f)
        }
    }
}
