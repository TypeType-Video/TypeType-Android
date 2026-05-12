package dev.typetype.android.feature.player

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WatchLater
import androidx.compose.material.icons.outlined.WatchLater
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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
    onDownload: () -> Unit,
    downloadInFlight: Boolean = false,
) {
    val context = LocalContext.current
    val shareChooserTitle = stringResource(R.string.video_menu_share_chooser)
    val serverBaseUrl = LocalServerBaseUrl.current
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        IconButton(onClick = onToggleFavorite) {
            Icon(
                imageVector = if (isFavorited) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = stringResource(
                    if (isFavorited) R.string.player_remove_from_favorites
                    else R.string.player_add_to_favorites,
                ),
                tint = if (isFavorited) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
            )
        }
        IconButton(onClick = onToggleWatchLater) {
            Icon(
                imageVector = if (isInWatchLater) Icons.Filled.WatchLater else Icons.Outlined.WatchLater,
                contentDescription = stringResource(
                    if (isInWatchLater) R.string.player_remove_from_watch_later
                    else R.string.player_add_to_watch_later,
                ),
                tint = if (isInWatchLater) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
            )
        }
        IconButton(onClick = onAddToPlaylist) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                contentDescription = stringResource(R.string.player_add_to_playlist),
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
        IconButton(
            onClick = onDownload,
            enabled = !downloadInFlight,
        ) {
            Icon(
                imageVector = Icons.Filled.Download,
                contentDescription = stringResource(R.string.player_download),
                tint = if (downloadInFlight) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onBackground
                },
            )
        }
        IconButton(onClick = {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, buildShareUrl(serverBaseUrl, shareUrl))
            }
            context.startActivity(Intent.createChooser(intent, shareChooserTitle))
        }) {
            Icon(
                imageVector = Icons.Filled.Share,
                contentDescription = stringResource(R.string.video_menu_share),
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}
