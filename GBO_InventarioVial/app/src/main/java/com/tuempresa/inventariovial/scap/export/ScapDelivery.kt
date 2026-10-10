package com.tuempresa.inventariovial.scap.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import com.tuempresa.inventariovial.BuildConfig
import com.tuempresa.inventariovial.camera.RequiredWatermark
import com.tuempresa.inventariovial.scap.data.ScapInspectionSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** One export path for local delivery and Drive. Never changes capture files. */
object ScapDelivery {
    // v4 = protocol 1/2 + JPEG photographs + deterministic human layout cache.
    const val SCAP_BUNDLE_VERSION = 7

    fun exporter(context: Context) = ScapExcelExporter({context.assets.open("scap_template.xlsx")},
        context.assets.open("scap_field_map.json").bufferedReader().use {it.readText()})
    fun exporter(context:Context,snapshot:ScapInspectionSnapshot):ScapExcelExporter {
        val type=com.tuempresa.inventariovial.scap.domain.ScapBridgeType.from(snapshot) ?: return exporter(context)
        val asset=ScapCanonicalLayout.forType(type).asset
        return ScapExcelExporter({context.assets.open("${asset}_template.xlsx")},
            context.assets.open("${asset}_field_map.json").bufferedReader().use {it.readText()},type)
    }

    suspend fun writeExcel(context: Context, snapshot: ScapInspectionSnapshot, target: File) {
        val temporary = mutableListOf<File>()
        val photoPaths = mutableSetOf<String>()
        try {
            val directory = File(context.cacheDir, "watermark-export")
            val delivery = snapshot.copy(photos = snapshot.photos.map { photo ->
                if(photo.localPath.isBlank())return@map photo
                require(photo.scapInspectionId == snapshot.inspection.id && photo.recordId == snapshot.inspection.roadRecordId)
                val marked = com.tuempresa.inventariovial.core.CleanPhotoPolicy.original(com.tuempresa.inventariovial.core.PhotoForDelivery(photo.id, photo.originalPath.ifBlank { photo.localPath }, com.tuempresa.inventariovial.core.PhotoCategory.fromStored(photo.photoCategory), "{}"))
                if (marked.parentFile?.canonicalPath == directory.canonicalPath) temporary += marked
                photoPaths += marked.absolutePath
                photo.copy(localPath = marked.absolutePath, stampedPath = marked.absolutePath)
            })
            withContext(Dispatchers.IO) {
                target.outputStream().use { exporter(context,delivery).write(it, delivery) { path ->
                    readImage(context,path,photo=path in photoPaths,workbook=true)
                } }
            }
        } finally { temporary.forEach {it.delete()} }
    }

