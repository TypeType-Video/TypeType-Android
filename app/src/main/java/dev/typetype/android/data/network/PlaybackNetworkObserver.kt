package dev.typetype.android.data.network

import kotlinx.coroutines.flow.StateFlow

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

interface NetworkAvailabilityObserver {
    val states: StateFlow<PlaybackNetworkState>
}

internal object AlwaysAvailablePlaybackNetworkObserver : PlaybackNetworkObserver {
    private val state = PlaybackNetworkState(isAvailable = true, generation = 0L)

    override fun snapshot(): PlaybackNetworkState = state

    override suspend fun awaitAvailableAfter(
        generation: Long,
        timeoutMs: Long,
    ): Boolean = false
}
