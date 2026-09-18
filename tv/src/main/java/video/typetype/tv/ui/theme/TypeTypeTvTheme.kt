package video.typetype.tv.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalResources
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import androidx.tv.material3.lightColorScheme

public val LocalTvAppearance = staticCompositionLocalOf { TvAppearance() }

@Composable
public fun TypeTypeTvTheme(
    appearance: TvAppearance,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val modeDark = when (appearance.colorMode) {
        TvColorMode.Light -> false
        TvColorMode.Dark -> true
        TvColorMode.System -> systemDark
    }
    val resources = LocalResources.current
    val dynamicPalette = if (
        appearance.colorTheme == TvColorTheme.Dynamic && !appearance.isManga && Build.VERSION.SDK_INT >= 31
    ) systemDynamicPalette(resources, modeDark) else null
    val dynamicAccent = if (appearance.colorTheme == TvColorTheme.Dynamic && Build.VERSION.SDK_INT >= 31) {
        resources.getColor(if (modeDark) android.R.color.system_accent1_200 else android.R.color.system_accent1_600, null)
    } else null
    val palette = dynamicPalette ?: paletteFor(appearance, modeDark, dynamicAccent?.let(::Color))
    CompositionLocalProvider(LocalTvAppearance provides appearance) {
        MaterialTheme(
            colorScheme = if (palette.isDark) {
                darkColorScheme(
                    primary = palette.accent,
                    onPrimary = contrastText(palette.accent),
                    background = palette.background,
                    onBackground = palette.onBackground,
                    surface = palette.surface,
                    onSurface = palette.onBackground,
                    surfaceVariant = palette.surfaceVariant,
                    onSurfaceVariant = palette.onSurfaceVariant,
                    border = palette.outline,
                    borderVariant = palette.outline,
                )
            } else {
                lightColorScheme(
                    primary = palette.accent,
                    onPrimary = contrastText(palette.accent),
                    background = palette.background,
                    onBackground = palette.onBackground,
                    surface = palette.surface,
                    onSurface = palette.onBackground,
                    surfaceVariant = palette.surfaceVariant,
                    onSurfaceVariant = palette.onSurfaceVariant,
                    border = palette.outline,
                    borderVariant = palette.outline,
                )
            },
            content = content,
        )
    }
}

internal data class TvPalette(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val onBackground: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val accent: Color,
)

internal fun paletteFor(appearance: TvAppearance, dark: Boolean, accentOverride: Color? = null): TvPalette {
    val accent = accentOverride ?: when (appearance.colorTheme) {
        TvColorTheme.TypeType, TvColorTheme.Dynamic -> Color(0xFF8AB4F8)
        TvColorTheme.Nord -> Color(0xFF88C0D0)
        TvColorTheme.Cream -> Color(0xFFD97706)
        TvColorTheme.Forest -> Color(0xFF6EE7A0)
        TvColorTheme.Plum -> Color(0xFFD8B4FE)
    }
    if (appearance.isManga) {
        val amoled = appearance.amoled && dark
        val paperDark = appearance.mangaPaper != TvMangaPaper.Day
        val paletteDark = amoled || paperDark
        val background = when {
            amoled -> Color.Black
            appearance.mangaPaper == TvMangaPaper.Night -> Color(0xFF171717)
            appearance.mangaPaper == TvMangaPaper.Nord -> Color(0xFF2E3440)
            else -> Color(0xFFFFFDF5)
        }
        val surface = when {
            amoled -> Color.Black
            appearance.mangaPaper == TvMangaPaper.Night -> Color(0xFF292929)
            appearance.mangaPaper == TvMangaPaper.Nord -> Color(0xFF3B4252)
            else -> Color(0xFFFFFAE8)
        }
        val ink = if (paletteDark) Color(0xFFF5F3EA) else Color(0xFF171717)
        return TvPalette(paletteDark, background, surface, surface, ink, ink.copy(alpha = .72f), ink.copy(alpha = .52f), accent)
    }
    if (appearance.amoled && dark) return TvPalette(true, Color.Black, Color.Black, Color(0xFF171717), Color(0xFFF5F5F5), Color(0xFFB8BBC2), Color(0xFF454852), accent)
    val surfaces = when (appearance.colorTheme) {
        TvColorTheme.Nord -> if (dark) Color(0xFF2E3440) to Color(0xFF3B4252) else Color(0xFFECEFF4) to Color.White
        TvColorTheme.Cream -> if (dark) Color(0xFF261A12) to Color(0xFF3B2418) else Color(0xFFFFF7ED) to Color(0xFFFFFBEB)
        TvColorTheme.Forest -> if (dark) Color(0xFF0B1F14) to Color(0xFF102A1B) else Color(0xFFF0FDF4) to Color.White
        TvColorTheme.Plum -> if (dark) Color(0xFF1D1028) to Color(0xFF2A1738) else Color(0xFFFAF5FF) to Color.White
        TvColorTheme.TypeType, TvColorTheme.Dynamic -> if (dark) Color(0xFF101114) to Color(0xFF191B20) else Color(0xFFF7F7F8) to Color.White
    }
    val onBackground = if (dark) Color(0xFFF5F5F5) else Color(0xFF15161A)
    return TvPalette(dark, surfaces.first, surfaces.second, if (dark) Color(0xFF242730) else Color(0xFFE4E4E7), onBackground, onBackground.copy(alpha = .72f), onBackground.copy(alpha = .42f), accent)
}

private fun systemDynamicPalette(resources: android.content.res.Resources, dark: Boolean): TvPalette {
    val background = resources.getColor(if (dark) android.R.color.system_neutral1_900 else android.R.color.system_neutral1_50, null)
    val surface = resources.getColor(if (dark) android.R.color.system_neutral1_800 else android.R.color.system_neutral1_100, null)
    val variant = resources.getColor(if (dark) android.R.color.system_neutral2_800 else android.R.color.system_neutral2_100, null)
    val ink = resources.getColor(if (dark) android.R.color.system_neutral1_50 else android.R.color.system_neutral1_900, null)
    val accent = resources.getColor(if (dark) android.R.color.system_accent1_200 else android.R.color.system_accent1_600, null)
    return TvPalette(
        isDark = dark,
        background = Color(background),
        surface = Color(surface),
        surfaceVariant = Color(variant),
        onBackground = Color(ink),
        onSurfaceVariant = Color(ink).copy(alpha = .72f),
        outline = Color(ink).copy(alpha = .42f),
        accent = Color(accent),
    )
}

internal fun contrastText(background: Color): Color =
    if ((background.luminance() + .05f) / .05f >= 1.05f / (background.luminance() + .05f)) {
        Color.Black
    } else {
        Color.White
    }
