package com.tuempresa.inventariovial.core

/** Only public capability numbers/booleans; never retain the server response or credentials. */
data class PhotoNamingDiagnostic(
    val enabled:Boolean,
    val readableVersion:Int,
    val familyPaths:Boolean,
    val legacyVersion:Int,
    val syncProtocol:Int
) {
    val nextDelivery:String get()=when {
        !familyPaths -> "Pendiente: requiere syncProtocol=3 y capacidad de rutas V4."
        enabled && readableVersion==2 -> "Nueva entrega: V3 / rutas V4 / nombres V2 con reserva."
        enabled -> "Nueva entrega: detenida hasta que el servidor anuncie readablePhotoNamingVersion=2."
        else -> "Nueva entrega: nombres anteriores según configuración local."
    }
    fun text()="V2 en compilación: ${if(enabled) "habilitado" else "deshabilitado"}\n"+
        "Servidor: syncProtocol=$syncProtocol · readablePhotoNamingVersion=$readableVersion · photoNamingVersion=$legacyVersion\n"+
        nextDelivery+"\nLas entregas ya preparadas conservan su protocolo y nombres."
}
