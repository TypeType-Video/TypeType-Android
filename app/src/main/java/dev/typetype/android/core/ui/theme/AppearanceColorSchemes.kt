package dev.typetype.android.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import dev.typetype.android.domain.preferences.AccentColor
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

internal fun mangaScheme(paper: MangaPaper, accent: Color, accentSoft: Color): ColorScheme {
    val dark = paper != MangaPaper.Day
    val background = when (paper) {
        MangaPaper.Day -> Color(0xFFFFFDF5)
        MangaPaper.Night -> Color.Black
        MangaPaper.Nord -> Color(0xFF2E3440)
    }
    val surface = when (paper) {
        MangaPaper.Day -> Color(0xFFFFFAE8)
        MangaPaper.Night -> Color(0xFF121212)
        MangaPaper.Nord -> Color(0xFF3B4252)
    }
    val ink = if (dark) Color(0xFFF5F3EA) else Color(0xFF171717)
    val scheme = if (dark) darkColorScheme() else lightColorScheme()
    return scheme.copy(
        primary = accent,
        onPrimary = Color(0xFF111111),
        primaryContainer = accentSoft,
        onPrimaryContainer = Color(0xFF111111),
        secondary = accent,
        onSecondary = Color(0xFF111111),
        tertiary = accent,
        background = background,
        onBackground = ink,
        surface = surface,
        onSurface = ink,
        surfaceVariant = surface,
        onSurfaceVariant = ink.copy(alpha = 0.72f),
        surfaceContainerHigh = surface,
        surfaceContainerHighest = surface,
        outline = ink,
        outlineVariant = ink.copy(alpha = 0.42f),
        error = if (dark) Red300 else Red600,
        onError = if (dark) Color.Black else White,
    )
}
