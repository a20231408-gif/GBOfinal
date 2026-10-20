package com.tuempresa.inventariovial.pavement

import org.json.JSONObject

/** Additive drawing metadata in the existing envelope. No measured field or contractual cell changes. */
object PavementPatchVisuals {
    const val ANCHORED="ANCHORED"
    const val INTERIOR="INTERIOR"
    private const val key="_gboPatchVisuals"
    private const val parallelRule="PARALLEL_AXIS_1"
    private fun entry(s:PavementSampleEntity,id:String)=s.sicDataJson?.let(::JSONObject)?.optJSONObject(key)?.optJSONObject(id)
    fun parallelAnchoring(s:PavementSampleEntity,id:String)=mode(s,id)==ANCHORED && entry(s,id)?.optString("anchorRule")==parallelRule
    fun mode(s:PavementSampleEntity,id:String):String {
        val e=s.sicDataJson?.let(::JSONObject)?.optJSONObject(key)?.optJSONObject(id) ?: return INTERIOR
        // Unknown or mismatched metadata must never relocate a historical observation.
        return if(e.optInt("version")==1 && e.optString("sampleId")==s.id && e.optString("damageId")==id &&
            e.optInt("rows")==s.slabRows && e.optInt("columns")==s.slabColumns &&
            e.optDouble("width")==s.anchoM && e.optDouble("length")==s.largoM && e.optString("mode")==ANCHORED) ANCHORED else INTERIOR
    }
    fun update(s:PavementSampleEntity,d:PavementDamageEntity,mode:String):String {
        require(d.sampleId==s.id && d.surface=="RIGID" && d.code in listOf(29,30) && d.geometry=="SLABS")
        require(mode in listOf(ANCHORED,INTERIOR))
        val root=s.sicDataJson?.let(::JSONObject) ?: JSONObject();val all=root.optJSONObject(key) ?: JSONObject()
        val old=entry(s,d.id)
        // A historical anchored entry without this optional key keeps its previous placement,
        // including reopening/confirming it unchanged. New anchors use the parallel-axis rule.
        val retainHistoricalAnchor=mode==ANCHORED && PavementPatchVisuals.mode(s,d.id)==ANCHORED && old?.optString("anchorRule").isNullOrBlank()
        val e=JSONObject(old?.toString() ?: "{}").put("version",1).put("sampleId",s.id).put("damageId",d.id).put("mode",mode)
            .put("rows",s.slabRows).put("columns",s.slabColumns).put("width",s.anchoM).put("length",s.largoM)
        if(mode==ANCHORED && !retainHistoricalAnchor) {
            if(PavementSlabGeometry.localized(d)) {
                val x1=d.x1;val x2=d.x2;val y1=d.y1;val y2=d.y2
                require(x1!=null && x2!=null && y1!=null && y2!=null && listOf(x1,x2,y1,y2).all{it.isFinite()}){"Completa los dos puntos del parche."}
                require(s.slabColumns>0 && s.slabRows>0 && kotlin.math.abs(x2-x1)<=s.anchoM/s.slabColumns && kotlin.math.abs(y2-y1)<=s.largoM/s.slabRows) {
                    "El parche no cabe en una losa para adosarlo sin reducirlo. Conserva las medidas y elige Interior o revisa los puntos."
                }
                require(PavementRules.slabIds(d).size==1){"El adosado requiere localizar el parche en una sola losa. Para varias losas conserva Interior."}
            }
            e.put("anchorRule",parallelRule)
        }
        all.put(d.id,e)
        return root.put(key,all).toString()
    }
}
