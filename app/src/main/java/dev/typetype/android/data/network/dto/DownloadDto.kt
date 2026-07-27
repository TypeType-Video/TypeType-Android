package dev.typetype.android.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateDownloadJobRequest(
    val url: String,
    val options: DownloadJobOptionsDto = DownloadJobOptionsDto(),
)

@Serializable
data class CreateDownloadJobResponse(
    val id: String,
    val cached: Boolean = false,
)

@Serializable
data class DownloadJobOptionsDto(
    val mode: DownloadModeDto = DownloadModeDto.Video,
    val quality: String = "best",
    val format: String = "mp4",
    val height: Int? = null,
    val videoCodec: String? = null,
    val audioCodec: String? = null,
    val allowQualityFallback: Boolean = true,
    val subtitles: DownloadSubtitlesOptionsDto = DownloadSubtitlesOptionsDto(),
)

@Serializable
enum class DownloadModeDto {
    @SerialName("video")
    Video,

    @SerialName("audio")
    Audio,
}

@Serializable
data class DownloadSubtitlesOptionsDto(
    val enabled: Boolean = false,
    val auto: Boolean = false,
    val embed: Boolean = false,
    val languages: List<String> = emptyList(),
    val format: String = "srt",
)

@Serializable
data class DownloadJobResponse(
    val id: String,
    val url: String,
    val status: DownloadJobStatusDto,
    val durationMs: Long = 0L,
    val title: String = "",
    val errorCode: String? = null,
    val error: String? = null,
    val artifactUrl: String? = null,
    val resolved: DownloadResolvedOutputDto? = null,
    val progressPercent: Int? = null,
    val downloadedBytes: Long? = null,
    val totalBytes: Long? = null,
    val etaSeconds: Long? = null,
    val speedBytesPerSecond: Long? = null,
    val stage: String? = null,
)

@Serializable
enum class DownloadJobStatusDto {
    @SerialName("queued")
    Queued,

    @SerialName("running")
    Running,

    @SerialName("done")
    Done,

    @SerialName("failed")
    Failed,
}

@Serializable
data class DownloadResolvedOutputDto(
    val fileName: String? = null,
    val container: String? = null,
)
