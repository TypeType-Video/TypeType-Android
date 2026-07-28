package dev.typetype.player

data class PlaybackLiveWindow(
    val active: Boolean,
    val postLiveDvr: Boolean,
    val headPositionUs: Long,
    val seekableStartPositionUs: Long,
    val seekableEndPositionUs: Long,
    val atLiveEdge: Boolean,
    val targetLatencyUs: Long,
) {
    init {
        require(active || postLiveDvr)
        require(headPositionUs >= 0L)
        require(seekableStartPositionUs >= 0L)
        require(seekableEndPositionUs > seekableStartPositionUs)
        require(headPositionUs >= seekableEndPositionUs)
        require(targetLatencyUs > 0L)
    }

    val defaultStartPositionUs: Long
        get() = (seekableEndPositionUs - targetLatencyUs)
            .coerceAtLeast(seekableStartPositionUs)
}
