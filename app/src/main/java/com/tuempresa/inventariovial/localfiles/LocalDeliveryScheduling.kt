package com.tuempresa.inventariovial.localfiles

import android.content.Context
import androidx.room.withTransaction
import com.tuempresa.inventariovial.core.AutomaticSync
import com.tuempresa.inventariovial.data.database.InventoryDatabase

/** Schedule original record contracts, never a tablet's route-wide workbook. */
object LocalDeliveryScheduling {
    suspend fun afterExport(c:Context,db:InventoryDatabase,kind:String,id:String){
        val enqueue=db.withTransaction {
            if(DeliveryDestination.deviceOnly(db,"$kind/$id"))return@withTransaction false
            val closed=when(kind){"SCAP"->db.scapDao().inspection(id)?.status=="COMPLETE";"SIC"->db.inventoryDao().recordById(id)?.status=="ACTIVE";"PAVEMENT"->db.pavementDao().sample(id)?.status=="CLOSED";else->false}
            if(!closed)return@withTransaction false
            val revision=DeliveryDestination.revision(db,kind,id) ?: return@withTransaction false
            // Read inside the SAME transaction: never restore a pre-ACK/pre-upload snapshot.
            val latest=db.automaticSyncDao().get("$kind/$id")
            if(latest==null || latest.revision<revision)AutomaticSync.request(db,kind,id,revision,
                kind=="SCAP"&&AutomaticSync.scapWarningConsent(db,id),"LOCAL_EXPORT")
            else if(AutomaticSync.compatibilityPending(latest))db.automaticSyncDao().put(latest.copy(state="QUEUED",error=null,attempts=0))
            val now=db.automaticSyncDao().get("$kind/$id")
            now!=null&&now.state !in setOf("SYNCED","CONFLICT","BLOCKED")
        }
        if(enqueue)AutomaticSync.enqueue(c)
    }
}
