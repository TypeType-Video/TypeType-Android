package dev.typetype.android.domain.stream

data class SabrPlaybackTarget(
    val videoId: String,
    val requestScope: StreamRequestScope,
    val videoItag: Int,
    val audioItag: Int,
    val audioTrackId: String?,
    val recoveryVideoItags: Set<Int> = emptySet(),
    val isLive: Boolean = false,
)

fun Stream.sabrPlaybackTarget(selection: SabrPlaybackSelection): SabrPlaybackTarget =
    SabrPlaybackTarget(
        videoId = id,
        requestScope = requireNotNull(requestScope),
        videoItag = selection.video.itag,
        audioItag = selection.audio.itag,
        audioTrackId = selection.audio.audioTrackId,
        recoveryVideoItags = selection.recoveryVideoItags,
        isLive = isLive,
    )

fun SabrPlaybackTarget.accept(session: SabrPlaybackSession): SabrPlaybackTarget {
    require(session.audioItag == audioItag && session.audioTrackId == audioTrackId)
    require(session.videoItag == videoItag || session.videoItag in recoveryVideoItags)
    return copy(
        videoItag = session.videoItag,
        recoveryVideoItags = recoveryVideoItags.filterTo(linkedSetOf()) {
            it != session.videoItag
        },
    )
}

val SabrPlaybackTarget.sourceKey: String
    get() = listOf(
        "sabr",
        videoId,
        videoItag,
        audioItag,
        audioTrackId.orEmpty(),
    ).joinToString(":")
