package com.tuempresa.inventariovial.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.security.MessageDigest

/** The pre-existing exporters save to internal storage. No Room schema change required. */
data class GeneratedFile(
    val id: String,
    val recordId: String?,
    val assetFamily: String,
    val fileType: String,
    val localPath: String,
    val mimeType: String,
    val createdAt: Long,
    val syncStatus: String,
    val driveId: String? = null
)

object GeneratedFileIndex {
    fun groupByElement(files:List<GeneratedFile>,records:List<com.tuempresa.inventariovial.data.entity.InventoryRecordEntity>,extensions:List<com.tuempresa.inventariovial.data.entity.SicExtensionEntity>):Map<String,List<GeneratedFile>> =
        files.groupBy {file->
            val record=records.find {it.id==file.recordId}
            if(record!=null) com.tuempresa.inventariovial.sic.InventoryElements.parentId(record,extensions,records)
            else file.recordId ?: file.assetFamily
        }
    fun recordIdFromName(name:String):String? =
        Regex("^SIC-(?:08|09|13|18A|18|34)_([A-Za-z0-9_-]+)\\.xlsx$").matchEntire(name)?.groupValues?.get(1)
            ?: Regex("^([A-Za-z0-9_-]+)_SIC-(?:08|09|13|18A|18|34)\\.xlsx$").matchEntire(name)?.groupValues?.get(1)
    private fun type(f: File) = when (f.extension.lowercase()) {
        "pdf" -> "application/pdf"
        "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        "zip" -> "application/zip"
        "json" -> "application/json"
        else -> null
    }

    /** Only known output locations, bounded recursion and no external/private configuration. */
    fun discover(context: Context): List<GeneratedFile> {
        val locations = listOf(
            File(context.cacheDir, "sic_exports") to "SIC",
            context.cacheDir to "Exportaciones",
            File(context.filesDir, "route_deliveries") to "SIC",
            File(context.filesDir, "scap-drive") to "SCAP",
            File(context.filesDir, "scap_exports") to "SCAP",
            File(context.filesDir, "pavement/exports") to "PAVIMENTOS"
        )
        val unique = LinkedHashMap<String, GeneratedFile>()
        for ((root, family) in locations) {
            if (!root.isDirectory) continue
            val base = root.canonicalFile
            root.walkTopDown().maxDepth(3).onEnter { directory ->
                directory.canonicalFile.toPath().startsWith(base.toPath())
            }.filter { it.isFile && it.length() > 0 && type(it) != null && it.name !in setOf("manifest.json","manifest.local.json") }.take(300).forEach { file ->
                if (!file.canonicalFile.toPath().startsWith(base.toPath())) return@forEach
                val path = file.canonicalPath
                unique.putIfAbsent(path, GeneratedFile(
                    id = MessageDigest.getInstance("SHA-256").digest(path.toByteArray()).take(12).joinToString("") { "%02x".format(it) },
                    recordId = recordIdFromName(file.name),
                    assetFamily = if (file.name.startsWith("SCAP", true)) "SCAP" else family,
                    fileType = Regex("SIC-(08|09|13|18A|18|34)").find(file.name)?.value ?: file.extension.uppercase(),
                    localPath = path,
                    mimeType = type(file)!!,
                    createdAt = file.lastModified(),
                    syncStatus = "LOCAL"
                ))
            }
        }
        unique.replaceAll { path,row ->
            val receipt=DriveReceiptStore.receipt(context,path)
            val valid=receipt!=null && receipt.optString("hash")==com.tuempresa.inventariovial.delivery.sha256(File(path).readBytes())
            if(valid) row.copy(syncStatus="SYNCED",driveId=receipt!!.optString("driveId")) else row
        }
        com.tuempresa.inventariovial.pavement.PavementGeneratedFiles.records(context).forEach { r ->
            val f=File(r.localPath)
            if(f.isFile && type(f)!=null) {
                val receipt=DriveReceiptStore.receipt(context,r.localPath)
                val synced=receipt?.optString("hash")==r.sha256
                unique[f.canonicalPath]=GeneratedFile(r.id,r.sampleId,"PAVIMENTOS",r.category,f.canonicalPath,r.mimeType,r.createdAt,
                    if(synced) "SYNCED" else "LOCAL",if(synced) receipt?.optString("driveId") else null)
            }
        }
        return unique.values.sortedByDescending { it.createdAt }
    }

    fun uri(context: Context, file: GeneratedFile): Uri {
        val selected = File(file.localPath).canonicalFile
        require(selected.isFile && selected.canRead()) { "El archivo ya no está disponible." }
        val roots = linkedMapOf("generated_cache_files" to context.cacheDir.canonicalFile,
            "route_deliveries" to File(context.filesDir,"route_deliveries").canonicalFile,
            "scap_drive" to File(context.filesDir,"scap-drive").canonicalFile,
            "scap_exports" to File(context.filesDir,"scap_exports").canonicalFile,
            "pavement_exports" to File(context.filesDir,"pavement/exports").canonicalFile)
        val root=roots.entries.firstOrNull { selected.toPath().startsWith(it.value.toPath()) }
        require(root!=null && type(selected) == file.mimeType) {
            "Archivo fuera del directorio de exportaciones"
        }
        // FileProvider's documented root-tag/relative-path URI. Path segments are encoded;
        // canonical containment is checked above, including symlinks and directory boundaries.
        val relative=root.value.toPath().relativize(selected.toPath()).map { it.toString() }
        return Uri.Builder().scheme("content").authority("${context.packageName}.fileprovider")
            .appendPath(root.key).apply { relative.forEach { appendPath(it) } }.build()
    }

    fun open(context: Context, file: GeneratedFile): Intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri(context, file), file.mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    fun share(context: Context, file: GeneratedFile): Intent = Intent.createChooser(
        Intent(Intent.ACTION_SEND).apply {
            type = file.mimeType
            putExtra(Intent.EXTRA_STREAM, uri(context, file))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }, "Compartir archivo"
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
