package com.tuempresa.inventariovial.scap.export

import android.content.Context
import android.util.Base64
import android.util.Base64OutputStream
import android.os.SystemClock
import android.util.Log
import com.tuempresa.inventariovial.BuildConfig
import androidx.room.withTransaction
import androidx.work.*
import com.tuempresa.inventariovial.DriveConfig
import com.tuempresa.inventariovial.DriveUploadPolicy
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.scap.data.ScapFieldValueEntity
import com.tuempresa.inventariovial.scap.data.ScapInspectionSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okio.BufferedSink
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

object ScapDriveSync {
    internal fun reviewForUpload(snapshot:ScapInspectionSnapshot,catalog:com.tuempresa.inventariovial.scap.catalog.ScapCatalog,warningsConfirmed:Boolean):com.tuempresa.inventariovial.scap.domain.ScapCompleteness {
        val completion=com.tuempresa.inventariovial.scap.domain.ScapCompletion.assess(snapshot,catalog)
        require(completion.pending.none{it.severity==com.tuempresa.inventariovial.scap.domain.ScapIssueSeverity.BLOCKING}) {"Corrige los valores inválidos antes de subir."}
        require(completion.pending.isEmpty() || warningsConfirmed) {"Confirma la subida con datos pendientes."}
        return completion
    }
    suspend fun enqueue(context: Context, snapshot: ScapInspectionSnapshot, reviewedLayout: ScapHumanLayout? = null,warningsConfirmed:Boolean=false) {
        DriveUploadPolicy.requireConfiguration()
        val db = InventoryDatabase.getInstance(context)
        val inspection = snapshot.inspection
        val catalog=com.tuempresa.inventariovial.scap.catalog.ScapCatalog.load(context).forSnapshot(snapshot)
        val completion=reviewForUpload(snapshot,catalog,warningsConfirmed)
        val layout = if(reviewedLayout!=null)ScapHumanDelivery.prepare(context,db,snapshot) else null
        require(reviewedLayout == layout) {"El orden cambió; prepara y revisa nuevamente la entrega SCAP."}
        val request = OneTimeWorkRequestBuilder<ScapDriveWorker>()
            .setInputData(workDataOf("inspectionId" to inspection.id))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS).build()
        // APPEND_OR_REPLACE retains edits queued while an earlier revision is being sent.
        // A second job is harmless: it exits if the current revision has a verified ACK.
        db.withTransaction {
            require(db.inventoryDao().recordById(inspection.roadRecordId)?.status in setOf("ACTIVE","DRAFT")) {"Comprueba que el registro padre no esté anulado."}
            require(db.scapDao().inspection(inspection.id)?.updatedAt == inspection.updatedAt) {"La ficha cambió; prepara nuevamente la entrega."}
            if(layout!=null)db.scapDao().putValue(ScapFieldValueEntity(inspection.id, "delivery", "layout", layout.json().toString(), "REVIEWED"))
            else db.scapDao().deleteValue(inspection.id,"delivery","layout")
            db.scapDao().putValue(ScapFieldValueEntity(inspection.id,"delivery","reviewed","true","REVIEWED"))
            db.scapDao().putValue(ScapFieldValueEntity(inspection.id,"completion","status",if(completion.pending.isEmpty())"COMPLETE" else "INCOMPLETE","REVIEWED"))
            db.scapDao().putValue(ScapFieldValueEntity(inspection.id,"completion","uploadedWithWarnings",completion.pending.isNotEmpty().toString(),"REVIEWED"))
            db.scapDao().putValue(ScapFieldValueEntity(inspection.id, "delivery", "revision", inspection.updatedAt.toString(), "REVIEWED"))
            db.scapDao().syncState(inspection.id, inspection.updatedAt, "QUEUED")
        }
        try {
            withContext(Dispatchers.IO) {
                WorkManager.getInstance(context).enqueueUniqueWork("scap-drive-${inspection.id}",
                    ExistingWorkPolicy.APPEND_OR_REPLACE, request).result.get()
            }
        } catch (e: Exception) {
            db.scapDao().syncState(inspection.id, inspection.updatedAt, "ERROR")
            throw e
        }
    }

    fun validAck(json: JSONObject, s: ScapInspectionSnapshot, hash: String, expectedLogicalPath:String?=s.values("delivery")["logicalPath"]): Boolean {
        val files = json.optJSONArray("files") ?: return false
        val paths = (0 until files.length()).map {files.optJSONObject(it) ?: return false}
        val layout = ScapHumanLayout.saved(s)
        val expected = setOf("SCAP.xlsx", "manifest.json") +
            (layout?.photoPaths?.values ?: s.photos.filter{it.localPath.isNotBlank()}.map {"fotos/${it.id}.jpg"}) +
            (layout?.sketchPaths?.values ?: s.sketches.map {"croquis/${it.id}.png"})
        return paths.size == expected.size && paths.map {it.optString("path")}.toSet() == expected &&
            paths.all {it.optString("fileId").isNotBlank() && it.optString("folderId").isNotBlank()} &&
            json.optBoolean("ok") && json.optInt("protocol") == (if(layout == null) 1 else 2) && json.optString("recordKind") == "SCAP" &&
            (layout == null || ((if(json.optInt("pathVersion")==2) expectedLogicalPath!=null && json.optString("logicalPath")==expectedLogicalPath && json.optString("path")==expectedLogicalPath else json.optString("path") == layout.path) && json.optString("bridgeFolderId").isNotBlank())) &&
            json.optString("scapInspectionId") == s.inspection.id && json.optLong("revision") == s.inspection.updatedAt &&
            json.optString("sha256") == hash && json.optString("fileId").isNotBlank() &&
            json.optString("folderId").isNotBlank()
    }

    internal suspend fun authorized(db:InventoryDatabase,s:ScapInspectionSnapshot):Boolean {
        val current=db.scapDao().snapshot(s.inspection.id) ?: return false
        return current.inspection.updatedAt==s.inspection.updatedAt &&
            db.inventoryDao().recordById(current.inspection.roadRecordId)?.status in setOf("ACTIVE","DRAFT") &&
            ((current.inspection.status=="COMPLETE" && ScapHumanLayout.saved(current)!=null) || current.values("delivery")["reviewed"]=="true") && current.values("delivery")["revision"]==current.inspection.updatedAt.toString()
    }
    suspend fun acknowledge(db: InventoryDatabase, s: ScapInspectionSnapshot, ack: JSONObject, hash: String, expectedLogicalPath:String?=s.values("delivery")["logicalPath"]): Boolean {
        require(validAck(ack, s, hash,expectedLogicalPath)) {"El servidor no confirmó la entrega SCAP."}
        return db.withTransaction {
            val i = s.inspection
            if(db.inventoryDao().recordById(i.roadRecordId)?.status !in setOf("ACTIVE","DRAFT") || !authorized(db,s)) return@withTransaction false
            if (db.scapDao().syncState(i.id, i.updatedAt, "SYNCED") != 1) false else {
                for ((key, value) in mapOf("revision" to i.updatedAt.toString(), "sha256" to hash,
                    "fileId" to ack.getString("fileId"), "folderId" to ack.getString("folderId"))) {
                    db.scapDao().putValue(ScapFieldValueEntity(i.id, "drive", key, value, "SERVER_ACK"))
                }
                val files=ack.getJSONArray("files")
                for(photo in s.photos.filter{it.localPath.isNotBlank()}) {
                    val path=ScapHumanLayout.saved(s)?.photoPaths?.getValue(photo.id) ?: "fotos/${photo.id}.jpg"
                    val file=(0 until files.length()).map {files.getJSONObject(it)}.single {it.getString("path")==path}
                    db.scapDao().photoSynced(i.id,photo.id,file.getString("folderId"),file.getString("fileId"))
                }
                true
            }
        }
    }
}