    /** Cache by saved revision + reviewed human layout so retries always send identical bytes. */
    suspend fun bundle(context: Context, s: ScapInspectionSnapshot, layout: ScapHumanLayout? = null): File = withContext(Dispatchers.IO) {
        require(s.inspection.id.matches(Regex("[a-fA-F0-9-]{36}")))
        val directory = File(context.filesDir, "scap-drive/${s.inspection.id}").apply {mkdirs()}
        val layoutSuffix = layout?.let { "_human_" + sha256(it.json().toString().toByteArray()).take(16) }.orEmpty()
        val target = File(directory, "${s.inspection.updatedAt}_bundle$SCAP_BUNDLE_VERSION$layoutSuffix.zip")
        if (target.isFile) return@withContext target
        val started = SystemClock.elapsedRealtime()
        var photoBytes = 0L
        var sketchBytes = 0L
        var workbookBytes = 0L
        val work = File(context.cacheDir, "scap-bundle-${java.util.UUID.randomUUID()}").apply {mkdirs()}
        val partial = File(directory, "${s.inspection.updatedAt}_bundle$SCAP_BUNDLE_VERSION$layoutSuffix.partial")
        try {
            val files = JSONArray()
            ZipOutputStream(partial.outputStream().buffered()).use { zip ->
                fun add(path: String, bytes: ByteArray, metadata: JSONObject = JSONObject()) {
                    zip.putNextEntry(ZipEntry(path).apply {time = 0})
                    zip.write(bytes)
                    zip.closeEntry()
                    files.put(metadata.put("path", path).put("sha256", sha256(bytes)))
                }
                fun addOriginal(path:String,file:File,metadata:JSONObject) {
                    zip.putNextEntry(ZipEntry(path).apply{time=0})
                    file.inputStream().buffered().use{it.copyTo(zip)}
                    zip.closeEntry()
                    files.put(metadata.put("path",path).put("sha256",sha256(file)))
                }

                // Prepare each media file exactly once; the workbook and ZIP reuse those same pixels.
                val photoPaths = mutableSetOf<String>()
                val preparedSketchesById = mutableMapOf<String, ByteArray>()
                val preparedSketchesByPath = mutableMapOf<String, ByteArray>()
                val delivery = s.copy(photos = s.photos.sortedBy {it.photoIndex}.map { p ->
                    if(p.localPath.isBlank())return@map p
                    require(p.scapInspectionId == s.inspection.id && p.recordId == s.inspection.roadRecordId)
                    val marked = com.tuempresa.inventariovial.core.CleanPhotoPolicy.original(com.tuempresa.inventariovial.core.PhotoForDelivery(p.id, p.originalPath.ifBlank { p.localPath }, com.tuempresa.inventariovial.core.PhotoCategory.fromStored(p.photoCategory), "{}"))
                    photoPaths += marked.absolutePath
                    val path = layout?.photoPaths?.getValue(p.id) ?: "fotos/${p.id}.jpg"
                    val metadata=JSONObject()
                        .put("photoId", p.id).put("order", p.photoIndex)
                        .put("category", p.photoCategory).put("elementCode", p.scapElementCode).put("description", p.description)
                        .put("capturedAt", p.capturedAt ?: JSONObject.NULL)
                        .put("latitude", p.photoLatitude ?: JSONObject.NULL).put("longitude", p.photoLongitude ?: JSONObject.NULL)
                        .put("altitudeM", p.photoAltitudeM ?: JSONObject.NULL).put("accuracyM", p.photoGpsAccuracyM ?: JSONObject.NULL)
                        .put("gpsTimestamp", p.photoGpsTimestamp ?: JSONObject.NULL).put("provider", p.photoGnssProvider ?: JSONObject.NULL)
                        .put("photoLocationSource", p.photoLocationSource)
                    val jpeg=marked.inputStream().use{it.read()==0xff&&it.read()==0xd8}
                    if(jpeg){photoBytes+=marked.length();addOriginal(path,marked,metadata)}
                    else {val jpg=readImage(context,marked.absolutePath,photo=true);photoBytes+=jpg.size;add(path,jpg,metadata)}
                    p.copy(localPath = marked.absolutePath, stampedPath = marked.absolutePath)
                })
                s.sketches.sortedBy {it.id}.forEach { sketch ->
                    val png = readImage(context, sketch.localUri, photo = false)
                    preparedSketchesById[sketch.id] = png
                    preparedSketchesByPath[sketch.localUri] = png
                    sketchBytes += png.size
                }

                val workbook = File(work, "SCAP.xlsx")
                workbook.outputStream().use { exporter(context,delivery).write(it, delivery) { path ->
                    if(path in photoPaths)readImage(context,path,photo=true,workbook=true) else preparedSketchesByPath[path] ?: readImage(context,path,workbook=true)
                } }
                workbookBytes = workbook.length()
                addOriginal("SCAP.xlsx",workbook,JSONObject())
                s.sketches.sortedBy {it.id}.forEach { sketch ->
                    add(layout?.sketchPaths?.getValue(sketch.id) ?: "croquis/${sketch.id}.png",
                        preparedSketchesById.getValue(sketch.id), JSONObject().put("type", sketch.type))
                }

                val manifest = JSONObject().put("protocol", if(layout == null) 1 else 2).put("recordKind", "SCAP")
                    .put("scapBundleVersion", SCAP_BUNDLE_VERSION).put("photoPolicy", "ORIGINAL_CLEAN")
                    
                    .put("scapInspectionId", s.inspection.id).put("revision", s.inspection.updatedAt)
                    .put("roadRecordId",s.inspection.roadRecordId).put("bridgeCode", s.inspection.bridgeCode).put("bridgeName", s.inspection.bridgeName)
                    .put("routeCode", s.values()["route"].orEmpty()).put("files", files)
                    .put("completionStatus",s.values("completion")["status"] ?: "INCOMPLETE")
                    .put("uploadedWithWarnings",s.values("completion")["uploadedWithWarnings"]=="true")
                layout?.let {manifest.put("delivery", it.json()).put("pathVersion",2).put("logicalPath",com.tuempresa.inventariovial.core.DrivePaths.bridge(context,s))}
                zip.putNextEntry(ZipEntry("manifest.json").apply {time = 0})
                zip.write(manifest.toString(2).toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            check(partial.renameTo(target)) {"No se pudo guardar el paquete SCAP."}
            if (BuildConfig.DEBUG) Log.d("ScapDelivery",
                "bundleVersion=$SCAP_BUNDLE_VERSION protocol=${if(layout==null)1 else 2} xlsxBytes=$workbookBytes " +
                    "photoBytes=$photoBytes sketchBytes=$sketchBytes zipBytes=${target.length()} " +
                    "base64Bytes=${((target.length()+2)/3)*4} generationMs=${SystemClock.elapsedRealtime()-started}")
            target
        } finally {
            partial.delete()
            work.deleteRecursively()
        }
    }

    fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") {"%02x".format(it)}
    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use {stream ->
            val buffer = ByteArray(65536)
            while (true) {
                val count = stream.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") {"%02x".format(it)}
    }

    /** Bound export image resolution without changing original captures. Photos use JPEG to keep SCAP uploads practical. */
    fun readImage(context: Context, path: String, photo: Boolean = false,workbook:Boolean=false): ByteArray {
        fun stream() = if (path.startsWith("content:")) requireNotNull(context.contentResolver.openInputStream(Uri.parse(path)))
        else File(path.removePrefix("file://")).inputStream()
        // JPEG captures are transferred byte-for-byte; conversion only for legacy PNG/WebP.
        if (photo && !workbook) {
            val bytes=stream().use { it.readBytes() }
            if (bytes.size > 2 && bytes[0] == 0xff.toByte() && bytes[1] == 0xd8.toByte()) return bytes
        }
        val bounds = BitmapFactory.Options().apply {inJustDecodeBounds = true}
        stream().use {BitmapFactory.decodeStream(it, null, bounds)}
        require(bounds.outWidth > 0 && bounds.outHeight > 0) {"Una imagen local no puede leerse."}
        val maxLongSide = if (photo) 1600 else 1200
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxLongSide * 2) sample *= 2
        val decoded = stream().use {BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply {inSampleSize = sample})}
            ?: error("Imagen ilegible.")
        val orientation=stream().use {ScapImageOrientation.read(it)}
        val bitmap=ScapImageOrientation.apply(decoded,orientation)
        if(bitmap!==decoded) decoded.recycle()
        var resized: Bitmap? = null
        return try {
            val longest = maxOf(bitmap.width, bitmap.height)
            val output = if (longest > maxLongSide) {
                val scale = maxLongSide.toDouble() / longest
                Bitmap.createScaledBitmap(bitmap, maxOf(1, (bitmap.width * scale).toInt()),
                    maxOf(1, (bitmap.height * scale).toInt()), true).also {resized = it}
            } else bitmap
            ByteArrayOutputStream().use {out ->
                check(output.compress(if(photo) Bitmap.CompressFormat.JPEG else Bitmap.CompressFormat.PNG,
                    if(photo) 85 else 100, out))
                out.toByteArray()
            }
        } finally {
            resized?.takeIf {it !== bitmap}?.recycle()
            bitmap.recycle()
        }
    }
}
