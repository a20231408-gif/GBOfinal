package com.tuempresa.inventariovial.pavement

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

internal object PavementHistory {
    fun visible(samples: List<PavementSampleEntity>, surface: PavementSurface, filter: String, inspector: String, showDiscarded:Boolean=false) =
        samples.filter { (showDiscarded || it.status!="DISCARDED_LOCAL") && it.surface==surface.name && (it.tramo+" "+it.ruta).contains(filter,true) &&
            (inspector.isBlank() || it.inspectorUserId==inspector) }
            .sortedWith(compareByDescending<PavementSampleEntity>{it.fecha}.thenByDescending{it.createdAt}.thenByDescending{it.muestra}.thenBy{it.id})
    fun dateLabel(date: String) = runCatching { LocalDate.parse(date).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) }.getOrDefault(date)
    fun canContinue(sample: PavementSampleEntity, userId: String?) = sample.status=="DRAFT" && sample.inspectorUserId==userId
    fun statusLabel(status: String) = when(status) { "DRAFT" -> "Borrador / En progreso"; "CLOSED" -> "Cerrada"; "DISCARDED_LOCAL" -> "Descartada localmente (conservada)"; else -> status }
}
internal object PavementDisplay {
    fun number(value: Double?) = value?.let { String.format(Locale.US,"%.2f",it) } ?: "—"
}
