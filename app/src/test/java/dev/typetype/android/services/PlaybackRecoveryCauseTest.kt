package dev.typetype.android.services

import dev.typetype.android.data.network.ServerError
import dev.typetype.android.data.network.ServerResponseException
import dev.typetype.android.data.stream.SABR_PREPARATION_TIMEOUT
import dev.typetype.android.data.stream.SabrControlException
import dev.typetype.android.data.stream.SabrPlaybackRecoveryException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackRecoveryCauseTest {
    @Test
    fun aTransportFailureUnderTheWindowWrapperIsANetworkFailure() {
        assertEquals(PlaybackRecoveryCause.Network, windowFailure(SocketTimeoutException("read")).playbackRecoveryCause())
        assertEquals(PlaybackRecoveryCause.Network, UnknownHostException("dns").playbackRecoveryCause())
    }

    @Test
    fun theWindowWrapperAloneIsNotANetworkFailure() {
        assertEquals(
            PlaybackRecoveryCause.Fatal,
            windowFailure(IllegalStateException("SABR changed the audio track")).playbackRecoveryCause(),
        )
    }

    @Test
    fun aStuckSessionAsksForAFreshSession() {
        val stuck = SabrControlException("SABR prefetch stuck: segment-pending", SABR_PREPARATION_TIMEOUT)
        assertEquals(PlaybackRecoveryCause.Session, windowFailure(stuck).playbackRecoveryCause())
    }

    @Test
    fun serverStatusesMapToRetryFreshSessionOrStop() {
        assertEquals(PlaybackRecoveryCause.Server, server(503).playbackRecoveryCause())
        assertEquals(PlaybackRecoveryCause.Server, server(429).playbackRecoveryCause())
        assertEquals(PlaybackRecoveryCause.Session, server(410).playbackRecoveryCause())
        assertEquals(PlaybackRecoveryCause.Fatal, server(401).playbackRecoveryCause())
    }

    @Test
    fun serverRecoveryActionsChooseBetweenAFreshSessionAndARebuild() {
        assertEquals(PlaybackRecoveryCause.Session, action("retry_fresh_session").playbackRecoveryCause())
        assertEquals(
            PlaybackRecoveryCause.Rebuild,
            action("retry_fresh_session_lower_video_itag").playbackRecoveryCause(),
        )
        assertEquals(PlaybackRecoveryCause.Fatal, action(null).playbackRecoveryCause())
    }

    @Test
    fun anExhaustedRecoveryIsNotRetriedAgain() {
        val exhausted = SabrPlaybackRecoveryExhaustedException(action("retry_fresh_session"))
        assertEquals(PlaybackRecoveryCause.Fatal, exhausted.playbackRecoveryCause())
    }

    private fun windowFailure(cause: Throwable) = IOException("Playback window failed", cause)

    private fun server(status: Int) = ServerResponseException(ServerError("failed", null, status))

    private fun action(name: String?) = SabrPlaybackRecoveryException("window failed", name, emptyList())
}
