package dev.typetype.android.feature.settings.player

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.res.stringResource
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.DropdownRow
import dev.typetype.android.core.ui.components.SettingsSectionHeader
import dev.typetype.android.core.ui.components.SwitchRow

private data class LanguageOption(val code: String, val display: String)

private val LANGUAGE_OPTIONS = listOf(
    LanguageOption("", "Auto"),
    LanguageOption("en", "English"),
    LanguageOption("fr", "Français"),
    LanguageOption("es", "Español"),
    LanguageOption("de", "Deutsch"),
    LanguageOption("it", "Italiano"),
    LanguageOption("pt", "Português"),
    LanguageOption("ru", "Русский"),
    LanguageOption("ja", "日本語"),
    LanguageOption("ko", "한국어"),
    LanguageOption("zh", "中文"),
    LanguageOption("ar", "العربية"),
    LanguageOption("hi", "हिन्दी"),
    LanguageOption("nl", "Nederlands"),
    LanguageOption("pl", "Polski"),
    LanguageOption("sv", "Svenska"),
    LanguageOption("tr", "Türkçe"),
)

internal fun LazyListScope.subtitleAndLanguageSettingsItems(
    state: PlayerSettingsState,
    onAction: (PlayerSettingsAction) -> Unit,
) {
    item { SettingsSectionHeader(stringResource(R.string.settings_player_section_subtitles)) }
    item {
        SwitchRow(
            title = stringResource(R.string.settings_player_subtitles_enabled),
            subtitle = stringResource(R.string.settings_player_subtitles_enabled_subtitle),
            checked = state.subtitlesEnabled,
            onCheckedChange = { onAction(PlayerSettingsAction.SetSubtitlesEnabled(it)) },
        )
    }
    item {
        DropdownRow(
            title = stringResource(R.string.settings_player_subtitle_language),
            subtitle = stringResource(R.string.settings_player_subtitle_language_subtitle),
            options = LANGUAGE_OPTIONS.map { it.code to it.display },
            selectedKey = state.defaultSubtitleLanguage,
            onSelect = { onAction(PlayerSettingsAction.SetSubtitleLanguage(it)) },
            enabled = state.subtitlesEnabled,
        )
    }
    item {
        DropdownRow(
            title = stringResource(R.string.settings_player_audio_language),
            subtitle = if (state.preferOriginalLanguage) {
                stringResource(R.string.settings_player_prefer_original_active)
            } else {
                stringResource(R.string.settings_player_audio_language_subtitle)
            },
            options = LANGUAGE_OPTIONS.map { it.code to it.display },
            selectedKey = state.defaultAudioLanguage,
            onSelect = { onAction(PlayerSettingsAction.SetAudioLanguage(it)) },
            enabled = !state.preferOriginalLanguage,
        )
    }
    item {
        SwitchRow(
            title = stringResource(R.string.settings_player_prefer_original),
            subtitle = stringResource(R.string.settings_player_prefer_original_subtitle),
            checked = state.preferOriginalLanguage,
            onCheckedChange = { onAction(PlayerSettingsAction.SetPreferOriginalLanguage(it)) },
        )
    }
}
