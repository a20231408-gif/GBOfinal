package com.tuempresa.inventariovial.core

import org.json.JSONObject

/** Transport metadata only: never changes the canonical revision, UUID, files or ACK. */
internal object RootRoutingEnvelope {
    fun apply(payload:JSONObject, capabilities:JSONObject):JSONObject {
        val out=JSONObject(payload.toString())
        listOf("rootRoutingVersion","rootEpoch").forEach(out::remove)
        val created=out.remove("captureCreatedAt")
        val version=capabilities.optInt("rootRoutingVersion")
        if(version==0)return out // The published V21 receives its original envelope.
        val epoch=capabilities.optString("rootEpoch")
        if(version!=2 || !epoch.matches(Regex("[a-f0-9]{32}")) ||
            capabilities.optLong("rootActivatedAt")<=0)
            throw SyncPending("El destino no tiene un corte compatible acreditado. Cola conservada.")
        // Missing frozen legacy metadata is never replaced by today's timestamp.
        if(created is Number && created.toLong()>0)out.put("captureCreatedAt",created)
        return out.put("rootRoutingVersion",2).put("rootEpoch",epoch)
    }
}
