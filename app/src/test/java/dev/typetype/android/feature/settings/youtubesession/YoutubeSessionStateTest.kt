package dev.typetype.android.feature.settings.youtubesession

import dev.typetype.android.domain.server.Server
import dev.typetype.android.domain.youtubesession.YoutubeRemoteBrowserPhase
import dev.typetype.android.domain.youtubesession.YoutubeSession
import dev.typetype.android.domain.youtubesession.YoutubeSessionStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YoutubeSessionStateTest {
    @Test
    fun startsOnlyWhenTheInstanceIsReadyAndNoBrowserIsOpen() {
        val ready = YoutubeSessionState(availability = YoutubeSessionAvailability.Available)

        assertTrue(ready.canStart)
        assertFalse(ready.copy(isStarting = true).canStart)
        assertFalse(ready.copy(remoteSessionId = "active").canStart)
        assertFalse(
            ready.copy(availability = YoutubeSessionAvailability.Unavailable).canStart,
        )
    }

    @Test
    fun disconnectsOnlyAStoredSession() {
        val connected = YoutubeSessionState(
            session = YoutubeSession(YoutubeSessionStatus.Connected, 2, 1),
        )
        val reconnect = connected.copy(
            session = connected.session?.copy(status = YoutubeSessionStatus.NeedsReconnect),
        )
        val disconnected = connected.copy(
            session = connected.session?.copy(status = YoutubeSessionStatus.Disconnected),
        )

        assertTrue(connected.canDisconnect)
        assertTrue(reconnect.canDisconnect)
        assertFalse(disconnected.canDisconnect)
        assertFalse(connected.copy(isDisconnecting = true).canDisconnect)
    }

    @Test
    fun distinguishesDisabledAndUnavailableServerCapabilities() {
        val supported = server().copy(youtubeRemoteLoginSupported = true)

        assertTrue(server().youtubeSessionAvailability() == YoutubeSessionAvailability.Disabled)
        assertTrue(
            supported.copy(
                youtubeRemoteLoginUnavailableReason = "disabled",
            ).youtubeSessionAvailability() == YoutubeSessionAvailability.Disabled,
        )
        assertTrue(
            supported.copy(
                youtubeRemoteLoginUnavailableReason = "not_configured",
            ).youtubeSessionAvailability() == YoutubeSessionAvailability.Unavailable,
        )
        assertTrue(
            supported.copy(
                youtubeRemoteLoginUnavailableReason = "token_unreachable",
            ).youtubeSessionAvailability() == YoutubeSessionAvailability.Unavailable,
        )
        assertTrue(
            supported.copy(
                youtubeRemoteLoginEnabled = true,
                youtubeRemoteLoginReady = true,
            ).youtubeSessionAvailability() == YoutubeSessionAvailability.Available,
        )
    }

    @Test
    fun accountChangeClearsSessionAndRemoteBrowserState() {
        val state = YoutubeSessionState(
            availability = YoutubeSessionAvailability.Available,
            session = YoutubeSession(YoutubeSessionStatus.Connected, 1, 2),
            remoteSessionId = "remote",
            remoteSessionExpiresAt = 3,
            remotePhase = YoutubeRemoteBrowserPhase.AwaitingLogin,
            frameBytes = byteArrayOf(1),
            errorMessage = "Old account error",
            errorRequestId = "old-request",
        )

        val cleared = state.clearedForAccountChange()

        assertTrue(cleared.availability == YoutubeSessionAvailability.Checking)
        assertTrue(cleared.session == null)
        assertFalse(cleared.remoteBrowserOpen)
        assertTrue(cleared.frameBytes == null)
        assertTrue(cleared.errorMessage == null)
        assertTrue(cleared.errorRequestId == null)
    }

    private fun server() = Server(
        id = "server",
        baseUrl = "https://example.test/api/",
        displayName = "Test",
        addedAt = 0,
    )
}
