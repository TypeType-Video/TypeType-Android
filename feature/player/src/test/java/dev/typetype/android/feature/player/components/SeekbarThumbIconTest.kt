package dev.typetype.android.feature.player.components

import dev.typetype.android.domain.usersettings.SEEKBAR_ICON_CUSTOM
import dev.typetype.android.domain.usersettings.SEEKBAR_ICON_NONE
import dev.typetype.android.domain.usersettings.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class SeekbarThumbIconTest {
    @Test
    fun `everyone starts with the TypeType icon`() {
        assertEquals(SeekbarThumbIcon.TypeType, UserSettings().seekbarThumbIcon())
    }

    @Test
    fun `a custom icon uses the uploaded image`() {
        val settings = UserSettings(seekbarIcon = SEEKBAR_ICON_CUSTOM, seekbarIconUrl = "https://host/icon")

        assertEquals(SeekbarThumbIcon.Remote("https://host/icon"), settings.seekbarThumbIcon())
    }

    @Test
    fun `a custom choice without an image falls back to the TypeType icon`() {
        assertEquals(SeekbarThumbIcon.TypeType, UserSettings(seekbarIcon = SEEKBAR_ICON_CUSTOM).seekbarThumbIcon())
    }

    @Test
    fun `none keeps the plain thumb`() {
        assertEquals(SeekbarThumbIcon.Plain, UserSettings(seekbarIcon = SEEKBAR_ICON_NONE).seekbarThumbIcon())
    }
}
