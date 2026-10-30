package com.tuempresa.inventariovial.pavement

import com.tuempresa.inventariovial.data.database.InventoryDatabase
import org.json.JSONArray
import org.json.JSONObject

object PavementDamageArchive {
    suspend fun manifest(db:InventoryDatabase,s:PavementSampleEntity):JSONObject {
        val records=db.pavementDao().retiredDamages(s.id)
        return JSONObject().put("sampleId",s.id).put("active",false).put("damages",JSONArray(records.map{d->
            JSONObject().put("id",d.id).put("code",d.code).put("severity",d.severity).put("correlativo",d.correlativo)
                .put("createdAt",d.createdAt).put("retiredAt",d.retiredAt).put("observation",d.observation)
                .put("x1",d.x1).put("y1",d.y1).put("x2",d.x2).put("y2",d.y2).put("lengthM",d.lengthM)
                .put("jointMetadata",s.sicDataJson?.let(::JSONObject))
                .put("photos",JSONArray(db.pavementDao().photoHistory(d.id).map{p->JSONObject().put("id",p.id).put("createdAt",p.createdAt).put("path",p.localPath)}))
        }))
    }
}
