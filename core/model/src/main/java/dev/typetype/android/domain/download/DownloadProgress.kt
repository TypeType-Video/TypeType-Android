package dev.typetype.android.domain.download

sealed interface DownloadProgress {
    data class Queued(
        val cached: Boolean,
    ) : DownloadProgress

    data class Running(
        val progressPercent: Int?,
        val stage: String?,
    ) : DownloadProgress

    data class Enqueued(
        val downloadId: Long,
        val fileName: String,
    ) : DownloadProgress
}
