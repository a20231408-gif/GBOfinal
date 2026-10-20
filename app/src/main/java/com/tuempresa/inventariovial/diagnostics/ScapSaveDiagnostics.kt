package com.tuempresa.inventariovial.diagnostics

import android.content.Context
import org.json.JSONObject

/** Aggregate local queue evidence only: no UUID, values, images, identity, paths or exception text. */
object ScapSaveDiagnostics {
    @Synchronized fun record(c:Context,elapsed:Long,failed:Int,pending:Int){runCatching{
        val p=c.getSharedPreferences("scap_save_diagnostics",0)
        p.edit().putLong("operations",p.getLong("operations",0)+1).putLong("totalMs",p.getLong("totalMs",0)+elapsed.coerceAtLeast(0))
            .putLong("maxMs",maxOf(elapsed,p.getLong("maxMs",0))).putInt("failedQueue",failed).putInt("pendingQueue",pending.coerceAtLeast(0)).commit()
    }}
    fun report(c:Context)=c.getSharedPreferences("scap_save_diagnostics",0).let{p->JSONObject().put("operations",p.getLong("operations",0)).put("totalMs",p.getLong("totalMs",0)).put("maxMs",p.getLong("maxMs",0)).put("failedQueue",p.getInt("failedQueue",0)).put("pendingQueue",p.getInt("pendingQueue",0)).put("scope","LOCAL_QUEUE_ONLY; not a measurement of connectivity or Google")}
}
