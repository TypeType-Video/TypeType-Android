package dev.typetype.android.domain.support

interface SupportRepository {
    suspend fun canSubmitReport(): Boolean
    suspend fun submitReport(draft: SupportReportDraft): Result<SupportReportReceipt>
}
