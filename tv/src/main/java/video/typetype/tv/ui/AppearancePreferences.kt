package video.typetype.tv.ui

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.focus.FocusRequester
import video.typetype.tv.ui.theme.TvAppearance
import video.typetype.tv.ui.theme.TvColorMode
import video.typetype.tv.ui.theme.TvColorTheme
import video.typetype.tv.ui.theme.TvHeadlineMarker
import video.typetype.tv.ui.theme.TvMangaPaper
import video.typetype.tv.ui.theme.TvMotion
import video.typetype.tv.ui.theme.TvPersonality

internal fun LazyListScope.appearancePreferenceItems(
    appearance: TvAppearance,
    initialFocus: FocusRequester,
    rightExit: FocusRequester,
    onChange: (TvAppearance) -> Unit,
) {
    item { SettingsSectionTitle("Interface personality", "Choose the visual rhythm that feels like yours.") }
    item {
        OptionRow(
            TvPersonality.entries.map { it.name },
            appearance.personality.name,
            rightExit,
            initialFocus,
        ) { value ->
            onChange(appearance.copy(personality = TvPersonality.valueOf(value)))
        }
    }
    item { SettingsSectionTitle("Color") }
    item {
        OptionRow(TvColorTheme.entries.map { it.name }, appearance.colorTheme.name, rightExit) { value ->
            onChange(appearance.copy(colorTheme = TvColorTheme.valueOf(value)))
        }
    }
    item {
        OptionRow(TvColorMode.entries.map { it.name }, appearance.colorMode.name, rightExit) { value ->
            onChange(appearance.copy(colorMode = TvColorMode.valueOf(value)))
        }
    }
    item {
        ToggleRow("AMOLED black", "Use pure black surfaces on compatible displays.", appearance.amoled, rightExit) {
            onChange(appearance.copy(amoled = it))
        }
    }
    if (appearance.isManga) {
        item { SettingsSectionTitle("Manga details", "Fine-tune paper, ink and motion without losing readability.") }
        item {
            OptionRow(TvMangaPaper.entries.map { it.name }, appearance.mangaPaper.name, rightExit) { value ->
                onChange(appearance.copy(mangaPaper = TvMangaPaper.valueOf(value)))
            }
        }
        item {
            OptionRow(TvHeadlineMarker.entries.map { it.name }, appearance.headlineMarker.name, rightExit) { value ->
                onChange(appearance.copy(headlineMarker = TvHeadlineMarker.valueOf(value)))
            }
        }
        item { ToggleRow("Screentone", "Add a subtle printed texture.", appearance.screentone, rightExit) { onChange(appearance.copy(screentone = it)) } }
        item { ToggleRow("Speed lines", "Bring more energy to focused surfaces.", appearance.speedLines, rightExit) { onChange(appearance.copy(speedLines = it)) } }
        item { ToggleRow("Starbursts", "Highlight key actions with manga accents.", appearance.starburst, rightExit) { onChange(appearance.copy(starburst = it)) } }
        item { ToggleRow("Inked icons", "Use stronger graphic icon treatment.", appearance.inkedIcons, rightExit) { onChange(appearance.copy(inkedIcons = it)) } }
        item { ToggleRow("Tilted panels", "Give cards a hand-drawn composition.", appearance.panelTilt, rightExit) { onChange(appearance.copy(panelTilt = it)) } }
    }
    item { SettingsSectionTitle("Motion") }
    item {
        OptionRow(TvMotion.entries.map { it.name }, appearance.motion.name, rightExit) { value ->
            onChange(appearance.copy(motion = TvMotion.valueOf(value)))
        }
    }
}
