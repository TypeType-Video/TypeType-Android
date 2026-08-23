package dev.typetype.android.feature.settings.player

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.DropdownRow
import dev.typetype.android.core.ui.components.SettingsSectionHeader
import dev.typetype.android.core.ui.components.SwitchRow

private val SEEK_SECONDS = listOf(5, 10, 15, 20, 30)

internal fun LazyListScope.playerGestureSettingsItems(
    state: PlayerSettingsState,
    onAction: (PlayerSettingsAction) -> Unit,
) {
    item { Spacer(Modifier.size(4.dp)) }
    item { SettingsSectionHeader(stringResource(R.string.settings_player_section_gestures)) }
    item {
        SwitchRow(
            title = stringResource(R.string.settings_player_double_tap_seek),
            subtitle = stringResource(R.string.settings_player_double_tap_seek_subtitle),
            checked = state.doubleTapSeekEnabled,
            onCheckedChange = { onAction(PlayerSettingsAction.SetDoubleTapSeek(it)) },
        )
    }
    item {
        DropdownRow(
            title = stringResource(R.string.settings_player_double_tap_seek_time),
            subtitle = null,
            options = SEEK_SECONDS.map { seconds ->
                seconds.toString() to pluralStringResource(
                    R.plurals.settings_player_autoplay_seconds,
                    seconds,
                    seconds,
                )
            },
            selectedKey = state.doubleTapSeekSeconds.toString(),
            onSelect = { onAction(PlayerSettingsAction.SetDoubleTapSeekSeconds(it.toInt())) },
            enabled = state.doubleTapSeekEnabled,
        )
    }
    item {
        SwitchRow(
            title = stringResource(R.string.settings_player_swipe_seek),
            subtitle = stringResource(R.string.settings_player_swipe_seek_subtitle),
            checked = state.swipeSeekEnabled,
            onCheckedChange = { onAction(PlayerSettingsAction.SetSwipeSeek(it)) },
        )
    }
    item {
        SwitchRow(
            title = stringResource(R.string.settings_player_swipe_brightness_volume),
            subtitle = stringResource(R.string.settings_player_swipe_brightness_volume_subtitle),
            checked = state.swipeBrightnessVolumeEnabled,
            onCheckedChange = { onAction(PlayerSettingsAction.SetSwipeBrightnessVolume(it)) },
        )
    }
    item {
        SwitchRow(
            title = stringResource(R.string.settings_player_long_press_speed),
            subtitle = stringResource(R.string.settings_player_long_press_speed_subtitle),
            checked = state.longPressSpeedEnabled,
            onCheckedChange = { onAction(PlayerSettingsAction.SetLongPressSpeed(it)) },
        )
    }
}
