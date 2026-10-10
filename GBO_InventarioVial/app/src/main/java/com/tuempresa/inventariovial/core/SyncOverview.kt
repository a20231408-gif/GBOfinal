package com.tuempresa.inventariovial.core

/** Shared state translation; business-specific sync protocols remain separate. */
enum class UserSyncState(val label: String) {
    PREPARADO("Preparado"), PENDIENTE("Pendiente"), SUBIENDO("Subiendo"),
    SINCRONIZADO("Sincronizado"), ERROR("Error"), REVISION("Requiere revisión")
}

object SyncOverview {
    fun status(raw: String): UserSyncState = when (raw.uppercase()) {
        "PREPARED" -> UserSyncState.PREPARADO
        "PENDING", "QUEUED", "ENQUEUED" -> UserSyncState.PENDIENTE
        "UPLOADING", "SYNCING" -> UserSyncState.SUBIENDO
        "SYNCED" -> UserSyncState.SINCRONIZADO
        "ERROR", "FAILED" -> UserSyncState.ERROR
        "BLOCKED" -> UserSyncState.REVISION
        else -> UserSyncState.REVISION
    }
    fun canRetry(raw: String) = status(raw) == UserSyncState.ERROR
    fun counts(states: List<String>): Map<UserSyncState, Int> = states.map(::status).groupingBy { it }.eachCount()
}

data class ValidationIssue(val field: String, val reason: String, val severity: Severity) {
    enum class Severity { MISSING_USER_INPUT, WARNING, BLOCKING_EXPORT, UNRESOLVED_MAPPING }
}
data class CompletionResult(val issues: List<ValidationIssue>) {
    val canSaveDraft: Boolean get() = true
    val canFinalize: Boolean get() = issues.none {
        it.severity == ValidationIssue.Severity.MISSING_USER_INPUT ||
        it.severity == ValidationIssue.Severity.BLOCKING_EXPORT ||
        it.severity == ValidationIssue.Severity.UNRESOLVED_MAPPING
    }
}
