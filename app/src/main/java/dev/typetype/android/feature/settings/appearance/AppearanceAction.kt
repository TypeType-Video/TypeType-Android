package dev.typetype.android.feature.settings.appearance

import dev.typetype.android.domain.preferences.AccentColor
import dev.typetype.android.domain.preferences.AppearanceFont
import dev.typetype.android.domain.preferences.AppearanceMode
import dev.typetype.android.domain.preferences.AppearanceMotion
import dev.typetype.android.domain.preferences.AppearancePersonality
import dev.typetype.android.domain.preferences.AppearanceTheme
import dev.typetype.android.domain.preferences.MangaHeadlineMarker
import dev.typetype.android.domain.preferences.MangaPaper

sealed interface AppearanceAction {
    data class SelectAccent(val accent: AccentColor) : AppearanceAction
    data class SelectPersonality(val personality: AppearancePersonality) : AppearanceAction
    data class SelectMode(val mode: AppearanceMode) : AppearanceAction
    data class SetAmoled(val enabled: Boolean) : AppearanceAction
    data class SelectTheme(val theme: AppearanceTheme) : AppearanceAction
    data class SelectFont(val font: AppearanceFont) : AppearanceAction
    data class SelectMotion(val motion: AppearanceMotion) : AppearanceAction
    data class SelectMangaPaper(val paper: MangaPaper) : AppearanceAction
    data class SelectHeadlineMarker(val marker: MangaHeadlineMarker) : AppearanceAction
    data class SetMangaDecoration(
        val decoration: MangaDecoration,
        val enabled: Boolean,
    ) : AppearanceAction
}

enum class MangaDecoration {
    Screentone,
    SpeedLines,
    Starburst,
    InkedIcons,
    PanelTilt,
}
