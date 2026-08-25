package dev.typetype.android.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import dev.typetype.android.domain.preferences.AppearanceMotion
import dev.typetype.android.domain.preferences.AppearancePersonality
import dev.typetype.android.domain.preferences.MangaHeadlineMarker

@Immutable
data class TypeTypeAppearance(
    val personality: AppearancePersonality,
    val motion: AppearanceMotion,
    val headlineMarker: MangaHeadlineMarker,
    val screentone: Boolean,
    val speedLines: Boolean,
    val starburst: Boolean,
    val inkedIcons: Boolean,
    val panelTilt: Boolean,
) {
    val isManga: Boolean get() = personality == AppearancePersonality.Manga

    val transitionMillis: Int
        get() = when (motion) {
            AppearanceMotion.Full -> 360
            AppearanceMotion.Subtle -> 180
            AppearanceMotion.Off -> 0
        }
}

val LocalTypeTypeAppearance = staticCompositionLocalOf {
    TypeTypeAppearance(
        personality = AppearancePersonality.Classic,
        motion = AppearanceMotion.Subtle,
        headlineMarker = MangaHeadlineMarker.None,
        screentone = false,
        speedLines = false,
        starburst = false,
        inkedIcons = false,
        panelTilt = false,
    )
}
