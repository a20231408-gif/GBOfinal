package com.tuempresa.inventariovial.core

/** A code collision is not a communication failure, even if an old response calls it retryable. */
object DeliveryRejectionPolicy {
    const val BRIDGE_IDENTITY_CONFLICT="BRIDGE_IDENTITY_CONFLICT"
    fun bridgeIdentityConflict(intent:DriveSyncIntent)=intent.error.orEmpty().contains(BRIDGE_IDENTITY_CONFLICT)
    fun conflict(code:String)=code in setOf("REVISION_CONFLICT","STALE_REVISION","BASE_REVISION_CONFLICT",BRIDGE_IDENTITY_CONFLICT)
    const val BRIDGE_MESSAGE="El código del puente ya está vinculado a otra identidad en el servidor. Revisa la ficha y solicita conciliación antes de enviar; no crees otra ficha para evitar el conflicto. El registro y sus archivos se conservan."
}
