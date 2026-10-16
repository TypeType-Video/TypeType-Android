package dev.typetype.android.feature.player

import dev.typetype.android.data.network.PlaybackNetworkState
import dev.typetype.android.feature.player.error.StreamErrorClass
import dev.typetype.android.feature.player.error.StreamErrorKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayerStreamNetworkRecoveryTest {
    private val failure = StreamErrorClass(StreamErrorKind.NetworkUnavailable, rawMessage = null)
    private val state = PlayerState(videoUrl = "https://video.example/public", isLoading = false, error = failure)

    @Test
    fun recoversOncePerNetworkGeneration() {
        val gate = PlayerStreamNetworkRetryGate()
        assertNull(gate.retryUrl(state, PlaybackNetworkState(false, 1L)))
        assertEquals(state.videoUrl, gate.retryUrl(state, PlaybackNetworkState(true, 2L)))
        assertNull(gate.retryUrl(state, PlaybackNetworkState(true, 2L)))
        assertEquals(state.videoUrl, gate.retryUrl(state, PlaybackNetworkState(true, 3L)))
    }

    @Test
    fun loadingAndOtherErrorsDoNotRetry() {
        val gate = PlayerStreamNetworkRetryGate()
        val online = PlaybackNetworkState(true, 2L)
        assertNull(gate.retryUrl(state.copy(isLoading = true), online))
        assertNull(gate.retryUrl(state.copy(error = null), online))
        assertNull(gate.retryUrl(state.copy(videoUrl = ""), online))
        assertNull(gate.retryUrl(state.copy(error = StreamErrorClass(
            StreamErrorKind.SabrUnavailable, rawMessage = null,
        )), online))
        assertEquals(state.videoUrl, gate.retryUrl(state, online))
    }
}
