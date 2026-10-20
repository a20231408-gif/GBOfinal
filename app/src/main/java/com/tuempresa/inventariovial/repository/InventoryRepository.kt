package com.tuempresa.inventariovial.repository

import androidx.room.withTransaction
import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.supplementary.*

import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.InventoryRecordEntity
import com.tuempresa.inventariovial.data.entity.PhotoEntity
import com.tuempresa.inventariovial.data.entity.Sic17Entity
import com.tuempresa.inventariovial.data.entity.Sic18Entity
import com.tuempresa.inventariovial.data.entity.Sic19Entity
import com.tuempresa.inventariovial.data.entity.Sic20Entity
import com.tuempresa.inventariovial.data.entity.Sic21Entity
import com.tuempresa.inventariovial.data.entity.Sic22Entity


class InventoryRepository(
    private val database:
    InventoryDatabase
) {

    private val dao =
        database.inventoryDao()


    fun observeRecordCountByDate(
        date: String
    ) =
        dao.observeRecordCountByDate(
            date
        )


    fun observePendingPhotoCount() =
        dao.observePendingPhotoCount()


    fun observeHistory() = dao.observeHistory()

    fun observeAllRecords() =
        dao.observeAllRecords()


    suspend fun saveSic17(
        record: InventoryRecordEntity,
        detail: Sic17Entity,
        photos: List<PhotoEntity>
    ) {

        database.withTransaction {

            dao.insertOrCompleteDraft(record)

            require(record.sicCode=="SIC-17" && detail.recordId==record.id)
            if(dao.snapshot(record.id)?.sic17==null) dao.insertSic17(detail) else dao.updateSic17(detail)

            mergePhotos(record,photos)
            enqueue(record)
        }
    }


    suspend fun saveSic18(
        record: InventoryRecordEntity,
        detail: Sic18Entity,
        photos: List<PhotoEntity>
    ) {

        database.withTransaction {

            dao.insertOrCompleteDraft(record)

            require(record.sicCode=="SIC-18" && detail.recordId==record.id)
            if(dao.snapshot(record.id)?.sic18==null) dao.insertSic18(detail) else dao.updateSic18(detail)

            mergePhotos(record,photos)
            enqueue(record)
        }
    }


    suspend fun saveSic19(
        record: InventoryRecordEntity,
        detail: Sic19Entity,
        photos: List<PhotoEntity>
    ) {

        database.withTransaction {

            dao.insertOrCompleteDraft(record)

            require(record.sicCode=="SIC-19" && detail.recordId==record.id)
            if(dao.snapshot(record.id)?.sic19==null) dao.insertSic19(detail) else dao.updateSic19(detail)

            mergePhotos(record,photos)
            enqueue(record)
        }
    }


    suspend fun saveSic20(
        record: InventoryRecordEntity,
        detail: Sic20Entity,
        photos: List<PhotoEntity>,
        lengthMode: String = "HISTORICAL"
    ) {

        database.withTransaction {

            com.tuempresa.inventariovial.model.form.Sic20Identity.requireDetail(record.assetType,detail.classCode)
            dao.snapshot(record.id)?.sic20?.let {com.tuempresa.inventariovial.model.form.Sic20Identity.resolve(record.assetType,it.classCode,detail.classCode)}
            if(detail.classCode in setOf("13","14")) {
                val allowed=if(detail.classCode=="13")setOf("S") else setOf("I","D")
                val old=dao.snapshot(record.id)
                require(record.sideCode in allowed || old?.sic20?.classCode==detail.classCode && old.record.sideCode==record.sideCode) {
                    "Túnel: solo SIN OBJETO. Muro: IZQUIERDA o DERECHA."
                }
            }

            dao.insertOrCompleteDraft(record)

            require(record.sicCode=="SIC-20" && detail.recordId==record.id)
            if(dao.snapshot(record.id)?.sic20==null) dao.insertSic20(detail) else dao.updateSic20(detail)
            if(detail.classCode=="14") TechnicalLengthStore.remember(database,record,lengthMode)

            mergePhotos(record,photos)
            enqueue(record)
        }
    }


    suspend fun saveSic21(
        record: InventoryRecordEntity,
        detail: Sic21Entity,
        photos: List<PhotoEntity>
    ) {

        database.withTransaction {

            dao.insertOrCompleteDraft(record)

            require(record.sicCode=="SIC-21" && detail.recordId==record.id)
            if(dao.snapshot(record.id)?.sic21==null) dao.insertSic21(detail) else dao.updateSic21(detail)

            mergePhotos(record,photos)
            enqueue(record)
        }
    }


    suspend fun saveSic22(
        record: InventoryRecordEntity,
        detail: Sic22Entity,
        photos: List<PhotoEntity>
    ) {

        database.withTransaction {

            dao.insertOrCompleteDraft(record)

            require(record.sicCode=="SIC-22" && detail.recordId==record.id)
            if(dao.snapshot(record.id)?.sic22==null) dao.insertSic22(detail) else dao.updateSic22(detail)

            mergePhotos(record,photos)
            enqueue(record)
        }
    }
    suspend fun saveSic23(record: InventoryRecordEntity, detail: Sic23Entity, photos: List<PhotoEntity>) {
        database.withTransaction {
            dao.insertOrCompleteDraft(record)
            require(record.sicCode=="SIC-23" && detail.recordId==record.id)
            if(dao.snapshot(record.id)?.sic23==null) dao.insertSic23(detail) else dao.updateSic23(detail)
            mergePhotos(record,photos)
            enqueue(record)
        }
    }

    /** Draft completion is additive; explicit removal/replacement stays in the authorized editor. */
    private suspend fun mergePhotos(record:InventoryRecordEntity,photos:List<PhotoEntity>) {
        val existing=requireNotNull(dao.snapshot(record.id)).photos.toMutableList()
        var next=(existing.maxOfOrNull {it.photoIndex} ?: 0)+1
        for(photo in photos.distinctBy {it.id}) {
            require(photo.recordId==record.id) {"Fotografía de otra ficha"}
            val sameId=dao.photoById(photo.id)
            require(sameId==null || sameId.recordId==record.id && sameId.localPath==photo.localPath) {"La identidad de la fotografía cambió"}
            val old=sameId ?: existing.firstOrNull {it.localPath==photo.localPath}
            if(old==null) {
                val added=photo.copy(photoIndex=next++,isPrimary=existing.isEmpty())
                dao.insertPhoto(added);existing.add(added)
            }
            else if(old.photoCategory!=photo.photoCategory) dao.updatePhotoMetadata(old.copy(photoCategory=photo.photoCategory,syncStatus="PENDING"))
        }
        dao.refreshRecordPhotoStatus(record.id)
    }

    private suspend fun enqueue(record:InventoryRecordEntity) {
        if(record.status=="ACTIVE") com.tuempresa.inventariovial.core.AutomaticSync.request(database,"SIC",record.id,requireNotNull(dao.recordById(record.id)).updatedAt)
    }

    suspend fun saveSupplementary(recordId: String, state: SupplementaryFormState,expectedUpdatedAt:Long?=null) {
        check(state.format.enabled) { "Formato deshabilitado." }
        require(state.validate().isEmpty()) { state.validate().joinToString("\n") }
        database.withTransaction {
            val snapshot=dao.snapshot(recordId) ?: error("Registro inexistente.")
            require(expectedUpdatedAt==null || snapshot.record.updatedAt==expectedUpdatedAt){"La ficha cambió desde que la abriste; conserva tus cambios y vuelve a abrirla."}
            require(snapshot.record.status=="ACTIVE") { "Solo se pueden editar registros activos." }
            if(state.format!=SupplementaryFormat.SIC18A) {
                require(snapshot.record.sicCode=="SIC-17") { "El formato requiere un puente." }
                val bridge=snapshot.sic17?.bridgeCode.orEmpty()
                require(state.values["bridgeCode"].orEmpty()==bridge) { "El código de puente debe coincidir con SIC-17." }
            } else {
                require(snapshot.record.sicCode=="SIC-18") { "El formato requiere una alcantarilla." }
                val parent=requireNotNull(snapshot.sic18)
                require(state.values["classCode"]==parent.classCode && state.values["typeCode"]==parent.typeCode && state.values["spans"]==parent.spans?.toString()) {"Clase, tipo y ojos/vanos deben coincidir con SIC-18; vuelve a abrir la ficha."}
            }
            when(state.format) {
                SupplementaryFormat.SIC17A -> dao.saveSic17A(Sic17AEntity.from(recordId,state.values))
                SupplementaryFormat.SIC17B -> dao.saveSic17B(Sic17BEntity.from(recordId,state.values))
                SupplementaryFormat.SIC18A -> dao.saveSic18A(Sic18AEntity.from(recordId,state.values))
            }
            val revision=maxOf(System.currentTimeMillis(),snapshot.record.updatedAt+1)
            state.values["observations"]?.let{dao.updateRecord(snapshot.record.copy(observations=it.ifBlank{null}))}
            dao.markTechnicalExcelPending(recordId)
            dao.markServerPending(recordId,revision)
            com.tuempresa.inventariovial.core.AutomaticSync.request(database,"SIC",recordId,revision)
        }
    }
}
