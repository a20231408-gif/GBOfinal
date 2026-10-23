package com.tuempresa.inventariovial.diagnostics

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Temporary 1.4.11 field trace. Bounded local codes only: no UUID, user, route, text, media or credentials. */
object Sic20DraftDiagnostics {
    private val stages=setOf("FORM_SELECTED","DRAFT_SAVED","DRAFT_READ","DRAFT_ROUTED","FORM_RESTORED","DRAFT_BLOCKED")
    private val assets=setOf("WALL","MURO","FORD","BADEN","BADÉN","TUNNEL","TUNEL","TÚNEL")
    private fun code(value:String?)=value?.takeIf{it in setOf("12","13","14")} ?: "UNKNOWN"
    @Synchronized fun record(c:Context,stage:String,asset:String,clazz:String?=null,type:String?=null,rawClass:String?=null) {
        if(stage !in stages)return
        runCatching {
            val prefs=c.getSharedPreferences("sic20_draft_trace_111",Context.MODE_PRIVATE)
            val old=JSONArray(prefs.getString("events","[]"));val events=JSONArray()
            for(n in maxOf(0,old.length()-63)until old.length())events.put(old.getJSONObject(n))
            val seq=prefs.getLong("seq",0)+1
            events.put(JSONObject().put("seq",seq).put("stage",stage).put("asset",asset.takeIf{it in assets} ?: "UNKNOWN")
                .put("class",code(clazz)).put("type",type?.takeIf{it.matches(Regex("[0-9]{1,2}"))} ?: "UNKNOWN").put("rawClass",code(rawClass)))
            prefs.edit().putLong("seq",seq).putString("events",events.toString()).apply()
        }
    }
    fun report(c:Context):JSONObject=runCatching{JSONObject().put("scope","TEMPORARY_SIC20_111")
        .put("events",JSONArray(c.getSharedPreferences("sic20_draft_trace_111",Context.MODE_PRIVATE).getString("events","[]")))}
        .getOrElse{JSONObject().put("available",false)}
}
