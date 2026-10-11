package com.tuempresa.inventariovial.pavement

import android.content.Context
import org.json.JSONObject
import org.json.JSONArray
import kotlin.math.*

data class CurvePoint(val x: Double, val y: Double)
data class PavementTotal(val code: Int, val severity: String?, val quantity: Double, val density: Double?, val vd: Double?)
data class CorrectionIteration(val values: List<Double>, val q: Int, val total: Double, val cdv: Double)
data class PavementResult(val totals: List<PavementTotal>, val iterations: List<CorrectionIteration>, val index: Double?, val rating: String?, val pending: List<String>)

/** No extrapolation, no invented origin and no interpolation between undocumented q values. */
object CurveInterpolation {
    fun value(points: List<CurvePoint>, x: Double, logarithmic: Boolean): Double {
        require(points.size >= 2 && x.isFinite()) { "Curva sin digitalizar." }
        require(points.all { it.x.isFinite() && it.y.isFinite() && it.y in 0.0..100.0 }) { "Curva inválida." }
        require(points.zipWithNext().all { it.first.x < it.second.x }) { "La curva debe estar ordenada sin duplicados." }
        require(x in points.first().x..points.last().x) { "Densidad/total fuera del rango digitalizado." }
        if (logarithmic) require(x > 0 && points.all { it.x > 0 }) { "Escala logarítmica requiere valores positivos." }
        points.firstOrNull { it.x == x }?.let { return it.y }
        val (a,b) = points.zipWithNext().first { x in it.first.x..it.second.x }
        fun axis(v: Double) = if(logarithmic) log10(v) else v
        return a.y + (b.y-a.y)*(axis(x)-axis(a.x))/(axis(b.x)-axis(a.x))
    }
}

object PciCorrection {
    // Manual pp. 52–54 (PCI) and 59–60, equation 7 (URCI).
    fun calculate(values: List<Double>, unpaved: Boolean = false, correction: (Int,Double)->Double): Pair<Double,List<CorrectionIteration>> {
        val threshold = if (unpaved) 5.0 else 2.0
        val denominator = if (unpaved) 95.0 else 98.0
        require(values.all { it.isFinite() && it in 0.0..100.0 })
        if(values.count { it > threshold } <= 1) return values.sum() to emptyList()
        val sorted=values.sortedDescending()
        val m=min(10.0,1.0+(9.0/denominator)*(100.0-sorted.first()))
        val retained=sorted.take(floor(m).toInt()).toMutableList()
        if(sorted.size > floor(m).toInt() && m%1 > 0) retained.add(sorted[floor(m).toInt()]*(m%1))
        val rows=mutableListOf<CorrectionIteration>()
        while(true) {
            val q=retained.count { it > threshold };val total=retained.sum()
            val cdv=correction(q,total)
            require(cdv.isFinite() && cdv in 0.0..100.0)
            rows.add(CorrectionIteration(retained.toList(),q,total,cdv))
            if(q<=1) break
            val smallest=retained.indices.filter { retained[it]>threshold }.minBy { retained[it] }
            retained[smallest]=threshold
        }
        return rows.maxOf { it.cdv } to rows
    }
    fun rating(index: Double): String = when { index>=85->"Bueno";index>=70->"Satisfactorio";index>=55->"Regular";index>=40->"Malo";index>=25->"Muy malo";index>=10->"Grave";else->"Fallado" }
}

