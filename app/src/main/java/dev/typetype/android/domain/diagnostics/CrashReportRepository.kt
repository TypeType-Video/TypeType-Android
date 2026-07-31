package dev.typetype.android.domain.diagnostics

interface CrashReportRepository {
    fun recordCurrent(report: CrashReport): Boolean
    suspend fun pendingCurrent(): CrashReport?
    suspend fun latestCurrent(): CrashReport?
    suspend fun acknowledgeCurrent()
    suspend fun clearCurrent()
}
