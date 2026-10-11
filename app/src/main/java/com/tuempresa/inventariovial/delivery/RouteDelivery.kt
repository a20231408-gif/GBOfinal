package com.tuempresa.inventariovial.delivery

import android.content.Context
import androidx.room.withTransaction
import androidx.work.*
import com.tuempresa.inventariovial.*
import com.tuempresa.inventariovial.camera.PhotoExif
import com.tuempresa.inventariovial.camera.RequiredWatermark
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.export.*
import com.tuempresa.inventariovial.road.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class PreparedRouteDelivery(val batch: DeliveryBatchEntity, val plan: DrivePathPlan, val fileCount: Int)

class RouteDelivery(private val context: Context, private val db: InventoryDatabase) {
    private val dao = db.inventoryDao()
    suspend fun prepare(route: String, reference: RoadReferenceData, config: MatchConfig,
        policy: DeliveryNamingPolicy = DeliveryNamingPolicy.FOLDER_COMPACT): PreparedRouteDelivery = withContext(Dispatchers.IO) {
        require(FileNameComplianceValidator.safeComponent(route))
        RoadAxisStorage(dao).refresh(reference, config, route)
        val snapshots = snapshots(route)
        require(snapshots.isNotEmpty()) { "No hay inventario SIC activo para esta ruta. SCAP se entrega desde su sección." }
        val recordList=snapshots.map {it.record}
        val extensionList=snapshots.mapNotNull {it.sicExtension}
        val elements=com.tuempresa.inventariovial.sic.InventoryElements.group(recordList,extensionList)
        val planRows=elements.map {element->
            val root=row(snapshots.single {it.record.id==element.record.id})
            root.copy(photos=root.photos+snapshots.filter {child->element.children.any {it.id==child.record.id}}
                .flatMap {it.photos.filter {p->p.scapInspectionId==null}.map {p->p.copy(recordId=root.record.id)}})
        }
        val plan = DrivePathPlanner.plan(planRows, ChainageCalculator(reference.prs), policy)
        require(plan.folders.all { it.plannedPath != null }) { "Hay familias sin SIB o carpetas válidas. Revisa las advertencias antes de entregar." }
        val identity = fingerprint(snapshots, policy)
        val previous = dao.deliveries(route).firstOrNull { it.status != "PREPARED" }
        if (previous != null) {
            require(previous.fingerprint == identity) { "La ruta ya tiene una entrega iniciada con otro contenido. Se conserva intacta; acuerda una nueva versión de entrega antes de sincronizar capturas posteriores." }
            val manifest = JSONObject(File(previous.manifestPath).readText())
            return@withContext PreparedRouteDelivery(previous, routed(plan), manifest.getJSONArray("files").length())
        }
        val id = UUID.randomUUID().toString()
        val directory = File(context.filesDir, "route_deliveries/$id").apply { check(mkdirs()) }
        val entries = JSONArray()
        fun add(file: File, payload: JSONObject) {
            payload.put("sha256", sha256(file.readBytes()))
            entries.put(json("localFile" to file.name, "payload" to payload))
        }
        plan.folders.forEach { folder ->
            val photos = folder.asset.item.photos.associateBy { it.id }
            val photoMetadata = folder.photos.map { p ->
                val photo = photos.getValue(p.photoId)
                val marked = com.tuempresa.inventariovial.core.CleanPhotoPolicy.original(com.tuempresa.inventariovial.core.PhotoForDelivery(photo.id, photo.originalPath.ifBlank { photo.localPath }, com.tuempresa.inventariovial.core.PhotoCategory.fromStored(photo.photoCategory), "{}"))
                val copy = File(directory, "${photo.id}.jpg")
                marked.copyTo(copy)
                // Metadata is held in the manifest; preserve the original bytes.
                val payload = DeliveryMetadata.photoPayload(folder, photo, p).put("kind", "photo")
                    .put("logicalPath", com.tuempresa.inventariovial.core.DrivePaths.inventory(context, folder, photo.photoCategory ?: "GENERAL"))
                    .put("pathVersion", 2).put("photoPolicy", "ORIGINAL_CLEAN")
                add(copy, payload)
                DeliveryMetadata.photo(photo, p, payload.getString("sha256"))
            }
            val metadata = File(directory, "${folder.asset.recordId}.json")
            metadata.writeText(DeliveryMetadata.element(folder, photoMetadata).toString(2))
            add(metadata, json("kind" to "metadata", "recordId" to folder.asset.recordId,
                "routeCode" to route, "sibCode" to folder.asset.sibCode, "sicCode" to folder.asset.item.record.sicCode,
                "elementFolder" to folder.asset.folderName, "fileName" to "metadata.json", "mimeType" to "application/json",
                "pathVersion" to 2, "logicalPath" to com.tuempresa.inventariovial.core.DrivePaths.inventory(context, folder)))
        }
        val exports = dao.recordsForExport(null, route)
        val headingPrefs = context.getSharedPreferences("sic_export", Context.MODE_PRIVATE)
        val heading = ExportHeading(headingPrefs.getString("project", "").orEmpty(), headingPrefs.getString("road", "").orEmpty(), headingPrefs.getString("section", "").orEmpty())
        plan.folders.forEach { folder ->
            val record=folder.asset.item.record
            val records=exports.filter { it.record.id==record.id }
            val format=SicExportFormat.entries.single { it.code==record.sicCode }
            val file=File(directory,"${record.id}_${format.code}.xlsx")
            file.outputStream().use { SicExcelWriter.write(it,records,format,heading) }
            add(file,json("kind" to "sic","recordId" to record.id,"routeCode" to route,"sibCode" to folder.asset.sibCode,
                "sicCode" to format.code,"elementFolder" to folder.asset.folderName,"fileName" to "${format.code}.xlsx",
                "mimeType" to SicExcelWriter.XLSX_MIME,"pathVersion" to 2,
                "logicalPath" to com.tuempresa.inventariovial.core.DrivePaths.inventory(context,folder)))
            // Supplement shares the parent folder/UUID; drafts are never sent as final documents.
            val supplementary=records.filter {SicExportFormat.SIC18A.appliesTo(it) && it.sic18?.let {detail->
                com.tuempresa.inventariovial.sic.InventoryElements.requiresCulvertDetail(detail.structuralConditionCode,detail.functionalConditionCode)}==true}
            if(supplementary.isNotEmpty()) {
                val extra=File(directory,"${record.id}_SIC-18A.xlsx")
                extra.outputStream().use {SicExcelWriter.write(it,supplementary,SicExportFormat.SIC18A,heading)}
                add(extra,json("kind" to "sic","recordId" to record.id,"routeCode" to route,"sibCode" to folder.asset.sibCode,
                    "sicCode" to "SIC-18A","elementFolder" to folder.asset.folderName,"fileName" to "SIC-18A.xlsx",
                    "mimeType" to SicExcelWriter.XLSX_MIME,"pathVersion" to 2,
                    "logicalPath" to com.tuempresa.inventariovial.core.DrivePaths.inventory(context,folder)))
            }
            // Width records retain their UUID/ACK identity; their documents use the parent element folder.
            val widths=exports.filter {it.record.sicCode=="SIC-09" && it.sicExtension?.sourceRecordId==record.id}
            if(record.sicCode=="SIC-08" && widths.isNotEmpty()) {
                widths.forEach {width->
                    val extra=File(directory,"${width.record.id}_SIC-09.xlsx")
                    extra.outputStream().use {SicExcelWriter.write(it,listOf(width),SicExportFormat.SIC09,heading)}
                    add(extra,json("kind" to "sic","recordId" to width.record.id,"routeCode" to route,"sibCode" to folder.asset.sibCode,
                        "sicCode" to "SIC-09","elementFolder" to folder.asset.folderName,
                        "fileName" to if(widths.size==1) "SIC-09.xlsx" else "SIC-09_${width.record.id}.xlsx",
                        "mimeType" to SicExcelWriter.XLSX_MIME,"pathVersion" to 2,
                        "logicalPath" to com.tuempresa.inventariovial.core.DrivePaths.inventory(context,folder)))
                }
            }
        }
        check(fingerprint(snapshots(route), policy) == identity) { "El inventario cambió mientras se preparaba. Vuelve a preparar la entrega." }
        val manifest = File(directory, "manifest.local.json")
        manifest.writeText(json("deliveryId" to id, "routeCode" to route, "fingerprint" to identity, "files" to entries).toString())
        val batch = DeliveryBatchEntity(id, route, identity, manifest.absolutePath, policy.name, "PREPARED", System.currentTimeMillis())
        dao.insertDelivery(batch)
        PreparedRouteDelivery(batch, routed(plan), entries.length())
    }

