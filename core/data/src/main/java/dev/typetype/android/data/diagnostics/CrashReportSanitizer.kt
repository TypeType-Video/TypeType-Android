package dev.typetype.android.data.diagnostics

import dev.typetype.android.domain.diagnostics.CrashReport
import dev.typetype.android.domain.diagnostics.CrashRequestMetadata
import dev.typetype.android.domain.diagnostics.SabrDiagnosticDetail
import java.security.MessageDigest
import java.util.Collections
import java.util.IdentityHashMap

internal data class CrashEnvironment(
    val appVersion: String,
    val appVersionCode: Long,
    val androidVersion: String,
    val apiLevel: Int,
    val deviceManufacturer: String,
    val deviceModel: String,
)

internal data class CrashDiagnosticContext(
    val lastRequest: CrashRequestMetadata?,
    val lastSabr: SabrDiagnosticDetail?,
)

internal object CrashReportSanitizer {
    fun create(
        throwable: Throwable,
        environment: CrashEnvironment,
        diagnostics: CrashDiagnosticContext,
        timestampEpochMillis: Long = System.currentTimeMillis(),
    ): CrashReport {
        val trace = sanitizedTrace(throwable)
        val exceptionType = safeIdentifier(throwable.javaClass.name)
        return CrashReport(
            occurredAtEpochMillis = timestampEpochMillis,
            appVersion = safeMetadata(environment.appVersion),
            appVersionCode = environment.appVersionCode.coerceAtLeast(0),
            androidVersion = safeMetadata(environment.androidVersion),
            apiLevel = environment.apiLevel.coerceIn(1, MAX_API_LEVEL),
            deviceManufacturer = safeMetadata(environment.deviceManufacturer),
            deviceModel = safeMetadata(environment.deviceModel),
            exceptionType = exceptionType,
            fingerprint = fingerprint(exceptionType, trace),
            stackTrace = trace,
            lastRequest = diagnostics.lastRequest?.sanitized(),
            lastSabrSummary = diagnostics.lastSabr?.redactedSummary(),
        )
    }

    private fun sanitizedTrace(throwable: Throwable): List<String> {
        val lines = mutableListOf<String>()
        val visited = Collections.newSetFromMap(IdentityHashMap<Throwable, Boolean>())
        var current: Throwable? = throwable
        var causeCount = 0
        while (current != null && causeCount < MAX_CAUSES && visited.add(current)) {
            val prefix = if (causeCount == 0) "Exception" else "Caused by"
            lines += "$prefix: ${safeIdentifier(current.javaClass.name)}"
            current.stackTrace.take(MAX_FRAMES_PER_CAUSE).forEach { frame ->
                if (lines.size < MAX_TRACE_LINES) lines += frame.sanitized()
            }
            current = current.cause
            causeCount += 1
        }
        return lines.take(MAX_TRACE_LINES)
    }

    private fun StackTraceElement.sanitized(): String {
        val owner = safeIdentifier(className)
        val method = safeIdentifier(methodName)
        val location = when {
            isNativeMethod -> "native"
            lineNumber > 0 -> lineNumber.toString()
            else -> "unknown"
        }
        return "at $owner.$method:$location"
    }

    private fun CrashRequestMetadata.sanitized(): CrashRequestMetadata? {
        val safeMethod = method.takeIf { it in ALLOWED_METHODS } ?: return null
        val safeRoute = route.takeIf(SAFE_ROUTE_PATTERN::matches) ?: return null
        return CrashRequestMetadata(
            method = safeMethod,
            route = safeRoute,
            requestId = requestId?.takeIf(REQUEST_ID_PATTERN::matches),
        )
    }

    private fun fingerprint(exceptionType: String, trace: List<String>): String {
        val signature = buildString {
            appendLine(exceptionType)
            trace.take(FINGERPRINT_TRACE_LINES).forEach(::appendLine)
        }
        return MessageDigest.getInstance("SHA-256")
            .digest(signature.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
            .take(FINGERPRINT_LENGTH)
    }

    private fun safeIdentifier(value: String): String = value
        .take(MAX_IDENTIFIER_LENGTH)
        .map { character -> if (character in SAFE_IDENTIFIER_CHARACTERS) character else '?' }
        .joinToString("")

    private fun safeMetadata(value: String): String = value
        .take(MAX_METADATA_LENGTH)
        .map { character -> if (character.isLetterOrDigit() || character in SAFE_METADATA) character else '?' }
        .joinToString("")

    private const val MAX_API_LEVEL = 100
    private const val MAX_CAUSES = 6
    private const val MAX_FRAMES_PER_CAUSE = 48
    private const val MAX_TRACE_LINES = 128
    private const val FINGERPRINT_TRACE_LINES = 32
    private const val FINGERPRINT_LENGTH = 16
    private const val MAX_IDENTIFIER_LENGTH = 180
    private const val MAX_METADATA_LENGTH = 80
    private val ALLOWED_METHODS = setOf("DELETE", "GET", "PATCH", "POST", "PUT")
    private val SAFE_ROUTE_PATTERN = Regex("/[a-z0-9/-]{1,63}")
    private val REQUEST_ID_PATTERN = Regex("[A-Za-z0-9_-]{8,64}")
    private val SAFE_IDENTIFIER_CHARACTERS =
        ('a'..'z') + ('A'..'Z') + ('0'..'9') + setOf('.', '$', '_', '<', '>', '-')
    private val SAFE_METADATA = setOf(' ', '.', '_', '-', '(', ')')
}
