package com.tuempresa.inventariovial.repository

import androidx.room.withTransaction
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.model.form.*
import com.tuempresa.inventariovial.catalog.EngineeringConditions
import com.tuempresa.inventariovial.validation.CaptureValidation
import java.util.UUID

object EngineeringEdits {
    fun form(s:InventorySnapshot):SicFormDetail = when(s.record.sicCode) {
        "SIC-17" -> with(requireNotNull(s.sic17)) {SicFormDetail.Sic17(Sic17FormState(classCode,typeCode,bridgeCode.orEmpty(),inventoriedCode,spans?.toString().orEmpty(),dimension1LengthM?.toString().orEmpty(),dimension2LowerHeightM?.toString().orEmpty(),structuralConditionCode,functionalConditionCode,serviceTypeCode,singularityCode,singularityName.orEmpty(),dimension3UpperHeightM?.toString().orEmpty()))}
        "SIC-23" -> with(requireNotNull(s.sic23)) {SicFormDetail.Sic23(Sic23FormState(classCode,typeCode.orEmpty(),widthM?.toString().orEmpty(),description))}
        "SIC-18" -> with(requireNotNull(s.sic18)) {SicFormDetail.Sic18(Sic18FormState(classCode,typeCode,spans?.toString().orEmpty(),crossSectionCode,dimension1M?.toString().orEmpty(),dimension2M?.toString().orEmpty(),structuralConditionCode,functionalConditionCode,sectionShape=sectionShape.orEmpty()))}
        "SIC-19" -> with(requireNotNull(s.sic19)) {SicFormDetail.Sic19(Sic19FormState(classCode,typeCode,crossSectionCode,structuralConditionCode,functionalConditionCode,structuralCriterion.orEmpty()))}
        "SIC-20" -> with(requireNotNull(s.sic20)) {SicFormDetail.Sic20(Sic20FormState(classCode,typeCode,dimension1M?.toString().orEmpty(),dimension2M?.toString().orEmpty(),structuralConditionCode,functionalConditionCode.orEmpty(),wallLengthMeters?.toString().orEmpty(),s.sicExtension?.values()?.get("_lengthMode") ?: "HISTORICAL"))}
        "SIC-21" -> with(requireNotNull(s.sic21)) {SicFormDetail.Sic21(Sic21FormState(classCode,typeCode,materialCode,conditionCode))}
        "SIC-22" -> with(requireNotNull(s.sic22)) {SicFormDetail.Sic22(Sic22FormState(classCode,typeCode,materialCode,signalCode.orEmpty(),kilometerPostNumber.orEmpty(),conditionCode,
            signWidthM?.toString().orEmpty(),signHeightM?.toString().orEmpty(),lowerEdgeHeightM?.toString().orEmpty(),
            signalCodeSource,signalDescription,signalObservation))}
        else -> error("Este editor requiere SIC-18 a SIC-22.")
    }
    suspend fun save(db:InventoryDatabase,id:String,detail:SicFormDetail,paths:List<String>,categories:Map<String,String>,captures:Map<String,com.tuempresa.inventariovial.camera.PhotoCaptureMetadata> = emptyMap(),expectedUpdatedAt:Long?=null,observations:String?=null,coordinateEdit:Boolean=false,coordinate:com.tuempresa.inventariovial.location.GeoLocation?=null)=db.withTransaction {
        val dao=db.inventoryDao();val s=requireNotNull(dao.snapshot(id));val r=s.record
        require(expectedUpdatedAt==null || r.updatedAt==expectedUpdatedAt) {"La ficha cambió desde que la abriste. Conserva tus cambios y vuelve a abrir la revisión actual."}
        require(r.status=="ACTIVE") {"Solo se pueden editar registros activos."}
        val request=InventorySaveRequest(r.sicCode,r.assetType,r.routeCode,r.roadbedCode,r.startPrCode,r.startDistanceM.toString(),
            r.endPrCode,r.endDistanceM?.toString(),r.sideCode,r.latitude ?: Double.NaN,r.longitude ?: Double.NaN,r.gpsAccuracyM?.toFloat(),r.surveyDate,r.observations.orEmpty(),"",detail,photoPaths=paths)
        val unchangedLegacyType=detail is SicFormDetail.Sic22 && detail.state.typeCode==s.sic22?.typeCode && detail.state.typeCode in setOf("4","5","6")
        // NaN is only a validation sentinel; absent coordinates are never written or fabricated.
        val unchangedHistoricalSide=detail is SicFormDetail.Sic20 && detail.state.classCode==s.sic20?.classCode
        val errors=CaptureValidation.errors(request,validateLocation=r.latitude!=null || r.longitude!=null).filterNot {(unchangedHistoricalSide && it.field=="side") || (unchangedLegacyType && it.field=="typeCode") || it.field in setOf("startPrCode","startDistanceM","endPrCode","endDistanceM")}
        require(errors.isEmpty()) {errors.joinToString("\n") {it.message}}
        when(detail) {
            is SicFormDetail.Sic17 -> with(detail.state) {dao.updateSic17(requireNotNull(s.sic17).copy(classCode=classCode,typeCode=typeCode,bridgeCode=bridgeCode,inventoriedCode=inventoriedCode,spans=spans.toIntOrNull(),dimension1LengthM=dimension1LengthM.replace(',','.').toDoubleOrNull(),dimension2LowerHeightM=dimension2LowerHeightM.replace(',','.').toDoubleOrNull(),structuralConditionCode=structuralConditionCode,functionalConditionCode=functionalConditionCode,serviceTypeCode=serviceTypeCode,singularityCode=singularityCode,singularityName=singularityName,dimension3UpperHeightM=dimension3UpperHeightM.replace(',','.').toDoubleOrNull()))}
            is SicFormDetail.Sic23 -> with(detail.state) {dao.updateSic23(requireNotNull(s.sic23).copy(classCode=classCode,typeCode=typeCode.ifBlank {null},widthM=if(usesWidth)widthM.replace(',','.').toDoubleOrNull() else null,description=description))}

            is SicFormDetail.Sic18 -> with(detail.state) {
                val old=requireNotNull(s.sic18)
                dao.updateSic18(old.copy(classCode=classCode,typeCode=typeCode,spans=spans.toInt(),crossSectionCode=crossSectionCode,
                    dimension1M=dimension1M.replace(',','.').toDoubleOrNull(),dimension2M=if(usesDimension2)dimension2M.replace(',','.').toDoubleOrNull() else old.dimension2M,
                    structuralConditionCode=structuralConditionCode,functionalConditionCode=functionalConditionCode,sectionShape=sectionShape))
                s.sic18a?.let {dao.saveSic18A(it.copy(classCode=classCode,typeCode=typeCode,spans=spans))}
            }
            is SicFormDetail.Sic19 -> with(detail.state) {dao.updateSic19(requireNotNull(s.sic19).copy(classCode=classCode,typeCode=typeCode,crossSectionCode=crossSectionCode,structuralConditionCode=structuralConditionCode,functionalConditionCode=functionalConditionCode,structuralCriterion=structuralCriterion.ifBlank {null}))}
            is SicFormDetail.Sic20 -> with(detail.state) {dao.updateSic20(requireNotNull(s.sic20).copy(classCode=classCode,typeCode=typeCode,dimension1M=dimension1M.replace(',','.').toDoubleOrNull(),dimension2M=if(usesDimension2)dimension2M.replace(',','.').toDoubleOrNull() else s.sic20.dimension2M,structuralConditionCode=structuralConditionCode,functionalConditionCode=functionalConditionCode,wallLengthMeters=wallLengthMeters.replace(',','.').toDoubleOrNull()))}
            is SicFormDetail.Sic21 -> with(detail.state) {dao.updateSic21(requireNotNull(s.sic21).copy(classCode=classCode,typeCode=typeCode,materialCode=materialCode,conditionCode=conditionCode))}
            is SicFormDetail.Sic22 -> with(detail.state) {
                // Removed capture dimensions and historical marker number remain intact in Room.
                dao.updateSic22(requireNotNull(s.sic22).copy(classCode=classCode,typeCode=typeCode,materialCode=materialCode,
                    signalCode=persistedSignalCode,conditionCode=conditionCode,signalCodeSource=signalCodeSource,
                    signalDescription=signalDescription.trim(),signalObservation=signalObservation.trim(),
                    kilometerPostNumber=if(usesKilometerPostNumber) kilometerPostNumber.trim().ifBlank {null} else s.sic22.kilometerPostNumber))
            }
            else -> error("Detalle incompatible")
        }
        run {
            if(detail is SicFormDetail.Sic20 && detail.state.classCode=="14") TechnicalLengthStore.remember(db,r,detail.state.lengthMode)
            InventoryPhotoEdits.save(db,id,paths,categories,captures,maxOf(System.currentTimeMillis(),r.updatedAt+1))
            dao.refreshRecordPhotoStatus(id)
        }
        val revision=maxOf(System.currentTimeMillis(),r.updatedAt+1)
        if(observations!=null || coordinateEdit) {
            coordinate?.let{require(com.tuempresa.inventariovial.road.GeoMath.validCoordinate(it.latitude,it.longitude)){"Coordenadas inválidas"}}
            val current=requireNotNull(dao.recordById(id))
            dao.updateRecord(current.copy(observations=observations?.ifBlank{null} ?: if(observations==null)current.observations else null,
                latitude=if(coordinateEdit)coordinate?.latitude else current.latitude,longitude=if(coordinateEdit)coordinate?.longitude else current.longitude,
                gpsAccuracyM=if(coordinateEdit)coordinate?.horizontalAccuracy?.takeIf{it.isFinite()}?.toDouble() else current.gpsAccuracyM,
                altitudeM=if(coordinateEdit)coordinate?.altitude else current.altitudeM,gpsTimestamp=if(coordinateEdit)coordinate?.timestamp else current.gpsTimestamp,
                locationSource=if(coordinateEdit)coordinate?.source ?: "CONTRACTUAL_PR_ONLY" else current.locationSource,
                gnssProvider=if(coordinateEdit)coordinate?.provider?.name ?: "TABLET" else current.gnssProvider,
                gnssFixType=if(coordinateEdit)coordinate?.fixType else current.gnssFixType,
                verticalAccuracyM=if(coordinateEdit)coordinate?.verticalAccuracy?.toDouble() else current.verticalAccuracyM,
                satellites=if(coordinateEdit)coordinate?.satellites else current.satellites,hdop=if(coordinateEdit)coordinate?.hdop else current.hdop,
                correctionAge=if(coordinateEdit)coordinate?.correctionAge else current.correctionAge,
                isRtkFixed=if(coordinateEdit)coordinate?.isRtkFixed ?: false else current.isRtkFixed))
        }
        dao.markServerPending(id,revision)
        dao.markTechnicalExcelPending(id)
        com.tuempresa.inventariovial.core.AutomaticSync.request(db,"SIC",id,revision)
    }
}
