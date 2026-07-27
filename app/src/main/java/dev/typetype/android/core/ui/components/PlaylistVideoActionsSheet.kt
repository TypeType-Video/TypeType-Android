package dev.typetype.android.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.typetype.android.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistVideoActionsSheet(
    removeLabel: String,
    isWatched: Boolean,
    canMoveUp: Boolean = false,
    canMoveDown: Boolean = false,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onRemoveFromList: () -> Unit,
    onToggleWatched: () -> Unit,
    onShare: () -> Unit,
    onBlockVideo: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            ActionRow(
                icon = Icons.Filled.SkipNext,
                label = stringResource(R.string.video_menu_play_next),
                onClick = { onPlayNext(); onDismiss() },
            )
            ActionRow(
                icon = Icons.AutoMirrored.Filled.QueueMusic,
                label = stringResource(R.string.video_menu_add_to_queue),
                onClick = { onAddToQueue(); onDismiss() },
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            if (onMoveUp != null && onMoveDown != null) {
                ActionRow(
                    icon = Icons.Filled.KeyboardArrowUp,
                    label = stringResource(R.string.playlist_action_move_up),
                    onClick = { onMoveUp(); onDismiss() },
                    enabled = canMoveUp,
                )
                ActionRow(
                    icon = Icons.Filled.KeyboardArrowDown,
                    label = stringResource(R.string.playlist_action_move_down),
                    onClick = { onMoveDown(); onDismiss() },
                    enabled = canMoveDown,
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            }
            ActionRow(
                icon = Icons.Filled.RemoveCircleOutline,
                label = removeLabel,
                onClick = { onRemoveFromList(); onDismiss() },
                emphasized = true,
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            ActionRow(
                icon = if (isWatched) Icons.Filled.CheckCircle else Icons.Filled.CheckCircleOutline,
                label = stringResource(
                    if (isWatched) R.string.playlist_action_unmark_watched
                    else R.string.playlist_action_mark_watched,
                ),
                onClick = { onToggleWatched(); onDismiss() },
            )
            ActionRow(
                icon = Icons.Filled.Share,
                label = stringResource(R.string.playlist_action_share),
                onClick = { onShare(); onDismiss() },
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            ActionRow(
                icon = Icons.Filled.Block,
                label = stringResource(R.string.playlist_action_block_video),
                onClick = { onBlockVideo(); onDismiss() },
            )
        }
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    emphasized: Boolean = false,
    enabled: Boolean = true,
) {
    val tint = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        emphasized -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = tint,
        )
    }
}
