package dev.typetype.android.data.youtubesession

import dev.typetype.android.data.network.dto.YoutubeSessionStatusResponse
import dev.typetype.android.domain.youtubesession.YoutubeSessionStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class YoutubeSessionMappingsTest {
    @Test
    fun mapsEveryDocumentedStatus() {
        assertEquals(YoutubeSessionStatus.Disconnected, status("disconnected"))
        assertEquals(YoutubeSessionStatus.Connected, status("connected"))
        assertEquals(YoutubeSessionStatus.NeedsReconnect, status("needs_reconnect"))
    }

    @Test
    fun preservesAnUnknownFutureStatusWithoutCrashing() {
        assertEquals(YoutubeSessionStatus.Unknown, status("temporarily_paused"))
    }

    private fun status(value: String) = YoutubeSessionStatusResponse(
        status = value,
        updatedAt = 0,
        lastUsedAt = 0,
    ).toDomain().status
}
