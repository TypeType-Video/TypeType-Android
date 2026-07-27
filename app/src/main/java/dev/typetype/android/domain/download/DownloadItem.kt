package dev.typetype.android.domain.download

data class DownloadItem(
    val requestId: String,
    val systemDownloadId: Long?,
    val title: String,
    val fileName: String,
    val selection: DownloadSelection,
    val status: DownloadStatus,
    val progressPercent: Int?,
    val stage: DownloadStage?,
    val failure: DownloadFailure?,
    val createdAtMillis: Long,
)

enum class DownloadStatus {
    Pending,
    Running,
    Successful,
    Failed,
    Cancelled,
}

enum class DownloadStage {
    Preparing,
    Downloading,
    Finalizing,
}

enum class DownloadFailure {
    Authentication,
    InsufficientStorage,
    Network,
    Rejected,
    ServerUnavailable,
    TimedOut,
    Unknown,
}
