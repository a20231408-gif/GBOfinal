package com.tuempresa.inventariovial.pavement

import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.*

/** Optional versioned visual metadata in the existing JSON envelope, outside contractual SIC formats. */
object PavementJointAnnotations {
    private const val key="_gboJointAnnotations"
    data class Mark(val kind:String,val row:Int,val column:Int)
    const val legend="Evaluación global de la unidad de inspección. Las juntas resaltadas son referencias visuales del levantamiento y no constituyen unidades adicionales de medición."
    private fun grid(s:PavementSampleEntity)=JSONObject().put("rows",s.slabRows).put("columns",s.slabColumns).put("width",s.anchoM).put("length",s.largoM)
    private fun entry(s:PavementSampleEntity,id:String)=s.sicDataJson?.let(::JSONObject)?.optJSONObject(key)?.optJSONObject(id)
    fun needsReview(s:PavementSampleEntity,id:String):Boolean = entry(s,id)?.let {
        val g=it.optJSONObject("grid")
        it.optInt("version")!=1 || it.optString("sampleId")!=s.id || it.optString("damageId")!=id || g==null ||
            g.optInt("rows")!=s.slabRows || g.optInt("columns")!=s.slabColumns || g.optDouble("width")!=s.anchoM || g.optDouble("length")!=s.largoM
    } ?: false
    fun marks(s:PavementSampleEntity,id:String):Set<Mark> {
        val e=entry(s,id) ?: return emptySet()
        if(needsReview(s,id))return emptySet() // Never move observations to another joint silently.
        val a=e.optJSONArray("marks") ?: return emptySet()
        return (0 until a.length()).map {a.getJSONObject(it).let {m->Mark(m.getString("kind"),m.getInt("row"),m.getInt("column"))}}.filter { valid(s,it) }.toSet()
    }
    fun valid(s:PavementSampleEntity,m:Mark)=s.surface=="RIGID" && when(m.kind) {
        "V"->m.column in 1 until s.slabColumns && m.row in 0 until s.slabRows
        "H"->m.row in 1 until s.slabRows && m.column in 0 until s.slabColumns
        "N"->m.row in 1 until s.slabRows && m.column in 1 until s.slabColumns
        else->false
    }
    fun update(s:PavementSampleEntity,d:PavementDamageEntity,marks:Set<Mark>):String {
        require(d.sampleId==s.id && d.code==26 && d.geometry=="GLOBAL") { "Las marcas pertenecen al daño global 26 de esta muestra." }
        require(marks.all {valid(s,it)}) { "Marca de junta fuera de la malla." }
        val root=s.sicDataJson?.let(::JSONObject) ?: JSONObject();val all=root.optJSONObject(key) ?: JSONObject()
        all.put(d.id,JSONObject().put("version",1).put("sampleId",s.id).put("damageId",d.id).put("grid",grid(s))
            .put("marks",JSONArray(marks.sortedWith(compareBy({it.kind},{it.row},{it.column})).map {JSONObject().put("kind",it.kind).put("row",it.row).put("column",it.column)})))
        root.put(key,all);return root.toString()
    }
    fun hit(s:PavementSampleEntity,x:Double,y:Double,tolerance:Double):Mark? {
        if(s.surface!="RIGID" || s.slabRows<1 || s.slabColumns<1 || !x.isFinite() || !y.isFinite() || !tolerance.isFinite() || tolerance<=0 || x !in 0.0..s.anchoM || y !in 0.0..s.largoM)return null
        val w=s.anchoM/s.slabColumns;val h=s.largoM/s.slabRows
        val radius=min(tolerance,min(w,h)*.22)
        val col=(x/w).roundToInt();val row=(y/h).roundToInt()
        if(col in 1 until s.slabColumns && row in 1 until s.slabRows && hypot(x-col*w,y-row*h)<=radius*.65)return Mark("N",row,col)
        val v=Mark("V",floor(y/h).toInt().coerceIn(0,s.slabRows-1),col)
        val horizontal=Mark("H",row,floor(x/w).toInt().coerceIn(0,s.slabColumns-1))
        val dv=abs(x-col*w);val dh=abs(y-row*h)
        return if(valid(s,v) && dv<=radius && (!valid(s,horizontal) || dh>radius || dv<dh))v
            else if(valid(s,horizontal) && dh<=radius)horizontal else null
    }
    fun endpoints(s:PavementSampleEntity,m:Mark):Pair<SketchPoint,SketchPoint> {
        require(valid(s,m));val w=s.anchoM/s.slabColumns;val h=s.largoM/s.slabRows
        val a=SketchPoint(m.column*w,m.row*h)
        return a to when(m.kind){"V"->a.copy(y=a.y+h);"H"->a.copy(x=a.x+w);else->a}
    }
}
