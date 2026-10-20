package com.tuempresa.inventariovial.pavement

import org.json.JSONObject
import kotlin.math.*

/** Optional visual choice in the existing opaque JSON envelope. No Room fields or PCI units change. */
object PavementBoundaryGeometry {
    private const val metadata="_gboBoundaryVisuals"
    data class Joint(val key:String,val vertical:Boolean,val at:Double,val low:Double,val high:Double,val label:String)
    data class Projection(val joint:Joint,val a:SketchPoint,val b:SketchPoint,val markerA:SketchPoint,val markerB:SketchPoint) {
        fun traces()=listOf(PavementRigidVisualGeometry.Trace(listOf(a,b)),PavementRigidVisualGeometry.Trace(listOf(markerA,markerB)))
    }
    private fun source(d:PavementDamageEntity)=listOf(min(d.x1!!,d.x2!!),max(d.x1,d.x2),min(d.y1!!,d.y2!!),max(d.y1,d.y2)).joinToString("/")+"/"+PavementRules.slabIds(d).sorted().joinToString(",")
    private fun entry(s:PavementSampleEntity,d:PavementDamageEntity)=s.sicDataJson?.let(::JSONObject)?.optJSONObject(metadata)?.optJSONObject(d.id)
    private fun validEntry(s:PavementSampleEntity,d:PavementDamageEntity,e:JSONObject)=PavementSlabGeometry.localized(d) &&
        listOf(d.x1,d.y1,d.x2,d.y2).all{it!=null && it.isFinite()} && e.optInt("version")==1 && e.optString("sampleId")==s.id &&
        e.optInt("code")==d.code && e.optString("source")==source(d) && e.optInt("rows")==s.slabRows && e.optInt("columns")==s.slabColumns &&
        e.optDouble("width")==s.anchoM && e.optDouble("length")==s.largoM && e.optString("lane")==s.carril && e.optString("roadbed")==s.calzada &&
        e.optString("prStart")==s.prInicial && e.optString("prEnd")==s.prFinal
    fun choice(s:PavementSampleEntity,d:PavementDamageEntity):String?=entry(s,d)?.takeIf{validEntry(s,d,it)}?.optString("joint")
    private fun inputs(s:PavementSampleEntity,d:PavementDamageEntity) {
        require(s.surface=="RIGID" && s.anchoM.isFinite() && s.largoM.isFinite() && s.anchoM>0 && s.largoM>0 && s.slabRows>0 && s.slabColumns>0 && d.code in setOf(25,27) && d.geometry=="SLABS") {"Define una malla rígida válida."}
        require(listOf(d.x1,d.y1,d.x2,d.y2).all{it!=null && it.isFinite()}) {"Localiza el tramo con dos puntos; el registro histórico se conserva."}
        require(d.x1!! in 0.0..s.anchoM && d.x2!! in 0.0..s.anchoM && d.y1!! in 0.0..s.largoM && d.y2!! in 0.0..s.largoM) {"Los puntos deben quedar dentro de la muestra."}
        require(PavementRules.slabIds(d).isNotEmpty() && PavementRules.slabIds(d).all{it in 1..s.slabRows*s.slabColumns}) {"Selecciona las losas afectadas, sin agregar la vecina automáticamente."}
    }
    fun options(s:PavementSampleEntity,d:PavementDamageEntity):List<Joint> {
        inputs(s,d)
        val w=s.anchoM/s.slabColumns;val h=s.largoM/s.slabRows
        val candidates=if(d.code==27)listOf(Joint("EXTERIOR_X0",true,0.0,0.0,s.largoM,"Berma en X=0 · borde izquierdo del croquis"),
            Joint("EXTERIOR_XW",true,s.anchoM,0.0,s.largoM,"Berma en X=${s.anchoM} · borde derecho del croquis"))
        else PavementRules.slabIds(d).flatMap{n->
            val row=(n-1)/s.slabColumns;val col=(n-1)%s.slabColumns
            buildList {
                if(col>0)add(Joint("V:$row:$col",true,col*w,row*h,(row+1)*h,"Junta izquierda de losa $n"))
                if(col<s.slabColumns-1)add(Joint("V:$row:${col+1}",true,(col+1)*w,row*h,(row+1)*h,"Junta derecha de losa $n"))
                if(row>0)add(Joint("H:$row:$col",false,row*h,col*w,(col+1)*w,"Junta inferior de losa $n"))
                if(row<s.slabRows-1)add(Joint("H:${row+1}:$col",false,(row+1)*h,col*w,(col+1)*w,"Junta superior de losa $n"))
            }
        }.distinctBy{it.key}
        val tolerance=min(w,h)*.15
        return candidates.filter{j->
            val u1=if(j.vertical)d.y1!! else d.x1!!;val u2=if(j.vertical)d.y2!! else d.x2!!
            val v1=if(j.vertical)d.x1!! else d.y1!!;val v2=if(j.vertical)d.x2!! else d.y2!!
            abs(v1-j.at)<=tolerance && abs(v2-j.at)<=tolerance && min(u1,u2)>=j.low-1e-8 && max(u1,u2)<=j.high+1e-8 && abs(u2-u1)>1e-6
        }.sortedWith(compareBy({j->max(abs((if(j.vertical)d.x1!! else d.y1!!)-j.at),abs((if(j.vertical)d.x2!! else d.y2!!)-j.at))},{it.key}))
    }
    fun resolve(s:PavementSampleEntity,d:PavementDamageEntity,explicitChoice:String?=null):Projection {
        val options=options(s,d)
        if(explicitChoice==null)entry(s,d)?.let{require(validEntry(s,d,it)){"Cambió la malla, cabecera o los puntos: confirma nuevamente la junta o berma en Editar."}}
        val selected=explicitChoice ?: choice(s,d)
        require(d.code!=27 || !selected.isNullOrBlank()) {"Confirma qué borde exterior corresponde físicamente a la berma; el carril y las progresivas no bastan para inferirlo."}
        require(options.isNotEmpty()) {if(d.code==25)"Los puntos no definen una junta compartida cercana. Revisa sus coordenadas y la losa vecina." else "Los puntos no están junto a un borde lateral exterior. Una junta interior no es una berma."}
        val joint=if(!selected.isNullOrBlank())options.firstOrNull{it.key==selected}
            ?: error("La junta o berma elegida no está junto a los dos puntos; no se proyecta al lado opuesto.")
        else {
            fun distance(j:Joint)=max(abs((if(j.vertical)d.x1!! else d.y1!!)-j.at),abs((if(j.vertical)d.x2!! else d.y2!!)-j.at))
            require(options.size==1 || distance(options[1])-distance(options[0])>min(s.anchoM/s.slabColumns,s.largoM/s.slabRows)*.02) {"Cruce de juntas ambiguo: elige y confirma la junta en la vista previa."}
            options.first()
        }
        val lo=min(if(joint.vertical)d.y1!! else d.x1!!,if(joint.vertical)d.y2!! else d.x2!!).coerceIn(joint.low,joint.high)
        val hi=max(if(joint.vertical)d.y1!! else d.x1!!,if(joint.vertical)d.y2!! else d.x2!!).coerceIn(joint.low,joint.high)
        val a=if(joint.vertical)SketchPoint(joint.at,lo)else SketchPoint(lo,joint.at)
        val b=if(joint.vertical)SketchPoint(joint.at,hi)else SketchPoint(hi,joint.at)
        if(d.code==27) {
            val col=if(joint.at==0.0)0 else s.slabColumns-1;val slabH=s.largoM/s.slabRows
            val first=(lo/slabH).toInt().coerceIn(0,s.slabRows-1);val last=(max(lo,hi-1e-8)/slabH).toInt().coerceIn(0,s.slabRows-1)
            require((first..last).all{it*s.slabColumns+col+1 in PavementRules.slabIds(d)}) {"Indica las losas del tramo junto a la berma; no se modifica la cantidad automáticamente."}
        }
        val mid=SketchPoint((a.x+b.x)/2,(a.y+b.y)/2);val half=min(s.anchoM/s.slabColumns,s.largoM/s.slabRows)*.045
        val m1=if(joint.vertical)mid.copy(x=max(0.0,mid.x-half))else mid.copy(y=max(0.0,mid.y-half))
        val m2=if(joint.vertical)mid.copy(x=min(s.anchoM,mid.x+half))else mid.copy(y=min(s.largoM,mid.y+half))
        return Projection(joint,a,b,m1,m2)
    }
    fun update(s:PavementSampleEntity,d:PavementDamageEntity,choice:String):String {
        require(d.sampleId==s.id) {"Daño de otra muestra."}
        val p=resolve(s,d,choice)
        val root=s.sicDataJson?.let(::JSONObject) ?: JSONObject();val all=root.optJSONObject(metadata) ?: JSONObject()
        all.put(d.id,JSONObject().put("version",1).put("sampleId",s.id).put("code",d.code).put("source",source(d)).put("joint",p.joint.key)
            .put("rows",s.slabRows).put("columns",s.slabColumns).put("width",s.anchoM).put("length",s.largoM)
            .put("lane",s.carril).put("roadbed",s.calzada).put("prStart",s.prInicial).put("prEnd",s.prFinal))
        root.put(metadata,all);return root.toString()
    }
}