    private fun routed(plan:DrivePathPlan)=plan.copy(folders=plan.folders.map { folder ->
        folder.copy(plannedPath=com.tuempresa.inventariovial.core.DrivePaths.inventory(context,folder),photos=folder.photos.map { p ->
            val category=folder.asset.item.photos.single { it.id==p.photoId }.photoCategory ?: "GENERAL"
            p.copy(plannedPath=com.tuempresa.inventariovial.core.DrivePaths.inventory(context,folder,category)+"/"+p.photoName)
        })
    })

    suspend fun enqueue(prepared: PreparedRouteDelivery) {
        DriveUploadPolicy.requireConfiguration()
        require(prepared.plan.folders.flatMap { it.photos }.all { FileNameComplianceValidator.warning(it.photoName) == null }) {
            "La nomenclatura no cumple el límite de 20 caracteres. Selecciona FOLDER_COMPACT y vuelve a preparar."
        }
        db.withTransaction {
            val batch = dao.delivery(prepared.batch.id) ?: error("Entrega inexistente.")
            check(fingerprint(snapshots(batch.routeCode), DeliveryNamingPolicy.valueOf(batch.namingPolicy)) == batch.fingerprint) {
                "El inventario cambió después de la revisión. Vuelve a preparar antes de sincronizar."
            }
            check(dao.deliveries(batch.routeCode).none { it.id != batch.id && it.status != "PREPARED" }) { "Ya existe una entrega iniciada para esta ruta." }
            dao.deliveryStatus(batch.id, "QUEUED", null)
        }
        val job = OneTimeWorkRequestBuilder<DriveUploadWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setInputData(workDataOf("deliveryId" to prepared.batch.id)).build()
        withContext(Dispatchers.IO) {
            WorkManager.getInstance(context).enqueueUniqueWork("route-delivery-${prepared.batch.routeCode}", ExistingWorkPolicy.KEEP, job).result.get()
        }
    }

    suspend fun snapshots(route: String) = dao.activeSnapshots().filter { normalizedRoadCode(it.record.routeCode) == route && it.record.sicCode != "SCAP" }
    companion object {
        fun row(s: InventorySnapshot) = InventoryRecordWithPhotos(s.record, s.photos.filter { it.scapInspectionId == null }, s.sic23, s.sic18, s.sic20, s.sic19, s.sic18a, s.sicExtension)
        fun fingerprint(snapshots: List<InventorySnapshot>, policy: DeliveryNamingPolicy): String = sha256((policy.name + snapshots.sortedBy { it.record.id }.joinToString {
            it.copy(record = it.record.copy(photoSyncStatus = "", excelSyncStatus = "", serverSyncStatus = "", serverSyncError = null),
                photos = it.photos.filter { p -> p.scapInspectionId == null }.sortedBy { p -> p.id }.map { p ->
                    p.copy(syncStatus = "", driveFolderId = null, driveFileId = null)
                }).toString()
        }).toByteArray())
    }
}
