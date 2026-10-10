package com.tuempresa.inventariovial.pavement

import kotlin.math.ceil

/** Local Canvas Y, with the existing bottom-left origin. Translation is applied by Canvas. */
internal data class PavementScale(val length: Double, val pixelsPerMetre: Float) {
    fun canvasY(metres: Double): Float = ((length - metres) * pixelsPerMetre).toFloat()
    fun metresY(canvasY: Float): Double = length - canvasY / pixelsPerMetre
}

internal object PavementRuler {
    fun labelStep(length: Double, height: Float, minimumSpacing: Float): Int =
        when {
            height / length >= minimumSpacing -> 1
            height / length * 2 >= minimumSpacing -> 2
            else -> ceil(minimumSpacing * length / height.coerceAtLeast(1f) / 5).toInt().coerceAtLeast(1) * 5
        }
    fun ticks(length: Double): List<Double> {
        val step = ceil(length / 1000).toInt().coerceAtLeast(1)
        return (0..(length / step).toInt()).map { it.toDouble() * step }.let {
            if (it.lastOrNull() == length) it else it + length
        }
    }
}

/** Shared viewport transform for drawing, ruler, hit coordinates and drag movement. */
internal data class PavementViewport(val length: Double, val scale: Float, val originX: Float, val originY: Float) {
    fun zoomAt(x: Float, y: Float, factor: Float, dx: Float = 0f, dy: Float = 0f) =
        copy(scale=scale*factor, originX=x+(originX-x)*factor+dx, originY=y+(originY-y)*factor+dy)
    fun canvasY(metres: Double) = originY + PavementScale(length,scale).canvasY(metres)
    fun metresY(y: Float) = PavementScale(length,scale).metresY(y-originY)
    fun metresX(x: Float) = (x-originX).toDouble()/scale
    companion object {
        fun fit(width: Double,length: Double,canvasWidth: Float,canvasHeight: Float,gutter: Float,zoom: Float,panX: Float,panY: Float) =
            PavementViewport(length,(kotlin.math.min((canvasWidth-gutter-15)/width,(canvasHeight-30)/length)*zoom).toFloat(),gutter+panX,15+panY)
    }
}
