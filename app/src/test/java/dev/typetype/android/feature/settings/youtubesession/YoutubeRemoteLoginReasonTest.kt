package dev.typetype.android.feature.settings.youtubesession

import dev.typetype.android.R
import org.junit.Assert.assertEquals
import org.junit.Test

class YoutubeRemoteLoginReasonTest {
    @Test
    fun mapsServerReadinessReasonsToUsefulMessages() {
        assertEquals(
            R.string.youtube_session_reason_not_configured,
            youtubeRemoteLoginReason("not_configured"),
        )
        assertEquals(
            R.string.youtube_session_reason_token_unreachable,
            youtubeRemoteLoginReason("token_unreachable"),
        )
        assertEquals(
            R.string.youtube_session_reason_unknown,
            youtubeRemoteLoginReason("future_reason"),
        )
    }
}
