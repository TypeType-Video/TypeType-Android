package dev.typetype.android.domain.diagnostics

data class DiagnosticEntry(
    val timestampEpochMillis: Long,
    val method: String,
    val route: String,
    val statusCode: Int?,
    val durationMillis: Long,
    val requestId: String?,
    val sabr: SabrDiagnosticDetail? = null,
    val failureCode: String? = null,
)
