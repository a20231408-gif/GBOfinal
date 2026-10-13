package com.tuempresa.inventariovial.core

import android.graphics.Matrix
import kotlin.math.min

/** TextureView already applies sensor orientation, but stretches that image to its view.
 * Undo that stretch, compensate display rotation, then fit with a single pixel scale.
 * Reference: https://developer.android.com/codelabs/android-camera2-preview
 */
internal fun cameraPreviewTransform(viewWidth:Int,viewHeight:Int,bufferWidth:Int,bufferHeight:Int,
    sensorOrientation:Int,displayDegrees:Int):Matrix {
    require(listOf(viewWidth,viewHeight,bufferWidth,bufferHeight).all {it>0})
    val sensorSwaps=sensorOrientation%180!=0
    val naturalWidth=(if(sensorSwaps)bufferHeight else bufferWidth).toFloat()
    val naturalHeight=(if(sensorSwaps)bufferWidth else bufferHeight).toFloat()
    val displaySwaps=displayDegrees%180!=0
    val orientedWidth=if(displaySwaps)naturalHeight else naturalWidth
    val orientedHeight=if(displaySwaps)naturalWidth else naturalHeight
    val scale=min(viewWidth/orientedWidth,viewHeight/orientedHeight)
    return Matrix().apply {
        setScale(naturalWidth/viewWidth,naturalHeight/viewHeight)
        postTranslate(-naturalWidth/2,-naturalHeight/2)
        postRotate(-displayDegrees.toFloat())
        postScale(scale,scale)
        postTranslate(viewWidth/2f,viewHeight/2f)
    }
}
