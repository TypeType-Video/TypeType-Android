package dev.typetype.android.feature.settings.player

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.DropdownRow
import dev.typetype.android.core.ui.components.SettingsSectionHeader
import dev.typetype.android.domain.usersettings.CaptionStyles
import dev.typetype.android.domain.usersettings.resolvedDisplayBackground
import dev.typetype.android.domain.usersettings.resolvedDisplayBackgroundOpacity
import dev.typetype.android.domain.usersettings.resolvedFontFamily
import dev.typetype.android.domain.usersettings.resolvedFontSize
import dev.typetype.android.domain.usersettings.resolvedTextBackground
import dev.typetype.android.domain.usersettings.resolvedTextBackgroundOpacity
import dev.typetype.android.domain.usersettings.resolvedTextColor
import dev.typetype.android.domain.usersettings.resolvedTextOpacity
import dev.typetype.android.domain.usersettings.resolvedTextShadow

internal fun LazyListScope.captionStyleSettingsItems(
    state: PlayerSettingsState,
    onAction: (PlayerSettingsAction) -> Unit,
) {
    item { Spacer(Modifier.size(4.dp)) }
    item {
        SettingsSectionHeader(stringResource(R.string.settings_player_caption_appearance))
    }
    captionDropdown(
        titleRes = R.string.settings_player_caption_font,
        options = FONT_OPTIONS,
        selected = state.captionStyles.resolvedFontFamily(),
        state = state,
        onAction = onAction,
    ) { styles, value -> styles.copy(fontFamily = value.canonical(CaptionStyles.DEFAULT_FONT_FAMILY)) }
    captionDropdown(
        titleRes = R.string.settings_player_caption_size,
        options = SIZE_OPTIONS,
        selected = state.captionStyles.resolvedFontSize(),
        state = state,
        onAction = onAction,
    ) { styles, value -> styles.copy(fontSize = value.canonical(CaptionStyles.DEFAULT_FONT_SIZE)) }
    captionDropdown(
        titleRes = R.string.settings_player_caption_text_color,
        options = COLOR_OPTIONS,
        selected = state.captionStyles.resolvedTextColor(),
        state = state,
        onAction = onAction,
    ) { styles, value -> styles.copy(textColor = value.canonical(CaptionStyles.DEFAULT_TEXT_COLOR)) }
    captionDropdown(
        titleRes = R.string.settings_player_caption_text_opacity,
        options = OPACITY_OPTIONS,
        selected = state.captionStyles.resolvedTextOpacity(),
        state = state,
        onAction = onAction,
    ) { styles, value -> styles.copy(textOpacity = value.canonical(CaptionStyles.DEFAULT_TEXT_OPACITY)) }
    captionDropdown(
        titleRes = R.string.settings_player_caption_edge,
        options = EDGE_OPTIONS,
        selected = state.captionStyles.resolvedTextShadow(),
        state = state,
        onAction = onAction,
    ) { styles, value -> styles.copy(textShadow = value.canonical(CaptionStyles.DEFAULT_TEXT_SHADOW)) }
    captionDropdown(
        titleRes = R.string.settings_player_caption_background_color,
        options = COLOR_OPTIONS,
        selected = state.captionStyles.resolvedTextBackground(),
        state = state,
        onAction = onAction,
    ) { styles, value ->
        styles.copy(textBackground = value.canonical(CaptionStyles.DEFAULT_TEXT_BACKGROUND))
    }
    captionDropdown(
        titleRes = R.string.settings_player_caption_background_opacity,
        options = OPACITY_OPTIONS,
        selected = state.captionStyles.resolvedTextBackgroundOpacity(),
        state = state,
        onAction = onAction,
    ) { styles, value ->
        styles.copy(
            textBackgroundOpacity = value.canonical(
                CaptionStyles.DEFAULT_TEXT_BACKGROUND_OPACITY,
            ),
        )
    }
    captionDropdown(
        titleRes = R.string.settings_player_caption_window_color,
        options = COLOR_OPTIONS,
        selected = state.captionStyles.resolvedDisplayBackground(),
        state = state,
        onAction = onAction,
    ) { styles, value ->
        styles.copy(displayBackground = value.canonical(CaptionStyles.DEFAULT_DISPLAY_BACKGROUND))
    }
    captionDropdown(
        titleRes = R.string.settings_player_caption_window_opacity,
        options = OPACITY_OPTIONS,
        selected = state.captionStyles.resolvedDisplayBackgroundOpacity(),
        state = state,
        onAction = onAction,
    ) { styles, value ->
        styles.copy(
            displayBackgroundOpacity = value.canonical(
                CaptionStyles.DEFAULT_DISPLAY_BACKGROUND_OPACITY,
            ),
        )
    }
}

