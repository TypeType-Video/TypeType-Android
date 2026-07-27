package dev.typetype.android.feature.player.queue

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.typetype.android.R
import dev.typetype.android.domain.playback.PlaybackQueueState
import dev.typetype.android.domain.playback.PlaybackRepeatMode

@Composable
internal fun PlaybackQueueSheetHeader(
    state: PlaybackQueueState,
    onShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text(
            text = state.title.ifBlank { stringResource(R.string.playback_queue_title) },
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        )
        Text(
            text = pluralStringResource(
                R.plurals.playback_queue_count,
                state.entries.size,
                state.entries.size,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = false,
                onClick = onShuffle,
                enabled = state.entries.size - state.currentIndex - 1 > 1,
                label = { Text(stringResource(R.string.playback_queue_shuffle_remaining)) },
                leadingIcon = { Icon(Icons.Filled.Shuffle, contentDescription = null) },
            )
            FilterChip(
                selected = state.repeatMode != PlaybackRepeatMode.Off,
                onClick = onCycleRepeat,
                label = { Text(stringResource(state.repeatMode.labelResource())) },
                leadingIcon = {
                    Icon(
                        imageVector = if (state.repeatMode == PlaybackRepeatMode.One) {
                            Icons.Filled.RepeatOne
                        } else {
                            Icons.Filled.Repeat
                        },
                        contentDescription = null,
                    )
                },
            )
        }
    }
}

private fun PlaybackRepeatMode.labelResource(): Int = when (this) {
    PlaybackRepeatMode.Off -> R.string.playback_queue_repeat_off
    PlaybackRepeatMode.All -> R.string.playback_queue_repeat_all
    PlaybackRepeatMode.One -> R.string.playback_queue_repeat_one
}
