package dev.typetype.android.domain.diagnostics

interface DiagnosticsRepository {
    suspend fun listCurrent(): List<DiagnosticEntry>
    suspend fun clearCurrent()
}
