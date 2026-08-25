package dev.typetype.android.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.typetype.android.domain.preferences.AccentColor
import dev.typetype.android.domain.preferences.AppearanceFont
import dev.typetype.android.domain.preferences.AppearanceMode
import dev.typetype.android.domain.preferences.AppearancePersonality
import dev.typetype.android.domain.preferences.AppPreferences

@Composable
fun TypeTypeTheme(
    preferences: AppPreferences,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (preferences.appearanceMode) {
        AppearanceMode.System -> systemDark
        AppearanceMode.Light -> false
        AppearanceMode.Dark -> true
    }
    val (accent, accentSoft) = accentColors(preferences.accentColor)
    val context = LocalContext.current
    val dynamic = preferences.accentColor == AccentColor.System &&
        preferences.appearancePersonality == AppearancePersonality.Classic &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val colorScheme = when {
        preferences.appearancePersonality == AppearancePersonality.Manga ->
            mangaScheme(preferences.mangaPaper, accent, accentSoft)
        dynamic && dark -> dynamicDarkColorScheme(context)
        dynamic -> dynamicLightColorScheme(context)
        dark -> classicDarkScheme(accent, accentSoft, preferences.appearanceAmoled)
        else -> classicLightScheme(accent, accentSoft)
    }
    val appearance = preferences.toAppearance()
    CompositionLocalProvider(LocalTypeTypeAppearance provides appearance) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = shapesFor(appearance),
            typography = typographyFor(preferences.appearanceFont, appearance.isManga),
            content = content,
        )
    }
}

private fun AppPreferences.toAppearance() = TypeTypeAppearance(
    personality = appearancePersonality,
    motion = appearanceMotion,
    headlineMarker = mangaHeadlineMarker,
    screentone = mangaScreentone,
    speedLines = mangaSpeedLines,
    starburst = mangaStarburst,
    inkedIcons = mangaInkedIcons,
    panelTilt = mangaPanelTilt,
)

private fun shapesFor(appearance: TypeTypeAppearance): Shapes = if (appearance.isManga) {
    Shapes(
        extraSmall = RoundedCornerShape(0.dp),
        small = RoundedCornerShape(0.dp),
        medium = RoundedCornerShape(2.dp),
        large = RoundedCornerShape(2.dp),
        extraLarge = RoundedCornerShape(2.dp),
    )
} else {
    Shapes()
}

private fun typographyFor(font: AppearanceFont, manga: Boolean): Typography {
    val default = Typography()
    val displayFamily = when {
        manga -> FontFamily.Monospace
        font == AppearanceFont.Expressive -> FontFamily.Serif
        else -> FontFamily.Default
    }
    val titleWeight = if (manga) FontWeight.Black else FontWeight.SemiBold
    fun title(style: TextStyle) = style.copy(fontFamily = displayFamily, fontWeight = titleWeight)
    return default.copy(
        displayLarge = title(default.displayLarge),
        displayMedium = title(default.displayMedium),
        displaySmall = title(default.displaySmall),
        headlineLarge = title(default.headlineLarge),
        headlineMedium = title(default.headlineMedium),
        headlineSmall = title(default.headlineSmall),
        titleLarge = title(default.titleLarge),
        titleMedium = title(default.titleMedium),
        titleSmall = title(default.titleSmall),
    )
}
