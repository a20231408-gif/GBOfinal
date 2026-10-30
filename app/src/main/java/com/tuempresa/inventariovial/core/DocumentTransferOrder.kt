package com.tuempresa.inventariovial.core

import org.json.JSONArray

/** Transfer order only. Never reorders the manifest or changes its contentHash. */
object DocumentTransferOrder {
    fun indices(files:JSONArray):List<Int> = (0 until files.length()).sortedBy {n->
        val f=files.getJSONObject(n)
        when {
            f.optString("mimeType") in setOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet","application/pdf")->0
            f.optString("key").startsWith("photo/")->2
            f.optString("mimeType").startsWith("video/")->3
            else->1
        }
    }
}
