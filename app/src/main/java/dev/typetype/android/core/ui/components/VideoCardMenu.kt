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
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.WatchLater
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.typetype.android.R

sealed interface VideoMenuAction {
    data object PlayNext : VideoMenuAction
    data object AddToQueue : VideoMenuAction
    data object ToggleFavorite : VideoMenuAction
    data object ToggleWatchLater : VideoMenuAction
    data object AddToPlaylist : VideoMenuAction
    data object ToggleWatched : VideoMenuAction
    data object Download : VideoMenuAction
    data object Share : VideoMenuAction
    data object OpenChannel : VideoMenuAction
    data object BlockVideo : VideoMenuAction
    data object BlockChannel : VideoMenuAction
}

@Immutable
data class VideoMenuItemState(
    val isFavorite: Boolean = false,
    val isInWatchLater: Boolean = false,
    val isWatched: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoCardMenu(
    onAction: (VideoMenuAction) -> Unit,
    onDismiss: () -> Unit,
    state: VideoMenuItemState = VideoMenuItemState(),
    showOpenChannel: Boolean = true,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            VideoMenuItem(
                icon = Icons.Filled.SkipNext,
                label = stringResource(R.string.video_menu_play_next),
                onClick = { onAction(VideoMenuAction.PlayNext); onDismiss() },
            )
            VideoMenuItem(
                icon = Icons.AutoMirrored.Filled.QueueMusic,
                label = stringResource(R.string.video_menu_add_to_queue),
                onClick = { onAction(VideoMenuAction.AddToQueue); onDismiss() },
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            VideoMenuItem(
                icon = if (state.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                label = stringResource(
                    if (state.isFavorite) R.string.video_menu_remove_from_favorites
                    else R.string.video_menu_add_to_favorites,
                ),
                onClick = { onAction(VideoMenuAction.ToggleFavorite); onDismiss() },
            )
            VideoMenuItem(
                icon = Icons.Filled.WatchLater,
                label = stringResource(
                    if (state.isInWatchLater) R.string.video_menu_remove_from_watch_later
                    else R.string.video_menu_add_to_watch_later,
                ),
                onClick = { onAction(VideoMenuAction.ToggleWatchLater); onDismiss() },
            )
            VideoMenuItem(
                icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                label = stringResource(R.string.video_menu_add_to_playlist),
                onClick = { onAction(VideoMenuAction.AddToPlaylist); onDismiss() },
            )
            VideoMenuItem(
                icon = if (state.isWatched) Icons.Filled.CheckCircle else Icons.Filled.CheckCircleOutline,
                label = stringResource(
                    if (state.isWatched) R.string.video_menu_unmark_as_watched
                    else R.string.video_menu_mark_as_watched,
                ),
                onClick = { onAction(VideoMenuAction.ToggleWatched); onDismiss() },
            )
            VideoMenuItem(
                icon = Icons.Filled.Download,
                label = stringResource(R.string.video_menu_download),
                onClick = { onAction(VideoMenuAction.Download); onDismiss() },
            )
            VideoMenuItem(
                icon = Icons.Filled.Share,
                label = stringResource(R.string.video_menu_share),
                onClick = { onAction(VideoMenuAction.Share); onDismiss() },
            )
            if (showOpenChannel) {
                VideoMenuItem(
                    icon = Icons.Filled.Person,
                    label = stringResource(R.string.video_menu_open_channel),
                    onClick = { onAction(VideoMenuAction.OpenChannel); onDismiss() },
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            VideoMenuItem(
                icon = Icons.Filled.Block,
                label = stringResource(R.string.video_menu_block_video),
                onClick = { onAction(VideoMenuAction.BlockVideo); onDismiss() },
            )
            if (showOpenChannel) {
                VideoMenuItem(
                    icon = Icons.Filled.PersonOff,
                    label = stringResource(R.string.video_menu_block_channel),
                    onClick = { onAction(VideoMenuAction.BlockChannel); onDismiss() },
                )
            }
        }
    }
}

@Composable
private fun VideoMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
