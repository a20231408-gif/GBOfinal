package com.tuempresa.inventariovial.pavement

import kotlin.math.*

/** Measured X/Y stay in Room. Only the drawing endpoints are projected. Metres, bottom-left origin. */
object PavementCornerGeometry {
    data class Corner(val slab: Int, val a: SketchPoint, val b: SketchPoint, val left: Double, val bottom: Double, val width: Double, val length: Double) {
        fun crack(id: String) = PavementSketch.crack(id, SketchPoint(a.x-left,a.y-bottom), SketchPoint(b.x-left,b.y-bottom), width,length)
            .map { SketchPoint(it.x+left,it.y+bottom) }
    }
    fun resolve(s: PavementSampleEntity, a: SketchPoint, b: SketchPoint, slab: Int? = null, boundaryTolerance: Double = 0.0): Corner {
        require(s.surface=="RIGID" && s.slabColumns>0 && s.slabRows>0 && s.anchoM>0 && s.largoM>0) { "Define la malla de losas." }
        val w=s.anchoM/s.slabColumns;val h=s.largoM/s.slabRows
        for(p in listOf(a,b)) require(p.x.isFinite() && p.y.isFinite() && p.x in 0.0..s.anchoM && p.y in 0.0..s.largoM) { "Punto fuera de la muestra." }
        val n=slab ?: PavementSlabGeometry.slabAt(s,a.x,a.y)
        require(n in 1..s.slabRows*s.slabColumns) { "Selecciona una losa válida." }
        val left=(n-1)%s.slabColumns*w;val bottom=(n-1)/s.slabColumns*h
        val tolerance=boundaryTolerance.coerceIn(0.0,min(w,h)*.04)
        if(slab==null) for(p in listOf(a,b)) {
            require((1 until s.slabColumns).none {abs(p.x-it*w)<=max(1e-8,tolerance)} &&
                (1 until s.slabRows).none {abs(p.y-it*h)<=max(1e-8,tolerance)}) { "Toque junto a dos losas: mueve el punto hacia el interior de la losa observada." }
        }
        fun local(p: SketchPoint):SketchPoint {
            require(p.x in left-1e-8..left+w+1e-8 && p.y in bottom-1e-8..bottom+h+1e-8) { "Marca ambos puntos dentro de la misma losa." }
            return SketchPoint((p.x-left).coerceIn(0.0,w),(p.y-bottom).coerceIn(0.0,h))
        }
        val p=local(a);val q=local(b)
        require(hypot(p.x-q.x,p.y-q.y)>=max(.01,min(w,h)*.01)) { "Los puntos están demasiado cerca; sepáralos." }
        fun edge(p:SketchPoint):Int {
            val distances=listOf(p.x,w-p.x,p.y,h-p.y)
            val sorted=distances.withIndex().sortedBy {it.value}
            require(sorted[1].value-sorted[0].value>max(1e-7,min(w,h)*.01)) { "Esquina ambigua: mueve cada punto cerca de uno de sus dos bordes." }
            return sorted[0].index
        }
        val ea=edge(p);val eb=edge(q)
        require(ea!=eb) { "Ambos puntos corresponden al mismo borde; selecciona dos bordes adyacentes." }
        require((ea<2)!=(eb<2)) { "Los bordes son opuestos; selecciona los bordes de una misma esquina." }
        fun snap(p:SketchPoint,e:Int)=when(e) {0->p.copy(x=0.0);1->p.copy(x=w);2->p.copy(y=0.0);else->p.copy(y=h)}
        val pa=snap(p,ea);val pb=snap(q,eb)
        return Corner(n,SketchPoint(pa.x+left,pa.y+bottom),SketchPoint(pb.x+left,pb.y+bottom),left,bottom,w,h)
    }
    fun resolve(s:PavementSampleEntity,d:PavementDamageEntity):Corner {
        require(d.code==22 && d.geometry=="SLABS" && PavementRules.slabIds(d).size==1) { "Localiza la rotura en una sola losa." }
        return resolve(s,SketchPoint(requireNotNull(d.x1),requireNotNull(d.y1)),SketchPoint(requireNotNull(d.x2),requireNotNull(d.y2)),PavementRules.slabIds(d).single())
    }
}
