package dev.typetype.android.feature.shorts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WatchLater
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.WatchLater
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.VideoCardMenu
import dev.typetype.android.core.ui.components.VideoMenuAction
import dev.typetype.android.core.ui.components.VideoMenuItemState
import dev.typetype.android.domain.feed.Video

@Composable
internal fun ShortsActionRail(
    video: Video,
    state: VideoMenuItemState,
    onOpenPlayer: () -> Unit,
    onAction: (VideoMenuAction) -> Unit,
    onShowComments: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    var menuVisible by remember(video.id) { mutableStateOf(false) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ShortsActionButton(
            icon = Icons.Filled.Fullscreen,
            contentDescription = stringResource(R.string.shorts_open_player, video.title),
            onClick = onOpenPlayer,
        )
        ShortsActionButton(
            icon = if (state.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            contentDescription = stringResource(
                if (state.isFavorite) R.string.player_remove_from_favorites
                else R.string.player_add_to_favorites,
            ),
            selected = state.isFavorite,
            onClick = { onAction(VideoMenuAction.ToggleFavorite) },
        )
        ShortsActionButton(
            icon = if (state.isInWatchLater) Icons.Filled.WatchLater else Icons.Outlined.WatchLater,
            contentDescription = stringResource(
                if (state.isInWatchLater) R.string.player_remove_from_watch_later
                else R.string.player_add_to_watch_later,
            ),
            selected = state.isInWatchLater,
            onClick = { onAction(VideoMenuAction.ToggleWatchLater) },
        )
        onShowComments?.let {
            ShortsActionButton(
                icon = Icons.Outlined.ChatBubbleOutline,
                contentDescription = stringResource(R.string.comments_title),
                onClick = it,
            )
        }
        ShortsActionButton(
            icon = Icons.Filled.Share,
            contentDescription = stringResource(R.string.video_menu_share),
            onClick = { onAction(VideoMenuAction.Share) },
        )
        ShortsActionButton(
            icon = Icons.Filled.MoreVert,
            contentDescription = stringResource(R.string.video_more_actions),
            onClick = { menuVisible = true },
        )
    }
    if (menuVisible) {
        VideoCardMenu(
            onAction = onAction,
            onDismiss = { menuVisible = false },
            state = state,
        )
    }
}

@Composable
private fun ShortsActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    selected: Boolean = false,
) {
    Surface(
        shape = CircleShape,
        color = if (selected) Color.White else Color.Black.copy(alpha = 0.64f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (selected) Color.Black else Color.White,
            )
        }
    }
}
