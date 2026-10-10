package com.tuempresa.inventariovial.sic

import com.tuempresa.inventariovial.data.entity.DeliveryBatchEntity
import com.tuempresa.inventariovial.data.entity.InventoryRecordEntity
import org.json.JSONObject
import java.io.File

/** Reads the existing batch ACK result. A local export or PREPARED batch is never synced. */
object SicDeliveryStatus {
    fun acknowledged(batches:List<DeliveryBatchEntity>,records:List<InventoryRecordEntity>):Set<Pair<String,String>> {
        val byId=records.associateBy {it.id}
        return batches.filter {it.status=="SYNCED"}.flatMap {batch->runCatching {
            val manifest=JSONObject(File(batch.manifestPath).readText())
            require(manifest.getString("deliveryId")==batch.id && manifest.getString("fingerprint")==batch.fingerprint)
            val entries=manifest.getJSONArray("files")
            (0 until entries.length()).mapNotNull {index->
                val payload=entries.getJSONObject(index).getJSONObject("payload")
                val record=byId[payload.optString("recordId")]
                if(payload.optString("kind")=="sic" && record!=null && record.updatedAt<=batch.createdAt)
                    record.id to payload.getString("sicCode") else null
            }
        }.getOrDefault(emptyList())}.toSet()
    }
}
