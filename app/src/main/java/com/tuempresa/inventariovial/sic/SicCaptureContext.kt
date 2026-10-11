package com.tuempresa.inventariovial.sic

import android.content.Context
import com.tuempresa.inventariovial.data.entity.FieldSession
import com.tuempresa.inventariovial.field.SurveyPreferences
import org.json.JSONObject

/** Common location from the same active survey, with raw draft text excluded. */
class SicCaptureContext(private val context:Context) {
    private val prefs=context.getSharedPreferences("sic_capture_context",Context.MODE_PRIVATE)
    fun values(session:FieldSession?):Map<String,String> {
        if(session==null) return emptyMap()
        val position=SurveyPreferences(context).last()?.takeIf {it.sessionId==session.sessionId && it.route==session.road && it.roadbed==session.roadbed}
        val saved=runCatching {JSONObject(prefs.getString("completed",null) ?: return@runCatching null)}.getOrNull()
            ?.takeIf {it.optString("sessionId")==session.sessionId && it.optString("routeCode")==session.road && it.optString("roadbedCode")==session.roadbed}
        val reuse=saved?.takeIf {position==null || (it.optString("startPrCode").trimStart('0')==position.pr.trimStart('0') && SicFieldState.number(it.optString("startDistanceM"))==position.distance)}
        if(reuse!=null) return SicFieldRepository.sharedKeys.associateWith {reuse.optString(it)}
        return position?.let {mapOf("startPrCode" to it.pr,"startDistanceM" to it.distance.toString())} ?: emptyMap()
    }
    fun remember(session:FieldSession?,state:SicFieldState) {
        if(session==null || state.format==SicFieldFormat.SIC18A || !state.completion().canFinalize) return
        val saved=JSONObject(state.values.filterKeys {it in SicFieldRepository.sharedKeys}).put("sessionId",session.sessionId)
        prefs.edit().putString("completed",saved.toString()).apply()
    }
}
