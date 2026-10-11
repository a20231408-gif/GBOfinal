package com.tuempresa.inventariovial.core

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class CleanPhotoPolicyTest {
    @Test fun multiplePhotosSameCategoryStayDistinctAndMetadataSeparate() {
        val original = File.createTempFile("field_original_", ".jpg")
        try {
            val bytes = byteArrayOf(1,2,3,4,5)
            original.writeBytes(bytes)
            val a = PhotoForDelivery("a", original.path, PhotoCategory.ENTRADA, "{\"route\":\"PE-22A\"}")
            val b = a.copy(id = "b")
            val c = a.copy(id = "c", category = PhotoCategory.SALIDA)
            assertEquals(2, CleanPhotoPolicy.byCategory(listOf(a,b,c)).getValue(PhotoCategory.ENTRADA).size)
            assertEquals(1, CleanPhotoPolicy.byCategory(listOf(a,b,c)).getValue(PhotoCategory.SALIDA).size)
            assertArrayEquals(bytes, CleanPhotoPolicy.original(a).readBytes())
            assertArrayEquals(bytes, original.readBytes())
            assertFalse(original.readBytes().toString().contains("route"))
        } finally {original.delete()}
    }
}
