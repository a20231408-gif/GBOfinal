package com.tuempresa.inventariovial.pavement

import androidx.room.withTransaction
import com.tuempresa.inventariovial.auth.AuthSessionManager
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.util.UUID

class PavementRepository(private val db: InventoryDatabase, private val sessions: AuthSessionManager) {
    private val dao = db.pavementDao()
    fun samples() = dao.observeSamples()
    fun sections() = dao.observeSections()
    suspend fun saveSection(section:PavementSectionEntity) = db.withTransaction {
        val user=sessions.requireUser()
        val old=dao.observeSections().first().firstOrNull{it.ruta==section.ruta && it.tramo==section.tramo && it.surface==section.surface}
        require(old==null || old.inspectorUserId==user.id) { "Solo quien configuró la sección puede modificarla." }
        require(section.areaM2.isFinite() && section.areaM2>0) { "Área de sección positiva." }
        val samples=dao.observeSamples().first().filter{it.ruta==section.ruta && it.tramo==section.tramo && it.surface==section.surface}
        require(samples.any{it.inspectorUserId==user.id}) { "Debes tener una muestra en esta sección." }
        require(samples.map{it.id}.containsAll(section.additionalIds())) { "Muestras adicionales fuera de esta sección." }
        require(samples.any{it.id !in section.additionalIds()}) { "Conserva al menos una muestra representativa." }
        require(samples.sumOf{it.anchoM*it.largoM}<=section.areaM2+1e-6) { "El área de sección es menor que la suma de las muestras." }
        dao.saveSection(section.copy(inspectorUserId=old?.inspectorUserId?:user.id))
    }
    fun damages(id: String) = dao.observeDamages(id)
    fun observeSample(id: String) = dao.observeSample(id)
    suspend fun photos(id: String) = dao.photos(id)
    suspend fun saveSic(id: String, format: PavementSicFormat, damageId: String, values: Map<String,String>) = db.withTransaction {
        val user=sessions.requireUser()
        val sample=requireNotNull(dao.sample(id))
        require(sample.inspectorUserId==user.id) { "Solo el inspector creador puede completar el SIC." }
        require(dao.damage(damageId)?.sampleId==id) { "Daño ajeno a la muestra." }
        dao.saveSample(sample.copy(sicDataJson=PavementSic.update(sample,format,damageId,values),updatedAt=System.currentTimeMillis()))
    }
    suspend fun newSample(surface: PavementSurface): String = db.withTransaction {
        val user = sessions.requireUser()
        val id = UUID.randomUUID().toString()
        dao.saveSample(PavementHeader.newSample(id, surface, user.id, user.displayName, dao.observeSamples().first(),
            fieldSession = db.inventoryDao().currentSession()))
        id
    }
    private suspend fun editable(id: String): PavementSampleEntity {
        val user = sessions.requireUser()
        val sample = requireNotNull(dao.sample(id))
        require(sample.inspectorUserId == user.id) { "Solo el inspector creador puede editar esta muestra." }
        require(sample.status != "CLOSED") { "La muestra está cerrada." }
        return sample
    }
    suspend fun saveSample(input: PavementSampleEntity) = db.withTransaction {
        val sample = PavementHeader.normalize(input)
        val old = editable(sample.id)
        require(sample.anchoM.isFinite() && sample.anchoM > 0 && sample.largoM.isFinite() && sample.largoM > 0) { "Ancho y largo deben ser positivos." }
        require(sample.ruta.isNotBlank() && sample.tramo.isNotBlank() && sample.carril.isNotBlank()) { "Completa ruta, tramo y carril." }
        require(sample.muestra > 0) { "Muestra debe ser un consecutivo positivo." }
        require(sample.carril in PavementHeader.lanes) { "Selecciona Derecho o Izquierdo." }
        require(sample.calzada in PavementHeader.roadbeds) { "Selecciona una calzada del catálogo SIC." }
        require(!PavementHeader.duplicate(sample, dao.observeSamples().first())) { "Unidad duplicada en ruta, tramo, calzada y superficie. Modifica el número." }
        require(sample.surface == old.surface) { "No cambies la superficie de una muestra existente." }
        require(sample.surface != "RIGID" || (sample.slabRows>0 && sample.slabColumns>0 && sample.slabRows.toLong()*sample.slabColumns<=1000)) { "Define una grilla válida de losas (máximo 1000)." }
        require(sample.laneWidthM == null || (sample.laneWidthM.isFinite() && sample.laneWidthM>0)) { "El ancho de carril debe ser positivo." }
        val damages = dao.damages(sample.id)
        require(damages.isEmpty() || (old.anchoM == sample.anchoM && old.largoM == sample.largoM && old.slabRows == sample.slabRows && old.slabColumns == sample.slabColumns)) { "No cambies las dimensiones después de registrar daños." }
        dao.saveSample(sample.copy(inspectorUserId = old.inspectorUserId, inspectorNombre = old.inspectorNombre, status=old.status, fecha=old.fecha, createdAt=old.createdAt, calculationSnapshot=old.calculationSnapshot, sicDataJson=old.sicDataJson, updatedAt = System.currentTimeMillis()))
    }
    suspend fun saveDamage(damage: PavementDamageEntity) = db.withTransaction {
        val sample = editable(damage.sampleId)
        PavementRules.validate(sample, damage, dao.damages(sample.id))
        val user = sessions.requireUser()
        val old = dao.damage(damage.id)
        require(old == null || old.sampleId == sample.id) { "El daño pertenece a otra muestra." }
        val next = (dao.damages(sample.id).maxOfOrNull { it.correlativo } ?: 0) + 1
        dao.saveDamage(damage.copy(severity = PavementSeverity.canonical(damage.severity), correlativo = old?.correlativo ?: next, inspectorUserId = old?.inspectorUserId ?: user.id,
            inspectorName = old?.inspectorName ?: user.displayName))
    }
    suspend fun closeSample(id: String, calculator: PavementCalculator? = null) = db.withTransaction {
        val sample = editable(id)
        require(sample.anchoM > 0 && sample.largoM > 0 && sample.tramo.isNotBlank()) { "Guarda primero la cabecera." }
        val snapshot=calculator?.calculate(sample,dao.damages(id))?.let{PavementResultJson.encode(it)}
        dao.saveSample(sample.copy(status="CLOSED",updatedAt=System.currentTimeMillis(),calculationSnapshot=snapshot))
    }
    suspend fun deleteDamage(damage: PavementDamageEntity) = db.withTransaction {
        val stored=requireNotNull(dao.damage(damage.id)) { "El daño ya no existe." }
        editable(stored.sampleId); dao.deleteDamage(stored.id)
    }
    suspend fun savePhoto(photo: PavementDamagePhotoEntity, sampleId: String) = db.withTransaction {
        editable(sampleId)
        require(dao.damage(photo.damageId)?.sampleId==sampleId) { "La fotografía debe pertenecer a un daño de esta muestra." }
        val old=dao.photo(photo.id)
        require(old==null || old.damageId==photo.damageId) { "La fotografía pertenece a otro daño." }
        dao.savePhoto(photo)
    }
    suspend fun deletePhoto(photoId: String, damageId: String, imagesDirectory: java.io.File) {
        val path = db.withTransaction {
            val photo = requireNotNull(dao.photo(photoId)) { "La fotografía ya no existe." }
            require(photo.damageId == damageId) { "La fotografía pertenece a otro daño." }
            editable(requireNotNull(dao.damage(damageId)).sampleId)
            dao.deletePhoto(photoId)
            photo.localPath.takeIf { dao.photoPathCount(it) == 0 }
        }
        if (path != null) kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val file = java.io.File(path).canonicalFile
            if (file.parentFile == imagesDirectory.canonicalFile && file.name.startsWith("PAVIMENTO_")) file.delete()
        }
    }

}
