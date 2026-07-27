package dev.typetype.android.feature.settings.diagnostics

import dev.typetype.android.domain.diagnostics.DiagnosticEntry
import java.text.DateFormat
import java.util.Date

fun buildDiagnosticReport(entries: List<DiagnosticEntry>): String {
    val formatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM)
    return buildString {
        appendLine("TypeType Android diagnostics")
        appendLine("Redacted network metadata only")
        appendLine()
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
        }
    }
}
