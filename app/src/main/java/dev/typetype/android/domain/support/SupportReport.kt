package dev.typetype.android.domain.support

import dev.typetype.android.domain.diagnostics.DiagnosticEntry

enum class SupportReportCategory {
    Player,
    AudioLanguage,
    Subtitles,
    Interface,
    Functionality,
}

data class SupportReportDraft(
    val category: SupportReportCategory,
    val description: String,
    val diagnostics: List<DiagnosticEntry>,
)

data class SupportReportReceipt(
    val id: String,
    val status: String,
    val createdAtMillis: Long,
)
