package com.tuempresa.inventariovial.pavement

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.OutputStream
import java.security.MessageDigest
import java.util.UUID

data class PavementGeneratedFile(val id: String, val sampleId: String, val surface: String, val category: String,
    val mimeType: String, val localPath: String, val sha256: String, val destinationUri: String?, val createdAt: Long,
    val photoId: String? = null, val damageId: String? = null)

/** Durable, pavement-owned export manifest. CORE can consume it without changing its Drive router. */
object PavementGeneratedFiles {
    fun directory(context: Context) = File(context.filesDir,"pavement/exports").apply { mkdirs() }
    private fun manifest(context: Context)=File(directory(context),"manifest.json")
    @Synchronized fun records(context: Context): List<PavementGeneratedFile> {
        val file=manifest(context);if(!file.exists())return emptyList()
        val rows=JSONObject(file.readText()).getJSONArray("files")
        return (0 until rows.length()).map { i -> val r=rows.getJSONObject(i)
            PavementGeneratedFile(r.getString("id"),r.getString("sampleId"),r.getString("surface"),r.getString("category"),r.getString("mimeType"),r.getString("localPath"),r.getString("sha256"),if(r.isNull("destinationUri"))null else r.getString("destinationUri"),r.getLong("createdAt"),if(r.isNull("photoId"))null else r.getString("photoId"),if(r.isNull("damageId"))null else r.getString("damageId"))
        }
    }
    @Synchronized fun register(context: Context,s: PavementSampleEntity,category: String,mimeType: String,file: File,uri: String?=null,photoId: String?=null,damageId: String?=null): PavementGeneratedFile {
        val previous=records(context)
        val record=PavementGeneratedFile(previous.firstOrNull { it.localPath==file.absolutePath }?.id ?: UUID.randomUUID().toString(),s.id,s.surface,category,mimeType,file.absolutePath,
            file.inputStream().use { input -> val digest=MessageDigest.getInstance("SHA-256");val buffer=ByteArray(8192);while(true) {val n=input.read(buffer);if(n<0)break;digest.update(buffer,0,n)};digest.digest().joinToString(""){"%02x".format(it)} },uri,System.currentTimeMillis(),photoId,damageId)
        val rows=previous.filter { it.localPath != record.localPath } + record
        val json=JSONObject().put("schema",1).put("files",JSONArray(rows.map { r -> JSONObject().put("id",r.id).put("sampleId",r.sampleId).put("surface",r.surface).put("category",r.category).put("mimeType",r.mimeType).put("localPath",r.localPath).put("sha256",r.sha256).put("destinationUri",r.destinationUri?:JSONObject.NULL).put("createdAt",r.createdAt).put("photoId",r.photoId?:JSONObject.NULL).put("damageId",r.damageId?:JSONObject.NULL) }))
        val target=manifest(context);val atomic=android.util.AtomicFile(target)
        val out=atomic.startWrite()
        try {out.write(json.toString(2).toByteArray());atomic.finishWrite(out)}catch(e:Exception){atomic.failWrite(out);throw e}
        return record
    }
    fun preservePrepared(context: Context,s: PavementSampleEntity,category: String,mimeType: String,file: File,photoId: String?=null,damageId: String?=null): PavementGeneratedFile {
        val copy=File(directory(context),"${category}_${s.id}_${com.tuempresa.inventariovial.delivery.sha256(file.readBytes()).take(16)}_${file.name}")
        if(!copy.exists()) file.copyTo(copy)
        return register(context,s,category,mimeType,copy,photoId=photoId,damageId=damageId)
    }
    fun export(context: Context, uri: Uri, s: PavementSampleEntity, category: String, mimeType: String, extension: String,
               write: (OutputStream)->Unit): PavementGeneratedFile {
        val file=File(directory(context),"${category}_${s.id}_${UUID.randomUUID()}.$extension")
        var registered=false
        try {
            file.outputStream().use(write)
            // Register the durable local artifact even if copying to the chosen destination fails.
            register(context,s,category,mimeType,file)
            registered=true
            context.contentResolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } } ?: error("No se pudo abrir el destino")
            val record=register(context,s,category,mimeType,file,uri.toString())
            if(s.status=="CLOSED") {
                com.tuempresa.inventariovial.core.DriveReceiptStore.sample(context,s,"PENDING")
                if(com.tuempresa.inventariovial.DriveUploadPolicy.configurationError()==null) com.tuempresa.inventariovial.core.PavementSyncAdapter.enqueue(context,s)
            }
            return record
        } catch(e:Exception) {
            if(!registered)file.delete()
            throw e
        }
    }
}
