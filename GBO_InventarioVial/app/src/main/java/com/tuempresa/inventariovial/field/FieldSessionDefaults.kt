package com.tuempresa.inventariovial.field

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import com.tuempresa.inventariovial.data.dao.InventoryDao
import com.tuempresa.inventariovial.data.entity.FieldSession
import com.tuempresa.inventariovial.data.entity.InventoryRecordEntity
import com.tuempresa.inventariovial.road.ProjectRouteCatalog

data class FieldFormLocation(
    val route: String = "",
    val roadbed: String = "",
    val segment: String = "",
    val direction: String = "INCREASING"
)

/** Defaults for creation only. A recovered record owns even its empty values. */
object FieldSessionDefaults {
    fun location(session: FieldSession?, existing: InventoryRecordEntity? = null,
                 routes: ProjectRouteCatalog? = null): FieldFormLocation {
        if (existing != null) return FieldFormLocation(existing.routeCode, existing.roadbedCode,
            existing.segment.orEmpty(), existing.surveyDirection ?: "INCREASING")
        val active = session?.takeIf { it.endTime == null } ?: return FieldFormLocation()
        val route = active.road.orEmpty()
        return FieldFormLocation(route, active.roadbed.orEmpty(),
            routes?.segment(route, active.segment) ?: active.segment.orEmpty(), active.direction)
    }

    /** PR continuity is reusable only on the same session route and roadbed. */
    fun matchingPosition(session: FieldSession?, previous: SurveyPosition?): SurveyPosition? =
        previous?.takeIf { session != null && session.endTime == null &&
            it.sessionId == session.sessionId && it.route == session.road && it.roadbed == session.roadbed }
}

data class FieldCaptureContext(val session: FieldSession?, val location: FieldFormLocation)

/** Wait for Room instead of freezing the initial null of the session StateFlow. */
@Composable
fun rememberFieldCaptureContext(dao: InventoryDao, routes: ProjectRouteCatalog,
                                existing: InventoryRecordEntity? = null): FieldCaptureContext? {
    val capture by produceState<FieldCaptureContext?>(null, dao, routes, existing?.id) {
        val session = dao.currentSession()
        value = FieldCaptureContext(session, FieldSessionDefaults.location(session, existing, routes))
    }
    return capture
}
