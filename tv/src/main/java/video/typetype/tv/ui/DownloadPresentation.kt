package video.typetype.tv.ui

import video.typetype.sdk.core.DownloadJob
import video.typetype.sdk.core.DownloadJobStatus

internal fun downloadStatus(job: DownloadJob, saving: Boolean, message: String?): String = when {
    message != null -> "Saved"
    saving -> "Saving to Android"
    job.errorCode == "cancelled" -> "Download cancelled"
    job.status == DownloadJobStatus.Queued -> "Waiting for the downloader"
    job.status == DownloadJobStatus.Running -> when (job.stage) {
        "mux" -> "Finalizing"
        "download" -> "Downloading"
        else -> "Preparing"
    }
    job.status == DownloadJobStatus.Done -> "Ready"
    else -> "Download failed"
}

private fun downloadDetails(job: DownloadJob): String = listOfNotNull(
    job.progressPercent?.let { "$it%" },
    job.etaSeconds?.let { "${it}s remaining" },
    job.resolved?.fileName,
).joinToString(" · ").ifBlank { "The server is preparing the selected media." }

internal fun downloadDescription(job: DownloadJob, message: String?, error: String?): String = when {
    message != null -> message
    error != null -> error
    job.errorCode == "cancelled" -> "The download was stopped."
    job.error != null -> job.error.orEmpty()
    else -> downloadDetails(job)
}
