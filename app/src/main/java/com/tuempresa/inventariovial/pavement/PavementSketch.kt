package com.tuempresa.inventariovial.pavement

import kotlin.math.*

/** Visual recipes do not alter the manual's measurement geometry or measured quantity. */
enum class PavementSketchKind { LINE, IRREGULAR_LINE, AREA, NETWORK, PATCH, POINT, SLAB, CORNER, JOINT, TRANSVERSE_PATTERN, LONGITUDINAL_BANDS, PARABOLIC, DEPRESSION, SWELL, SCATTER, GLOBAL }
data class SketchPoint(val x: Double, val y: Double)
object PavementSketch {
    fun kind(code: Int): PavementSketchKind = when (code) {
        1, 3, 23, 24, 36 -> PavementSketchKind.NETWORK
        7, 8, 10, 28, 37 -> PavementSketchKind.IRREGULAR_LINE
        4, 5, 14, 21, 35, 81, 83 -> PavementSketchKind.TRANSVERSE_PATTERN
        9, 27, 82, 87 -> PavementSketchKind.LINE
        11, 29, 30 -> PavementSketchKind.PATCH
        13, 85 -> PavementSketchKind.POINT
        15, 86 -> PavementSketchKind.LONGITUDINAL_BANDS
        17 -> PavementSketchKind.PARABOLIC
        6 -> PavementSketchKind.DEPRESSION
        16, 18 -> PavementSketchKind.SWELL
        19, 20, 32 -> PavementSketchKind.SCATTER
        22, 38 -> PavementSketchKind.CORNER
        25, 33, 39 -> PavementSketchKind.JOINT
        34 -> PavementSketchKind.SLAB
        26, 84 -> PavementSketchKind.GLOBAL
        2, 12, 31 -> PavementSketchKind.AREA
        else -> error("Daño sin receta de croquis: $code")
    }
    /** Fixed algorithm, UUID seed, metric offsets, invariant measured endpoints. */
    fun crack(id: String, a: SketchPoint, b: SketchPoint, width: Double, length: Double): List<SketchPoint> {
        val distance = hypot(b.x-a.x, b.y-a.y)
        if (distance == 0.0) return listOf(a,b)
        val segments = ceil(distance / 0.4).toInt().coerceIn(6,160)
        var seed = id.fold(0x811c9dc5.toInt()) { h,c -> (h xor c.code) * 16777619 }
        val amplitude = min(0.13, distance * 0.04)
        return (0..segments).map { i ->
            if (i == 0) a else if (i == segments) b else {
                seed = seed xor (seed shl 13); seed = seed xor (seed ushr 17); seed = seed xor (seed shl 5)
                val t = i.toDouble()/segments
                val offset = ((seed.toLong() and 0xffffffffL).toDouble()/0xffffffffL*2-1)*amplitude*sin(PI*t)
                SketchPoint((a.x+(b.x-a.x)*t-(b.y-a.y)/distance*offset).coerceIn(0.0,width),
                    (a.y+(b.y-a.y)*t+(b.x-a.x)/distance*offset).coerceIn(0.0,length))
            }
        }
    }
}

data class PavementPdfSegment(val index: Int, val count: Int, val start: Double, val end: Double) {
    val length get() = end-start
    fun contains(s: PavementSampleEntity, d: PavementDamageEntity): Boolean {
        if (d.geometry == "GLOBAL") return index == 1
        val bounds = if (d.geometry == "SLABS" && !PavementSlabGeometry.localized(d)) {
            val rows = PavementRules.slabIds(d).map { (it-1)/s.slabColumns }
            if (rows.isEmpty()) return false
            rows.min()*s.largoM/s.slabRows to (rows.max()+1)*s.largoM/s.slabRows
        } else {
            val ys=listOfNotNull(d.y1,d.y2)
            if (ys.isEmpty()) return false
            val radius = if (d.geometry == "POTHOLE") (d.diameterM?:0.0)/2 else 0.0
            ys.min()-radius to ys.max()+radius
        }
        // Shared boundary belongs to the segment above; extended shapes are clipped in both.
        return bounds.second >= start && (bounds.first < end || index == count && bounds.first <= end) &&
            !(bounds.second == start && bounds.first < start)
    }
}
object PavementPdfLayout {
    fun segments(width: Double, length: Double, availableWidth: Double = 420.0, availableHeight: Double = 400.0): List<PavementPdfSegment> {
        require(width.isFinite() && length.isFinite() && width > 0 && length > 0)
        // At least 20 pt/m for ordinary field widths; a 50 m sample becomes 0–20/20–40/40–50.
        val pixelsPerMetre = min(20.0, availableWidth/width)
        val span = availableHeight / pixelsPerMetre
        val count = ceil(length/span).toInt().coerceAtLeast(1)
        return (0 until count).map { i -> PavementPdfSegment(i+1,count,i*span,min(length,(i+1)*span)) }
    }
}
