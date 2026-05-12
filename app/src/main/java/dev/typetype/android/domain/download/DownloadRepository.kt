package dev.typetype.android.domain.download

import kotlinx.coroutines.flow.Flow

interface DownloadRepository {
    fun observeDownloads(): Flow<List<DownloadItem>>

    fun refreshDownloads()

    fun downloadVideo(
        videoUrl: String,
        title: String,
        quality: String,
    ): Flow<DownloadProgress>

    suspend fun openDownload(downloadId: Long): Result<Unit>
}