class PavementCalculator(context: Context) {
    private val assets=context.assets
    fun calculate(sample:PavementSampleEntity, damages:List<PavementDamageEntity>):PavementResult {
        if(sample.status=="CLOSED" && sample.calculationSnapshot!=null) return PavementResultJson.decode(sample.calculationSnapshot)
        val surface=when(sample.surface){"ASPHALT"->"asfalto";"RIGID"->"rigido";else->"no_pavimentado"}
        val issues=mutableListOf<String>()
        if(damages.any{PavementRules.isLargePothole(it) && (it.areaM2==null || !it.areaM2.isFinite() || it.areaM2<=0)}) issues.add("Falta área medida para huecos equivalentes (manual pp.108/233).")
        val area=sample.anchoM*sample.largoM
        if(!area.isFinite() || area<=0) return PavementResult(emptyList(),emptyList(),null,null,listOf("Dimensiones de muestra inválidas."))
        if(sample.surface=="RIGID" && (sample.slabRows<=0 || sample.slabColumns<=0))
            return PavementResult(emptyList(),emptyList(),null,null,listOf("Define filas y columnas de losas."))
        if(sample.surface=="RIGID" && PavementHeader.slabLength(sample)>=8.0)
            return PavementResult(emptyList(),emptyList(),null,null,listOf("Losas de 8 m o más: define unidades con juntas imaginarias según el manual pp.39/159 antes de calcular."))
        val root=assets.open("pavement/curvas/$surface.json").bufferedReader().use{JSONObject(it.readText())}
        val curves=root.getJSONArray("curvas_vd")
        val normalized = if(sample.surface=="RIGID") PavementSlabTotals.records(damages) else damages.map { it.copy(severity=PavementSeverity.canonical(it.severity)) }
        val totals=normalized.groupBy { it.code to it.severity }.map { (key, records) ->
            val geometry=records.first().geometry
            val count=if(geometry=="SLABS") records.flatMap{PavementRules.slabIds(it)}.toSet().size.toDouble() else records.sumOf{PavementRules.quantity(it)}
            val density=when(geometry){"GLOBAL"->null;"SLABS"-> if(sample.slabRows*sample.slabColumns>0) count/(sample.slabRows*sample.slabColumns)*100 else null;else->count/area*100}
            // Bundled validated curve identifiers still use legacy B/A. Do not rewrite curve data.
            val id="$surface:${key.first}:${PavementSeverity.legacy(key.second)?:"SIN_SEVERIDAD"}"
            val curve=(0 until curves.length()).map{curves.getJSONObject(it)}.firstOrNull{it.getString("id")==id}
            val value=runCatching {
                require(curve!=null && curve.optString("estado")=="VALIDADA") { "Falta curva $id (sin digitalizar/validar)." }
                if(geometry=="GLOBAL") curve.getDouble("valor_global") else {
                    val scale=curve.getString("escala_x");require(scale in listOf("LOG10","LINEAL"))
                    CurveInterpolation.value(points(curve),requireNotNull(density),scale=="LOG10")
                }
            }.getOrElse { issues.add(it.message?:"Falta curva $id");null }
            PavementTotal(key.first,key.second,count,density,value)
        }.sortedBy{it.code}
        if(issues.isNotEmpty()) return PavementResult(totals,emptyList(),null,null,issues)
        return runCatching {
            val (deducted,iterations)=PciCorrection.calculate(totals.map{it.vd!!}, unpaved=sample.surface=="UNSURFACED") { q,total ->
                val corrections=root.getJSONArray("curvas_correccion")
                val curve=(0 until corrections.length()).map{corrections.getJSONObject(it)}.firstOrNull{it.getInt("q")==q && it.optString("estado")=="VALIDADA"}
                require(curve!=null){"Falta curva de corrección $surface: q=$q."}
                CurveInterpolation.value(points(curve),total,false)
            }
            val index=100-deducted
            require(index in 0.0..100.0){"Resultado fuera de rango; revisar curvas."}
            PavementResult(totals,iterations,index,PciCorrection.rating(index),emptyList())
        }.getOrElse{PavementResult(totals,emptyList(),null,null,listOf(it.message?:"Cálculo pendiente."))}
    }
    private fun points(curve:JSONObject):List<CurvePoint> {
        val array=curve.getJSONArray("puntos")
        return (0 until array.length()).map{val p=array.getJSONArray(it);CurvePoint(p.getDouble(0),p.getDouble(1))}
    }
}

/** Immutable calculation state (including missing curves) captured when the sample is closed. */
object PavementResultJson {
    fun encode(r:PavementResult):String = JSONObject().put("index",r.index?:JSONObject.NULL).put("rating",r.rating?:JSONObject.NULL)
        .put("pending",JSONArray(r.pending)).put("totals",JSONArray(r.totals.map{JSONObject().put("code",it.code).put("severity",it.severity?:JSONObject.NULL)
            .put("quantity",it.quantity).put("density",it.density?:JSONObject.NULL).put("vd",it.vd?:JSONObject.NULL)}))
        .put("iterations",JSONArray(r.iterations.map{JSONObject().put("values",JSONArray(it.values)).put("q",it.q).put("total",it.total).put("cdv",it.cdv)})).toString()
    fun decode(text:String):PavementResult {
        val r=JSONObject(text);val totals=r.getJSONArray("totals");val iterations=r.getJSONArray("iterations");val pending=r.getJSONArray("pending")
        fun optional(o:JSONObject,key:String)=if(o.isNull(key))null else o.getDouble(key)
        return PavementResult((0 until totals.length()).map{val t=totals.getJSONObject(it);PavementTotal(t.getInt("code"),if(t.isNull("severity"))null else t.getString("severity"),t.getDouble("quantity"),optional(t,"density"),optional(t,"vd"))},
            (0 until iterations.length()).map{val row=iterations.getJSONObject(it);val values=row.getJSONArray("values");CorrectionIteration((0 until values.length()).map{j->values.getDouble(j)},row.getInt("q"),row.getDouble("total"),row.getDouble("cdv"))},
            optional(r,"index"),if(r.isNull("rating"))null else r.getString("rating"),(0 until pending.length()).map{pending.getString(it)})
    }
}
