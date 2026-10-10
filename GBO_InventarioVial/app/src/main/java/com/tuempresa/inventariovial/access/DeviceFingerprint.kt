package com.tuempresa.inventariovial.access

import java.security.MessageDigest

/**
 * Stable tablet identity used by the central user service.
 *
 * ANDROID_ID is stable for the same Android user + device + signing key on supported Android
 * versions, so APK updates/reinstalls signed with the same key no longer rotate the tablet ID.
 * The explicit scheme prefix prevents accidental collisions with the legacy random-install scheme.
 */
object DeviceFingerprint {
    const val SCHEME = "android-id-v2"
    private const val DOMAIN = "gbo-inventario-vial-device-v2:"

    fun fromAndroidId(androidId: String): String {
        val normalized = androidId.trim().lowercase()
        require(normalized.isNotBlank()) { "ANDROID_ID no disponible" }
        return MessageDigest.getInstance("SHA-256")
            .digest((DOMAIN + normalized).toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }
}
