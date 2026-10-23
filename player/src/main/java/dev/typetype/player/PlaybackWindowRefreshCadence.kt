package dev.typetype.player

internal class PlaybackWindowRefreshCadence(
    private val initialPollIntervalMs: Long = 500L,
    private val maximumPollIntervalMs: Long = 2_000L,
    private val progressThresholdUs: Long = 1_000_000L,
) {
    var pollIntervalMs: Long = initialPollIntervalMs
        private set

    fun reset() {
        pollIntervalMs = initialPollIntervalMs
    }

    fun onWindowAvailable(
        previousAheadUs: Long,
        nextAheadUs: Long,
        refreshThresholdUs: Long,
    ): Long {
        val progressUs = nextAheadUs - previousAheadUs
        pollIntervalMs = if (
            nextAheadUs >= refreshThresholdUs || progressUs >= progressThresholdUs
        ) {
            initialPollIntervalMs
        } else {
            (pollIntervalMs * 2).coerceAtMost(maximumPollIntervalMs)
        }
        return pollIntervalMs
    }
}

internal fun PlaybackWindow.availableWindowAheadUs(positionUs: Long): Long {
    val availableEndUs = listOfNotNull(audio.endPositionUs, video?.endPositionUs).min()
    return (availableEndUs - positionUs).coerceAtLeast(0L)
}