class ScapDriveWorker(context: Context, parameters: WorkerParameters): CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val id = inputData.getString("inspectionId") ?: return@withContext Result.failure()
        val db = InventoryDatabase.getInstance(applicationContext)
        val dao = db.scapDao()
        val snapshot = dao.snapshot(id) ?: return@withContext Result.failure()
        val revision = snapshot.inspection.updatedAt
        if (snapshot.inspection.syncStatus == "SYNCED") return@withContext Result.success()
        suspend fun state(value: String, message: String = "") {
            db.withTransaction {
                if (dao.syncState(id, revision, value) == 1)
                    dao.putValue(ScapFieldValueEntity(id, "drive", "message", message, "SYNC"))
            }
        }
        if (DriveUploadPolicy.configurationError() != null) {
            state("ERROR", "Drive no configurado."); return@withContext Result.failure()
        }
        try {
            require(ScapDriveSync.authorized(db,snapshot)) {"La entrega requiere un registro activo y una revisión vigente."}
            state("UPLOADING")
            val layout = ScapHumanLayout.saved(snapshot)
            require(layout == null || snapshot.values("delivery")["revision"] == revision.toString()) {"La entrega requiere una nueva revisión."}
            val file = ScapDelivery.bundle(applicationContext, snapshot, layout)
            val hash = ScapDelivery.sha256(file)
            require(ScapDriveSync.authorized(db,snapshot)) {"La ficha cambió o fue anulada antes del envío."}
            val response = ScapDriveClient.upload(snapshot, file, hash)
            val logicalPath=if(layout!=null) com.tuempresa.inventariovial.core.DrivePaths.bridge(applicationContext,snapshot) else null
            val ackValid = ScapDriveSync.validAck(response, snapshot, hash,logicalPath)
            if (BuildConfig.DEBUG) {
                Log.d("ScapDrive",
                    "ackOk=${response.optBoolean("ok")} ackValid=$ackValid retryable=${response.optBoolean("retryable", false)} " +
                        "stage=${response.optString("stage")} fileCount=${response.optInt("fileCount", -1)} error=${response.optString("error").take(160)}")
                Log.d("ScapDrive",
                    "drivePath=${response.optString("path")} configuredRootFolderId=${response.optString("configuredRootFolderId")} " +
                        "scapRootFolderId=${response.optString("scapRootFolderId")} routeFolderId=${response.optString("routeFolderId")} " +
                        "sibFolderId=${response.optString("sibFolderId")} bridgeFolderId=${response.optString("bridgeFolderId")} " +
                        "folderId=${response.optString("folderId")} fileId=${response.optString("fileId")}")
            }
            if (!ackValid) {
                val retry = response.optBoolean("retryable", false)
                state(if (retry) "QUEUED" else "ERROR", if (retry) "Drive ocupado; se reintentará automáticamente." else
                    "El servidor no confirmó SCAP. Comprueba que publicaste el script actualizado y su configuración.")
                return@withContext if (retry) Result.retry() else Result.failure()
            }
            val current = ScapDriveSync.acknowledge(db, snapshot, response, hash,logicalPath)
            // Also remove an acknowledged old revision after concurrent local edits.
            com.tuempresa.inventariovial.core.DriveReceiptStore.file(applicationContext,file.absolutePath,hash,response.getString("fileId"))
            if (current) {
                state("SYNCED")
                if (BuildConfig.DEBUG) Log.d("ScapDrive", "syncStatus=SYNCED revision=$revision protocol=${if(layout==null)1 else 2}")
                // The remote immutable package is complete. Capture originals remain untouched.
                Result.success()
            } else {
                if (BuildConfig.DEBUG) Log.d("ScapDrive", "ackValid=true newerRevisionPending=true revision=$revision")
                Result.retry() // A newer saved revision still needs delivery.
            }
        } catch (e: CancellationException) { throw e }
        catch (e: IOException) {
            state("QUEUED", "Conexión interrumpida; se reintentará automáticamente."); Result.retry()
        } catch (e: Exception) {
            state("ERROR", "No se pudo preparar SCAP. Revisa los archivos locales y los pendientes de exportación.")
            Result.failure()
        }
    }
}

