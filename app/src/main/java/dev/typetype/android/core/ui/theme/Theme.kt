package dev.typetype.android.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import dev.typetype.android.domain.preferences.AccentColor

private fun darkSchemeFor(accent: Color, accentSoft: Color) = darkColorScheme(
    primary = accent,
    onPrimary = Zinc950,
    primaryContainer = accent,
    onPrimaryContainer = Zinc950,
    secondary = accent,
    onSecondary = White,
    secondaryContainer = Zinc800,
    onSecondaryContainer = Zinc100,
    tertiary = accentSoft,
    onTertiary = Zinc950,
    background = Zinc950,
    onBackground = Zinc100,
    surface = Zinc900,
    onSurface = Zinc100,
    surfaceVariant = Zinc800,
    onSurfaceVariant = Zinc400,
    surfaceContainerHigh = Zinc800,
    surfaceContainerHighest = Zinc700,
    outline = Zinc800,
    outlineVariant = Zinc700,
    error = Red400,
    onError = White,
    errorContainer = Red500,
    onErrorContainer = White,
)

private fun colorsFor(accentColor: AccentColor): Pair<Color, Color> = when (accentColor) {
    AccentColor.Red -> AccentRed to Red300
    AccentColor.Blue -> AccentBlue to Blue300
    AccentColor.Yellow -> AccentYellow to Color(0xFFFDE68A)
    AccentColor.Green -> AccentGreen to Color(0xFF86EFAC)
    AccentColor.Purple -> AccentPurple to Color(0xFFE9D5FF)
    AccentColor.Violet -> AccentViolet to Color(0xFFC4B5FD)
    AccentColor.Monochrome -> AccentMonochrome to Zinc300
    AccentColor.System -> AccentBlue to Blue300
}

@Composable
fun TypeTypeTheme(
    accentColor: AccentColor = AccentColor.Blue,
    content: @Composable () -> Unit,
) {
    val (accent, accentSoft) = colorsFor(accentColor)
    MaterialTheme(colorScheme = darkSchemeFor(accent, accentSoft), content = content)
}
