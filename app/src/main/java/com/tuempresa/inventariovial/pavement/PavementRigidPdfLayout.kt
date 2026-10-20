package com.tuempresa.inventariovial.pavement

import kotlin.math.*

/** PDF presentation only. Regions crop the existing geometry; they never change measurements. */
object PavementRigidPdfLayout {
    const val WIDTH=750.0
    const val HEIGHT=350.0
    const val GAP=8.0
    data class Region(val index:Int,val count:Int,val x0:Double,val x1:Double,val y0:Double,val y1:Double,val scale:Double) {
        val width get()=(y1-y0)*scale
        val height get()=(x1-x0)*scale
        fun project(p:SketchPoint)=SketchPoint((p.y-y0)*scale,(p.x-x0)*scale)
    }
    data class Ref(val damage:PavementDamageEntity,val point:SketchPoint?,val referential:Boolean,val width:Double)
    data class Group(val refs:List<Ref>,val top:Boolean,val anchor:SketchPoint?,val left:Double=0.0) {
        val width get()=refs.sumOf{it.width}+4*(refs.size-1)
        val center get()=left+width/2
    }
    data class Panel(val region:Region,val groups:List<Group>,val index:Int,val count:Int) {
        val damages get()=groups.flatMap{it.refs}.map{it.damage}.distinctBy{it.id}
    }
    fun regions(s:PavementSampleEntity):List<Region> {
        require(s.anchoM.isFinite() && s.largoM.isFinite() && s.anchoM>0 && s.largoM>0)
        val ny=ceil(s.largoM/(WIDTH*s.anchoM/HEIGHT)).toInt().coerceAtLeast(1)
        val span=s.largoM/ny
        val fit=min(WIDTH/span,HEIGHT/s.anchoM)
        // For a short/wide specimen, magnify transverse strips rather than shrink the whole plot.
        val strips=if(span*fit<WIDTH*.85)ceil(s.anchoM*(WIDTH/span)/HEIGHT).toInt().coerceAtLeast(1)else 1
        val scale=if(strips>1)WIDTH/span else fit
        return (0 until ny).flatMap {y->(0 until strips).map{x->
            Region(y*strips+x+1,ny*strips,x*s.anchoM/strips,(x+1)*s.anchoM/strips,y*span,if(y==ny-1)s.largoM else (y+1)*span,scale)
        }}
    }
    private fun paths(s:PavementSampleEntity,d:PavementDamageEntity):List<List<SketchPoint>> {
        if(d.code in setOf(25,27) && d.geometry=="SLABS")return runCatching{PavementBoundaryGeometry.resolve(s,d).traces().map{it.points}}.getOrDefault(emptyList())
        if(d.geometry=="GLOBAL")return if(d.code==26)PavementJointAnnotations.marks(s,d.id).map{PavementJointAnnotations.endpoints(s,it).let{(a,b)->listOf(a,b)}}else emptyList()
        if(d.code in listOf(24,29,30) && d.geometry=="SLABS")return PavementRigidVisualGeometry.traces(s,d).filter {d.code==24 || it.closed}.map{if(it.closed)it.points+it.points.first()else it.points}
        if(d.code==22 && PavementSlabGeometry.localized(d))return listOf(runCatching{PavementCornerGeometry.resolve(s,d).crack(d.id)}.getOrElse{listOf(SketchPoint(d.x1!!,d.y1!!),SketchPoint(d.x2!!,d.y2!!))})
        if(d.geometry=="SLABS" && !PavementSlabGeometry.localized(d))return PavementRules.slabIds(d).sorted().map {n->
            val x=(n-1)%s.slabColumns*s.anchoM/s.slabColumns;val y=(n-1)/s.slabColumns*s.largoM/s.slabRows
            val w=s.anchoM/s.slabColumns;val h=s.largoM/s.slabRows
            listOf(SketchPoint(x,y),SketchPoint(x+w,y),SketchPoint(x+w,y+h),SketchPoint(x,y+h),SketchPoint(x,y))
        }
        val a=if(d.x1!=null && d.y1!=null)SketchPoint(d.x1,d.y1)else return emptyList()
        val b=if(d.x2!=null && d.y2!=null)SketchPoint(d.x2,d.y2)else a
        return if(d.geometry=="AREA" || d.geometry=="SLABS" && PavementSlabGeometry.mark(d.code)==SlabMark.AREA)
            listOf(listOf(a,SketchPoint(b.x,a.y),b,SketchPoint(a.x,b.y),a))else listOf(listOf(a,b))
    }
    /** Exact clipping of polyline segments supplies references on visible geometry, not original P1. */
    fun visiblePoints(s:PavementSampleEntity,d:PavementDamageEntity,r:Region):List<SketchPoint> = paths(s,d).flatMap {path->
        path.zipWithNext().flatMap {(a,b)->
            val dx=b.x-a.x;val dy=b.y-a.y;var lo=0.0;var hi=1.0;var valid=true
            fun edge(p:Double,q:Double){if(abs(p)<1e-12){if(q<0)valid=false}else{val t=q/p;if(p<0)lo=max(lo,t)else hi=min(hi,t)}}
            edge(-dx,a.x-r.x0);edge(dx,r.x1-a.x);edge(-dy,a.y-r.y0);edge(dy,r.y1-a.y)
            if(!valid || lo>hi)emptyList()else listOf(lo,hi).map{t->r.project(SketchPoint((a.x+dx*t).coerceIn(r.x0,r.x1),(a.y+dy*t).coerceIn(r.y0,r.y1)))}
        }
    }
    fun panels(s:PavementSampleEntity,damages:List<PavementDamageEntity>,widthOf:(PavementDamageEntity)->Double={72.0}):List<Panel> = regions(s).flatMap {r->
        val refs=damages.mapNotNull {d->
            val p=visiblePoints(s,d,r)
            val noLocation=d.geometry=="GLOBAL" && paths(s,d).isEmpty() && r.index==1
            if(p.isEmpty() && !noLocation)null else Ref(d,if(p.isEmpty())null else p[p.size/2],d.geometry=="SLABS" && !PavementSlabGeometry.localized(d),widthOf(d).coerceIn(40.0,r.width))
        }.sortedWith(compareBy({it.point?.y?.let{y->y>r.height/2} ?: false},{it.point?.x ?: 0.0},{it.damage.correlativo},{it.damage.id}))
        val clusters=mutableListOf<Group>()
        for(ref in refs) {
            val top=ref.point==null || ref.point.y<=r.height/2
            val last=clusters.lastOrNull()
            // Nearby projected columns share one zone callout, avoiding coincident leader bundles.
            if(ref.point!=null && last?.anchor!=null && last.top==top && abs(ref.point.x-last.anchor.x)<=14 && last.width+4+ref.width<=r.width) {
                val merged=last.refs+ref;clusters[clusters.lastIndex]=Group(merged,top,SketchPoint(merged.mapNotNull{it.point}.map{it.x}.average(),if(top)merged.mapNotNull{it.point}.minOf{it.y}else merged.mapNotNull{it.point}.maxOf{it.y}))
            } else clusters+=Group(listOf(ref),top,ref.point)
        }
        val batches=mutableListOf<MutableList<Group>>(mutableListOf())
        for(group in clusters) {
            val batch=batches.last();val side=batch.filter{it.top==group.top}
            if(side.sumOf{it.width}+GAP*side.size+group.width>r.width+1e-8)batches+=mutableListOf(group)else batch+=group
        }
        batches.mapIndexed {i,groups->
            val placed=listOf(true,false).flatMap {top->
                val side=groups.filter{it.top==top};val left=DoubleArray(side.size)
                for(k in side.indices)left[k]=max(if(k==0)0.0 else left[k-1]+side[k-1].width+GAP,((side[k].anchor?.x ?: 0.0)-side[k].width/2).coerceIn(0.0,r.width-side[k].width))
                if(side.isNotEmpty() && left.last()+side.last().width>r.width) {
                    left[left.lastIndex]=r.width-side.last().width
                    for(k in side.lastIndex-1 downTo 0)left[k]=min(left[k],left[k+1]-GAP-side[k].width)
                }
                side.mapIndexed{k,g->g.copy(left=left[k])}
            }
            Panel(r,placed,i+1,batches.size)
        }
    }
}