internal object ScapDriveClient {
    private val client = OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(360, TimeUnit.SECONDS).writeTimeout(360, TimeUnit.SECONDS).callTimeout(420, TimeUnit.SECONDS).build()
    suspend fun upload(snapshot: ScapInspectionSnapshot, file: File, hash: String): JSONObject = withContext(Dispatchers.IO) {
        DriveUploadPolicy.requireConfiguration()
        val layout = ScapHumanLayout.saved(snapshot)
        val body = JSONObject().put("token", DriveConfig.API_TOKEN).put("action", "uploadScap")
            .put("recordKind", "SCAP").put("protocol", if(layout == null) 1 else 2).put("scapInspectionId", snapshot.inspection.id)
            .put("revision", snapshot.inspection.updatedAt).put("bridgeCode", snapshot.inspection.bridgeCode)
            .put("bridgeName", snapshot.inspection.bridgeName).put("sha256", hash)
        layout?.let {body.put("delivery", it.json())}
        val request = Request.Builder().url(DriveConfig.WEB_APP_URL).post(ScapUploadBody(body, file)).build()
        val started = SystemClock.elapsedRealtime()
        try { client.newCall(request).execute().use {r ->
            if (!r.isSuccessful) throw IOException("Respuesta no disponible")
            runCatching {JSONObject(r.body?.string().orEmpty())}.getOrElse {throw IOException("Respuesta inválida")}
        } } finally {
            if (BuildConfig.DEBUG) Log.d("ScapDrive", "httpElapsedMs=${SystemClock.elapsedRealtime()-started} protocol=${if(layout==null)1 else 2} zipBytes=${file.length()}")
        }
    }
}

/** Stream base64 from disk instead of holding the complete package twice in memory. Never logged. */
internal class ScapUploadBody(metadata: JSONObject, private val file: File): RequestBody() {
    private val prefix = metadata.toString().dropLast(1) + ",\"base64\":\""
    override fun contentType() = "application/json; charset=utf-8".toMediaType()
    override fun contentLength() = prefix.toByteArray(Charsets.UTF_8).size + ((file.length() + 2) / 3) * 4 + 2
    override fun writeTo(sink: BufferedSink) {
        sink.writeUtf8(prefix)
        Base64OutputStream(sink.outputStream(), Base64.NO_WRAP or Base64.NO_CLOSE).use {out -> file.inputStream().use {it.copyTo(out)}}
        sink.writeUtf8("\"}")
    }
}
