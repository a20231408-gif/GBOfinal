package com.tuempresa.inventariovial.localfiles

import androidx.room.withTransaction
import com.tuempresa.inventariovial.core.AutomaticDelivery
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import org.json.JSONObject

/** Read-only evidence, never permission to delete. No HTTP, worker, file or record mutations. */
object RemovalPreview {
    data class Row(val item:LocalInventoryItem,val documents:Int,val remoteIds:Int?,val sync:String,
        val validCurrentAck:Boolean,val sharedOrLinked:Boolean,val revisionChanged:Boolean)
    const val BLOCK="BLOQUEADO: falta inventario completo y autorización administrativa verificable del servidor. No se eliminará nada."
    fun confirmationMatches(text:String,count:Int,secondAdministrativeConfirmation:Boolean)=
        text=="ELIMINAR" && count>0 && (count==1 || secondAdministrativeConfirmation)
    suspend fun inspect(db:InventoryDatabase,items:List<LocalInventoryItem>):List<Row> = db.withTransaction {
        require(items.isNotEmpty() && items.distinctBy{it.key}.size==items.size)
        items.map { item ->
            val files=db.localFilesDao().filesForRecord(item.key)
            val intent=db.automaticSyncDao().get(item.key)
            val valid=runCatching {
                intent!=null && intent.state=="SYNCED" && intent.revision==item.revision && intent.syncedRevision==item.revision &&
                    JSONObject(requireNotNull(intent.preparedJson)).let {p ->
                        p.getString("key")==item.key && p.getLong("revision")==item.revision &&
                            AutomaticDelivery.validAck(p,JSONObject(requireNotNull(intent.receiptJson)))
                    }
            }.getOrDefault(false)
            val ids=if(valid)runCatching {
                val ack=JSONObject(requireNotNull(intent?.receiptJson));val result=mutableSetOf<String>()
                for(name in listOf("files","sicFiles")) {
                    val array=ack.getJSONArray(name)
                    for(n in 0 until array.length())result+=array.getJSONObject(n).getString("fileId")
                };result.size
            }.getOrNull() else null
            val linked=item.kind=="SCAP" || (item.kind=="SIC" &&
                (db.inventoryDao().criticalSicLinks(item.id)>0 || db.scapDao().forRoadRecord(item.id)!=null))
            Row(item,files.size,ids,intent?.state ?: "Sin outbox",valid,
                linked || db.localFilesDao().sharedDraftReferences(item.key)>0 || db.localFilesDao().sharedDraftPhotoReferences(item.key)>0,
                DeliveryDestination.revision(db,item.kind,item.id)!=item.revision)
        }
    }
}
