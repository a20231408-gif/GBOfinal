package com.tuempresa.inventariovial.pavement

import android.content.Context
import android.util.Base64
import com.tuempresa.inventariovial.DriveConfig
import com.tuempresa.inventariovial.DriveUploadPolicy
import com.tuempresa.inventariovial.camera.RequiredWatermark
import com.tuempresa.inventariovial.delivery.sha256
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Adapter del protocolo de pavimentos: ruta CORE v2 con compatibilidad del servidor v1.
 */
object PavementDriveUploader {
    private val uploadMutex=Mutex()
    private data class UploadFile(
        val category: String,
        val file: File,
        val mimeType: String,
        val photoId: String? = null,
        val damageId: String? = null
    )

    data class Result(val uploaded: Int, val folderPath: String, val message: String)

    suspend fun uploadSample(
        context: Context,
        repo: PavementRepository,
        sample: PavementSampleEntity,
        damages: List<PavementDamageEntity>
    ): Result = uploadMutex.withLock { withContext(Dispatchers.IO) {
        DriveUploadPolicy.requireConfiguration()
        require(sample.status == "CLOSED") { "Cierra la muestra antes de enviarla a Drive." }
        require(sample.routeReady()) { "Completa Ruta y Tramo antes de sincronizar pavimentos." }

        val surface = PavementSurface.valueOf(sample.surface)
        val surfaceCode = when (surface) {
            PavementSurface.ASPHALT -> "ASF"
            PavementSurface.RIGID -> "RIG"
            PavementSurface.UNSURFACED -> "NPV"
        }
        val sampleNo = sample.muestra.coerceAtLeast(1)
        val sourceStamp=com.tuempresa.inventariovial.core.DriveReceiptStore.stamp(context,sample)
        val directory = File(context.filesDir, "pavement/drive_staging/${sample.id}/${sha256(sourceStamp.toByteArray()).take(16)}").apply {
            check(isDirectory || mkdirs()) { "No se pudo preparar la entrega de pavimentos." }
        }
        val uploads = mutableListOf<UploadFile>()
        val preparedManifest=File(directory,"prepared.json")
        if(preparedManifest.isFile) {
            val rows=JSONObject(preparedManifest.readText()).getJSONArray("files")
            for(i in 0 until rows.length()) {
                val row=rows.getJSONObject(i);val file=File(directory,row.getString("fileName"))
                require(file.canonicalFile.parentFile==directory.canonicalFile && sha256(file.readBytes())==row.getString("sha256")) { "La copia preparada cambió." }
                uploads+=UploadFile(row.getString("category"),file,row.getString("mimeType"),
                    if(row.isNull("photoId"))null else row.getString("photoId"),if(row.isNull("damageId"))null else row.getString("damageId"))
            }
        } else {
        val calculator = PavementCalculator(context)
        val photosByDamage = damages.associate { it.id to repo.photos(it.id) }
        val current = PavementExportSample(sample, damages, calculator.calculate(sample, damages))

        // Hoja de campo / reporte de la muestra.
        val pdf = File(directory, "HC_${sampleNo.toString().padStart(3, '0')}.pdf")
        pdf.outputStream().use { PavementPdf.write(it, current, photosByDamage) }
        uploads += UploadFile("FIELD", pdf, "application/pdf")

        // Base nativa sin rutas privadas del dispositivo.
        val native = File(directory, "M_${sampleNo.toString().padStart(3, '0')}.json")
        native.writeText(nativeJson(sample, damages, current.result, photosByDamage).toString(2))
        uploads += UploadFile("SAMPLE_DATA", native, "application/json")

        // Excel por ruta/tramo/superficie. Conserva Hoja Fallas con las 13 columnas del formato de trabajo.
        val groupSamples = repo.samples().first().filter {
            it.ruta == sample.ruta && it.tramo == sample.tramo && it.surface == sample.surface && it.status == "CLOSED"
        }
        val groupData = groupSamples.map { row ->
            val ds = repo.damages(row.id).first()
            PavementExportSample(row, ds, calculator.calculate(row, ds))
        }
        val excel = File(directory, "PAV_${surfaceCode}.xlsx")
        excel.outputStream().use { PavementExcel.write(it, groupData, repo.sections().first()) }
        uploads += UploadFile("GROUP_DATA", excel, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")

        // Copia limpia; los metadatos acompañan a la imagen en JSON.
        var sequence = 0
        damages.sortedBy { it.correlativo }.forEach { damage ->
            photosByDamage[damage.id].orEmpty().sortedBy { it.createdAt }.forEach { photo ->
                sequence++
                val marked = com.tuempresa.inventariovial.core.CleanPhotoPolicy.original(
                    com.tuempresa.inventariovial.core.PhotoForDelivery(photo.id,photo.localPath,
                        com.tuempresa.inventariovial.core.PhotoCategory.GENERAL,"{}"))
                val name = "%03d_D%03d.jpg".format(sequence, damage.code)
                val copy = File(directory, name)
                marked.copyTo(copy, overwrite = true)
                uploads += UploadFile("PHOTO", copy, "image/jpeg", photo.id, damage.id)
            }
        }

        PavementGeneratedFiles.records(context).filter { it.sampleId==sample.id && it.category.startsWith("SIC_") }.groupBy { it.category }.values.forEach { versions ->
            val latest=versions.maxBy { it.createdAt };val f=File(latest.localPath)
            if(f.isFile) {
                val copy=File(directory,"${latest.category.removePrefix("SIC_")}.xlsx")
                f.copyTo(copy,overwrite=true)
                uploads+=UploadFile(latest.category,copy,latest.mimeType)
            }
        }
        val json=JSONObject().put("files",JSONArray(uploads.map { item ->
            JSONObject().put("category",item.category).put("fileName",item.file.name).put("mimeType",item.mimeType)
                .put("sha256",sha256(item.file.readBytes())).put("photoId",item.photoId?:JSONObject.NULL).put("damageId",item.damageId?:JSONObject.NULL)
        }))
        val atomic=android.util.AtomicFile(preparedManifest);val out=atomic.startWrite()
        try {out.write(json.toString().toByteArray());atomic.finishWrite(out)}catch(e:Exception){atomic.failWrite(out);throw e}
        }
        val revision=sha256(uploads.joinToString(";") { it.category+":"+it.file.name+":"+sha256(it.file.readBytes()) }.toByteArray()).take(8)
        val preserved = uploads.associateWith { item -> PavementGeneratedFiles.preservePrepared(context,sample,item.category,item.mimeType,item.file,item.photoId,item.damageId) }
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build()
        var uploaded = 0
        var pathV2Confirmed=true
        suspend fun verifyCurrent() {
            val latest=repo.samples().first().firstOrNull { it.id==sample.id }
            require(latest?.status=="CLOSED" && latest.updatedAt==sample.updatedAt &&
                com.tuempresa.inventariovial.core.DriveReceiptStore.stamp(context,latest)==sourceStamp) { "La muestra cambió durante el envío. Se conservaron los archivos para reintentar." }
        }
        uploads.forEach { item ->
            verifyCurrent()
            val bytes = item.file.readBytes()
            val payload = JSONObject()
                .put("token", DriveConfig.API_TOKEN)
                .put("action", "uploadPavementFile")
                .put("protocol", 1).put("pathVersion",2)
                .put("logicalPath",com.tuempresa.inventariovial.core.DrivePaths.pavement(context,sample,if(item.category=="PHOTO") "GENERAL" else null))
                .put("photoPolicy","ORIGINAL_CLEAN")
                .put("routeCode", sample.ruta.trim())
                .put("tramo", sample.tramo.trim())
                .put("surface", sample.surface)
                .put("sampleNo", sampleNo)
                .put("sampleId", sample.id)
                .put("revision", revision)
                .put("category", item.category)
                .put("fileName", item.file.name)
                .put("mimeType", item.mimeType)
                .put("sha256", sha256(bytes))
                .put("base64", Base64.encodeToString(bytes, Base64.NO_WRAP))
            item.photoId?.let { payload.put("photoId", it) }
            item.damageId?.let { payload.put("damageId", it) }
            val request = Request.Builder().url(DriveConfig.WEB_APP_URL)
                .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()
            val response = client.newCall(request).execute().use { http ->
                require(http.isSuccessful) { "Drive respondió HTTP ${http.code}." }
                JSONObject(http.body?.string() ?: "{}")
            }
            require(response.optBoolean("ok")) { response.optString("error", "Drive rechazó el archivo.") }
            require(response.optString("sha256") == payload.getString("sha256")) { "Drive confirmó un hash distinto." }
            require(response.optString("fileId").isNotBlank() && response.optString("folderId").isNotBlank() && response.optString("fileName") == item.file.name) { "ACK incompleto de pavimentos." }
            require(response.optInt("protocol")==1) { "ACK con protocolo incorrecto." }
            if(response.optInt("pathVersion")==2) require(response.optString("logicalPath")==payload.getString("logicalPath") && response.optString("sampleId")==sample.id && response.optString("revision")==revision) { "ACK de otra muestra o revisión." }
            com.tuempresa.inventariovial.core.DriveReceiptStore.file(context,preserved.getValue(item).localPath,payload.getString("sha256"),response.getString("fileId"))
            pathV2Confirmed=pathV2Confirmed && response.optInt("pathVersion")==2 && response.optString("logicalPath")==payload.getString("logicalPath")
            uploaded++
        }
        val displaySurface = when (surface) {
            PavementSurface.ASPHALT -> "ASFALTICO"
            PavementSurface.RIGID -> "RIGIDO"
            PavementSurface.UNSURFACED -> "NO_PAVIMENTADO"
        }
        val path = if(pathV2Confirmed) com.tuempresa.inventariovial.core.DrivePaths.pavement(context,sample) else "${sample.ruta}/EVALUACION_SUPERFICIAL/${sample.tramo}/$displaySurface"
        verifyCurrent()
        com.tuempresa.inventariovial.core.DriveReceiptStore.sample(context,sample,"SYNCED")
        Result(uploaded, path, "$uploaded archivos sincronizados. Carpeta: $path")
    } }

    private fun PavementSampleEntity.routeReady() = ruta.isNotBlank() && tramo.isNotBlank()

    private fun nativeJson(
        sample: PavementSampleEntity,
        damages: List<PavementDamageEntity>,
        result: PavementResult,
        photos: Map<String, List<PavementDamagePhotoEntity>>
    ) = JSONObject()
        .put("schema", 1)
        .put("sample", JSONObject()
            .put("id", sample.id).put("tramo", sample.tramo).put("ruta", sample.ruta)
            .put("muestra", sample.muestra).put("carril", sample.carril).put("calzada", sample.calzada)
            .put("prInicial", sample.prInicial).put("prFinal", sample.prFinal)
            .put("anchoM", sample.anchoM).put("largoM", sample.largoM).put("surface", sample.surface)
            .put("fecha", sample.fecha).put("inspector", sample.inspectorNombre).put("status", sample.status)
            .put("slabRows",sample.slabRows).put("slabColumns",sample.slabColumns)
            .put("slabWidthM",sample.slabWidthM?:JSONObject.NULL).put("slabLengthM",sample.slabLengthM?:JSONObject.NULL)
            .put("sicData",sample.sicDataJson?.let(::JSONObject)?:JSONObject.NULL))
        .put("result", JSONObject().put("index", result.index ?: JSONObject.NULL).put("rating", result.rating ?: JSONObject.NULL)
            .put("pending", JSONArray(result.pending)))
        .put("photoPolicy","ORIGINAL_CLEAN")
        .put("photos",JSONArray().apply {
            photos.entries.sortedBy { it.key }.forEach { (damageId,items) -> items.sortedBy { it.createdAt }.forEach { photo ->
                put(JSONObject().put("photoId",photo.id).put("damageId",damageId).put("capturedAt",photo.createdAt).put("category","GENERAL"))
            } }
        })
        .put("calculation",JSONObject(PavementResultJson.encode(result)))
        .put("damages", JSONArray().apply {
            damages.sortedBy { it.correlativo }.forEach { d ->
                put(JSONObject().put("id", d.id).put("correlativo", d.correlativo).put("code", d.code).put("name", d.name)
                    .put("severity", d.severity ?: JSONObject.NULL).put("geometry", d.geometry)
                    .put("x1", d.x1 ?: JSONObject.NULL).put("y1", d.y1 ?: JSONObject.NULL)
                    .put("x2", d.x2 ?: JSONObject.NULL).put("y2", d.y2 ?: JSONObject.NULL)
                    .put("diameterM", d.diameterM ?: JSONObject.NULL).put("widthM", d.widthM ?: JSONObject.NULL)
                    .put("lengthM", d.lengthM ?: JSONObject.NULL).put("areaM2", d.areaM2 ?: JSONObject.NULL)
                    .put("crackWidthMm", d.crackWidthMm ?: JSONObject.NULL).put("depthMm", d.depthMm ?: JSONObject.NULL)
                    .put("slabs", d.slabs).put("observation", d.observation ?: "")
                    .put("photoCount", photos[d.id].orEmpty().size))
            }
        })
}
