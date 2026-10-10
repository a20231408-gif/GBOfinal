package com.tuempresa.inventariovial.sic

import androidx.room.withTransaction
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.camera.PhotoCaptureMetadata
import org.json.JSONObject
import java.util.UUID

class SicFieldRepository(private val db:InventoryDatabase) {
    private val dao get()=db.inventoryDao()
    suspend fun load(id:String,format:SicFieldFormat):SicFieldState? {
        val r=dao.recordById(id) ?: return null
        val ext=dao.sicExtension(id)
        if(ext!=null && ext.formatCode==format.code) return inheritCurrent(SicFieldState(format,ext.values(),ext.sourceRecordId))
        if(format==SicFieldFormat.SIC18A) {
            val s=dao.snapshot(id) ?: return null
            val parent=s.sic18 ?: return null
            return SicFieldState(format,core(r)+com.tuempresa.inventariovial.supplementary.Sic18AStatus.inherited(parent,s.sic18a),id)
        }
        return null
    }
    suspend fun inheritCurrent(s:SicFieldState):SicFieldState {
        if(s.format==SicFieldFormat.SIC09 && s.sourceRecordId!=null) {
            val parent=dao.sicExtension(s.sourceRecordId)
            if(parent?.formatCode=="SIC-08") return s.copy(values=s.values+parent.values().filterKeys {it in sharedKeys || it=="faja"})
        }
        if(s.format!=SicFieldFormat.SIC18A) return s
        val p=s.sourceRecordId?.let {dao.snapshot(it)} ?: return s
        return s.copy(values=s.values+core(p.record)+com.tuempresa.inventariovial.supplementary.Sic18AStatus.inherited(requireNotNull(p.sic18),null))
    }
    suspend fun fromFaja(parentId:String):SicFieldState {
        val r=requireNotNull(dao.recordById(parentId))
        require(r.sicCode=="SIC-08" && r.status in setOf("ACTIVE","DRAFT")) {"Selecciona una faja activa o en borrador"}
        val parent=requireNotNull(dao.sicExtension(parentId))
        val v=parent.values()
        return SicFieldState(SicFieldFormat.SIC09,v.filterKeys {it in sharedKeys}+mapOf("faja" to v["faja"].orEmpty()),parentId)
    }
    suspend fun save(id:String,state:SicFieldState,owner:String,finalize:Boolean,
        photoPaths:List<String>? = null,categories:Map<String,String> = emptyMap(),captures:Map<String,PhotoCaptureMetadata> = emptyMap(),
        sessionId:String?=null):String = db.withTransaction {
        require(owner.isNotBlank()) {"Operador autenticado requerido"}
        val s=inheritCurrent(state)
        if(finalize) require(s.completion().canFinalize) {s.completion().issues.joinToString("\n") {"${it.field}: ${it.reason}"}}
        val key=if(s.format==SicFieldFormat.SIC18A) requireNotNull(s.sourceRecordId) else id
        val old=dao.recordById(key);val previous=dao.sicExtension(key)
        require(previous==null || previous.ownerUserId==owner) {"Ficha de otro operador"}
        require(previous==null || previous.status!="COMPLETE") {"La ficha ya está finalizada"}
        require(old==null || s.format==SicFieldFormat.SIC18A || old.status=="DRAFT") {"La ficha ya está finalizada"}
        if(s.sourceRecordId!=null) {
            val parent=requireNotNull(dao.recordById(s.sourceRecordId)) {"Registro fuente inexistente"}
            require(parent.status=="ACTIVE" || !finalize && s.format==SicFieldFormat.SIC09 && parent.status=="DRAFT") {"El registro fuente debe estar activo"}
            require(parent.sicCode==if(s.format==SicFieldFormat.SIC18A) "SIC-18" else "SIC-08") {"Relación incompatible"}
        }
        val now=maxOf(System.currentTimeMillis(),(old?.updatedAt ?: 0)+1)
        if(s.format!=SicFieldFormat.SIC18A) {
            val r=InventoryRecordEntity(key,s.format.code,s.format.family,s.text("routeCode"),s.text("roadbedCode"),s.text("startPrCode"),
                SicFieldState.number(s.text("startDistanceM")) ?: 0.0,s.text("endPrCode").ifBlank {null},SicFieldState.number(s.text("endDistanceM")),s.text("sideCode").ifBlank {null},
                SicFieldState.number(s.text("_latitude")) ?: old?.latitude ?: 0.0,SicFieldState.number(s.text("_longitude")) ?: old?.longitude ?: 0.0,
                SicFieldState.number(s.text("_altitude")) ?: old?.altitudeM,SicFieldState.number(s.text("_accuracy")) ?: old?.gpsAccuracyM,s.text("surveyDate"),null,
                if(finalize) "ACTIVE" else "DRAFT","PENDING","PENDING",old?.createdAt ?: now,now,
                endLatitude=SicFieldState.number(s.text("_endLatitude")) ?: old?.endLatitude,endLongitude=SicFieldState.number(s.text("_endLongitude")) ?: old?.endLongitude,
                endGpsAccuracyM=SicFieldState.number(s.text("_endAccuracy")) ?: old?.endGpsAccuracyM,
                locationSource=if(s.text("_latitude").isNotBlank()) "TABLET_GNSS" else old?.locationSource ?: "CONTRACTUAL_PR_ONLY",sessionId=sessionId ?: old?.sessionId)
            dao.insertOrCompleteDraft(r)
            if(photoPaths!=null) savePhotos(key,photoPaths,categories,captures,now)
        } else {
            require(old?.sicCode=="SIC-18" && old.status=="ACTIVE") {"SIC-18A requiere una alcantarilla activa"}
            dao.saveSic18A(Sic18AEntity.from(key,s.values))
            // Same parent UUID and photographs; never alter its contractual fields.
            if(photoPaths!=null) {
            var next=(dao.snapshot(key)?.photos?.maxOfOrNull {it.photoIndex} ?: 0)+1
            val existing=dao.snapshot(key)!!.photos
            existing.forEach {p->
                if(p.localPath !in photoPaths) dao.deleteLocalPhoto(p.id,key)
                else dao.updatePhotoMetadata(p.copy(photoCategory=categories[p.localPath] ?: p.photoCategory))
            }
            photoPaths.distinct().filter {path->existing.none {it.localPath==path}}.forEach {path->
                require(categories[path] in com.tuempresa.inventariovial.catalog.EngineeringConditions.culvertPhotos.keys)
                val photo=PhotoEntity(UUID.randomUUID().toString(),key,next++,path,false,null,null,null,"PENDING",now,photoCategory=categories[path])
                dao.insertPhoto((captures[path] ?: PhotoCaptureMetadata.fromOriginal(path)).applyTo(photo))
            }
            }
            dao.markServerPending(key,now);dao.refreshRecordPhotoStatus(key)
        }
        dao.saveSicExtension(SicExtensionEntity(key,s.format.code,JSONObject(s.values).toString(),s.sourceRecordId,owner,if(finalize) "COMPLETE" else "DRAFT"))
        key
    }
    private suspend fun savePhotos(id:String,paths:List<String>,categories:Map<String,String>,captures:Map<String,PhotoCaptureMetadata>,now:Long) {
        val existing=dao.snapshot(id)!!.photos
        var next=(existing.maxOfOrNull {it.photoIndex} ?: 0)+1
        existing.forEach { p ->
            if(p.localPath !in paths) dao.deleteLocalPhoto(p.id,id)
            else dao.updatePhotoMetadata(p.copy(photoCategory=categories[p.localPath] ?: p.photoCategory))
        }
        paths.distinct().filter {path->existing.none {it.localPath==path}}.forEach {path->
            val p=PhotoEntity(UUID.randomUUID().toString(),id,next++,path,false,null,null,null,"PENDING",now,photoCategory=categories[path] ?: "GENERAL")
            dao.insertPhoto((captures[path] ?: PhotoCaptureMetadata.fromOriginal(path)).applyTo(p))
        }
        dao.refreshRecordPhotoStatus(id)
    }
    /** Both formats are saved atomically; widths may remain absent or a raw draft. */
    suspend fun saveFaja(id:String,characteristics:SicFieldState,widthId:String,widths:Map<String,String>,owner:String,finalize:Boolean,
        photoPaths:List<String>?=null,captures:Map<String,PhotoCaptureMetadata> = emptyMap()):String = db.withTransaction {
        require(characteristics.format==SicFieldFormat.SIC08)
        if(dao.recordById(id)?.status!="ACTIVE") save(id,characteristics,owner,finalize,photoPaths,captures=captures)
        else require(dao.sicExtension(id)?.ownerUserId==owner) {"Ficha de otro operador"}
        if(widths.values.any {it.isNotBlank()} || dao.recordById(widthId)!=null) {
            val width=fromFaja(id).copy(values=fromFaja(id).values+widths.filterKeys {it in setOf("usefulWidthM","totalWidthM")})
            if(dao.sicExtension(widthId)?.status!="COMPLETE") save(widthId,width,owner,finalize && width.completion().canFinalize)
        }
        id
    }
    suspend fun saveCulvertDraft(id:String,values:Map<String,String>,owner:String) = db.withTransaction {
        val record=requireNotNull(dao.recordById(id))
        require(record.sicCode=="SIC-18" && record.status=="DRAFT")
        val previous=dao.sicExtension(id)
        require(previous==null || previous.ownerUserId==owner) {"Borrador de otro operador"}
        if(values.isNotEmpty()) dao.saveSicExtension(SicExtensionEntity(id,"SIC-18A",JSONObject(values).toString(),id,owner,"DRAFT"))
    }
    companion object {
        val sharedKeys=setOf("routeCode","roadbedCode","startPrCode","startDistanceM","endPrCode","endDistanceM","surveyDate")
        fun core(r:InventoryRecordEntity)=mapOf("routeCode" to r.routeCode,"roadbedCode" to r.roadbedCode,"startPrCode" to r.startPrCode,
            "startDistanceM" to r.startDistanceM.toString(),"endPrCode" to r.endPrCode.orEmpty(),"endDistanceM" to r.endDistanceM?.toString().orEmpty(),"surveyDate" to r.surveyDate)
    }
}
