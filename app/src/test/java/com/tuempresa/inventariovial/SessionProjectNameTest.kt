package com.tuempresa.inventariovial

import com.tuempresa.inventariovial.field.defaultSessionProjectName
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class SessionProjectNameTest {
    @Test fun includesTheProvidedLocalDayWithLeadingZeros() {
        assertEquals("Inventario Vial - 05/01/2027", defaultSessionProjectName(LocalDate.of(2027, 1, 5)))
    }
    @Test fun nextDayAndYearDoNotReuseAnEarlierDefault() {
        assertEquals("Inventario Vial - 31/12/2026", defaultSessionProjectName(LocalDate.of(2026, 12, 31)))
        assertEquals("Inventario Vial - 01/01/2027", defaultSessionProjectName(LocalDate.of(2027, 1, 1)))
    }
    @Test fun supportsLeapDays() {
        assertEquals("Inventario Vial - 29/02/2028", defaultSessionProjectName(LocalDate.of(2028, 2, 29)))
    }
    @Test fun numericFormatDoesNotDependOnDeviceLanguage() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("ar-EG"))
            assertEquals("Inventario Vial - 15/10/2026", defaultSessionProjectName(LocalDate.of(2026, 10, 15)))
        } finally { Locale.setDefault(previous) }
    }
}
