package dev.typetype.android.data.download

import dev.typetype.android.data.network.dto.DownloadJobResponse

object DownloadFileNames {
    fun from(job: DownloadJobResponse, fallbackTitle: String): String {
        val extension = job.resolved?.container?.takeIf { it.isNotBlank() }
            ?: job.resolved?.fileName?.substringAfterLast('.', "")?.takeIf { it.isNotBlank() }
            ?: DEFAULT_EXTENSION
        val baseName = fallbackTitle.ifBlank { job.title }.ifBlank { DEFAULT_NAME }
        return "${sanitize(baseName)}.$extension"
    }

    private fun sanitize(value: String): String =
        value.replace(Regex("""[\\/:*?"<>|]+"""), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(MAX_LENGTH)
            .ifBlank { DEFAULT_NAME }

    private const val DEFAULT_EXTENSION = "mp4"
    private const val DEFAULT_NAME = "TypeType video"
    private const val MAX_LENGTH = 180
}
