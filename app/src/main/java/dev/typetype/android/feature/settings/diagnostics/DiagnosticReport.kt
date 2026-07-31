package dev.typetype.android.feature.settings.diagnostics

import dev.typetype.android.domain.diagnostics.DiagnosticEntry
import dev.typetype.android.domain.diagnostics.CrashReport
import java.text.DateFormat
import java.util.Date

fun buildDiagnosticReport(entries: List<DiagnosticEntry>, crashReport: CrashReport? = null): String {
    val formatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM)
    return buildString {
        appendLine("TypeType Android diagnostics")
        appendLine("Redacted local diagnostics")
        appendLine()
        crashReport?.let {
            appendLine(buildCrashReportMarkdown(it))
            appendLine()
        }
        entries.asReversed().forEach { entry ->
            append(formatter.format(Date(entry.timestampEpochMillis)))
            append("  ${entry.method} ${entry.route}")
            if (entry.method != "LOCAL" && entry.method != "APP") {
                append("  ")
                append(entry.statusCode?.let { "HTTP $it" } ?: "network error")
                append("  ${entry.durationMillis} ms")
            }
            entry.requestId?.let { append("  request $it") }
            appendLine()
            entry.sabr?.let {
                appendLine("  SABR ${it.redactedSummary()}")
            }
        }
    }
}
