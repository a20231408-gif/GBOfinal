package com.tuempresa.inventariovial.core

/** Shared Drive credentials and tablet-administration grants cannot authorize inventory mutations. */
object InventoryLifecyclePolicy {
    const val enabled = false
    const val unavailableReason = "Anulación/restauración pendiente de autorización individual y confirmación del servidor."
    fun requireAvailable() { check(enabled) { unavailableReason } }
}
