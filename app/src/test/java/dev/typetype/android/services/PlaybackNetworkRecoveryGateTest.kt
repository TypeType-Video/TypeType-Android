package dev.typetype.android.services

import androidx.media3.common.PlaybackException
import dev.typetype.android.core.error.CodedFailure
import dev.typetype.android.data.network.PlaybackNetworkState
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackNetworkRecoveryGateTest {
    @Test
    fun `offline failure waits and retries immediately on restored network`() {
        val gate = PlaybackNetworkRecoveryGate()

        val offline = gate.failed("video", PlaybackNetworkState(false, 1L))
        val restored = gate.networkChanged(PlaybackNetworkState(true, 2L))

        assertEquals(PlaybackNetworkRecoveryAction.Wait, offline)
        assertEquals(PlaybackNetworkRecoveryAction.RetryAfter(0L), restored)
        assertTrue(gate.isPending("video"))
    }

    @Test
    fun `same route recovery uses a bounded progressive schedule`() {
        val gate = PlaybackNetworkRecoveryGate()
        val network = PlaybackNetworkState(true, 3L)
        val expectedDelays = listOf(1_000L, 3_000L, 10_000L, 30_000L) +
            List(4) { 60_000L }

        val actions = expectedDelays.map { gate.failed("video", network) }
        val exhausted = gate.failed("video", network)

        assertEquals(expectedDelays.map(PlaybackNetworkRecoveryAction::RetryAfter), actions)
        assertEquals(PlaybackNetworkRecoveryAction.Wait, exhausted)
    }

    @Test
    fun `ready playback clears pending recovery`() {
        val gate = PlaybackNetworkRecoveryGate()
        gate.failed("video", PlaybackNetworkState(true, 0L))

        gate.recovered()

        assertFalse(gate.isPending("video"))
        assertEquals(
            PlaybackNetworkRecoveryAction.Wait,
            gate.networkChanged(PlaybackNetworkState(true, 1L)),
        )
    }

    @Test
    fun `media transition prevents a stale retry`() {
        val gate = PlaybackNetworkRecoveryGate()
        gate.failed("first", PlaybackNetworkState(false, 0L))

        gate.transition("second")

        assertFalse(gate.isPending("first"))
        assertEquals(
            PlaybackNetworkRecoveryAction.Wait,
            gate.networkChanged(PlaybackNetworkState(true, 1L)),
        )
    }

    @Test
    fun `typed SABR failure is not classified as a network interruption`() {
        val failure = object : IOException(), CodedFailure {
            override val failureCode: String = "youtube_sabr_contract_mismatch"
            override val requestId: String? = null
            override val statusCode: Int? = null
        }
        val isNetworkFailure = isNetworkPlaybackFailure(
            PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
            failure,
        )

        assertFalse(isNetworkFailure)
    }

    @Test
    fun `explicit connection failure is classified as network interruption`() {
        val isNetworkFailure = isNetworkPlaybackFailure(
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            IOException("offline"),
        )

        assertTrue(isNetworkFailure)
    }
}
