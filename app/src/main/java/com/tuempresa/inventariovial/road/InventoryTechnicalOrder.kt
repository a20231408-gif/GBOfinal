package com.tuempresa.inventariovial.road

import com.tuempresa.inventariovial.data.entity.InventoryRecordEntity

/** Contractual order; never a geographical coordinate or a remote folder number. */
object InventoryTechnicalOrder {
    fun direction(record: InventoryRecordEntity): String = record.surveyDirection?.trim()?.uppercase().orEmpty()
    fun descending(direction: String) = direction in setOf("D", "DECRECIENTE", "DESCENDING", "REVERSE", "DECREASING")
    fun measure(record: InventoryRecordEntity): Double? = record.startPrCode.trim().toIntOrNull()
        ?.takeIf { it >= 0 }?.let { pr -> record.startDistanceM.takeIf { it.isFinite() && it >= 0 }?.let { pr * 1000.0 + it } }
    fun signed(record: InventoryRecordEntity): Double = measure(record)?.let {
        if(descending(direction(record))) -it else it
    } ?: Double.POSITIVE_INFINITY
    val comparator: Comparator<InventoryRecordEntity> = compareBy<InventoryRecordEntity> { normalizedRoadCode(it.routeCode) }
        .thenBy { normalizedRoadCode(it.roadbedCode) }.thenBy { direction(it) }
        .thenBy { signed(it) }.thenBy { it.id }
}
