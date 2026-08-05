package dev.typetype.android.feature.settings.youtubesession

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
}
