package dev.typetype.android.data.usersettings

import dev.typetype.android.data.network.dto.UserSettingsDto
import dev.typetype.android.domain.usersettings.SEEKBAR_ICON_CUSTOM
import dev.typetype.android.domain.usersettings.SEEKBAR_ICON_NONE
import dev.typetype.android.domain.usersettings.UserSettings
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SeekbarIconSettingsTest {
    @Test
    fun `the icon url resolves against the instance`() {
        val settings = UserSettingsDto(seekbarIcon = SEEKBAR_ICON_CUSTOM, seekbarIconUrl = "/seekbar-icon/user/v1")
            .toDomain("https://typetype.example.com/api/")

        assertEquals(SEEKBAR_ICON_CUSTOM, settings.seekbarIcon)
        assertEquals("https://typetype.example.com/api/seekbar-icon/user/v1", settings.seekbarIconUrl)
    }

    @Test
    fun `no uploaded icon leaves no url`() {
        assertNull(UserSettingsDto().toDomain("https://typetype.example.com/api/").seekbarIconUrl)
    }

    @Test
    fun `an icon url from another host is dropped`() {
        val settings = UserSettingsDto(seekbarIconUrl = "https://elsewhere.example/icon.gif")
            .toDomain("https://typetype.example.com/api/")

        assertNull(settings.seekbarIconUrl)
    }

    @Test
    fun `choosing an icon only patches the choice, never the url`() {
        val previous = UserSettings(seekbarIconUrl = "https://typetype.example.com/api/seekbar-icon/user/v1")
        val patch = previous.copy(seekbarIcon = SEEKBAR_ICON_NONE, seekbarIconUrl = null).patchFrom(previous)

        assertEquals(setOf("seekbarIcon"), patch.keys)
        assertEquals(SEEKBAR_ICON_NONE, patch["seekbarIcon"]?.jsonPrimitive?.content)
    }
}
