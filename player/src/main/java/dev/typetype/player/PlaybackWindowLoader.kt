package dev.typetype.player

fun interface PlaybackLoadCancellation {
    fun cancel()
}

interface PlaybackWindowLoader {
    fun load(
        request: () -> PlaybackWindowRequest,
        callback: (Result<PlaybackWindow>) -> Unit,
    ): PlaybackLoadCancellation

    fun seek(
        positionUs: Long,
        callback: (Result<PlaybackWindow>) -> Unit,
    ): PlaybackLoadCancellation

    fun release()
}

data class PlaybackWindowRequest(
    val positionUs: Long,
    val bufferedRanges: List<PlaybackBufferedRange>,
)
