package com.tuempresa.inventariovial.pavement

import kotlin.math.*

/** Drawing only: metres in the original frame. Never writes measured coordinates or PCI quantity. */
object PavementRigidVisualGeometry {
    data class Trace(val points: List<SketchPoint>, val closed: Boolean = false)
    data class Bounds(val left: Double, val bottom: Double, val right: Double, val top: Double) {
        val width get() = right-left
        val height get() = top-bottom
    }
    fun traces(s: PavementSampleEntity, d: PavementDamageEntity): List<Trace> {
        if(s.surface!="RIGID" || s.slabRows<1 || s.slabColumns<1 || s.anchoM<=0 || s.largoM<=0 || d.code !in listOf(24,29,30))return emptyList()
        if(d.code==24)return PavementDurabilityGeometry.project(s,d).traces
        val w=s.anchoM/s.slabColumns;val h=s.largoM/s.slabRows
        return PavementRules.slabIds(d).filter { it in 1..s.slabRows*s.slabColumns }.flatMap { n ->
            val slab=Bounds((n-1)%s.slabColumns*w,(n-1)/s.slabColumns*h,(n-1)%s.slabColumns*w+w,(n-1)/s.slabColumns*h+h)
            val located=PavementSlabGeometry.localized(d)
            var zone=if(located) {
                val a=d.x1;val b=d.x2;val c=d.y1;val e=d.y2
                if(a==null||b==null||c==null||e==null||listOf(a,b,c,e).any {!it.isFinite()})return@flatMap emptyList()
                Bounds(max(slab.left,min(a,b)),max(slab.bottom,min(c,e)),min(slab.right,max(a,b)),min(slab.top,max(c,e)))
            } else Bounds(slab.left+w*.18,slab.bottom+h*.18,slab.right-w*.18,slab.top-h*.18)
            if(zone.width<=0 || zone.height<=0)return@flatMap emptyList()
            run {
                if(located && PavementPatchVisuals.mode(s,d.id)==PavementPatchVisuals.ANCHORED) {
                    val parallel=PavementPatchVisuals.parallelAnchoring(s,d.id)
                    // Current anchors preserve the full measured size; historical clipped symbols
                    // keep their previous placement instead of moving on application update.
                    if(parallel)zone=Bounds(min(d.x1!!,d.x2!!),min(d.y1!!,d.y2!!),max(d.x1!!,d.x2!!),max(d.y1!!,d.y2!!))
                    zone=anchor(zone,slab,parallel)
                }
                patch(zone)
            }
        }
    }
    /** New anchors only consider parallel edges. Missing rule metadata preserves historical symbols. */
    private fun anchor(z:Bounds,slab:Bounds,parallel:Boolean):Bounds {
        val distances=listOf(z.left-slab.left,slab.right-z.right,z.bottom-slab.bottom,slab.top-z.top)
        if(parallel) {
            require(z.width<=slab.width && z.height<=slab.height){"El parche no cabe en una losa sin reducirlo; revisa sus puntos o elige Interior."}
            val candidates=when {z.width==z.height->listOf(0,1,2,3);z.width>=z.height->listOf(2,3);else->listOf(0,1)}
            // Deterministic ties: left/right for vertical, bottom/top for horizontal; square L/R/B/T.
            val edge=candidates.minBy{distances[it]}
            val dx=when(edge){0->slab.left-z.left;1->slab.right-z.right;else->0.0}
            val dy=when(edge){2->slab.bottom-z.bottom;3->slab.top-z.top;else->0.0}
            return Bounds(z.left+dx,z.bottom+dy,z.right+dx,z.top+dy)
        }
        val nearest=distances.min();val tied=distances.indices.filter {abs(distances[it]-nearest)<1e-8}
        val horizontal=tied.firstOrNull {it<2};val vertical=tied.firstOrNull {it>=2}
        val dx=when(horizontal){0->slab.left-z.left;1->slab.right-z.right;else->0.0}
        val dy=when(vertical){2->slab.bottom-z.bottom;3->slab.top-z.top;else->0.0}
        return Bounds(z.left+dx,z.bottom+dy,z.right+dx,z.top+dy)
    }
    private fun patch(z:Bounds):List<Trace> {
        val outline=Trace(listOf(SketchPoint(z.left,z.bottom),SketchPoint(z.right,z.bottom),SketchPoint(z.right,z.top),SketchPoint(z.left,z.top)),true)
        // Diagonal hatch in metric proportions; endpoints clipped analytically to the repair.
        val spacing=max(min(z.width,z.height)/5,(z.width+z.height)/32)
        val lines=mutableListOf<Trace>();var offset=-z.height+spacing
        while(offset<z.width) {
            val y0=max(0.0,-offset);val y1=min(z.height,z.width-offset)
            fun point(y:Double)=SketchPoint((z.left+offset+y).coerceIn(z.left,z.right),(z.bottom+y).coerceIn(z.bottom,z.top))
            if(y1>y0)lines+=Trace(listOf(point(y0),point(y1)))
            offset+=spacing
        }
        return listOf(outline)+lines
    }
}
