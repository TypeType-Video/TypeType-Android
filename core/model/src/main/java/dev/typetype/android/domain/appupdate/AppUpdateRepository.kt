package dev.typetype.android.domain.appupdate

import java.io.File

interface AppUpdateRepository {
    suspend fun check(
        channel: AppUpdateChannel,
        installedVersionName: String,
        installedVersionCode: Long,
    ): AppUpdateCheck

    suspend fun download(
        release: AppUpdateRelease,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit,
    ): File
}
