package com.tuempresa.inventariovial.delivery

import android.content.Context
import android.util.Base64
import androidx.work.ListenableWorker
import androidx.work.workDataOf
import com.tuempresa.inventariovial.DriveConfig
import com.tuempresa.inventariovial.DriveUploadPolicy
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal interface DeliveryTransport {
    fun supportsDelivery(): Boolean
    fun post(payload: JSONObject): JSONObject
}

object DeliverySync {
    suspend fun run(context: Context, id: String): ListenableWorker.Result {
        DriveUploadPolicy.configurationError()?.let { return ListenableWorker.Result.failure(workDataOf("error" to it)) }
        com.tuempresa.inventariovial.core.DriveSyncClock.attempt(context)
        val result = sync(InventoryDatabase.getInstance(context), id, HttpDeliveryTransport()) { file,ack ->
            com.tuempresa.inventariovial.core.DriveReceiptStore.file(context,file.canonicalPath,ack.getString("sha256"),ack.getString("fileId"))
        }
        if (result is ListenableWorker.Result.Success) com.tuempresa.inventariovial.core.DriveSyncClock.success(context)
        return result
    }
    internal suspend fun sync(db: InventoryDatabase, id: String, transport: DeliveryTransport, onAck: ((File,JSONObject)->Unit)?=null): ListenableWorker.Result = withContext(Dispatchers.IO) {
        val dao = db.inventoryDao()
        val batch = dao.delivery(id) ?: return@withContext ListenableWorker.Result.failure()
        if(batch.status=="SYNCED") return@withContext ListenableWorker.Result.success()
        if(batch.status !in setOf("QUEUED","ERROR","UPLOADING")) return@withContext ListenableWorker.Result.failure()
        var uploadingPhoto: String? = null
        try {
            if (!transport.supportsDelivery()) {
                val message = "Actualiza la implementación Apps Script: se requiere deliveryProtocol 1. No se enviaron archivos."
                dao.deliveryStatus(id, "BLOCKED", message)
                return@withContext ListenableWorker.Result.failure(workDataOf("error" to message))
            }
            val manifest = JSONObject(File(batch.manifestPath).readText())
            require(manifest.getString("deliveryId") == id && manifest.getString("fingerprint") == batch.fingerprint)
            val root = File(batch.manifestPath).parentFile!!
            suspend fun verifyCurrent() {
                val records=dao.activeSnapshots().filter { com.tuempresa.inventariovial.road.normalizedRoadCode(it.record.routeCode)==batch.routeCode && it.record.sicCode!="SCAP" }
                require(RouteDelivery.fingerprint(records,DeliveryNamingPolicy.valueOf(batch.namingPolicy))==batch.fingerprint) {
                    "El inventario cambió o fue anulado después de su revisión."
                }
            }
            verifyCurrent()
            dao.deliveryStatus(id, "UPLOADING", null)
            val entries = manifest.getJSONArray("files")
            for (i in 0 until entries.length()) {
                verifyCurrent()
                val entry = entries.getJSONObject(i)
                val file = File(root, entry.getString("localFile"))
                require(file.canonicalFile.parentFile == root.canonicalFile)
                val payload = JSONObject(entry.getJSONObject("payload").toString())
                val bytes = file.readBytes()
                require(sha256(bytes) == payload.getString("sha256")) { "La copia de entrega cambió. No se enviará." }
                payload.put("action", "uploadDeliveryFile").put("protocol", 1)
                    .put("deliveryId", id).put("fingerprint", batch.fingerprint)
                    .put("base64", Base64.encodeToString(bytes, Base64.NO_WRAP))
                uploadingPhoto = payload.optString("photoId").takeIf { payload.getString("kind") == "photo" }
                uploadingPhoto?.let { dao.setPhotoSyncStatus(it, "UPLOADING") }
                val response = transport.post(payload)
                if (!response.optBoolean("ok") || response.optString("sha256") != payload.getString("sha256") || response.optString("fileId").isBlank() || response.optString("folderId").isBlank() || response.optInt("protocol")!=1 || response.optString("deliveryId")!=id || response.optString("fileName")!=payload.getString("fileName")) {
                    val retryable = response.optBoolean("retryable", false)
                    val message = if (retryable) "Drive pendiente de reintento." else "Drive rechazó la entrega. Revisa configuración o conflicto de versión."
                    if (payload.getString("kind") == "photo") dao.setPhotoSyncStatus(payload.getString("photoId"), "ERROR")
                    dao.deliveryStatus(id, if (retryable) "ERROR" else "BLOCKED", message)
                    return@withContext if (retryable) ListenableWorker.Result.retry() else ListenableWorker.Result.failure(workDataOf("error" to message))
                }
                onAck?.invoke(file,response)
                uploadingPhoto?.let { dao.completeDeliveryPhotoUpload(it, response.getString("fileId")) }
                uploadingPhoto = null
            }
            verifyCurrent()
            dao.deliveryStatus(id, "SYNCED", null)
            ListenableWorker.Result.success()
        } catch (_: IllegalArgumentException) {
            val message = "La copia preparada o su hash no son válidos. No se continúa la entrega."
            dao.deliveryStatus(id, "BLOCKED", message)
            ListenableWorker.Result.failure(workDataOf("error" to message))
        } catch (_: Exception) {
            uploadingPhoto?.let { dao.setPhotoSyncStatus(it, "ERROR") }
            dao.deliveryStatus(id, "ERROR", "No se completó la entrega. Se conservaron las copias para reintentar.")
            ListenableWorker.Result.retry()
        }
    }
}

private class HttpDeliveryTransport : DeliveryTransport {
    private val client = OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).readTimeout(120, TimeUnit.SECONDS).writeTimeout(120, TimeUnit.SECONDS).build()
    override fun post(payload: JSONObject): JSONObject {
        val request = Request.Builder().url(DriveConfig.WEB_APP_URL)
            .post(JSONObject(payload.toString()).put("token", DriveConfig.API_TOKEN).toString().toRequestBody("application/json; charset=utf-8".toMediaType())).build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) json("ok" to false, "retryable" to true)
            else JSONObject(response.body?.string() ?: "{}")
        }
    }
    override fun supportsDelivery(): Boolean {
        return client.newCall(Request.Builder().url(DriveConfig.WEB_APP_URL).get().build()).execute().use {
            check(it.isSuccessful) { "Servidor no disponible." }
            JSONObject(it.body?.string() ?: "{}").optInt("deliveryProtocol") == 1
        }
    }
}
