package dev.typetype.android.feature.player.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import dev.typetype.android.feature.player.state.PLAYBACK_SPEEDS
import dev.typetype.android.feature.player.state.TrackOption
import dev.typetype.android.feature.player.state.currentSpeedOrDefault
import dev.typetype.android.feature.player.state.optionsForType

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
@Composable
fun PlaybackOptionsSheet(
    player: Player,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val tracks = player.currentTracks
    val videoOptions = remember(tracks) { tracks.optionsForType(C.TRACK_TYPE_VIDEO) }
    val audioOptions = remember(tracks) { tracks.optionsForType(C.TRACK_TYPE_AUDIO) }
    val textOptions = remember(tracks) { tracks.optionsForType(C.TRACK_TYPE_TEXT) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item { SectionTitle(text = "Playback speed") }
            items(PLAYBACK_SPEEDS) { speed ->
                OptionRow(
                    label = "${formatSpeed(speed)}x",
                    selected = player.currentSpeedOrDefault() == speed,
                    onSelect = {
                        player.playbackParameters = PlaybackParameters(speed)
                    },
                )
            }
            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            item { SectionTitle(text = "Quality") }
            items(videoOptions, key = { it.label + it.trackIndex }) { option ->
                OptionRow(
                    label = option.label,
                    selected = isOverrideSelected(player, option),
                    onSelect = { applyOverride(player, option) },
                )
            }
            if (videoOptions.isEmpty()) item { EmptyHint(text = "Auto") }
            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            item { SectionTitle(text = "Audio") }
            items(audioOptions, key = { it.label + it.trackIndex }) { option ->
                OptionRow(
                    label = option.label,
                    selected = isOverrideSelected(player, option),
                    onSelect = { applyOverride(player, option) },
                )
            }
            if (audioOptions.isEmpty()) item { EmptyHint(text = "Default") }
            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            item { SectionTitle(text = "Subtitles") }
            item {
                OptionRow(
                    label = "Off",
                    selected = player.trackSelectionParameters.disabledTrackTypes
                        .contains(C.TRACK_TYPE_TEXT),
                    onSelect = { disableType(player, C.TRACK_TYPE_TEXT) },
                )
            }
            items(textOptions, key = { it.label + it.trackIndex }) { option ->
                OptionRow(
                    label = option.label,
                    selected = isOverrideSelected(player, option),
                    onSelect = { applyOverride(player, option) },
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 8.dp),
    )
}

@Composable
private fun OptionRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun EmptyHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp),
    )
}

@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
private fun applyOverride(player: Player, option: TrackOption) {
    val override = TrackSelectionOverride(option.mediaTrackGroup, listOf(option.trackIndex))
    val params = player.trackSelectionParameters.buildUpon()
        .setOverrideForType(override)
        .setTrackTypeDisabled(option.groupType, false)
        .build()
    player.trackSelectionParameters = params
}

@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
private fun disableType(player: Player, type: Int) {
    val params = player.trackSelectionParameters.buildUpon()
        .clearOverridesOfType(type)
        .setTrackTypeDisabled(type, true)
        .build()
    player.trackSelectionParameters = params
}

@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
private fun isOverrideSelected(player: Player, option: TrackOption): Boolean {
    val override = player.trackSelectionParameters.overrides[option.mediaTrackGroup] ?: return false
    return override.trackIndices.contains(option.trackIndex)
}

private fun formatSpeed(value: Float): String =
    if (value % 1f == 0f) value.toInt().toString() else value.toString()
