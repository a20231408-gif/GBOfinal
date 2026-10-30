package com.tuempresa.inventariovial

import com.tuempresa.inventariovial.field.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class RecordDateFilterTest {
    @Test fun readsBothHistoricalDateFormatsWithoutNormalizingInvalidDays() {
        assertEquals(LocalDate.of(2026,10,30),parseRecordDate("30/10/2026"))
        assertEquals(LocalDate.of(2026,10,30),parseRecordDate("2026-10-30"))
        assertNull(parseRecordDate("31/02/2026"))
        assertNull(parseRecordDate("2026-02-29"))
        assertEquals(LocalDate.of(2024,2,29),parseRecordDate("2024-02-29"))
        assertNull(parseRecordDate(""))
    }
    @Test fun filtersDayMonthAndYearIndependentlyAndKeepsUnknownDatesWhenCleared() {
        val date=LocalDate.of(2026,10,30)
        assertTrue(RecordDateSelection(month=10).matches(date))
        assertTrue(RecordDateSelection(year=2026,day=30).matches(date))
        assertFalse(RecordDateSelection(year=2025).matches(date))
        assertFalse(RecordDateSelection(month=9).matches(date))
        assertFalse(RecordDateSelection(day=29).matches(date))
        assertFalse(RecordDateSelection(month=10).matches(null))
        assertTrue(RecordDateSelection().matches(null))
        assertFalse(RecordDateSelection(year=2026,month=2,day=31).matches(date))
    }
}
