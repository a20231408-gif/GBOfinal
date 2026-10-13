package com.tuempresa.inventariovial

import com.tuempresa.inventariovial.core.DriveSyncIntent
import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.export.*
import org.json.JSONObject

/** Read-only projections; identity, revisions and contractual columns remain in their existing stores. */
object RecordDetailPresentation {
    fun title(r: InventoryRecordEntity): String =
        com.tuempresa.inventariovial.model.SignalizationType.entries.firstOrNull {it.name==r.assetType}?.title
            ?: com.tuempresa.inventariovial.model.RoadAssetType.entries.firstOrNull {it.name==r.assetType}?.title
            ?: com.tuempresa.inventariovial.dashboard.DashboardDrafts.title(r)

    fun summary(s: InventorySnapshot): List<Pair<String,String>> = runCatching {
        val r=s.record
        val request=com.tuempresa.inventariovial.model.form.InventorySaveRequest(r.sicCode,title(r),r.routeCode,r.roadbedCode,
            r.startPrCode,r.startDistanceM.toString(),r.endPrCode,r.endDistanceM?.toString(),r.sideCode,
            r.latitude ?: Double.NaN,r.longitude ?: Double.NaN,r.gpsAccuracyM?.toFloat(),r.surveyDate,r.observations.orEmpty(),"",
            com.tuempresa.inventariovial.repository.EngineeringEdits.form(s),photoPaths=photos(s.photos,r.id).values.flatten().map {it.localPath},
            segment=r.segment.orEmpty(),direction=r.surveyDirection ?: "INCREASING")
        com.tuempresa.inventariovial.field.captureSummary(request).filterNot {it.first.startsWith("GPS")}
    }.getOrDefault(emptyList())

    fun status(sync: DriveSyncIntent?, active: Boolean): String = when {
        sync?.state == "CONFLICT" -> "Conflicto · edición local conservada"
        !active -> if ((sync?.syncedRevision ?: 0) > 0) "Actualización pendiente · borrador" else "Borrador"
        sync?.state == "ERROR" || sync?.state == "BLOCKED" -> "Error"
        sync?.state == "UPLOADING" -> "Pendiente de confirmación"
        com.tuempresa.inventariovial.core.SyncIntentPresentation.confirmed(sync) -> "Sincronizado"
        (sync?.syncedRevision ?: 0) > 0 -> "Actualización pendiente"
        else -> "Pendiente de sincronización"
    }

    fun photos(photos: List<PhotoEntity>, recordId: String, inspectionId: String? = null) =
        photos.filter { it.recordId == recordId && it.scapInspectionId == inspectionId }
            .sortedWith(compareBy({ it.photoIndex }, { it.id }))
            .groupBy { it.photoCategory?.takeIf(String::isNotBlank) ?: "GENERAL" }

    fun exportRow(s: InventorySnapshot) = InventoryRecordForExport(s.record, s.sic17, s.sic18,
        s.sic19, s.sic20, s.sic21, s.sic22, s.sic23, s.sic18a, s.sicExtension)

    /** Only links from an actual ACK, never an inferred path or a fabricated download. */
    fun remoteFile(sync: DriveSyncIntent?, format: String): String? {
        if (sync == null || sync.syncedRevision <= 0 || sync.lastSyncedAt == null) return null
        return runCatching {
            val receipt = JSONObject(sync.receiptJson ?: return null)
            if (!receipt.optBoolean("ok") || receipt.optString("key") != sync.key || receipt.optLong("revision") != sync.syncedRevision) return null
            val files = receipt.optJSONArray("sicFiles") ?: return null
            (0 until files.length()).map { files.getJSONObject(it) }
                .firstOrNull { it.optString("format") == format }?.optString("fileId")
                ?.takeIf { it.matches(Regex("[A-Za-z0-9_-]+")) }
        }.getOrNull()
    }
}
