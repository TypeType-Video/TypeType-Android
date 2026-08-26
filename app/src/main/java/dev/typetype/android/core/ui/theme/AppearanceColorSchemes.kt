package dev.typetype.android.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import dev.typetype.android.domain.preferences.AccentColor
import dev.typetype.android.domain.preferences.AppearanceTheme
import dev.typetype.android.domain.preferences.MangaPaper

internal fun accentColors(accentColor: AccentColor): Pair<Color, Color> = when (accentColor) {
    AccentColor.Red -> AccentRed to Red300
    AccentColor.Blue -> AccentBlue to Blue300
    AccentColor.Yellow -> AccentYellow to Color(0xFFFDE68A)
    AccentColor.Green -> AccentGreen to Color(0xFF86EFAC)
    AccentColor.Purple -> AccentPurple to Color(0xFFE9D5FF)
    AccentColor.Violet -> AccentViolet to Color(0xFFC4B5FD)
    AccentColor.Monochrome -> AccentMonochrome to Zinc300
    AccentColor.System -> AccentBlue to Blue300
}

internal fun classicDarkScheme(accent: Color, accentSoft: Color, amoled: Boolean): ColorScheme {
    val background = if (amoled) Color.Black else Zinc950
    val surface = if (amoled) Color.Black else Zinc900
    return darkColorScheme(
        primary = accent,
        onPrimary = Zinc950,
        primaryContainer = accent,
        onPrimaryContainer = Zinc950,
        secondary = accent,
        secondaryContainer = Zinc800,
        tertiary = accentSoft,
        onTertiary = Zinc950,
        background = background,
        onBackground = Zinc100,
        surface = surface,
        onSurface = Zinc100,
        surfaceVariant = Zinc800,
        onSurfaceVariant = Zinc400,
        surfaceContainerHigh = Zinc800,
        surfaceContainerHighest = Zinc700,
        outline = Zinc700,
        outlineVariant = Zinc800,
        error = Red400,
        onError = White,
    )
}

internal fun classicLightScheme(accent: Color, accentSoft: Color): ColorScheme = lightColorScheme(
    primary = accent,
    onPrimary = Zinc950,
    primaryContainer = accentSoft,
    onPrimaryContainer = Zinc950,
    secondary = accent,
    tertiary = accent,
    background = Color(0xFFF7F7F8),
    onBackground = Zinc950,
    surface = White,
    onSurface = Zinc950,
    surfaceVariant = Color(0xFFE4E4E7),
    onSurfaceVariant = Zinc700,
    surfaceContainerHigh = Color(0xFFE4E4E7),
    surfaceContainerHighest = Zinc300,
    outline = Zinc500,
    outlineVariant = Zinc300,
    error = Red600,
    onError = White,
)

internal fun themedDarkScheme(
    theme: AppearanceTheme,
    accent: Color,
    accentSoft: Color,
    amoled: Boolean,
): ColorScheme {
    val base = classicDarkScheme(accent, accentSoft, amoled)
    if (theme == AppearanceTheme.TypeType || theme == AppearanceTheme.Dynamic || amoled) return base
    val colors = themeSurfaceColors(theme)
    return base.copy(
        background = colors.darkBackground,
        surface = colors.darkSurface,
        surfaceVariant = colors.darkVariant,
        surfaceContainerHigh = colors.darkVariant,
        surfaceContainerHighest = colors.darkOutline,
        outline = colors.darkOutline,
        outlineVariant = colors.darkVariant,
    )
}

internal fun themedLightScheme(
    theme: AppearanceTheme,
    accent: Color,
    accentSoft: Color,
): ColorScheme {
    val base = classicLightScheme(accent, accentSoft)
    if (theme == AppearanceTheme.TypeType || theme == AppearanceTheme.Dynamic) return base
    val colors = themeSurfaceColors(theme)
    return base.copy(
        background = colors.lightBackground,
        surface = colors.lightSurface,
        surfaceVariant = colors.lightVariant,
        surfaceContainerHigh = colors.lightVariant,
        surfaceContainerHighest = colors.lightOutline,
        outline = colors.lightOutline,
        outlineVariant = colors.lightOutline,
    )
}

private data class ThemeSurfaceColors(
    val lightBackground: Color,
    val lightSurface: Color,
    val lightVariant: Color,
    val lightOutline: Color,
    val darkBackground: Color,
    val darkSurface: Color,
    val darkVariant: Color,
    val darkOutline: Color,
)

