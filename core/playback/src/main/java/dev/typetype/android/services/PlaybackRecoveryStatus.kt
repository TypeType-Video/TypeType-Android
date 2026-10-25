package dev.typetype.android.services

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface PlaybackRecoveryState {
    data object Idle : PlaybackRecoveryState

    data class Recovering(val mediaId: String) : PlaybackRecoveryState

    data class GaveUp(val mediaId: String) : PlaybackRecoveryState
}

// What the playback service is doing about a failed item. The player shows buffering while the service retries,
// and the error only once it gave up or the failure cannot be retried, instead of guessing from the exception.
@Singleton
class PlaybackRecoveryStatus @Inject constructor() {
    private val mutableState = MutableStateFlow<PlaybackRecoveryState>(PlaybackRecoveryState.Idle)
    val state: StateFlow<PlaybackRecoveryState> = mutableState.asStateFlow()

    internal fun recovering(mediaId: String) {
        mutableState.value = PlaybackRecoveryState.Recovering(mediaId)
    }

    internal fun gaveUp(mediaId: String) {
        mutableState.value = PlaybackRecoveryState.GaveUp(mediaId)
    }

    internal fun clear() {
        mutableState.value = PlaybackRecoveryState.Idle
    }
}

fun PlaybackRecoveryState.isRecovering(mediaId: String?): Boolean =
    this is PlaybackRecoveryState.Recovering && this.mediaId == mediaId
