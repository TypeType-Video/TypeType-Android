package dev.typetype.android.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class SabrPlaybackRequest(
    val videoItag: Int? = null,
    val audioItag: Int? = null,
    val audioTrackId: String? = null,
    val startTimeMs: Long? = null,
    val playerTimeMs: Long? = null,
    val audioOnly: Boolean = false,
    val isLive: Boolean = false,
)

@Serializable
data class SabrPlaybackResponse(
    val sessionId: String,
    val videoId: String,
    val manifestUrl: String? = null,
    val videoItag: Int,
    val audioItag: Int,
    val audioTrackId: String? = null,
    val startTimeMs: Long = 0L,
    val generation: Long,
    val ready: Boolean,
    val status: String,
    val retryAfterMs: Long? = null,
    val live: SabrLivePlaybackDto? = null,
)

@Serializable
data class SabrLivePlaybackDto(
    val active: Boolean,
    val postLiveDvr: Boolean,
    val headSequence: Long,
    val headTimeMs: Long,
    val seekableStartMs: Long,
    val seekableEndMs: Long,
    val atLiveEdge: Boolean,
    val targetLatencyMs: Long,
)

@Serializable
data class SabrPlaybackBufferedRangeDto(
    val itag: Int,
    val startMs: Long,
    val endMs: Long,
    val startSequence: Int? = null,
    val endSequence: Int? = null,
)

@Serializable
data class SabrPlaybackPositionRequestDto(
    val generation: Long,
    val playerTimeMs: Long,
    val videoItag: Int,
    val audioItag: Int,
    val audioTrackId: String? = null,
    val playbackRate: Float = 1.0f,
    val bufferedRanges: List<SabrPlaybackBufferedRangeDto> = emptyList(),
    val audioOnly: Boolean = false,
)

@Serializable
data class SabrPlaybackWindowRequestDto(
    val generation: Long,
    val playerTimeMs: Long,
    val videoItag: Int,
    val audioItag: Int,
    val audioTrackId: String? = null,
    val playbackRate: Float = 1.0f,
    val bufferGoalMs: Long = 30_000L,
    val backBufferMs: Long = 30_000L,
    val bufferedRanges: List<SabrPlaybackBufferedRangeDto> = emptyList(),
    val audioOnly: Boolean = false,
)

@Serializable
data class SabrPlaybackPositionResponseDto(
    val sessionId: String,
    val generation: Long,
    val playerTimeMs: Long,
    val readerHeadMs: Long,
    val readerTailMs: Long,
    val bufferedEdgeMs: Long,
    val live: SabrLivePlaybackDto? = null,
)

@Serializable
data class SabrPlaybackWindowResponseDto(
    val sessionId: String,
    val generation: Long,
    val ready: Boolean,
    val retryAfterMs: Long? = null,
    val status: String? = null,
    val durationMs: Long? = null,
    val endOfStream: Boolean? = null,
    val audio: SabrPlaybackWindowTrackDto? = null,
    val video: SabrPlaybackWindowTrackDto? = null,
    val startTimeMs: Long? = null,
    val playerTimeMs: Long? = null,
    val readerHeadMs: Long? = null,
    val readerTailMs: Long? = null,
    val bufferedEdgeMs: Long? = null,
    val terminalError: String? = null,
    val recoveryAction: String? = null,
    val retryVideoItags: List<Int> = emptyList(),
    val live: SabrLivePlaybackDto? = null,
)

@Serializable
data class SabrPlaybackWindowTrackDto(
    val mime: String,
    val initUrl: String,
    val segments: List<SabrPlaybackWindowSegmentDto>,
)

@Serializable
data class SabrPlaybackWindowSegmentDto(
    val url: String,
    val startMs: Long,
    val durationMs: Long,
)
