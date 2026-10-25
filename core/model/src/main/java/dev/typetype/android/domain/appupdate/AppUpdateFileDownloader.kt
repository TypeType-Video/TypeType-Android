package dev.typetype.android.domain.appupdate

import java.io.File

interface AppUpdateFileDownloader {
    suspend fun download(
        release: AppUpdateRelease,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit,
    ): File
}
