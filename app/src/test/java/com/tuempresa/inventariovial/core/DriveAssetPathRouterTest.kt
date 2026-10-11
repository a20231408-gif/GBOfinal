package com.tuempresa.inventariovial.core

import org.junit.Assert.*
import org.junit.Test

class DriveAssetPathRouterTest {
    @Test fun separateElementsAndCategories() {
        val a = DriveAssetPathRouter.photoFolder("PE-22A", "Alcantarillas", "Alcantarilla_001", PhotoCategory.ENTRADA)
        val b = DriveAssetPathRouter.photoFolder("PE-22A", "Alcantarillas", "Alcantarilla_002", PhotoCategory.ENTRADA)
        val c = DriveAssetPathRouter.photoFolder("PE-22A", "Alcantarillas", "Alcantarilla_001", PhotoCategory.SALIDA)
        assertEquals("PE-22A/ALCANTARILLAS/ALCANTARILLA_001/ENTRADA", a)
        assertNotEquals(a, b)
        assertNotEquals(a, c)
        assertEquals(a, DriveAssetPathRouter.photoFolder("PE-22A", "Alcantarillas", "Alcantarilla_001", PhotoCategory.ENTRADA))
    }
    @Test fun defaultCategoryAndPavementSurface() {
        assertEquals("PE-22A/PAVIMENTOS/ASFALTICO/MUESTRA_001/GENERAL",
            DriveAssetPathRouter.photoFolder("PE-22A", "PAVIMENTOS", "MUESTRA_001", pavementSurface = "ASFALTICO"))
        assertEquals(PhotoCategory.GENERAL, PhotoCategory.fromStored(null))
        assertEquals(PhotoCategory("TECHO"), PhotoCategory.fromStored("TECHO"))
    }
    @Test fun invalidComponentsCannotTraverse() {
        listOf("", "../root", "sub/path", "a\\b", "..", "  ").forEach { bad ->
            assertThrows(IllegalArgumentException::class.java) { DriveAssetPathRouter.component(bad) }
        }
    }
    @Test fun photosNotMixedAndNamesIndependent() {
        val source = "photo.jpg"
        val category = PhotoCategory.PANORAMICA
        assertEquals("PE-22A/PUENTES/PUENTE_001/PANORAMICA", DriveAssetPathRouter.photoFolder("PE-22A", "PUENTES", "PUENTE_001", category))
        assertEquals("photo.jpg", source) // category does not depend on filename
    }
}
