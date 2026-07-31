package dev.typetype.android.domain.diagnostics

data class CrashReport(
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
    val lastRequest: CrashRequestMetadata?,
    val lastSabrSummary: String?,
)

data class CrashRequestMetadata(
    val method: String,
    val route: String,
    val requestId: String?,
)
