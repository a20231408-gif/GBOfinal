package com.tuempresa.inventariovial.core

import com.tuempresa.inventariovial.BuildConfig
import org.json.JSONObject

/** Filters requirements, never rewrites a frozen preparation, its hash or its receipt. */
object SicContributionPolicy {
    fun enabled(format:String,a:Boolean=BuildConfig.SIC17A_ENABLED,b:Boolean=BuildConfig.SIC17B_ENABLED)=when(format){"SIC-17A"->a;"SIC-17B"->b;else->true}
    fun pending(prepared:JSONObject?,a:Boolean=BuildConfig.SIC17A_ENABLED,b:Boolean=BuildConfig.SIC17B_ENABLED):List<String>{
        val rows=prepared?.optJSONArray("pendingSic") ?: return emptyList()
        return (0 until rows.length()).map{rows.optString(it)}.filter{enabled(it.substringBefore(':').trim(),a,b)}
    }
    fun disabledPending(prepared:JSONObject?):List<String>{
        val rows=prepared?.optJSONArray("pendingSic") ?: return emptyList()
        return (0 until rows.length()).map{rows.optString(it)}.filterNot{enabled(it.substringBefore(':').trim())}
    }
}
