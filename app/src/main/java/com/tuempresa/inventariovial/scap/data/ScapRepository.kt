package com.tuempresa.inventariovial.scap.data

import androidx.room.withTransaction
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.location.GeoLocation
import com.tuempresa.inventariovial.road.GeoMath
import com.tuempresa.inventariovial.scap.catalog.ScapCatalog
import com.tuempresa.inventariovial.scap.domain.*
import java.util.UUID
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ScapRepository(private val db:InventoryDatabase,val catalog:ScapCatalog, private val projectRoutes:com.tuempresa.inventariovial.road.ProjectRouteCatalog? = null) {
    val dao=db.scapDao()
    private fun uuid()=UUID.randomUUID().toString()
    suspend fun create(operator:String,device:String,sessionId:String?,bridgeType:ScapBridgeType?=null):String=db.withTransaction {
        val now=System.currentTimeMillis();val id=uuid();val roadId=uuid()
        val session=sessionId?.let {db.inventoryDao().sessionById(it)}?.takeIf {it.endTime==null}
        val defaults=com.tuempresa.inventariovial.field.FieldSessionDefaults.location(session,routes=projectRoutes)
        // Technical draft: absent coordinates are not exported as an observed (0,0) location.
        db.inventoryDao().insertRecord(InventoryRecordEntity(roadId,"SCAP","PUENTE",defaults.route,defaults.roadbed,"",0.0,null,null,null,
            null,null,null,null,LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),null,"DRAFT","PENDING","PENDING",now,now,
            sessionId=sessionId,segment=defaults.segment.takeIf {it.isNotBlank()},surveyDirection=session?.direction))
        dao.insertInspection(ScapInspectionEntity(id,roadId,"","",now,now,operator,device,"DRAFT","PENDING",null,null,null,null,"MANUAL"))
        if(defaults.route.isNotBlank()) {
            dao.putValue(ScapFieldValueEntity(id,"inspection","route",defaults.route,"SESSION_DEFAULT"))
            dao.putValue(ScapFieldValueEntity(id,"inspection","segment",defaults.segment,"SESSION_DEFAULT"))
        }
        if(defaults.roadbed.isNotBlank()) dao.putValue(ScapFieldValueEntity(id,"inspection","sic17.roadbedCode",defaults.roadbed,"SESSION_DEFAULT"))
        dao.putValue(ScapFieldValueEntity(id,"inspection","categoryName","PUENTE","NEW_INSPECTION_DEFAULT"))
        dao.putValue(ScapFieldValueEntity(id,"inspection","nameContractVersion","1","NEW_INSPECTION_DEFAULT"))
        dao.putValue(ScapFieldValueEntity(id,"inspection","routeType","Nacional","NEW_INSPECTION_DEFAULT"))
        bridgeType?.let {dao.putValue(ScapFieldValueEntity(id,"inspection",ScapBridgeType.FIELD,it.name,"USER_SELECTED"))}
        if(bridgeType!=null) ensureBridgeDefaults(id)
        id
    }
    private suspend fun editable(id:String):ScapInspectionSnapshot {
        val s=dao.snapshot(id) ?: error("Inspección inexistente.")
        check(s.inspection.status!="COMPLETE") {"Reabre la inspección antes de editar."}
        return s
    }
    suspend fun setField(id:String,owner:String,key:String,value:String,source:String="MANUAL")=db.withTransaction {
        var s=editable(id)
        ScapPresentation.fixedStructures.firstOrNull { owner==ScapPresentation.fixedId(id,it) && s.substructures.none {row->row.kind==it} }?.let {
            dao.putSubstructure(ScapPresentation.emptyStructure(id,it))
            s=requireNotNull(dao.snapshot(id))
        }
        val ownerType=ScapValidation.ownerType(s,owner)
        require(ownerType!=null || owner=="G2" || owner=="completion" || s.elements.any {it.element.id==owner} || s.photos.any{it.id==owner}) {"Sección inexistente."}
        if (owner == "inspection" && key == "route" && value.isNotBlank()) projectRoutes?.requireRoute(value)
        if (owner == "inspection" && key == "segment") projectRoutes?.find(s.values()["route"])?.let {
            require(value == it.segmentCode) { "El tramo se deriva de la ruta." }
        }
        require(!(owner=="inspection" && key=="categoryName") || value=="PUENTE") {"La categoría A es PUENTE."}
        dao.putValue(ScapFieldValueEntity(id,owner,key,value,source))
        if(owner=="inspection" && key=="singularityName") {
            if(!s.values().containsKey("categoryName"))dao.putValue(ScapFieldValueEntity(id,owner,"categoryName",s.values()["bridgeName"] ?: s.inspection.bridgeName,"HISTORICAL_A_PRESERVED"))
            dao.putValue(ScapFieldValueEntity(id,owner,"nameContractVersion","1","EXPLICIT_B_EDIT"))
            dao.putValue(ScapFieldValueEntity(id,owner,"bridgeName",value,source))
            dao.updateInspection(s.inspection.copy(bridgeName=value))
        }
        if (owner == "inspection" && key == "route") projectRoutes?.find(value)?.let {
            dao.putValue(ScapFieldValueEntity(id,owner,"segment",it.segmentCode,"PROJECT_ROUTE"))
        }
        val values=s.values(owner)+(key to value)
        if(owner=="inspection" && key==ScapBridgeType.FIELD) require(ScapBridgeType.entries.any {it.name==value}) {"Tipo de puente desconocido."}
        if(owner=="inspection") {
            if(key in setOf("bridgeName","bridgeCode","createdBy")) dao.updateInspection(s.inspection.copy(
                bridgeName=values["bridgeName"] ?: s.inspection.bridgeName,bridgeCode=values["bridgeCode"] ?: s.inspection.bridgeCode,
                createdBy=if(key=="createdBy") value else s.inspection.createdBy))
            val road=db.inventoryDao().recordById(s.inspection.roadRecordId)!!
            // Only bootstrap an absent location from explicitly entered SCAP location fields.
            // Arbitrary SCAP edits must never replace the linked contractual PR/distance.
            val chainage=ScapNumbers.chainage(values["progressive"])
            val seedPr=key=="progressive" && road.startPrCode.isBlank() && chainage!=null && road.locationSource!="SIC_CONFIRMED"
            val seedRoute=key=="route" && road.startPrCode.isBlank() && road.locationSource!="SIC_CONFIRMED"
            val seedRoadbed=key=="sic17.roadbedCode" && road.startPrCode.isBlank() && road.locationSource!="SIC_CONFIRMED"
            if(seedPr || seedRoute || seedRoadbed) db.inventoryDao().updateRecord(road.copy(
                routeCode=if(seedRoute) value else road.routeCode,
                roadbedCode=if(seedRoadbed) value else road.roadbedCode,
                segment=if(seedRoute) projectRoutes?.segment(value,values["segment"]) else road.segment,
                startPrCode=if(seedPr) (chainage!!/1000).toInt().toString().padStart(4,'0') else road.startPrCode,
                startDistanceM=if(seedPr) chainage!!%1000 else road.startDistanceM,
                locationSource=if(seedPr) "SCAP_PROGRESSIVE" else road.locationSource,
                updatedAt=maxOf(System.currentTimeMillis(),road.updatedAt+1)))
        }
        s.spans.find {it.id==owner}?.let {span->
            if(key=="category") {dao.putValue(ScapFieldValueEntity(id,owner,"type","",source))}
            dao.putSpan(span.copy(lengthM=ScapNumbers.decimal(values["lengthM"]),category=values["category"].orEmpty(),
                type=if(key=="category") "" else values["type"].orEmpty(),secondaryCharacteristic=values["secondaryCharacteristic"].orEmpty(),
                edgeCondition=values["edgeCondition"].orEmpty(),predominantMaterial=values["predominantMaterial"].orEmpty()))
        }
        s.substructures.find {it.id==owner}?.let {e->dao.putSubstructure(e.copy(elevationType=values["elevationType"].orEmpty(),
            elevationMaterial=values["elevationMaterial"].orEmpty(),foundationType=values["foundationType"].orEmpty(),foundationMaterial=values["foundationMaterial"].orEmpty(),soil=values["soil"].orEmpty()))}
        s.supports.find {it.id==owner}?.let {e->dao.putSupport(e.copy(type=values["type"].orEmpty(),material=values["material"].orEmpty(),location=values["location"].orEmpty(),number=values["number"]?.toIntOrNull()))}
        s.joints.find {it.id==owner}?.let {e->dao.putJoint(e.copy(type=values["jointType"].orEmpty(),material=values["jointMaterial"].orEmpty()))}
        s.profile.find {it.id==owner}?.let {e->dao.putProfilePoint(e.copy(distanceM=ScapNumbers.decimal(values["distanceM"]),downstreamM=ScapNumbers.decimal(values["downstreamM"]),upstreamM=ScapNumbers.decimal(values["upstreamM"]),axisM=ScapNumbers.decimal(values["axisM"])))}
        s.elements.find {it.element.id==owner}?.let {entry->
            if(key=="quantity") dao.putElement(entry.element.copy(quantity=ScapNumbers.decimal(value)))
            else if(key=="description") dao.putElement(entry.element.copy(description=value))
            else if(key=="unit") dao.putElement(entry.element.copy(unit=value))
            else if(key=="importanceFactor") dao.putElement(entry.element.copy(importanceFactor=ScapNumbers.decimal(value) ?: 0.0))
            else if(key.matches(Regex("percent[0-5]"))) {
                val c=entry.condition
                val previous=c?.let {com.tuempresa.inventariovial.scap.calculator.ScapPercentages.values(it)}
                val p=(0..5).map {i->if(values.containsKey("percent$i")) ScapNumbers.decimal(values["percent$i"]) else previous?.get(i)}
                dao.putCondition(ScapElementConditionEntity(c?.id ?: uuid(),owner,p[0],p[1],p[2],p[3],p[4],p[5]))
            }
        }
        dao.touch(id,System.currentTimeMillis())
    }
    suspend fun saveSicLocation(id:String,route:String,roadbed:String,pr:String,distance:String,endPr:String="",endDistance:String="")=db.withTransaction {
        val s=editable(id);val daoRoad=db.inventoryDao()
        val road=requireNotNull(daoRoad.recordById(s.inspection.roadRecordId))
        require(route.isNotBlank());projectRoutes?.requireRoute(route)
        require(roadbed in com.tuempresa.inventariovial.catalog.SicCatalogRepository.roadbedCodes) { "Selecciona una calzada del catálogo SIC." }
        require(pr.matches(Regex("[0-9]{1,4}"))) { "Código PR: de 1 a 4 dígitos." }
        val metres=requireNotNull(ScapNumbers.decimal(distance)) { "Ingresa la distancia desde PR." }
        require(com.tuempresa.inventariovial.validation.ContractualLocationInput.distanceError(distance)==null) { "Distancia: máximo tres cifras enteras." }
        require(endPr.isBlank()==endDistance.isBlank()) { "Completa ambos campos de ubicación final." }
        val end=if(endPr.isBlank()) null else requireNotNull(ScapNumbers.decimal(endDistance))
        require(end==null || (endPr.matches(Regex("[0-9]{1,4}")) && com.tuempresa.inventariovial.validation.ContractualLocationInput.distanceError(endDistance)==null)) { "Ubicación final inválida." }
        daoRoad.updateRecord(road.copy(routeCode=route,roadbedCode=roadbed,startPrCode=pr.padStart(4,'0'),startDistanceM=metres,
            endPrCode=endPr.takeIf{it.isNotBlank()}?.padStart(4,'0'),endDistanceM=end,locationSource="SIC_CONFIRMED",
            updatedAt=maxOf(System.currentTimeMillis(),road.updatedAt+1)))
        mapOf("routeCode" to route,"roadbedCode" to roadbed,"startPrCode" to pr,"startDistanceM" to distance,"endPrCode" to endPr,"endDistanceM" to endDistance).forEach {(key,value)->
            dao.putValue(ScapFieldValueEntity(id,"inspection","sicLocation.$key",value,if(key in setOf("startPrCode","endPrCode","startDistanceM","endDistanceM")) "MANUAL_PENDING_SIC3" else "MANUAL_SIC"))
        }
        // Retire only the obsolete duplicate calzada value; the linked record owns location.
        dao.deleteValue(id,"inspection","sic17.roadbedCode")
        dao.touch(id,System.currentTimeMillis())
    }
    suspend fun setSicField(id:String,format:String,key:String,value:String)=db.withTransaction {
        val s=editable(id);val prefix=BridgeSicContract.prefix(format)
        require(key in BridgeSicContract.fields(format).map{it.key}) { "Campo SIC desconocido." }
        require(key !in BridgeSicContract.unresolved(format)) { "Campo pendiente de definición contractual." }
        require(format!="SIC-17" || key !in BridgeSicContract.locationKeys) { "Edita la ubicación vinculada." }
        val kind=BridgeSicContract.fields(format,BridgeSicContract.manualValues(s,format)).first{it.key==key}.kind
        val stored=if(kind==com.tuempresa.inventariovial.supplementary.FieldKind.CODE) value.substringBefore(" - ") else value
        val inherited=when(format) {"SIC-17"->ScapToSicMapper.mapToSic17(s);"SIC-17A"->ScapToSicMapper.mapToSic17A(s);else->ScapToSicMapper.mapToSic17B(s)}.fields.firstOrNull{it.sicField==key && !it.scapField.startsWith(prefix)}
        val source=s.values()[prefix+"override."+key]?.takeIf{it.startsWith("SCAP_OVERRIDE:")} ?: inherited?.let{"SCAP_OVERRIDE:${it.scapField}"} ?: "MANUAL_SIC"
        val values=BridgeSicContract.manualValues(s,format)+(key to stored)
        when(format) {
            "SIC-17A" -> db.inventoryDao().saveSic17A(Sic17AEntity.from(s.inspection.roadRecordId,values))
            "SIC-17B" -> db.inventoryDao().saveSic17B(Sic17BEntity.from(s.inspection.roadRecordId,values))
            else -> dao.putValue(ScapFieldValueEntity(id,"inspection",prefix+key,stored,source))
        }
        dao.putValue(ScapFieldValueEntity(id,"inspection",prefix+"override."+key,source,source))
        if(key=="wearingSurface") dao.putValue(ScapFieldValueEntity(id,"inspection",prefix+"wearingSurfaceDescription",value,"MANUAL_SIC"))
        if(format=="SIC-17" && key=="classCode") {
            dao.putValue(ScapFieldValueEntity(id,"inspection","sic17.typeCode","","MANUAL_SIC"))
            dao.putValue(ScapFieldValueEntity(id,"inspection","sic17.override.typeCode","MANUAL_SIC","MANUAL_SIC"))
        }
        dao.touch(id,System.currentTimeMillis())
    }
    suspend fun restoreSicField(id:String,format:String,key:String)=db.withTransaction {
        val s=editable(id);val prefix=BridgeSicContract.prefix(format)
        require(key in BridgeSicContract.fields(format).map{it.key} && key !in BridgeSicContract.unresolved(format))
        val values=BridgeSicContract.manualValues(s,format)+(key to "")
        when(format) {
            "SIC-17A" -> db.inventoryDao().saveSic17A(Sic17AEntity.from(s.inspection.roadRecordId,values))
            "SIC-17B" -> db.inventoryDao().saveSic17B(Sic17BEntity.from(s.inspection.roadRecordId,values))
            else -> dao.deleteValue(id,"inspection",prefix+key)
        }
        dao.deleteValue(id,"inspection",prefix+"override."+key)
        dao.touch(id,System.currentTimeMillis())
    }
    suspend fun addRow(id:String,kind:String)=db.withTransaction {
        val s=editable(id);val rowId=uuid()
        when(kind) {
            "SPAN" -> {
                val model=ScapBridgeType.from(s)?.let(ScapCaptureModels::forType)
                dao.putSpan(ScapSpanEntity(rowId,id,(s.spans.maxOfOrNull {it.spanIndex} ?: 0)+1,null,model?.category.orEmpty(),model?.structuralType.orEmpty(),"","",""))
                model?.let {
                    dao.putValue(ScapFieldValueEntity(id,rowId,"category",it.category,"BRIDGE_TYPE"))
                    dao.putValue(ScapFieldValueEntity(id,rowId,"type",it.structuralType,"BRIDGE_TYPE"))
                }
            }
            "JOINT" -> dao.putJoint(ScapJointEntity(rowId,id,(s.joints.maxOfOrNull {it.jointIndex} ?: 0)+1,"",""))
            "BEARING" -> dao.putSupport(ScapSupportEntity(rowId,id,(s.supports.maxOfOrNull {it.supportIndex} ?: 0)+1,"","","",null))
            "PROFILE" -> dao.putProfilePoint(ScapProfilePointEntity(rowId,id,(s.profile.maxOfOrNull {it.pointIndex} ?: 0)+1,null,null,null,null))
            else -> {
                require(kind in setOf("LEFT_ABUTMENT","RIGHT_ABUTMENT","PIER","LEFT_ANCHOR","RIGHT_ANCHOR"))
                if(kind!="PIER") require(s.substructures.none {it.kind==kind}) {"Este elemento ya existe."}
                dao.putSubstructure(ScapSubstructureEntity(rowId,id,kind,(s.substructures.filter {it.kind==kind}.maxOfOrNull {it.elementIndex} ?: 0)+1,"","","","",""))
            }
        }
        dao.touch(id,System.currentTimeMillis())
    }
    suspend fun removeRow(id:String,rowId:String)=db.withTransaction {
        editable(id);dao.deleteValues(id,rowId);dao.deleteSpan(id,rowId);dao.deleteSubstructure(id,rowId)
        dao.deleteJoint(id,rowId);dao.deleteSupport(id,rowId);dao.deleteProfilePoint(id,rowId);dao.touch(id,System.currentTimeMillis())
    }
    suspend fun selectElement(id:String,code:String,present:Boolean)=db.withTransaction {
        val s=editable(id);val item=catalog.forSnapshot(s).element(code)
        val old=s.elements.find {it.element.elementCode==code}?.element
        require(item!=null || (old!=null && s.values(old.id)["elementSource"]=="MANUAL")) {"Código SCAP desconocido."}
        val element=old?.copy(isPresent=present) ?: requireNotNull(item).let{ScapElementEntity(uuid(),id,code,it.name,null,it.unit,it.importanceFactor,it.group,present)}
        dao.putElement(element)
        dao.putValue(ScapFieldValueEntity(id,element.id,"presence",if(present) "PRESENT" else "NOT_APPLICABLE","MANUAL"))
        dao.touch(id,System.currentTimeMillis())
    }
    suspend fun addElementForReview(id:String,code:String)=db.withTransaction {
        val s=editable(id)
        if(s.elements.any {it.element.elementCode==code}) return@withTransaction
        val item=requireNotNull(catalog.forSnapshot(s).element(code))
        val element=ScapElementEntity(uuid(),id,code,item.name,null,item.unit,item.importanceFactor,item.group,false)
        dao.putElement(element)
        dao.putValue(ScapFieldValueEntity(id,element.id,"presence","UNSET","MANUAL"))
        dao.touch(id,System.currentTimeMillis())
    }
    suspend fun setLocation(id:String,fix:GeoLocation)=db.withTransaction {
        val s=editable(id);require(GeoMath.validCoordinate(fix.latitude,fix.longitude))
        dao.updateInspection(s.inspection.copy(latitude=fix.latitude,longitude=fix.longitude,gpsAccuracyM=fix.horizontalAccuracy.toDouble().takeIf {it.isFinite()},gpsTimestamp=fix.timestamp,locationSource=fix.provider.name))
        if(ScapBridgeType.from(s)!=null) {
            dao.putValue(ScapFieldValueEntity(id,"inspection","latitude",fix.latitude.toString(),fix.provider.name))
            dao.putValue(ScapFieldValueEntity(id,"inspection","longitude",fix.longitude.toString(),fix.provider.name))
        }
        val r=db.inventoryDao().recordById(s.inspection.roadRecordId)!!
        db.inventoryDao().updateRecord(r.copy(latitude=fix.latitude,longitude=fix.longitude,gpsAccuracyM=fix.horizontalAccuracy.toDouble().takeIf {it.isFinite()},gpsTimestamp=fix.timestamp,gnssProvider=fix.provider.name))
        dao.touch(id,System.currentTimeMillis())
    }
    suspend fun saveDefect(id:String,defect:ScapDefectEntity)=db.withTransaction {
        val s=editable(id);require(defect.inspectionId==id)
        require(defect.elementCode==null || catalog.forSnapshot(s).element(defect.elementCode)!=null || s.elements.any{it.element.elementCode==defect.elementCode && s.values(it.element.id)["elementSource"]=="MANUAL"})
        require(defect.photoId==null || s.photos.any {it.id==defect.photoId})
        dao.putDefect(defect);dao.touch(id,System.currentTimeMillis())
    }
    suspend fun removeDefect(id:String,defectId:String)=db.withTransaction {editable(id);dao.deleteDefect(id,defectId);dao.touch(id,System.currentTimeMillis())}
    suspend fun setDefectField(id:String,defectId:String,key:String,value:String,draft:ScapDefectEntity?=null)=db.withTransaction {
        val s=editable(id);val d=s.defects.singleOrNull {it.id==defectId} ?: requireNotNull(draft)
        require(d.id==defectId && d.inspectionId==id)
        val updated=when(key) {
            "description"->d.copy(description=value)
            "locationDescription"->d.copy(locationDescription=value)
            "elementCode"->d.copy(elementCode=value.ifBlank{null})
            "photoId"->d.copy(photoId=value.ifBlank{null})
            "validatedByUser"->d.copy(validatedByUser=value=="true")
            else->error("Campo de defecto desconocido")
        }
        saveDefect(id,updated)
    }
    suspend fun setPhotoMetadata(id:String,photoId:String,category:String,code:String?)=db.withTransaction {
        val s=editable(id);require(category in PHOTO_CATEGORIES);require(knownElement(s,code))
        val p=s.photos.single{it.id==photoId}
        if(p.photoCategory!=category) com.tuempresa.inventariovial.repository.InventoryPhotoEdits.archive(db,p,maxOf(System.currentTimeMillis(),s.inspection.updatedAt+1),"RECATEGORIZED")
        dao.updatePhoto(p.copy(photoCategory=category,scapElementCode=code,syncStatus="PENDING"));dao.touch(id,System.currentTimeMillis())
    }
    suspend fun describePhoto(id:String,photoId:String,description:String)=db.withTransaction {
        val s=editable(id);dao.updatePhoto(s.photos.single {it.id==photoId}.copy(description=description));dao.touch(id,System.currentTimeMillis())
    }
    suspend fun stampPhoto(id:String,photoId:String,path:String?)=db.withTransaction {
        val s=editable(id);val photo=s.photos.single {it.id==photoId}
        if(path!=null) {
            require(java.io.File(path).isFile)
            require(java.io.File(path).canonicalPath != java.io.File(photo.originalPath ?: photo.localPath).canonicalPath)
        }
        dao.updatePhoto(photo.copy(originalPath=photo.originalPath ?: photo.localPath,stampedPath=path))
        dao.touch(id,System.currentTimeMillis())
    }
    suspend fun removePhoto(id:String,photoId:String)=db.withTransaction {
        val s=editable(id)
        com.tuempresa.inventariovial.repository.InventoryPhotoEdits.archive(db,s.photos.single {it.id==photoId},maxOf(System.currentTimeMillis(),s.inspection.updatedAt+1),"REMOVED")
        s.defects.filter {it.photoId==photoId}.forEach {dao.putDefect(it.copy(photoId=null))}
        dao.deletePhoto(id,photoId);dao.deleteValues(id,photoId)
        s.photos.filter{it.id!=photoId}.sortedBy{it.photoIndex}.forEachIndexed{i,p->dao.updatePhoto(p.copy(photoIndex=i+1,isPrimary=i==0))}
        dao.touch(id,System.currentTimeMillis())
    }
    suspend fun movePhoto(id:String,photoId:String,direction:Int)=db.withTransaction {
        val s=editable(id);val ordered=s.photos.sortedBy {it.photoIndex};val index=ordered.indexOfFirst {it.id==photoId}
        val other=ordered.getOrNull(index+direction) ?: return@withTransaction
        val current=ordered[index]
        // A free temporary index avoids the unique recordId/photoIndex constraint during the swap.
        dao.updatePhoto(current.copy(photoIndex=(ordered.maxOfOrNull {it.photoIndex} ?: 0)+1))
        dao.updatePhoto(other.copy(photoIndex=current.photoIndex,isPrimary=current.isPrimary))
        dao.updatePhoto(current.copy(photoIndex=other.photoIndex,isPrimary=other.isPrimary));dao.touch(id,System.currentTimeMillis())
    }
    suspend fun removeSketch(id:String,sketchId:String)=db.withTransaction {editable(id);dao.deleteSketch(id,sketchId);dao.touch(id,System.currentTimeMillis())}
    suspend fun addPhoto(id:String,path:String,category:String,code:String?,stampedPath:String?=null,capture:com.tuempresa.inventariovial.camera.PhotoCaptureMetadata?=null)=db.withTransaction {
        val s=editable(id);require(category in PHOTO_CATEGORIES);require(knownElement(s,code))
        val index=(s.photos.maxOfOrNull {it.photoIndex} ?: 0)+1
        val writable = stampedPath?.let { com.tuempresa.inventariovial.camera.PhotoExif.writableCopy(
            java.io.File(path), java.io.File(it), java.io.File(it).parentFile!!) }
        val photo=(capture ?: com.tuempresa.inventariovial.camera.PhotoCaptureMetadata.fromOriginal(path)).applyTo(PhotoEntity(uuid(),s.inspection.roadRecordId,index,path,index==1,null,null,null,"PENDING",System.currentTimeMillis(),scapInspectionId=id,scapElementCode=code,photoCategory=category,originalPath=path,stampedPath=writable?.absolutePath))
        if (writable != null) com.tuempresa.inventariovial.camera.PhotoExif.write(writable,java.io.File(path),photo)
        db.inventoryDao().insertPhoto(photo)
        dao.touch(id,System.currentTimeMillis())
    }
    suspend fun addPhotoRecord(id:String,name:String,description:String=""):String=db.withTransaction {
        val s=editable(id);val index=(s.photos.maxOfOrNull{it.photoIndex} ?: 0)+1;val photoId=uuid()
        db.inventoryDao().insertPhoto(PhotoEntity(photoId,s.inspection.roadRecordId,index,"",index==1,null,null,null,"PENDING",System.currentTimeMillis(),scapInspectionId=id,photoCategory="OTHER",description=description))
        dao.putValue(ScapFieldValueEntity(id,photoId,"photoName",name,"MANUAL"));dao.touch(id,System.currentTimeMillis());photoId
    }
    suspend fun replacePhoto(id:String,photoId:String,path:String,capture:com.tuempresa.inventariovial.camera.PhotoCaptureMetadata?=null)=db.withTransaction {
        val s=editable(id);val old=s.photos.single{it.id==photoId};require(java.io.File(path).isFile)
        com.tuempresa.inventariovial.repository.InventoryPhotoEdits.archive(db,old,maxOf(System.currentTimeMillis(),s.inspection.updatedAt+1),"REPLACED")
        val metadata=capture ?: com.tuempresa.inventariovial.camera.PhotoCaptureMetadata.fromOriginal(path)
        dao.updatePhoto(metadata.applyTo(old.copy(localPath=path,originalPath=path,stampedPath=null,createdAt=metadata.capturedAt ?: System.currentTimeMillis(),syncStatus="PENDING")))
        dao.touch(id,System.currentTimeMillis())
    }
    suspend fun clearPhotoImage(id:String,photoId:String)=db.withTransaction {
        val s=editable(id);val old=s.photos.single{it.id==photoId}
        com.tuempresa.inventariovial.repository.InventoryPhotoEdits.archive(db,old,maxOf(System.currentTimeMillis(),s.inspection.updatedAt+1),"REMOVED")
        dao.updatePhoto(old.copy(localPath="",originalPath="",stampedPath=null,syncStatus="PENDING"));dao.touch(id,System.currentTimeMillis())
    }
    suspend fun updatePhotoCapture(id:String,path:String,capture:com.tuempresa.inventariovial.camera.PhotoCaptureMetadata)=db.withTransaction {
        val s=editable(id)
        s.photos.firstOrNull {it.localPath==path}?.let { photo ->
            val updated=capture.applyTo(photo)
            updated.stampedPath?.let {com.tuempresa.inventariovial.camera.PhotoExif.write(java.io.File(it),java.io.File(path),updated)}
            dao.updatePhoto(updated);dao.touch(id,System.currentTimeMillis())
        }
    }
    suspend fun addSketch(id:String,type:String,path:String)=db.withTransaction {
        editable(id);require(type in SKETCH_TYPES);dao.insertSketch(ScapSketchEntity(uuid(),id,type,path));dao.touch(id,System.currentTimeMillis())
    }
    suspend fun reopen(id:String)=db.withTransaction {requireNotNull(dao.inspection(id));dao.touch(id,System.currentTimeMillis());com.tuempresa.inventariovial.core.AutomaticSync.draft(db,"SCAP",id)}
    /** Flushes a draft without applying completion/export validation. Raw input stays in scap_values. */
    suspend fun markExcelGenerated(id:String,revision:Long)=db.withTransaction {
        val s=dao.snapshot(id) ?: return@withTransaction
        if(s.inspection.updatedAt==revision)dao.putValue(ScapFieldValueEntity(id,"completion","excelRevision",revision.toString(),"LOCAL_EXPORT"))
    }
    suspend fun saveDraft(id:String)=db.withTransaction {
        val s=requireNotNull(dao.snapshot(id)) {"Inspección inexistente."}
        check(s.inspection.status!="COMPLETE") {"Reabre la ficha para guardar un borrador."}
        val completion=ScapCompletion.assess(s,catalog.forSnapshot(s))
        dao.putValue(ScapFieldValueEntity(id,"completion","status",if(completion.pending.isEmpty())"COMPLETE" else "INCOMPLETE","REVIEW"))
        dao.updateInspection(s.inspection.copy(status="DRAFT",syncStatus="PENDING",updatedAt=maxOf(System.currentTimeMillis(),s.inspection.updatedAt+1)))
    }
    suspend fun addManualElement(id:String,code:String,description:String,quantity:String,unit:String,factor:String)=db.withTransaction {
        val s=editable(id);val active=catalog.forSnapshot(s)
        require(code.isNotBlank()) {"Ingresa un código para identificar el elemento manual."}
        require(active.element(code.trim())==null && s.elements.none {it.element.elementCode==code.trim()}) {"El código ya existe; usa el elemento del catálogo."}
        val row=ScapElementEntity(uuid(),id,code.trim(),description,ScapNumbers.decimal(quantity),unit,ScapNumbers.decimal(factor) ?: 0.0,"DETALLES",true)
        dao.putElement(row)
        mapOf("elementSource" to "MANUAL","presence" to "PRESENT","quantity" to quantity,"unit" to unit,"importanceFactor" to factor,"description" to description).forEach {(key,value)->dao.putValue(ScapFieldValueEntity(id,row.id,key,value,"MANUAL"))}
        dao.touch(id,System.currentTimeMillis())
    }
    suspend fun setSicLocationDraft(id:String,key:String,value:String)=db.withTransaction {
        editable(id);require(key in BridgeSicContract.locationKeys)
        dao.putValue(ScapFieldValueEntity(id,"inspection","sicLocation.$key",value,if(key in setOf("startPrCode","endPrCode","startDistanceM","endDistanceM")) "MANUAL_PENDING_SIC3" else "MANUAL_SIC"))
        dao.touch(id,System.currentTimeMillis())
    }
    suspend fun complete(id:String,warningsConfirmed:Boolean=false)=db.withTransaction {
        val s=editable(id);val review=ScapValidation.review(s,catalog.forSnapshot(s))
        require(review.errors.isEmpty()) {review.errors.joinToString("\n")}
        val completion=ScapCompletion.assess(s,catalog.forSnapshot(s))
        dao.putValue(ScapFieldValueEntity(id,"completion","status",if(completion.pending.isEmpty())"COMPLETE" else "INCOMPLETE","REVIEW"))
        require(completion.pending.isEmpty() || warningsConfirmed) {"Confirma el cierre con datos pendientes."}
        val revision=maxOf(System.currentTimeMillis(),s.inspection.updatedAt+1)
        dao.updateInspection(s.inspection.copy(status="COMPLETE",syncStatus="PENDING",updatedAt=revision))
        com.tuempresa.inventariovial.core.AutomaticSync.request(db,"SCAP",id,revision,warningsConfirmed)
    }
    private fun knownElement(snapshot:ScapInspectionSnapshot,code:String?)=code==null || catalog.forSnapshot(snapshot).element(code)!=null ||
        snapshot.elements.any{it.element.elementCode==code && snapshot.values(it.element.id)["elementSource"]=="MANUAL"}
    suspend fun ensureBridgeDefaults(id:String)=db.withTransaction {
        val s=dao.snapshot(id) ?: return@withTransaction
        val active=catalog.forSnapshot(s)
        if(active.bridgeType==null || s.inspection.status=="COMPLETE" || s.values()["bridgeDefaultsVersion"]=="1")return@withTransaction
        active.canonicalElements.forEach {d->
            val old=s.elements.find{it.element.elementCode==d.templateCode}?.element
            val e=old ?: ScapElementEntity("$id:${d.stableId}",id,d.templateCode,d.defaultName,null,d.unit,d.importanceFactor ?: 0.0,d.group,false)
            if(old==null)dao.putElement(e)
            val metadata=mapOf("stableTemplateId" to d.stableId,"templateCode" to d.templateCode,"exportCode" to d.exportCode,"defaultName" to d.defaultName)
            metadata.forEach {(key,value)->if(!s.values(e.id).containsKey(key))dao.putValue(ScapFieldValueEntity(id,e.id,key,value,"TEMPLATE"))}
            if(old==null) {
                dao.putValue(ScapFieldValueEntity(id,e.id,"presence","UNSET","TEMPLATE"))
                dao.putValue(ScapFieldValueEntity(id,e.id,"globalDescription","","TEMPLATE"))
                // No G.1 example quantities or ratings are ever copied into a new inspection.
                dao.putValue(ScapFieldValueEntity(id,e.id,"importanceFactor",d.importanceFactor?.toString().orEmpty(),"TEMPLATE"))
            }
        }
          for(index in 1..2)if(s.supports.none{it.supportIndex==index})dao.putSupport(ScapSupportEntity("$id:support:$index",id,index,"","","",null))
        dao.putValue(ScapFieldValueEntity(id,"inspection","bridgeDefaultsVersion","1","TEMPLATE"))
          dao.touch(id,System.currentTimeMillis())
    }
    suspend fun removeManualElement(id:String,elementId:String)=db.withTransaction {
        val s=editable(id);require(s.values(elementId)["elementSource"]=="MANUAL") {"Los elementos de la plantilla se marcan No aplica."}
        val e=s.elements.single{it.element.id==elementId}.element
        // Associated photos remain; linked observations become general observations.
        s.defects.filter{it.elementCode==e.elementCode}.forEach{dao.putDefect(it.copy(elementCode=null))}
        s.photos.filter{it.scapElementCode==e.elementCode}.forEach{dao.updatePhoto(it.copy(scapElementCode=null))}
        dao.deleteValues(id,elementId);dao.deleteElement(id,elementId);dao.touch(id,System.currentTimeMillis())
    }
    companion object {
        val PHOTO_CATEGORIES=ScapPhotoCategories.labels.keys.toList()
        val SKETCH_TYPES=listOf("ELEVATION","PLAN","CROSS_SECTION")
    }
}
