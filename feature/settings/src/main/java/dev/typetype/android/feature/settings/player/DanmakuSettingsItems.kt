package dev.typetype.android.feature.settings.player

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.res.stringResource
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.DropdownRow
import dev.typetype.android.core.ui.components.SettingsSectionHeader
import dev.typetype.android.core.ui.components.SwitchRow

private val DANMAKU_VALUES = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

internal fun LazyListScope.danmakuSettingsItems(
    state: PlayerSettingsState,
    onAction: (PlayerSettingsAction) -> Unit,
) {
    item { SettingsSectionHeader(stringResource(R.string.settings_player_section_danmaku)) }
    item {
        SwitchRow(
            title = stringResource(R.string.settings_player_danmaku_enabled),
            subtitle = stringResource(R.string.settings_player_danmaku_enabled_subtitle),
            checked = state.danmakuEnabled,
            onCheckedChange = { onAction(PlayerSettingsAction.SetDanmakuEnabled(it)) },
        )
    }
    item {
        DropdownRow(
            title = stringResource(R.string.settings_player_danmaku_speed),
            subtitle = null,
            options = DANMAKU_VALUES.map { it.toString() to "${it}x" },
            selectedKey = state.danmakuSpeed.toString(),
            onSelect = { onAction(PlayerSettingsAction.SetDanmakuSpeed(it.toFloat())) },
            enabled = state.danmakuEnabled,
        )
    }
    item {
        DropdownRow(
            title = stringResource(R.string.settings_player_danmaku_size),
            subtitle = null,
            options = DANMAKU_VALUES.map { it.toString() to "${(it * 100).toInt()}%" },
            selectedKey = state.danmakuSize.toString(),
            onSelect = { onAction(PlayerSettingsAction.SetDanmakuSize(it.toFloat())) },
            enabled = state.danmakuEnabled,
        )
    }
}
