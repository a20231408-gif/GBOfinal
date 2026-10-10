package com.tuempresa.inventariovial.road

import com.tuempresa.inventariovial.data.entity.InventoryRecordEntity
import kotlin.math.max

object LocationSource {
    const val MANUAL = "MANUAL"
    const val GNSS_CONFIRMED = "GNSS_CONFIRMED"
    const val GNSS_PR_CONFIRMED = "GNSS_PR_CONFIRMED"
    const val MANUAL_OVERRIDE = "MANUAL_OVERRIDE"
    fun edited(previous: String, hasSuggestion: Boolean) =
        if (hasSuggestion || previous != MANUAL) MANUAL_OVERRIDE else MANUAL
}

data class LocationSuggestion(
    val suggestedRouteCode: String? = null,
    val pr: PrSuggestion? = null,
    val match: RoadMatchResult? = null,
    val routeMatchAmbiguous: Boolean = false,
    val candidates: List<RoadMatchResult> = emptyList(),
    val discrepancyRoute: String? = null,
    val message: String? = null
) {
    val suggestedPrCode get() = pr?.suggestedPrCode
    val suggestedDistanceFromPrM get() = pr?.suggestedDistanceFromPrM
    val routeMatchDistanceM get() = match?.distanceToRoadAxisM
    val routeMatchConfidence get() = match?.confidence
}

/** Uses the existing global matcher; scoped runs expose candidates when the global run is ambiguous. */
class LocationSuggestionEngine(private val reference: RoadReferenceData, private val config: MatchConfig = MatchConfig()) {
    fun suggest(route: String, roadbed: String, lat: Double?, lon: Double?, accuracy: Double?): LocationSuggestion {
        if (lat == null || lon == null || accuracy == null) return LocationSuggestion(message = "Obtén una posición GNSS válida.")
        val geometry = reference.copy(prs = emptyList())
        val global = RoadMatcher(geometry, config).match(lat, lon, accuracy)
        val candidates = geometry.segments.map { normalizedRoadCode(it.routeCode) }.distinct().mapNotNull { code ->
            RoadMatcher(geometry.forSelectedRoute(code), config).match(lat, lon, accuracy)
        }.sortedWith(compareBy({ it.distanceToRoadAxisM }, { it.routeCode }))
        val nearest = candidates.firstOrNull()
        val nearby = nearest?.let { first -> candidates.filter {
            it.distanceToRoadAxisM - first.distanceToRoadAxisM <= max(config.ambiguityDistanceM, accuracy)
        } }.orEmpty()
        val ambiguous = global == null && nearby.size > 1
        val selected = if (route.isBlank()) global else RoadMatcher(geometry.forSelectedRouteAndRoadbed(route, roadbed), config).match(lat, lon, accuracy)
        val discrepancy = global?.routeCode?.takeIf { route.isNotBlank() && normalizedRoadCode(it) != normalizedRoadCode(route) && selected == null }
        if (selected == null) return LocationSuggestion(routeMatchAmbiguous = ambiguous, candidates = if (ambiguous) nearby else candidates,
            discrepancyRoute = discrepancy, message = when {
                discrepancy != null -> "El GPS parece corresponder a otra ruta."
                ambiguous -> "Se detectaron varias rutas posibles."
                else -> "No se pudo determinar la ruta."
            })
        val pr = PrSuggestionEngine(reference, config).suggest(selected.routeCode, roadbed, lat, lon, accuracy)
        return LocationSuggestion(selected.routeCode, pr.suggestion, selected, ambiguous, if (ambiguous) nearby else candidates,
            discrepancy, if (reference.prs.isEmpty()) "Autodetección de PR no disponible: falta catálogo georreferenciado de puntos de referencia." else pr.message)
    }
}

/** Saves the proposal independently of confirmed fields, including a manual override. */
fun InventoryRecordEntity.withSuggestions(start: LocationSuggestion?, end: LocationSuggestion? = null): InventoryRecordEntity {
    val initial = if (start == null) this else copy(
        suggestedRouteCode = start.suggestedRouteCode ?: start.discrepancyRoute,
        suggestedPrCode = start.suggestedPrCode, suggestedDistanceFromPrM = start.suggestedDistanceFromPrM,
        routeMatchDistanceM = start.routeMatchDistanceM, routeMatchConfidence = start.routeMatchConfidence,
        routeMatchAmbiguous = start.routeMatchAmbiguous)
    return if (end == null) initial else initial.copy(
        endSuggestedPrCode = end.suggestedPrCode, endSuggestedDistanceFromPrM = end.suggestedDistanceFromPrM,
        endAxisMeasureM = end.match?.chainageM, endDistanceToRoadAxisM = end.match?.distanceToRoadAxisM,
        endMatchedSegmentId = end.match?.segmentId, endProjectedLatitude = end.match?.projectedLatitude,
        endProjectedLongitude = end.match?.projectedLongitude)
}

/** Resuming a draft must retain the saved proposal even if no new matching callback has completed. */
fun InventoryRecordEntity.withSavedSuggestionAudit(previous: InventoryRecordEntity?): InventoryRecordEntity =
    if (previous == null) this else copy(
        suggestedRouteCode = previous.suggestedRouteCode, suggestedPrCode = previous.suggestedPrCode,
        suggestedDistanceFromPrM = previous.suggestedDistanceFromPrM, routeMatchDistanceM = previous.routeMatchDistanceM,
        routeMatchConfidence = previous.routeMatchConfidence, routeMatchAmbiguous = previous.routeMatchAmbiguous,
        endSuggestedPrCode = previous.endSuggestedPrCode, endSuggestedDistanceFromPrM = previous.endSuggestedDistanceFromPrM,
        endAxisMeasureM = previous.endAxisMeasureM, endDistanceToRoadAxisM = previous.endDistanceToRoadAxisM,
        endMatchedSegmentId = previous.endMatchedSegmentId, endProjectedLatitude = previous.endProjectedLatitude,
        endProjectedLongitude = previous.endProjectedLongitude)
