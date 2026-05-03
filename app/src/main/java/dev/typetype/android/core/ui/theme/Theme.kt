package dev.typetype.android.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val TypeTypeDarkColors = darkColorScheme(
    primary = Zinc100,
    onPrimary = Zinc950,
    primaryContainer = Zinc100,
    onPrimaryContainer = Zinc950,
    secondary = Blue400,
    onSecondary = White,
    secondaryContainer = Blue400,
    onSecondaryContainer = White,
    tertiary = Blue300,
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

@Composable
fun TypeTypeTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(colorScheme = TypeTypeDarkColors, content = content)
}
