package dev.typetype.android.core.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.typetype.android.domain.preferences.AccentColor
import dev.typetype.android.domain.preferences.AppearanceTheme
import dev.typetype.android.domain.preferences.MangaPaper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppearanceSchemeTest {
    @Test
    fun mangaThemesChangeVisibleSurfaces() {
        val accent = accentColors(AccentColor.Blue)
        val default = mangaScheme(
            paper = MangaPaper.Day,
            theme = AppearanceTheme.TypeType,
            accent = accent.first,
            accentSoft = accent.second,
            amoled = false,
        )
        val forest = mangaScheme(
            paper = MangaPaper.Day,
            theme = AppearanceTheme.Forest,
            accent = accent.first,
            accentSoft = accent.second,
            amoled = false,
        )

        assertNotEquals(default.background, forest.background)
        assertNotEquals(default.surface, forest.surface)
    }

    @Test
    fun mangaAmoledUsesPureBlackSurfaces() {
        val accent = accentColors(AccentColor.Blue)
        val scheme = mangaScheme(
            paper = MangaPaper.Nord,
            theme = AppearanceTheme.Forest,
            accent = accent.first,
            accentSoft = accent.second,
            amoled = true,
        )

        assertEquals(Color.Black, scheme.background)
        assertEquals(Color.Black, scheme.surface)
    }
}
