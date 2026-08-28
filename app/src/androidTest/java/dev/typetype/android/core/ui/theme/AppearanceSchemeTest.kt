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
            isDark = false,
        )
        val forest = mangaScheme(
            paper = MangaPaper.Day,
            theme = AppearanceTheme.Forest,
            accent = accent.first,
            accentSoft = accent.second,
            amoled = false,
            isDark = true,
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
            isDark = true,
        )

        assertEquals(Color.Black, scheme.background)
        assertEquals(Color.Black, scheme.surface)
    }

    @Test
    fun mangaPaperKeepsTextContrastWhenColorModeDiffers() {
        val accent = accentColors(AccentColor.Blue)
        val dayPaperInDarkMode = mangaScheme(
            paper = MangaPaper.Day,
            theme = AppearanceTheme.TypeType,
            accent = accent.first,
            accentSoft = accent.second,
            amoled = false,
            isDark = true,
        )
        val nightPaperInLightMode = mangaScheme(
            paper = MangaPaper.Night,
            theme = AppearanceTheme.TypeType,
            accent = accent.first,
            accentSoft = accent.second,
            amoled = false,
            isDark = false,
        )

        assertEquals(Color(0xFFF5F3EA), dayPaperInDarkMode.onBackground)
        assertEquals(Color(0xFF211F1A), dayPaperInDarkMode.background)
        assertEquals(Color(0xFFF5F3EA), nightPaperInLightMode.onBackground)
    }

    @Test
    fun everyMangaPaperKeepsItsInkContrastAcrossColorModes() {
        val accent = accentColors(AccentColor.Blue)
        val paperThemes = listOf(AppearanceTheme.TypeType, AppearanceTheme.Dynamic)
        val modes = listOf(false, true)

        paperThemes.forEach { theme ->
            MangaPaper.entries.forEach { paper ->
                modes.forEach { isDark ->
                    val scheme = mangaScheme(
                        paper = paper,
                        theme = theme,
                        accent = accent.first,
                        accentSoft = accent.second,
                        amoled = false,
                        isDark = isDark,
                    )
                    val expectedInk = if (paper == MangaPaper.Day && !isDark) {
                        Color(0xFF171717)
                    } else {
                        Color(0xFFF5F3EA)
                    }

                    assertEquals(expectedInk, scheme.onBackground)
                    assertEquals(expectedInk, scheme.onSurface)
                }
            }
        }
    }
}
