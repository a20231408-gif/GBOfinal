package com.tuempresa.inventariovial.road

import com.tuempresa.inventariovial.data.entity.InventoryRecordEntity

/** Tabular ascending PR/metres only. Does not calculate contractual length or folder numbers. */
object SicTableOrder {
    val comparator:Comparator<InventoryRecordEntity> = compareBy<InventoryRecordEntity> {
        it.startPrCode.trim().toLongOrNull()?.takeIf{pr->pr>=0} ?: Long.MAX_VALUE
    }.thenBy {it.startDistanceM.takeIf{m->m.isFinite() && m>=0} ?: Double.POSITIVE_INFINITY}.thenBy{it.id}
}
