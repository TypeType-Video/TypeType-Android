package dev.typetype.android.domain.appupdate

data class AppUpdateRelease(
    val channel: AppUpdateChannel,
    val tagName: String,
    val versionName: String,
    val versionCode: Long?,
    val releaseNotes: String,
    val apkName: String,
    val apkUrl: String,
    val sha256: String,
    val apkSizeBytes: Long,
)
