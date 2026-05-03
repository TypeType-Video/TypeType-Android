package dev.typetype.android.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val TypeTypeDarkColors = darkColorScheme(
    primary = Blue400,
    onPrimary = Zinc950,
    primaryContainer = Zinc900,
    onPrimaryContainer = Zinc100,
    secondary = Blue500,
    onSecondary = Zinc950,
    background = Zinc950,
    onBackground = Zinc100,
    surface = Zinc900,
    onSurface = Zinc100,
    surfaceVariant = Zinc800,
    onSurfaceVariant = Zinc300,
    outline = Zinc800,
    outlineVariant = Zinc700,
    error = Red500,
    onError = Zinc100,
)

@Composable
fun TypeTypeTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(colorScheme = TypeTypeDarkColors, content = content)
}
