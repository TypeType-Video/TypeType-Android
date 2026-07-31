package dev.typetype.android.data.diagnostics

import dev.typetype.android.domain.diagnostics.CrashReport
import dev.typetype.android.domain.diagnostics.CrashRequestMetadata
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
private data class StoredCrashReport(
    val acknowledged: Boolean,
    val occurredAtEpochMillis: Long,
    val appVersion: String,
    val appVersionCode: Long,
    val androidVersion: String,
    val apiLevel: Int,
    val deviceManufacturer: String,
    val deviceModel: String,
    val exceptionType: String,
    val fingerprint: String,
    val stackTrace: List<String>,
    val lastRequestMethod: String? = null,
    val lastRequestRoute: String? = null,
    val lastRequestId: String? = null,
    val lastSabrSummary: String? = null,
)

internal data class DecodedCrashReport(
    val report: CrashReport,
    val acknowledged: Boolean,
)

internal object CrashReportCodec {
    fun encode(json: Json, report: CrashReport, acknowledged: Boolean): String =
        json.encodeToString(
            StoredCrashReport(
                acknowledged = acknowledged,
                occurredAtEpochMillis = report.occurredAtEpochMillis,
                appVersion = report.appVersion,
                appVersionCode = report.appVersionCode,
                androidVersion = report.androidVersion,
                apiLevel = report.apiLevel,
                deviceManufacturer = report.deviceManufacturer,
                deviceModel = report.deviceModel,
                exceptionType = report.exceptionType,
                fingerprint = report.fingerprint,
                stackTrace = report.stackTrace,
                lastRequestMethod = report.lastRequest?.method,
                lastRequestRoute = report.lastRequest?.route,
                lastRequestId = report.lastRequest?.requestId,
                lastSabrSummary = report.lastSabrSummary,
            ),
        )

    fun decode(json: Json, value: String): DecodedCrashReport? {
        val stored = runCatching { json.decodeFromString<StoredCrashReport>(value) }.getOrNull()
            ?: return null
        if (!stored.isValid()) return null
        val request = stored.lastRequestMethod?.let { method ->
            CrashRequestMetadata(method, stored.lastRequestRoute.orEmpty(), stored.lastRequestId)
        }
        return DecodedCrashReport(
            report = CrashReport(
                occurredAtEpochMillis = stored.occurredAtEpochMillis,
                appVersion = stored.appVersion,
                appVersionCode = stored.appVersionCode,
                androidVersion = stored.androidVersion,
                apiLevel = stored.apiLevel,
                deviceManufacturer = stored.deviceManufacturer,
                deviceModel = stored.deviceModel,
                exceptionType = stored.exceptionType,
                fingerprint = stored.fingerprint,
                stackTrace = stored.stackTrace,
                lastRequest = request,
                lastSabrSummary = stored.lastSabrSummary,
            ),
            acknowledged = stored.acknowledged,
        )
    }

    private fun StoredCrashReport.isValid(): Boolean =
        occurredAtEpochMillis > 0 &&
            appVersion.length <= 80 && appVersionCode >= 0 &&
            androidVersion.length <= 80 && apiLevel in 1..100 &&
            deviceManufacturer.length <= 80 && deviceModel.length <= 80 &&
            exceptionType.matches(IDENTIFIER_PATTERN) &&
            fingerprint.matches(FINGERPRINT_PATTERN) &&
            stackTrace.isNotEmpty() && stackTrace.size <= 128 &&
            stackTrace.all { it.length <= 240 && TRACE_PATTERN.matches(it) } &&
            requestIsValid() &&
            lastSabrSummary?.let { it.length <= 512 && SABR_PATTERN.matches(it) } != false

    private fun StoredCrashReport.requestIsValid(): Boolean {
        if (lastRequestMethod == null && lastRequestRoute == null && lastRequestId == null) return true
        return lastRequestMethod in ALLOWED_METHODS &&
            lastRequestRoute?.matches(ROUTE_PATTERN) == true &&
            lastRequestId?.matches(REQUEST_ID_PATTERN) != false
    }

    private val IDENTIFIER_PATTERN = Regex("[A-Za-z0-9_.$<>?-]{1,180}")
    private val FINGERPRINT_PATTERN = Regex("[0-9a-f]{16}")
    private val TRACE_PATTERN = Regex("[A-Za-z0-9_.$<>?: -]{1,240}")
    private val ROUTE_PATTERN = Regex("/[a-z0-9/-]{1,63}")
    private val REQUEST_ID_PATTERN = Regex("[A-Za-z0-9_-]{8,64}")
    private val SABR_PATTERN = Regex("[A-Za-z0-9_=. -]{1,512}")
    private val ALLOWED_METHODS = setOf("DELETE", "GET", "PATCH", "POST", "PUT")
}