private fun LazyListScope.captionDropdown(
    @StringRes titleRes: Int,
    options: List<CaptionOption>,
    selected: String,
    state: PlayerSettingsState,
    onAction: (PlayerSettingsAction) -> Unit,
    update: (CaptionStyles, String) -> CaptionStyles,
) {
    item {
        DropdownRow(
            title = stringResource(titleRes),
            subtitle = null,
            options = localized(options),
            selectedKey = selected,
            onSelect = { value ->
                onAction(PlayerSettingsAction.SetCaptionStyles(update(state.captionStyles, value)))
            },
        )
    }
}

@Composable
private fun localized(options: List<CaptionOption>): List<Pair<String, String>> =
    options.map { option ->
        option.value to if (option.labelRes == R.string.settings_player_caption_percent) {
            stringResource(option.labelRes, option.value)
        } else {
            stringResource(option.labelRes)
        }
    }

private fun String.canonical(default: String): String = takeUnless { it == default }.orEmpty()

private data class CaptionOption(val value: String, @StringRes val labelRes: Int)

private val FONT_OPTIONS = listOf(
    CaptionOption("pro-sans", R.string.settings_player_caption_font_sans),
    CaptionOption("mono-serif", R.string.settings_player_caption_font_mono_serif),
    CaptionOption("mono-sans", R.string.settings_player_caption_font_mono_sans),
    CaptionOption("casual", R.string.settings_player_caption_font_casual),
    CaptionOption("cursive", R.string.settings_player_caption_font_cursive),
    CaptionOption("capitals", R.string.settings_player_caption_font_capitals),
    CaptionOption("serif", R.string.settings_player_caption_font_serif),
)

private val SIZE_OPTIONS = listOf(50, 75, 100, 125, 150, 175, 200).map {
    CaptionOption("$it%", R.string.settings_player_caption_percent)
}

private val OPACITY_OPTIONS = listOf(0, 25, 50, 75, 100).map {
    CaptionOption("$it%", R.string.settings_player_caption_percent)
}

private val COLOR_OPTIONS = listOf(
    CaptionOption("#ffffff", R.string.settings_player_caption_color_white),
    CaptionOption("#ffff00", R.string.settings_player_caption_color_yellow),
    CaptionOption("#00ff00", R.string.settings_player_caption_color_green),
    CaptionOption("#00ffff", R.string.settings_player_caption_color_cyan),
    CaptionOption("#0000ff", R.string.settings_player_caption_color_blue),
    CaptionOption("#ff00ff", R.string.settings_player_caption_color_magenta),
    CaptionOption("#ff0000", R.string.settings_player_caption_color_red),
    CaptionOption("#000000", R.string.settings_player_caption_color_black),
)

private val EDGE_OPTIONS = listOf(
    CaptionOption("none", R.string.settings_player_caption_edge_none),
    CaptionOption("drop shadow", R.string.settings_player_caption_edge_shadow),
    CaptionOption("raised", R.string.settings_player_caption_edge_raised),
    CaptionOption("depressed", R.string.settings_player_caption_edge_depressed),
    CaptionOption("outline", R.string.settings_player_caption_edge_outline),
)
