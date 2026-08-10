package dev.typetype.android.data.server

import dev.typetype.android.data.network.dto.InstanceResponse
import dev.typetype.android.domain.server.Server
import kotlinx.serialization.json.Json
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerCapabilitiesTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val server = Server(
        id = "server",
        baseUrl = "https://example.test/api/",
        displayName = "Cached",
        addedAt = 1,
    )

    @Test
    fun oldInstanceResponseDisablesAbsentCapabilities() {
        val refreshed = server.withCapabilities(decode(BASE_INSTANCE))

        assertFalse(refreshed.youtubeRemoteLoginSupported)
        assertFalse(refreshed.youtubeRemoteLoginEnabled)
        assertFalse(refreshed.rss.enabled)
        assertNull(refreshed.youtubeRemoteLoginUnavailableReason)
    }

    @Test
    fun presentButDisabledRemoteLoginIsRecognized() {
        val refreshed = server.withCapabilities(
            decode(
                BASE_INSTANCE.dropLast(1) +
                    ",\"youtubeRemoteLoginEnabled\":false," +
                    "\"youtubeRemoteLoginReady\":false," +
                    "\"youtubeRemoteLoginUnavailableReason\":\"disabled\"}",
            ),
        )

        assertTrue(refreshed.youtubeRemoteLoginSupported)
        assertFalse(refreshed.youtubeRemoteLoginEnabled)
        assertFalse(refreshed.youtubeRemoteLoginReady)
    }

    @Test
    fun presentButDisabledRssStaysUnavailable() {
        val refreshed = server.withCapabilities(
            decode(
                BASE_INSTANCE.dropLast(1) +
                    ",\"rss\":{\"enabled\":false,\"maxFeedsPerUser\":10," +
                    "\"maxItems\":50,\"minimumPollMinutes\":5," +
                    "\"rateLimitPerMinute\":30}}",
            ),
        )

        assertFalse(refreshed.rss.enabled)
        assertTrue(refreshed.rss.maxFeedsPerUser == 10)
    }

    @Test
    fun enabledCapabilitiesPreserveServerLimitsAndReadiness() {
        val refreshed = server.withCapabilities(
            decode(
                BASE_INSTANCE.dropLast(1) +
                    ",\"youtubeRemoteLoginEnabled\":true," +
                    "\"youtubeRemoteLoginReady\":true," +
                    "\"rss\":{\"enabled\":true,\"maxFeedsPerUser\":5," +
                    "\"maxItems\":80,\"minimumPollMinutes\":15," +
                    "\"rateLimitPerMinute\":30}}",
            ),
        )

        assertTrue(refreshed.youtubeRemoteLoginSupported)
        assertTrue(refreshed.youtubeRemoteLoginReady)
        assertTrue(refreshed.rss.enabled)
        assertTrue(refreshed.rss.maxFeedsPerUser == 5)
        assertTrue(refreshed.rss.maxItems == 80)
        assertTrue(refreshed.rss.minimumPollMinutes == 15)
        assertTrue(refreshed.rss.rateLimitPerMinute == 30)
    }

    private fun decode(value: String): InstanceResponse = json.decodeFromString(value)

    private companion object {
        const val BASE_INSTANCE =
            """{"name":"Test","version":"1","apiVersion":1,"registrationAllowed":true,"guestAllowed":true}"""
    }
}
