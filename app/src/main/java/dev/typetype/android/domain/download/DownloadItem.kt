package dev.typetype.android.domain.download

data class DownloadItem(
    val downloadId: Long,
    val title: String,
    val fileName: String,
    val status: DownloadStatus,
    val progressPercent: Int?,
    val createdAtMillis: Long,
)

enum class DownloadStatus {
    Pending,
    Running,
    Successful,
    Failed,
}
