package dev.typetype.android.feature.player

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WatchLater
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.WatchLater
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.typetype.android.R
import dev.typetype.android.core.ui.share.LocalServerBaseUrl
import dev.typetype.android.core.ui.share.buildShareUrl

@Composable
fun PlayerInteractionRow(
    isFavorited: Boolean,
    isInWatchLater: Boolean,
    shareUrl: String,
    onToggleFavorite: () -> Unit,
    onToggleWatchLater: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onShowComments: (() -> Unit)?,
    onDownload: () -> Unit,
    downloadInFlight: Boolean = false,
) {
    val context = LocalContext.current
    val shareChooserTitle = stringResource(R.string.video_menu_share_chooser)
    val serverBaseUrl = LocalServerBaseUrl.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        PlayerActionButton(
            icon = if (isFavorited) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            contentDescription = stringResource(
                if (isFavorited) R.string.player_remove_from_favorites
                else R.string.player_add_to_favorites,
            ),
            selected = isFavorited,
            onClick = onToggleFavorite,
        )
        PlayerActionButton(
            icon = if (isInWatchLater) Icons.Filled.WatchLater else Icons.Outlined.WatchLater,
            contentDescription = stringResource(
                if (isInWatchLater) R.string.player_remove_from_watch_later
                else R.string.player_add_to_watch_later,
            ),
            selected = isInWatchLater,
            onClick = onToggleWatchLater,
        )
        PlayerActionButton(
            icon = Icons.AutoMirrored.Filled.PlaylistAdd,
            contentDescription = stringResource(R.string.player_add_to_playlist),
            onClick = onAddToPlaylist,
        )
        onShowComments?.let {
            PlayerActionButton(
                icon = Icons.Outlined.ChatBubbleOutline,
                contentDescription = stringResource(R.string.comments_title),
                onClick = it,
            )
        }
        PlayerActionButton(
            icon = Icons.Filled.Download,
            contentDescription = stringResource(R.string.player_download),
            enabled = !downloadInFlight,
            onClick = onDownload,
        )
        PlayerActionButton(
            icon = Icons.Filled.Share,
            contentDescription = stringResource(R.string.video_menu_share),
            onClick = {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, buildShareUrl(serverBaseUrl, shareUrl))
                }
                context.startActivity(Intent.createChooser(intent, shareChooserTitle))
            },
        )
    }
}

@Composable
private fun PlayerActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    selected: Boolean = false,
    enabled: Boolean = true,
) {
    Surface(
        shape = CircleShape,
        color = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        IconButton(onClick = onClick, enabled = enabled) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = when {
                    selected -> MaterialTheme.colorScheme.primary
                    enabled -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}
