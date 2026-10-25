package dev.typetype.android.feature.settings.diagnostics

import dev.typetype.android.domain.diagnostics.CrashReport
import java.time.Instant

fun buildCrashReportMarkdown(report: CrashReport, userComment: String = ""): String = buildString {
    appendLine("## TypeType Android crash report")
    if (userComment.isNotBlank()) {
        appendLine()
        appendLine(userComment.trim().take(MAX_COMMENT_LENGTH))
    }
    appendLine()
    appendLine("- Action: Application crash")
    appendLine("- Time: ${Instant.ofEpochMilli(report.occurredAtEpochMillis)}")
    appendLine("- App: ${report.appVersion} (${report.appVersionCode})")
    appendLine("- Android: ${report.androidVersion} (API ${report.apiLevel})")
    appendLine("- Device: ${report.deviceManufacturer} ${report.deviceModel}")
    appendLine("- Exception: ${report.exceptionType}")
    appendLine("- Fingerprint: ${report.fingerprint}")
    report.lastRequest?.let { request ->
        append("- Last request: ${request.method} ${request.route}")
        request.requestId?.let { append(" (request $it)") }
        appendLine()
    }
    report.lastSabrSummary?.let { appendLine("- Last SABR state: $it") }
    appendLine()
    appendLine("<details><summary>Redacted stack trace</summary>")
    appendLine()
    appendLine("```text")
    report.stackTrace.forEach(::appendLine)
    appendLine("```")
    appendLine("</details>")
}

private const val MAX_COMMENT_LENGTH = 4_000
