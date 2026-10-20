package com.tuempresa.inventariovial.diagnostics

import com.tuempresa.inventariovial.core.SyncIntentPresentation
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import org.json.JSONArray
import org.json.JSONObject

/** Aggregate counts only. No record IDs, route/user names, file paths, contents or credentials. */
object SyncQueueDiagnostics {
    val states=listOf("DRAFT","QUEUED","UPDATE_PENDING","UPLOADING","ERROR","BLOCKED","CONFLICT","PENDING_VALIDATION","UNCONFIGURED","SYNCED")
    private fun family(kind:String,code:String?)=when(kind) {
        "SIC"->code?.takeIf{it in setOf("SIC-08","SIC-09","SIC-13","SIC-17","SIC-18","SIC-19","SIC-20","SIC-21","SIC-22","SIC-23","SIC-34")} ?: "SIC_OTHER"
        "PAVEMENT"->"PAVEMENT_"+(code?.takeIf{it in setOf("ASPHALT","RIGID","UNSURFACED")} ?: "OTHER")
        "SCAP"->"SCAP";else->"OTHER"
    }
    suspend fun snapshot(db:InventoryDatabase):JSONObject {
        val grouped=linkedMapOf<String,MutableMap<String,Int>>()
        val causes=linkedMapOf<String,Int>()
        var preparedFiles=0;var confirmedFiles=0;var awaitingAckFiles=0;var inputExcelRows=0;var confirmedExcel=0;var unknownFiles=0;var invalidReceipts=0
        for(i in db.automaticSyncDao().all()) {
            val code=when(i.kind){"SIC"->db.inventoryDao().recordById(i.recordId)?.sicCode;"PAVEMENT"->db.pavementDao().sample(i.recordId)?.surface;else->null}
            val counts=grouped.getOrPut(family(i.kind,code)){linkedMapOf()}
            val state=i.state.takeIf{it in states} ?: "OTHER"
            counts[state]=(counts[state] ?: 0)+1
            if(i.error!=null) {
                val code=listOf("BUSY","NOT_JPEG","REVISION_CONFLICT","BASE_REVISION_CONFLICT","STALE_REVISION","ORDERING_REQUIRED","REGISTRY_CAPACITY","WORKBOOK_TEMPLATE_CONFLICT","READABLE_NAMES_DISABLED","HISTORICAL_MIGRATION_REQUIRED")
                    .firstOrNull{i.error.contains("($it)") || i.error.contains("($it;")} ?: if(state=="UNCONFIGURED")"CONFIGURATION" else "OTHER"
                causes[code]=(causes[code] ?: 0)+1
            }
            val confirmed=SyncIntentPresentation.confirmed(i)
            if(state=="SYNCED" && !confirmed)invalidReceipts++
            val p=runCatching{i.preparedJson?.let(::JSONObject)}.getOrNull()
            if(p?.optLong("revision")!=i.revision && !confirmed && state!="DRAFT")unknownFiles++
            if(p?.optLong("revision")==i.revision) {
                preparedFiles+=p.optJSONArray("files")?.length() ?: 0
                if(!confirmed)awaitingAckFiles+=p.optJSONArray("files")?.length() ?: 0
                inputExcelRows+=p.optJSONArray("sicRows")?.length() ?: 0
            }
            if(confirmed)runCatching{JSONObject(i.receiptJson!!)}.getOrNull()?.let {ack->
                confirmedFiles+=ack.optJSONArray("files")?.length() ?: 0
                confirmedExcel+=ack.optJSONArray("sicFiles")?.length() ?: 0
            }
        }
        val local=JSONArray();var unqueued=0;val missingByKind=JSONObject()
        val queries=listOf(
            "SELECT 'SIC' kind,sicCode code,status,COUNT(*) n FROM inventory_records WHERE sicCode!='SCAP' AND status!='DISCARDED_LOCAL' GROUP BY sicCode,status",
            "SELECT 'SCAP' kind,'' code,status,COUNT(*) n FROM scap_inspections GROUP BY status",
            "SELECT 'PAVEMENT' kind,surface code,status,COUNT(*) n FROM pavement_samples GROUP BY surface,status")
        for(sql in queries)db.openHelper.readableDatabase.query(sql).use {c->while(c.moveToNext()) {
            val status=c.getString(2).takeIf{it in setOf("DRAFT","ACTIVE","COMPLETE","CLOSED","OPEN","ANNULLED")} ?: "OTHER"
            local.put(JSONObject().put("family",family(c.getString(0),c.getString(1))).put("localStatus",status).put("records",c.getInt(3)))
        }}
        for((table,kind,closed) in listOf(Triple("inventory_records","SIC","ACTIVE"),Triple("scap_inspections","SCAP","COMPLETE"),Triple("pavement_samples","PAVEMENT","CLOSED"))) {
            val filter=if(kind=="SIC")" AND r.sicCode!='SCAP'" else ""
            db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM $table r WHERE r.status='$closed'$filter AND NOT EXISTS(SELECT 1 FROM drive_sync_outbox o WHERE o.kind='$kind' AND o.recordId=r.id) AND NOT EXISTS(SELECT 1 FROM device_delivery_choices d WHERE d.`key`='$kind/'||r.id AND d.destination='DEVICE')").use {c->if(c.moveToFirst()){unqueued+=c.getInt(0);missingByKind.put(kind,c.getInt(0))}}
        }
        return JSONObject().put("outboxByFamily",JSONArray(grouped.map{(name,counts)->JSONObject().put("family",name).put("states",JSONObject(counts))}))
            .put("localRecords",local).put("validatedWithoutOutbox",unqueued).put("invalidSyncedReceipts",invalidReceipts)
            .put("validatedWithoutOutboxByKind",missingByKind).put("safeErrorCategories",JSONObject(causes))
            .put("preparedFilesCurrentRevision",preparedFiles).put("confirmedFilesCurrentRevision",confirmedFiles)
            .put("knownFilesAwaitingRecordAck",awaitingAckFiles)
            .put("excelRowInputsCurrentRevision",inputExcelRows).put("confirmedExcelReceiptsCurrentRevision",confirmedExcel)
            .put("pendingRecordsWithUnknownFileCount",unknownFiles)
            .put("note","Counts are records versus known prepared files; unprepared totals are unknown; persisted UPLOADING is not proof of a running worker; Excel receipts may refer to the same consolidated file")
    }
}
