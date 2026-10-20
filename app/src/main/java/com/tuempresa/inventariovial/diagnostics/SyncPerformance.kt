package com.tuempresa.inventariovial.diagnostics

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Numeric, bounded, allowlisted performance samples. Never accepts identities, paths or errors. */
object SyncPerformance {
    private val phases=setOf("PREPARE","RESERVE","CAPABILITIES","FILE_PREPARE","PHOTO_PREPARE","FILE_SEND","PHOTO_SEND",
        "FILE_WAIT_ACK","PHOTO_WAIT_ACK","COMMIT_SEND","COMMIT_WAIT_ACK","RESERVE_SEND","RESERVE_WAIT_ACK",
        "FILE_HTTP","PHOTO_HTTP","COMMIT_HTTP","RESERVE_HTTP","STAGE_REUSED","ACK","DELIVERY","DRAIN")
    private val outcomes=setOf("OK","SENT","BUSY","RETRY","REJECTED","HTTP_ERROR","UNCONFIRMED","CANCELLED","UNKNOWN")
    fun now()=System.nanoTime()/1_000_000
    @Synchronized fun record(context:Context,kind:String,phase:String,elapsedMs:Long,bytes:Long=0,outcome:String="OK") {
        // Diagnostics must never fail a delivery or serialize arbitrary remote strings.
        runCatching {
            require(kind in setOf("SIC","SCAP","PAVEMENT","ALL") && phase in phases && outcome in outcomes)
            val prefs=context.getSharedPreferences("sync_performance_146",Context.MODE_PRIVATE)
            val old=JSONArray(prefs.getString("samples","[]"));val next=JSONArray()
            for(n in maxOf(0,old.length()-255) until old.length())next.put(old.getJSONObject(n))
            next.put(JSONObject().put("kind",kind).put("phase",phase).put("elapsedMs",elapsedMs.coerceIn(0,86_400_000))
                .put("bytes",bytes.coerceIn(0,256L*1024*1024)).put("outcome",outcome))
            val totals=JSONObject(prefs.getString("totals","{}"));val key="$kind/$phase"
            val total=totals.optJSONObject(key) ?: JSONObject()
            total.put("count",total.optLong("count")+1).put("sumMs",total.optLong("sumMs")+elapsedMs.coerceIn(0,86_400_000))
                .put("maxMs",maxOf(total.optLong("maxMs"),elapsedMs.coerceIn(0,86_400_000))).put("bytes",total.optLong("bytes")+bytes.coerceIn(0,256L*1024*1024))
            val outcomesTotal=total.optJSONObject("outcomes") ?: JSONObject();outcomesTotal.put(outcome,outcomesTotal.optLong(outcome)+1);total.put("outcomes",outcomesTotal);totals.put(key,total)
            prefs.edit().putString("samples",next.toString()).putString("totals",totals.toString()).apply()
        }
    }
    fun report(context:Context):JSONObject {
        val samples=runCatching{JSONArray(context.getSharedPreferences("sync_performance_146",Context.MODE_PRIVATE).getString("samples","[]"))}.getOrDefault(JSONArray())
        val groups=(0 until samples.length()).map{samples.getJSONObject(it)}.groupBy {it.getString("kind")+"/"+it.getString("phase")}
        val totals=JSONArray(groups.map {(key,rows)->val times=rows.map{it.getLong("elapsedMs")}.sorted()
            JSONObject().put("kindPhase",key).put("count",rows.size).put("sumMs",times.sum()).put("maxMs",times.last())
                .put("p50Ms",times[(times.size-1)/2]).put("p95Ms",times[((times.size-1)*95)/100]).put("bytes",rows.sumOf{it.getLong("bytes")})})
        return JSONObject().put("sampleLimit",256).put("samples",samples).put("summaryOfRecentSamples",totals)
            .put("cumulative",runCatching{JSONObject(context.getSharedPreferences("sync_performance_146",Context.MODE_PRIVATE).getString("totals","{}"))}.getOrDefault(JSONObject()))
            .put("scope","LOCAL_OBSERVATIONS_ONLY; overlapping phases must not be added; HTTP wait includes network and server, not measured lock time")
    }
}
