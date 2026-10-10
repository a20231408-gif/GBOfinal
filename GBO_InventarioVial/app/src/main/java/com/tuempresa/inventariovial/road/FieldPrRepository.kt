package com.tuempresa.inventariovial.road

import com.tuempresa.inventariovial.data.dao.InventoryDao
import com.tuempresa.inventariovial.data.entity.FieldPrEntity
import com.tuempresa.inventariovial.location.GeoLocation
import java.util.UUID

class FieldPrRepository(private val dao: InventoryDao) {
    suspend fun merge(reference: RoadReferenceData): RoadReferenceData {
        val officialKeys = reference.prs.map { Triple(normalizedRoadCode(it.routeCode), normalizedRoadCode(it.roadbedCode), normalizedPr(it.prCode)) }.toSet()
        return reference.copy(prs = reference.prs + dao.fieldPrs().filter {
            Triple(it.routeCode, it.roadbedCode, it.prCode) !in officialKeys
        }.map { it.asRoadPr() })
    }
    suspend fun register(reference: RoadReferenceData, config: MatchConfig, route: String, bed: String,
        code: String, type: String, description: String, fix: GeoLocation): FieldPrEntity {
        require(code.matches(Regex("[0-9]{4}"))) { "Código PR: exactamente cuatro dígitos." }
        require(route.isNotBlank() && bed.isNotBlank()) { "Confirma ruta y calzada." }
        require(type in types) { "Selecciona el tipo de PR." }
        require(reference.prs.none { normalizedRoadCode(it.routeCode) == normalizedRoadCode(route) &&
            normalizedRoadCode(it.roadbedCode) == normalizedRoadCode(bed) && normalizedPr(it.prCode) == code }) { "Ya existe ese PR en la ruta/calzada." }
        require(System.currentTimeMillis() - fix.timestamp in 0..30_000) { "Actualiza el GNSS antes de guardar el PR." }
        val match = LinearReferenceEngine(reference.forSelectedRouteAndRoadbed(route, bed).copy(prs = emptyList()), config)
            .locate(fix.latitude, fix.longitude, fix.horizontalAccuracy.toDouble()) ?: error("No se pudo proyectar el PR sobre el eje seleccionado.")
        val pr = FieldPrEntity(UUID.randomUUID().toString(), normalizedRoadCode(route), normalizedRoadCode(bed), code,
            type, description.trim().ifBlank { null }, fix.latitude, fix.longitude, fix.altitude,
            fix.horizontalAccuracy.toDouble(), fix.timestamp, match.chainageM, match.segmentId, createdAt = System.currentTimeMillis())
        dao.insertFieldPr(pr)
        return pr
    }
    companion object {
        // No SIC-03 type catalogue is bundled. These are the field categories requested for this workflow.
        val types = listOf("Marca/Poste", "Marca/Concreto", "Marca/Roca", "Marca/Piedra", "Virtual")
    }
}
