package com.tuempresa.inventariovial.pavement

import kotlin.math.*

/** Visual recipes do not alter the manual's measurement geometry or measured quantity. */
enum class PavementSketchKind { DURABILITY_D, LINE, IRREGULAR_LINE, AREA, NETWORK, PATCH, POINT, SLAB, CORNER, JOINT, TRANSVERSE_PATTERN, LONGITUDINAL_BANDS, PARABOLIC, DEPRESSION, SWELL, SCATTER, GLOBAL }
data class SketchPoint(val x: Double, val y: Double)
object PavementSketch {
    fun kind(code: Int): PavementSketchKind = when (code) {
        24 -> PavementSketchKind.DURABILITY_D
        1, 3, 23, 36 -> PavementSketchKind.NETWORK
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
        if (d.geometry == "GLOBAL") {
            if(d.code==26 && PavementJointAnnotations.marks(s,d.id).isNotEmpty())return PavementJointAnnotations.marks(s,d.id).any {
                val (a,b)=PavementJointAnnotations.endpoints(s,it)
                maxOf(a.y,b.y)>=start && minOf(a.y,b.y)<=end
            } || index==1
            return index == 1
        }
        val visual=if(d.geometry=="SLABS" && d.code in setOf(25,27))runCatching{PavementBoundaryGeometry.resolve(s,d).traces().flatMap{it.points}}.getOrDefault(emptyList())
            else if(d.geometry=="SLABS" && d.code in listOf(24,29,30))PavementRigidVisualGeometry.traces(s,d).flatMap {it.points} else emptyList()
        val bounds = if(visual.isNotEmpty()) visual.minOf {it.y} to visual.maxOf {it.y}
        else if (d.geometry == "SLABS" && !PavementSlabGeometry.localized(d)) {
            val rows = PavementRules.slabIds(d).map { (it-1)/s.slabColumns }
            if (rows.isEmpty()) return false
            rows.min()*s.largoM/s.slabRows to (rows.max()+1)*s.largoM/s.slabRows
        } else if(d.code==22 && PavementSlabGeometry.localized(d)) {
            val corner=runCatching {PavementCornerGeometry.resolve(s,d)}.getOrNull()
            if(corner!=null)corner.crack(d.id).let {points->points.minOf {it.y} to points.maxOf {it.y}} else listOfNotNull(d.y1,d.y2).let {if(it.isEmpty())return false else it.min() to it.max()}
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
    const val PAGE_WIDTH=842
    const val PAGE_HEIGHT=595
    const val PLOT_WIDTH=718.0
    const val PLOT_HEIGHT=180.0
    fun scale(width:Double)=min(32.0,PLOT_HEIGHT/width)
    fun segments(width: Double, length: Double, availableWidth: Double = PLOT_WIDTH, availableHeight: Double = PLOT_HEIGHT, slabLength:Double?=null): List<PavementPdfSegment> {
        require(width.isFinite() && length.isFinite() && width > 0 && length > 0)
        require(availableWidth>0 && availableHeight>0)
        val pixelsPerMetre=min(32.0,availableHeight/width)
        val capacity=availableWidth/pixelsPerMetre
        // Prefer complete longitudinal slab rows; longer figures continue with the same ID.
        val span=if(slabLength!=null && slabLength>0 && slabLength<=capacity)floor(capacity/slabLength).coerceAtLeast(1.0)*slabLength else capacity
        val count=ceil(length/span).toInt().coerceAtLeast(1)
        return (0 until count).map { i -> PavementPdfSegment(i+1,count,i*span,min(length,(i+1)*span)) }
    }
    /** Visual quarter turn: longitudinal Y rightwards, transverse X downwards. */
    fun point(x:Double,y:Double,segment:PavementPdfSegment,scale:Double)=SketchPoint((y-segment.start)*scale,x*scale)
    /** Report reference only, and only when both recorded PRs agree with the measured length. */
    fun chainages(s:PavementSampleEntity,segment:PavementPdfSegment):String? = runCatching {
        val a=PavementChainage.parse(s.prInicial);val b=PavementChainage.parse(s.prFinal)
        require(abs(abs(b-a)-s.largoM)<=.01)
        val direction=if(b>=a)1 else -1
        "${PavementChainage.format(a+direction*segment.start)} → ${PavementChainage.format(a+direction*segment.end)}"
    }.getOrNull()
}
