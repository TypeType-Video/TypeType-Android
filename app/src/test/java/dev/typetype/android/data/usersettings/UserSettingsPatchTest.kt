package dev.typetype.android.data.usersettings

import dev.typetype.android.data.network.dto.UserSettingsDto
import dev.typetype.android.domain.usersettings.UserSettings
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UserSettingsPatchTest {
    @Test
    fun `unchanged settings produce an empty patch`() {
        val settings = UserSettings(disableWatchHistory = true)

        assertTrue(settings.patchFrom(settings).isEmpty())
    }

    @Test
    fun `history tracking toggle only patches the server privacy field`() {
        val patch = UserSettings(disableWatchHistory = true).patchFrom(UserSettings())

        assertEquals(setOf("disableWatchHistory"), patch.keys)
        assertTrue(requireNotNull(patch["disableWatchHistory"]).jsonPrimitive.boolean)
    }

    @Test
    fun `player update does not resend unrelated settings`() {
        val previous = UserSettings(
            defaultService = 6,
            autoplay = false,
            disableWatchHistory = true,
        )

        val patch = previous.copy(defaultService = 0).patchFrom(previous)

        assertEquals(setOf("defaultService"), patch.keys)
        assertEquals(0, requireNotNull(patch["defaultService"]).jsonPrimitive.int)
    }

    @Test
    fun `server privacy value maps into the domain policy`() {
        val dto = JSON.decodeFromString<UserSettingsDto>(
            """
                {
                  "disableWatchHistory": true,
                  "hideHomeRecommendations": true,
                  "hideContinueWatching": true,
                  "hideComments": true
                }
            """.trimIndent(),
        )

        assertTrue(dto.toDomain().disableWatchHistory)
        assertTrue(dto.toDomain().hideHomeRecommendations)
        assertTrue(dto.toDomain().hideContinueWatching)
        assertTrue(dto.toDomain().autoplay)
    }

    private companion object {
        val JSON = Json { ignoreUnknownKeys = true }
    }
}
