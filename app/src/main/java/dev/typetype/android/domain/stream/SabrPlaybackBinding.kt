package dev.typetype.android.domain.stream

data class SabrPlaybackBinding(
    val sessionId: String,
    val generation: Long,
    val videoItag: Int,
    val audioItag: Int,
    val audioTrackId: String? = null,
)

val SabrPlaybackSession.binding: SabrPlaybackBinding
    get() = SabrPlaybackBinding(
        sessionId = sessionId,
        generation = generation,
        videoItag = videoItag,
        audioItag = audioItag,
        audioTrackId = audioTrackId,
    )
