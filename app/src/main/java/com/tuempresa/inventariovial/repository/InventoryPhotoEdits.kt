package com.tuempresa.inventariovial.repository

import androidx.room.withTransaction
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.camera.PhotoCaptureMetadata
import org.json.JSONObject
import java.util.UUID

/** The same photos table and V3 keys; replacements retain UUID, order and remote IDs. */
object InventoryPhotoEdits {
    fun snapshot(photo:PhotoEntity):String = JSONObject().apply {
        photo.javaClass.declaredFields.filterNot {java.lang.reflect.Modifier.isStatic(it.modifiers) || it.isSynthetic}.forEach {
            it.isAccessible=true;put(it.name,it.get(photo) ?: JSONObject.NULL)
        }
    }.toString()
    suspend fun archive(db:InventoryDatabase,photo:PhotoEntity,revision:Long,action:String) {
        db.inventoryDao().insertPhotoHistory(PhotoHistoryEntity(UUID.randomUUID().toString(),photo.recordId,photo.id,revision,action,snapshot(photo)))
    }
    suspend fun save(db:InventoryDatabase,id:String,paths:List<String>,categories:Map<String,String>,captures:Map<String,PhotoCaptureMetadata>,revision:Long)=db.withTransaction {
        val dao=db.inventoryDao();val existing=requireNotNull(dao.snapshot(id)).photos.filter {it.scapInspectionId==null}
        val unique=paths.distinct();val replacements=unique.mapNotNull {p->captures[p]?.replacesPath?.let {it to p}}.toMap()
        require(replacements.size==unique.count {captures[it]?.replacesPath!=null}) {"Reemplazos repetidos"}
        require(replacements.all {(old,new)->old !in unique && new!=old && (existing.any {it.localPath==old} || existing.any {it.localPath==new})}) {"La fotografía a reemplazar cambió; vuelve a abrir la ficha"}
        var next=(existing.maxOfOrNull {it.photoIndex} ?: 0)+1
        existing.forEach {p->
            val replacement=replacements[p.localPath]
            when {
                replacement!=null -> {
                    require(java.io.File(replacement).isFile) {"Original de reemplazo no disponible"}
                    archive(db,p,revision,"REPLACED")
                    val updated=p.copy(localPath=replacement,originalPath=replacement,stampedPath=null,sha256=null,
                        photoCategory=categories[replacement] ?: p.photoCategory,syncStatus="PENDING")
                    dao.updatePhotoMetadata(requireNotNull(captures[replacement]).applyTo(updated))
                }
                p.localPath !in unique -> {archive(db,p,revision,"REMOVED");dao.deleteLocalPhoto(p.id,id)}
                else -> {
                    val category=categories[p.localPath] ?: p.photoCategory
                    if(category!=p.photoCategory) {archive(db,p,revision,"RECATEGORIZED");dao.updatePhotoMetadata(p.copy(photoCategory=category,syncStatus="PENDING"))}
                }
            }
        }
        unique.filter {p->existing.none {it.localPath==p} && p !in replacements.values}.forEach {path->
            val photo=PhotoEntity(UUID.nameUUIDFromBytes((id+"\n"+path).toByteArray()).toString(),id,next++,path,false,null,null,null,"PENDING",revision,photoCategory=categories[path] ?: "GENERAL")
            dao.insertPhoto((captures[path] ?: PhotoCaptureMetadata.fromOriginal(path)).applyTo(photo))
        }
        dao.refreshRecordPhotoStatus(id)
    }
}
