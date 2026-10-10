package com.tuempresa.inventariovial.core

import org.junit.Test
import org.junit.Assert.*

class SyncOverviewTest {
    @Test fun humanReadableStatesAndSafeRetries() {
        assertEquals("Preparado", SyncOverview.status("PREPARED").label)
        assertEquals("Requiere revisión", SyncOverview.status("BLOCKED").label)
        assertEquals("Sincronizado", SyncOverview.status("SYNCED").label)
        assertTrue(SyncOverview.canRetry("ERROR"))
        assertFalse(SyncOverview.canRetry("BLOCKED"))
        assertFalse(SyncOverview.canRetry("SYNCED"))
    }
    @Test fun draftAlwaysAllowedFinalNeedsCompletion() {
        val result = CompletionResult(listOf(ValidationIssue("PR", "Falta dato", ValidationIssue.Severity.MISSING_USER_INPUT)))
        assertTrue(result.canSaveDraft)
        assertFalse(result.canFinalize)
        assertTrue(CompletionResult(listOf(ValidationIssue("GPS", "Baja precisión", ValidationIssue.Severity.WARNING))).canFinalize)
    }
}
