package dev.typetype.android.data.usersettings

import dev.typetype.android.data.network.dto.UserSettingsDto
import dev.typetype.android.domain.usersettings.UserSettings
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackVisibilitySettingsTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `server values reach the app settings`() {
        val settings = json.decodeFromString<UserSettingsDto>(
            """{"holdPlaybackSpeed":3.0,"autoplayOnOpen":false,"hideUpcomingPlaylistVideos":true,
               "hideMembersOnlyContent":true}""",
        ).toDomain()

        assertEquals(3.0, settings.holdPlaybackSpeed, 0.0)
        assertFalse(settings.autoplayOnOpen)
        assertTrue(settings.hideUpcomingPlaylistVideos)
        assertTrue(settings.hideMembersOnlyContent)
    }

    @Test
    fun `an older server without these fields keeps the web defaults`() {
        val settings = json.decodeFromString<UserSettingsDto>("{}").toDomain()

        assertEquals(2.0, settings.holdPlaybackSpeed, 0.0)
        assertTrue(settings.autoplayOnOpen)
        assertFalse(settings.hideUpcomingPlaylistVideos)
        assertFalse(settings.hideMembersOnlyContent)
    }

    @Test
    fun `a hold speed outside the web choices falls back to twice the speed`() {
        val settings = json.decodeFromString<UserSettingsDto>("""{"holdPlaybackSpeed":1.75}""").toDomain()

        assertEquals(2.0, settings.holdPlaybackSpeed, 0.0)
    }

    @Test
    fun `changing them patches only their own fields`() {
        val previous = UserSettings()
        val patch = previous.copy(
            holdPlaybackSpeed = 2.5,
            autoplayOnOpen = false,
            hideUpcomingPlaylistVideos = true,
            hideMembersOnlyContent = true,
        ).patchFrom(previous)

        assertEquals(
            setOf("holdPlaybackSpeed", "autoplayOnOpen", "hideUpcomingPlaylistVideos", "hideMembersOnlyContent"),
            patch.keys,
        )
        assertEquals(2.5, requireNotNull(patch["holdPlaybackSpeed"]).jsonPrimitive.double, 0.0)
        assertFalse(requireNotNull(patch["autoplayOnOpen"]).jsonPrimitive.boolean)
    }
}
