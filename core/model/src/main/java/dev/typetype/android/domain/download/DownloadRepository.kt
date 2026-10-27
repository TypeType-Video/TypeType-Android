package dev.typetype.android.domain.download

import kotlinx.coroutines.flow.Flow

interface DownloadRepository {
    fun observeDownloads(): Flow<List<DownloadItem>>

    fun refreshDownloads()

    fun downloadVideo(
        videoUrl: String,
        title: String,
        selection: DownloadSelection,
    ): Flow<DownloadProgress>

    suspend fun openDownload(requestId: String): Result<Unit>

    suspend fun cancelDownload(requestId: String): Result<Unit>

    suspend fun retryDownload(requestId: String): Result<Unit>

    suspend fun removeDownload(requestId: String): Result<Unit>
}
