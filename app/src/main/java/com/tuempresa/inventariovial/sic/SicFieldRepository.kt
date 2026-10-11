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
        if(ext!=null && ext.formatCode==format.code) {
            val values=mapOf("_latitude" to r.latitude?.toString().orEmpty(),"_longitude" to r.longitude?.toString().orEmpty(),"_accuracy" to r.gpsAccuracyM?.toString().orEmpty(),"_coordinateSource" to r.locationSource,"_gpsTimestamp" to r.gpsTimestamp?.toString().orEmpty())+ext.values()
            val stored=if(r.observations!=null)
                values+("observations" to r.observations) else values
            return inheritCurrent(SicFieldState(format,stored,ext.sourceRecordId))
        }
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
            if(parent?.formatCode=="SIC-08") return s.copy(values=parent.values().filterKeys {it in sharedKeys || it=="faja"}+s.values)
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
        sessionId:String?=null,expectedUpdatedAt:Long?=null):String = db.withTransaction {
        require(owner.isNotBlank()) {"Operador autenticado requerido"}
        val s=inheritCurrent(state)
        require(s.text("_gpsError").isBlank()){s.text("_gpsError")}
        if(finalize) require(s.completion().canFinalize) {s.completion().issues.joinToString("\n") {"${it.field}: ${it.reason}"}}
        val key=if(s.format==SicFieldFormat.SIC18A) requireNotNull(s.sourceRecordId) else id
        val old=dao.recordById(key);val previous=dao.sicExtension(key)
        if(s.format!=SicFieldFormat.SIC18A) {
            val input=com.tuempresa.inventariovial.validation.ContractualLocationInput
            for((field,historic) in listOf("startPrCode" to old?.startPrCode,"endPrCode" to old?.endPrCode))
                require(input.acceptPr(s.text(field)) || historic==s.text(field)){"PR: máximo cuatro dígitos"}
            for((field,historic) in listOf("startDistanceM" to old?.startDistanceM,"endDistanceM" to old?.endDistanceM))
                require(input.acceptDistance(s.text(field)) || historic!=null && SicFieldState.number(s.text(field))==historic){"Distancia: máximo tres cifras enteras"}
        }
        require(expectedUpdatedAt==null || old?.updatedAt==expectedUpdatedAt) {"La ficha cambió desde que la abriste; conserva tus cambios y vuelve a abrirla."}
        require(old==null || old.sicCode==s.format.code || s.format==SicFieldFormat.SIC18A) {"Formato incompatible"}
        require(previous==null || previous.ownerUserId==owner) {"Ficha de otro operador"}
        
        require(old==null || old.status in setOf("ACTIVE","DRAFT")) {"Registro anulado."}
        if(s.sourceRecordId!=null) {
            val parent=requireNotNull(dao.recordById(s.sourceRecordId)) {"Registro fuente inexistente"}
            require(parent.status=="ACTIVE" || s.format==SicFieldFormat.SIC09 && parent.status=="DRAFT") {"El registro fuente debe estar activo"}
            require(parent.sicCode==if(s.format==SicFieldFormat.SIC18A) "SIC-18" else "SIC-08") {"Relación incompatible"}
        }
        val now=maxOf(System.currentTimeMillis(),(old?.updatedAt ?: 0)+1)
        if(s.format!=SicFieldFormat.SIC18A) {
            val r=InventoryRecordEntity(key,s.format.code,s.format.family,s.text("routeCode"),s.text("roadbedCode"),s.text("startPrCode"),
                SicFieldState.number(s.text("startDistanceM")) ?: 0.0,s.text("endPrCode").ifBlank {null},SicFieldState.number(s.text("endDistanceM")),s.text("sideCode").ifBlank {null},
                if(s.text("_gpsExplicit")=="true") SicFieldState.number(s.text("_latitude")) else SicFieldState.number(s.text("_latitude")) ?: old?.latitude,
                if(s.text("_gpsExplicit")=="true") SicFieldState.number(s.text("_longitude")) else SicFieldState.number(s.text("_longitude")) ?: old?.longitude,
                if(s.text("_gpsExplicit")=="true") null else old?.altitudeM,if(s.text("_gpsExplicit")=="true") SicFieldState.number(s.text("_accuracy")) else old?.gpsAccuracyM,s.text("surveyDate"),s.values["observations"] ?: old?.observations,
                if(finalize) "ACTIVE" else "DRAFT","PENDING","PENDING",old?.createdAt ?: now,now,
                endLatitude=SicFieldState.number(s.text("_endLatitude")) ?: old?.endLatitude,endLongitude=SicFieldState.number(s.text("_endLongitude")) ?: old?.endLongitude,
                endGpsAccuracyM=SicFieldState.number(s.text("_endAccuracy")) ?: old?.endGpsAccuracyM,
                sessionId=old?.sessionId ?: sessionId,
                gpsTimestamp=if(s.text("_gpsExplicit")=="true") s.text("_gpsTimestamp").toLongOrNull() else old?.gpsTimestamp,endGpsTimestamp=old?.endGpsTimestamp,gnssProvider=old?.gnssProvider ?: "TABLET",
                gnssFixType=old?.gnssFixType,verticalAccuracyM=old?.verticalAccuracyM,satellites=old?.satellites,hdop=old?.hdop,
                correctionAge=old?.correctionAge,isRtkFixed=old?.isRtkFixed ?: false,
                segment=s.values["_segment"] ?: old?.segment,surveyDirection=s.values["_direction"] ?: old?.surveyDirection,
                axisMeasureM=old?.axisMeasureM,distanceToRoadAxisM=old?.distanceToRoadAxisM,roadMatchConfidence=old?.roadMatchConfidence,
                matchedSegmentId=old?.matchedSegmentId,projectedLatitude=old?.projectedLatitude,projectedLongitude=old?.projectedLongitude,
                endAxisMeasureM=old?.endAxisMeasureM,endDistanceToRoadAxisM=old?.endDistanceToRoadAxisM,
                endMatchedSegmentId=old?.endMatchedSegmentId,endProjectedLatitude=old?.endProjectedLatitude,endProjectedLongitude=old?.endProjectedLongitude,
                locationSource=if(s.text("_gpsExplicit")=="true") s.text("_coordinateSource").ifBlank{"CONTRACTUAL_PR_ONLY"} else old?.locationSource ?: "CONTRACTUAL_PR_ONLY")
            val stored=if(old!=null) old.copy(
                routeCode=r.routeCode,roadbedCode=r.roadbedCode,startPrCode=r.startPrCode,startDistanceM=r.startDistanceM,
                endPrCode=r.endPrCode,endDistanceM=r.endDistanceM,sideCode=if(s.values.containsKey("sideCode"))r.sideCode else old.sideCode,surveyDate=r.surveyDate,observations=r.observations,
                latitude=r.latitude,longitude=r.longitude,altitudeM=r.altitudeM,gpsAccuracyM=r.gpsAccuracyM,
                endLatitude=r.endLatitude,endLongitude=r.endLongitude,endGpsAccuracyM=r.endGpsAccuracyM,
                status=r.status,photoSyncStatus=r.photoSyncStatus,excelSyncStatus=r.excelSyncStatus,
                serverSyncStatus="PENDING",serverSyncError=null,updatedAt=now,
                segment=r.segment,surveyDirection=r.surveyDirection,sessionId=old.sessionId,
                locationSource=r.locationSource,gpsTimestamp=r.gpsTimestamp,
                gnssProvider=if(s.text("_gpsExplicit")=="true") "TABLET" else old.gnssProvider,
                gnssFixType=if(s.text("_gpsExplicit")=="true")null else old.gnssFixType,
                verticalAccuracyM=if(s.text("_gpsExplicit")=="true")null else old.verticalAccuracyM,
                satellites=if(s.text("_gpsExplicit")=="true")null else old.satellites,
                hdop=if(s.text("_gpsExplicit")=="true")null else old.hdop,
                correctionAge=if(s.text("_gpsExplicit")=="true")null else old.correctionAge,
                isRtkFixed=if(s.text("_gpsExplicit")=="true")false else old.isRtkFixed) else r
            if(old?.status=="ACTIVE") dao.updateRecord(stored) else dao.insertOrCompleteDraft(stored)
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
                else {val category=categories[p.localPath] ?: p.photoCategory;dao.updatePhotoMetadata(p.copy(photoCategory=category,syncStatus=if(category!=p.photoCategory) "PENDING" else p.syncStatus))}
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
        if(finalize) {
            com.tuempresa.inventariovial.core.AutomaticSync.request(db,"SIC",key,now)
        }
        else com.tuempresa.inventariovial.core.AutomaticSync.draft(db,"SIC",key)
        key
    }
    private suspend fun savePhotos(id:String,paths:List<String>,categories:Map<String,String>,captures:Map<String,PhotoCaptureMetadata>,now:Long) {
        val existing=dao.snapshot(id)!!.photos
        var next=(existing.maxOfOrNull {it.photoIndex} ?: 0)+1
        existing.forEach { p ->
            if(p.localPath !in paths) dao.deleteLocalPhoto(p.id,id)
            else {val category=categories[p.localPath] ?: p.photoCategory;dao.updatePhotoMetadata(p.copy(photoCategory=category,syncStatus=if(category!=p.photoCategory) "PENDING" else p.syncStatus))}
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
        save(id,characteristics,owner,finalize,photoPaths,captures=captures)
        if(widths.values.any {it.isNotBlank()} || dao.recordById(widthId)!=null) {
            val width=fromFaja(id).copy(values=fromFaja(id).values+widths.filterKeys {it in setOf("usefulWidthM","totalWidthM")})
            save(widthId,width,owner,finalize && width.completion().canFinalize)
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