private fun themeSurfaceColors(theme: AppearanceTheme): ThemeSurfaceColors = when (theme) {
    AppearanceTheme.Nord -> ThemeSurfaceColors(
        lightBackground = Color(0xFFECEFF4),
        lightSurface = Color(0xFFFFFFFF),
        lightVariant = Color(0xFFD8DEE9),
        lightOutline = Color(0xFF8A96A8),
        darkBackground = Color(0xFF2E3440),
        darkSurface = Color(0xFF3B4252),
        darkVariant = Color(0xFF434C5E),
        darkOutline = Color(0xFF616E82),
    )
    AppearanceTheme.Cream -> ThemeSurfaceColors(
        lightBackground = Color(0xFFFFF7ED),
        lightSurface = Color(0xFFFFFBEB),
        lightVariant = Color(0xFFFED7AA),
        lightOutline = Color(0xFFB45309),
        darkBackground = Color(0xFF261A12),
        darkSurface = Color(0xFF3B2418),
        darkVariant = Color(0xFF51301D),
        darkOutline = Color(0xFF9A6A4A),
    )
    AppearanceTheme.Forest -> ThemeSurfaceColors(
        lightBackground = Color(0xFFF0FDF4),
        lightSurface = Color(0xFFFFFFFF),
        lightVariant = Color(0xFFDCFCE7),
        lightOutline = Color(0xFF4D7C5B),
        darkBackground = Color(0xFF0B1F14),
        darkSurface = Color(0xFF102A1B),
        darkVariant = Color(0xFF173A25),
        darkOutline = Color(0xFF5D9270),
    )
    AppearanceTheme.Plum -> ThemeSurfaceColors(
        lightBackground = Color(0xFFFAF5FF),
        lightSurface = Color(0xFFFFFFFF),
        lightVariant = Color(0xFFF3E8FF),
        lightOutline = Color(0xFF8B5BA8),
        darkBackground = Color(0xFF1D1028),
        darkSurface = Color(0xFF2A1738),
        darkVariant = Color(0xFF3B2050),
        darkOutline = Color(0xFF9567AD),
    )
    AppearanceTheme.TypeType, AppearanceTheme.Dynamic -> error("Theme has no static surfaces")
}

internal fun mangaScheme(
    paper: MangaPaper,
    theme: AppearanceTheme,
    accent: Color,
    accentSoft: Color,
    amoled: Boolean,
    isDark: Boolean,
): ColorScheme {
    val dark = isDark || amoled
    val background = if (amoled) {
        Color.Black
    } else {
        when (paper) {
            MangaPaper.Day -> Color(0xFFFFFDF5)
            MangaPaper.Night -> Color.Black
            MangaPaper.Nord -> Color(0xFF2E3440)
        }
    }
    val surface = if (amoled) {
        Color.Black
    } else {
        when (paper) {
            MangaPaper.Day -> Color(0xFFFFFAE8)
            MangaPaper.Night -> Color(0xFF121212)
            MangaPaper.Nord -> Color(0xFF3B4252)
        }
    }
    val ink = if (dark) Color(0xFFF5F3EA) else Color(0xFF171717)
    val base = if (dark) {
        themedDarkScheme(theme, accent, accentSoft, amoled = false)
    } else {
        themedLightScheme(theme, accent, accentSoft)
    }
    val usesPaperSurfaces = theme == AppearanceTheme.TypeType || theme == AppearanceTheme.Dynamic
    val activeBackground = if (amoled) Color.Black
    else if (usesPaperSurfaces) background else base.background
    val activeSurface = if (amoled) Color.Black
    else if (usesPaperSurfaces) surface else base.surface
    val activeVariant = if (usesPaperSurfaces) surface else base.surfaceVariant
    return base.copy(
        primary = accent,
        onPrimary = Color(0xFF111111),
        primaryContainer = accentSoft,
        onPrimaryContainer = Color(0xFF111111),
        secondary = accent,
        onSecondary = Color(0xFF111111),
        tertiary = accent,
        background = activeBackground,
        onBackground = ink,
        surface = activeSurface,
        onSurface = ink,
        surfaceVariant = activeVariant,
        onSurfaceVariant = ink.copy(alpha = 0.72f),
        surfaceContainerHigh = activeVariant,
        surfaceContainerHighest = activeVariant,
        surfaceContainerLow = activeSurface,
        surfaceContainerLowest = activeBackground,
        surfaceDim = activeBackground,
        surfaceBright = activeSurface,
        outline = ink,
        outlineVariant = ink.copy(alpha = 0.42f),
        error = if (dark) Red300 else Red600,
        onError = if (dark) Color.Black else White,
    )
}
