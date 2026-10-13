package com.tuempresa.inventariovial.core

import android.content.Context
import androidx.room.withTransaction
import com.tuempresa.inventariovial.auth.AuditLogEntity
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import java.io.File
import org.json.JSONObject

/** Reversible local discard. No physical deletion, queue removal or HTTP request. */
class LocalDraftDiscard(private val db:InventoryDatabase,private val context:Context) {
    private suspend fun unsubmitted(kind:String,id:String,route:String) {
        require(db.automaticSyncDao().all().none {it.recordId==id}) { "Existe una entrega pendiente o histórica; este registro no es exclusivamente local." }
        require(DriveInventorySchema.receipt(context,id)==null) { "Existe una identidad Drive reservada o confirmada." }
        val prefs=context.getSharedPreferences("drive_receipts_v2",Context.MODE_PRIVATE)
        require(!prefs.contains("sample/$id")) { "Existe una entrega anterior de esta muestra." }
        require(!File(context.filesDir,"automatic_drive/${com.tuempresa.inventariovial.delivery.sha256("$kind/$id".toByteArray())}").exists()) { "Existe una preparación de entrega conservada." }
        require(!File(context.filesDir,"pavement/drive_staging/$id").exists()) { "Existe una preparación de entrega de pavimentos." }
        for(batch in db.inventoryDao().deliveries(route)) {
            val file=File(batch.manifestPath)
            require(file.isFile) { "No se puede comprobar una entrega anterior de esta ruta." }
            val json=JSONObject(file.readText())
            require(!containsIdentity(json,id)) { "El borrador aparece en una entrega de ruta." }
        }
    }
    private fun containsIdentity(value:Any?,id:String):Boolean=when(value) {
        is JSONObject -> value.keys().asSequence().any {containsIdentity(value.opt(it),id)}
        is org.json.JSONArray -> (0 until value.length()).any {containsIdentity(value.opt(it),id)}
        is String -> value==id
        else -> false
    }
    suspend fun change(kind:String,id:String,restore:Boolean=false,userId:String?=null)=db.withTransaction {
        val expected=if(restore) "DISCARDED_LOCAL" else "DRAFT"
        val next=if(restore) "DRAFT" else "DISCARDED_LOCAL"
        when(kind) {
            "SIC" -> {
                val dao=db.inventoryDao();val snapshot=requireNotNull(dao.snapshot(id))
                val record=snapshot.record
                require(record.status==expected && record.sicCode!="SCAP" && db.scapDao().forRoadRecord(id)==null) { "Solo se descartan borradores locales SIC sin inspección SCAP vinculada." }
                require(record.serverSyncStatus=="PENDING" && record.excelSyncStatus=="PENDING") { "Existe una sincronización anterior." }
                require(snapshot.photos.all {it.syncStatus=="PENDING" && it.driveFileId==null && it.driveFolderId==null}) { "Existe evidencia enviada o en proceso." }
                require(com.tuempresa.inventariovial.tracking.TrackCaptureService.activeRecordId.value!=id) { "Finaliza el recorrido antes de descartar." }
                unsubmitted(kind,id,record.routeCode)
                dao.updateRecord(record.copy(status=next,updatedAt=maxOf(System.currentTimeMillis(),record.updatedAt+1)))
            }
            "PAVEMENT" -> {
                val dao=db.pavementDao();val sample=requireNotNull(dao.sample(id))
                require(sample.status==expected && userId!=null && sample.inspectorUserId==userId) { "Solo el inspector creador puede descartar este borrador local." }
                unsubmitted(kind,id,sample.ruta)
                // All samples, damages and original photos remain linked by their original UUIDs.
                dao.saveSample(sample.copy(status=next,updatedAt=maxOf(System.currentTimeMillis(),sample.updatedAt+1)))
            }
            else -> error("Este tipo de borrador no admite descarte local.")
        }
        db.auditDao().insert(AuditLogEntity(userId=userId,action=if(restore) "LOCAL_DRAFT_RESTORED" else "LOCAL_DRAFT_DISCARDED",
            details=JSONObject().put("kind",kind).put("uuid",id).put("device",com.tuempresa.inventariovial.access.DeviceAccessManager(context).fingerprint()).put("originalsPreserved",true).toString()))
    }
}
