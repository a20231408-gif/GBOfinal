package com.tuempresa.inventariovial

import com.tuempresa.inventariovial.access.DeviceFingerprint
import org.junit.Assert.*
import org.junit.Test

class DeviceFingerprintTest {
    @Test fun sameAndroidIdAlwaysProducesSame64HexFingerprint() {
        val a = DeviceFingerprint.fromAndroidId("9774d56d682e549c")
        val b = DeviceFingerprint.fromAndroidId("9774D56D682E549C")
        assertEquals(a, b)
        assertTrue(a.matches(Regex("^[a-f0-9]{64}$")))
        assertEquals(DeviceFingerprint.SCHEME, "android-id-v2")
    }

    @Test fun differentAndroidIdsProduceDifferentFingerprints() {
        assertNotEquals(DeviceFingerprint.fromAndroidId("tablet-a"), DeviceFingerprint.fromAndroidId("tablet-b"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun blankAndroidIdIsRejectedInsteadOfCreatingRandomIdentity() {
        DeviceFingerprint.fromAndroidId("   ")
    }
}
