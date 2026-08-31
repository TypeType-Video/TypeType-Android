package video.typetype.tv.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

public class TvPaletteTest {
    @Test
    public fun everyClassicPaletteKeepsReadableText(): Unit {
        TvColorTheme.entries.forEach { theme ->
            listOf(false, true).forEach { dark ->
                listOf(false, true).forEach { amoled ->
                    assertReadable(
                        paletteFor(
                            TvAppearance(
                                personality = TvPersonality.Classic,
                                colorTheme = theme,
                                amoled = amoled,
                            ),
                            dark,
                        ),
                    )
                }
            }
        }
    }

    @Test
    public fun everyMangaPaperKeepsReadableInkAcrossColorModes(): Unit {
        TvColorTheme.entries.forEach { theme ->
            TvMangaPaper.entries.forEach { paper ->
                listOf(false, true).forEach { dark ->
                    listOf(false, true).forEach { amoled ->
                        assertReadable(
                            paletteFor(
                                TvAppearance(
                                    personality = TvPersonality.Manga,
                                    colorTheme = theme,
                                    mangaPaper = paper,
                                    amoled = amoled,
                                ),
                                dark,
                            ),
                        )
                    }
                }
            }
        }
    }

    @Test
    public fun mangaPaperControlsBrightnessWithoutBreakingAmoledLightMode(): Unit {
        val dayOnDarkSystem = paletteFor(
            TvAppearance(personality = TvPersonality.Manga, mangaPaper = TvMangaPaper.Day),
            dark = true,
        )
        val nightOnLightSystem = paletteFor(
            TvAppearance(personality = TvPersonality.Manga, mangaPaper = TvMangaPaper.Night),
            dark = false,
        )
        val amoledInLightMode = paletteFor(
            TvAppearance(personality = TvPersonality.Manga, mangaPaper = TvMangaPaper.Day, amoled = true),
            dark = false,
        )
        assertFalse(dayOnDarkSystem.isDark)
        assertTrue(nightOnLightSystem.isDark)
        assertFalse(amoledInLightMode.isDark)
        assertTrue(amoledInLightMode.background != Color.Black)
    }

    private fun assertReadable(palette: TvPalette) {
        assertTrue(contrast(palette.onBackground, palette.background) >= 7.0f)
        assertTrue(contrast(palette.onBackground, palette.surface) >= 7.0f)
        val variantInk = palette.onSurfaceVariant.compositeOver(palette.surfaceVariant)
        assertTrue(contrast(variantInk, palette.surfaceVariant) >= 4.5f)
        assertTrue(contrast(contrastText(palette.accent), palette.accent) >= 4.5f)
    }

    private fun contrast(foreground: Color, background: Color): Float {
        val lighter = maxOf(foreground.luminance(), background.luminance())
        val darker = minOf(foreground.luminance(), background.luminance())
        return (lighter + .05f) / (darker + .05f)
    }
}
