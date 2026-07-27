package dev.typetype.android.feature.player.queue

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import dev.typetype.android.R
import dev.typetype.android.domain.playback.PlaybackQueueEntry
import dev.typetype.android.domain.playback.PlaybackQueueState

@Composable
internal fun PlaybackQueueControls(
    state: PlaybackQueueState,
    viewModel: PlaybackQueueViewModel = hiltViewModel(),
) {
    var sheetVisible by remember { mutableStateOf(false) }
    if (!state.isActive) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { sheetVisible = true }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                text = state.title.ifBlank { stringResource(R.string.playback_queue_title) },
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(
                    R.string.playback_queue_position,
                    state.currentIndex + 1,
                    state.entries.size,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (state.isPreparingNext) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.playback_queue_open),
            )
        }
    }
    if (sheetVisible) {
        PlaybackQueueSheet(
            state = state,
            onPlay = {
                sheetVisible = false
                viewModel.play(it)
            },
            onRetry = viewModel::retryNext,
            onPlayNext = viewModel::playNext,
            onRemove = viewModel::remove,
            onShuffle = viewModel::shuffleUpcoming,
            onCycleRepeat = viewModel::cycleRepeatMode,
            onDismiss = { sheetVisible = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaybackQueueSheet(
    state: PlaybackQueueState,
    onPlay: (Int) -> Unit,
    onRetry: () -> Unit,
    onPlayNext: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            PlaybackQueueSheetHeader(
                state = state,
                onShuffle = onShuffle,
                onCycleRepeat = onCycleRepeat,
            )
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                itemsIndexed(
                    items = state.entries,
                    key = { _, entry -> entry.videoUrl },
                ) { index, entry ->
                    PlaybackQueueRow(
                        entry = entry,
                        isCurrent = index == state.currentIndex,
                        canPlayNext = index != state.currentIndex + 1,
                        onClick = { onPlay(index) },
                        onPlayNext = { onPlayNext(index) },
                        onRemove = { onRemove(index) },
                    )
                }
            }
            if (state.failedVideoUrl != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.playback_queue_prepare_failed),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onRetry) {
                        Text(stringResource(R.string.state_retry))
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaybackQueueRow(
    entry: PlaybackQueueEntry,
    isCurrent: Boolean,
    canPlayNext: Boolean,
    onClick: () -> Unit,
    onPlayNext: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                else MaterialTheme.colorScheme.surface,
            )
            .clickable(enabled = !isCurrent, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(96.dp)
                .height(54.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            AsyncImage(
                model = entry.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
            if (isCurrent) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = stringResource(R.string.playback_queue_now_playing),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(4.dp),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (entry.channelName.isNotBlank()) {
                Text(
                    text = entry.channelName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (!isCurrent) {
            PlaybackQueueEntryMenu(
                canPlayNext = canPlayNext,
                onPlayNext = onPlayNext,
                onRemove = onRemove,
            )
        }
    }
}

@Composable
private fun PlaybackQueueEntryMenu(
    canPlayNext: Boolean,
    onPlayNext: () -> Unit,
    onRemove: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = stringResource(R.string.playback_queue_entry_options),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (canPlayNext) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.playback_queue_play_next)) },
                    leadingIcon = { Icon(Icons.Filled.SkipNext, contentDescription = null) },
                    onClick = {
                        expanded = false
                        onPlayNext()
                    },
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.playback_queue_remove)) },
                leadingIcon = { Icon(Icons.Filled.DeleteOutline, contentDescription = null) },
                onClick = {
                    expanded = false
                    onRemove()
                },
            )
        }
    }
}
