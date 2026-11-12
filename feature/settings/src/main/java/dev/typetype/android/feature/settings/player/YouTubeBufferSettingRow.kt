package dev.typetype.android.feature.settings.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.DropdownRow
import dev.typetype.android.domain.preferences.YOUTUBE_BUFFER_SECONDS

@Composable
internal fun YouTubeBufferSettingRow(seconds: Int, onSelect: (Int) -> Unit) {
    DropdownRow(
        title = stringResource(R.string.settings_player_youtube_buffer),
        subtitle = stringResource(R.string.settings_player_youtube_buffer_subtitle),
        options = YOUTUBE_BUFFER_SECONDS.map {
            it.toString() to pluralStringResource(R.plurals.settings_player_buffer_seconds, it, it)
        },
        selectedKey = seconds.toString(),
        onSelect = { onSelect(it.toInt()) },
    )
}
