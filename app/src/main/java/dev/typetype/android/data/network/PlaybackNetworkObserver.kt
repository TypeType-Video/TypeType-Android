package dev.typetype.android.data.network

data class PlaybackNetworkState(
    val isAvailable: Boolean,
    val generation: Long,
)

interface PlaybackNetworkObserver {
    fun snapshot(): PlaybackNetworkState

    suspend fun awaitAvailableAfter(
        generation: Long,
        timeoutMs: Long,
    ): Boolean
}

internal object AlwaysAvailablePlaybackNetworkObserver : PlaybackNetworkObserver {
    private val state = PlaybackNetworkState(isAvailable = true, generation = 0L)

    override fun snapshot(): PlaybackNetworkState = state

    override suspend fun awaitAvailableAfter(
        generation: Long,
        timeoutMs: Long,
    ): Boolean = false
}
