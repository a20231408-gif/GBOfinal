package com.tuempresa.inventariovial.pavement

import kotlin.math.*

/** D24 presentation only. No corner choice or transformed point is persisted. */
object PavementDurabilityGeometry {
    data class Projection(
        val traces: List<PavementRigidVisualGeometry.Trace>,
        val corner: SketchPoint? = null,
        val warning: String? = null
    )

    fun project(s: PavementSampleEntity, d: PavementDamageEntity): Projection {
        if(d.code!=24 || s.surface!="RIGID" || s.slabRows<1 || s.slabColumns<1 ||
            !s.anchoM.isFinite() || !s.largoM.isFinite() || s.anchoM<=0 || s.largoM<=0)return Projection(emptyList())
        val w=s.anchoM/s.slabColumns;val h=s.largoM/s.slabRows
        val ids=PavementRules.slabIds(d).filter{it in 1..s.slabRows*s.slabColumns}
        fun bounds(n:Int)=PavementRigidVisualGeometry.Bounds((n-1)%s.slabColumns*w,(n-1)/s.slabColumns*h,(n-1)%s.slabColumns*w+w,(n-1)/s.slabColumns*h+h)
        val coordinates=listOf(d.x1,d.y1,d.x2,d.y2)
        val located=coordinates.all{it!=null && it.isFinite()}
        fun unresolved(message:String):Projection {
            // Crosses mean unresolved location, never a fabricated D24 fan or repair outline.
            val traces=ids.flatMap{n->
                val z=bounds(n)
                val points=if(located)listOf(SketchPoint(d.x1!!,d.y1!!),SketchPoint(d.x2!!,d.y2!!))
                    .filter{it.x in z.left..z.right && it.y in z.bottom..z.top}
                    else listOf(SketchPoint((z.left+z.right)/2,(z.bottom+z.top)/2))
                val r=min(w,h)*.025
                points.distinct().flatMap{p->listOf(
                    PavementRigidVisualGeometry.Trace(listOf(SketchPoint(max(z.left,p.x-r),p.y),SketchPoint(min(z.right,p.x+r),p.y))),
                    PavementRigidVisualGeometry.Trace(listOf(SketchPoint(p.x,max(z.bottom,p.y-r)),SketchPoint(p.x,min(z.top,p.y+r))))) }
            }
            return Projection(traces,warning=message)
        }
        if(!located)return unresolved("D24 sin localizar: completa los dos puntos para definir la esquina. Las cruces no representan grietas.")
        if(ids.size!=1)return unresolved("Esquina D24 ambigua: indica una sola losa y revisa los dos puntos en la vista previa.")
        val z=bounds(ids.single())
        if(listOf(SketchPoint(d.x1!!,d.y1!!),SketchPoint(d.x2!!,d.y2!!)).any{
            it.x<z.left-1e-8 || it.x>z.right+1e-8 || it.y<z.bottom-1e-8 || it.y>z.top+1e-8
        })return unresolved("D24: ambos puntos deben pertenecer a la losa indicada; revisa el límite entre losas.")
        val b=abs(d.x2-d.x1);val depth=abs(d.y2-d.y1)
        if(b<=1e-8 || depth<=1e-8)return unresolved("D24 necesita extensión horizontal y vertical para representar el abanico. Corrige los puntos; no se inventa una dimensión.")
        val left=min(d.x1,d.x2)-z.left;val right=z.right-max(d.x1,d.x2)
        val bottom=min(d.y1,d.y2)-z.bottom;val top=z.top-max(d.y1,d.y2)
        // At most 15% of each slab axis; ambiguous ties never select an arbitrary corner.
        if(min(left,right)>w*.15 || min(bottom,top)>h*.15 || abs(left-right)<=w*.01 || abs(bottom-top)<=h*.01)
            return unresolved("Esquina D24 no determinada: corrige X1/Y1 y X2/Y2 cerca de dos bordes adyacentes. Revisa la vista previa; las cruces indican los puntos guardados.")
        val sx=if(left<right)1.0 else -1.0;val sy=if(bottom<top)1.0 else -1.0
        val c=SketchPoint(if(sx>0)z.left else z.right,if(sy>0)z.bottom else z.top)
        fun transform(p:SketchPoint)=SketchPoint((c.x+sx*b*p.x).coerceIn(z.left,z.right),(c.y+sy*depth*p.y).coerceIn(z.bottom,z.top))
        // Reuse the historic deterministic irregular-crack primitive, not its old NETWORK recipe.
        // Nested inclined cracks are denser near the corner; branches join existing vertices.
        val radii=listOf(.12,.21,.34,.50,.72,1.0)
        val local=radii.mapIndexed{i,r->PavementSketch.crack("${d.id}/D24/fan/$i",SketchPoint(0.0,r),SketchPoint(r,0.0),1.0,1.0)}
        val branches=listOf(1 to .28,2 to .63,4 to .43).mapIndexed{i,(inner,t)->
            val a=local[inner][(local[inner].lastIndex*t).roundToInt()]
            val outer=local[inner+1];val q=outer[(outer.lastIndex*(t+.12)).roundToInt().coerceAtMost(outer.lastIndex)]
            PavementSketch.crack("${d.id}/D24/branch/$i",a,q,1.0,1.0)
        }
        return Projection((local+branches).map{PavementRigidVisualGeometry.Trace(it.map(::transform))},c)
    }
}
