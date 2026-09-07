package dev.typetype.android.feature.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WatchLater
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.WatchLater
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.typetype.android.R
import dev.typetype.android.core.ui.share.LocalServerBaseUrl
import dev.typetype.android.core.ui.share.ShareChooserSheet

@Composable
@OptIn(ExperimentalLayoutApi::class)
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
    audioOnlyEnabled: Boolean = false,
    audioOnlyAvailable: Boolean = false,
    audioOnlyChanging: Boolean = false,
    onToggleAudioOnly: () -> Unit = {},
) {
    val serverBaseUrl = LocalServerBaseUrl.current
    var shareSheetOpen by remember { mutableStateOf(false) }
    val actions: @Composable (Boolean) -> Unit = { expanded ->
        PlayerActionButton(
            expanded = expanded,
            icon = if (isFavorited) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            contentDescription = stringResource(
                if (isFavorited) R.string.player_remove_from_favorites
                else R.string.player_add_to_favorites,
            ),
            selected = isFavorited,
            onClick = onToggleFavorite,
        )
        PlayerActionButton(
            expanded = expanded,
            icon = if (isInWatchLater) Icons.Filled.WatchLater else Icons.Outlined.WatchLater,
            contentDescription = stringResource(
                if (isInWatchLater) R.string.player_remove_from_watch_later
                else R.string.player_add_to_watch_later,
            ),
            selected = isInWatchLater,
            onClick = onToggleWatchLater,
        )
        PlayerActionButton(
            expanded = expanded,
            icon = Icons.AutoMirrored.Filled.PlaylistAdd,
            contentDescription = stringResource(R.string.player_add_to_playlist),
            onClick = onAddToPlaylist,
        )
        onShowComments?.let {
            PlayerActionButton(
                expanded = expanded,
                icon = Icons.Outlined.ChatBubbleOutline,
                contentDescription = stringResource(R.string.comments_title),
                onClick = it,
            )
        }
        if (audioOnlyAvailable) {
            PlayerActionButton(
                expanded = expanded,
                icon = Icons.Filled.GraphicEq,
                contentDescription = stringResource(R.string.player_audio_only),
                selected = audioOnlyEnabled,
                enabled = !audioOnlyChanging,
                onClick = onToggleAudioOnly,
            )
        }
        PlayerActionButton(
            expanded = expanded,
            icon = Icons.Filled.Download,
            contentDescription = stringResource(R.string.player_download),
            enabled = !downloadInFlight,
            onClick = onDownload,
        )
        PlayerActionButton(
            expanded = expanded,
            icon = Icons.Filled.Share,
            contentDescription = stringResource(R.string.video_menu_share),
            onClick = { shareSheetOpen = true },
        )
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth >= 600.dp) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) { actions(true) }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) { actions(false) }
        }
    }
    if (shareSheetOpen) {
        ShareChooserSheet(
            serverBaseUrl = serverBaseUrl,
            videoUrl = shareUrl,
            onDismiss = { shareSheetOpen = false },
        )
    }
}

@Composable
private fun PlayerActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    expanded: Boolean,
    selected: Boolean = false,
    enabled: Boolean = true,
) {
    Column(
        modifier = if (expanded) Modifier.width(100.dp) else Modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
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
            IconButton(
                onClick = onClick,
                enabled = enabled,
                modifier = Modifier.size(if (expanded) 72.dp else 48.dp),
            ) {
                Icon(
                    imageVector = icon,
                    modifier = Modifier.size(if (expanded) 36.dp else 24.dp),
                    contentDescription = contentDescription,
                    tint = when {
                        selected -> MaterialTheme.colorScheme.primary
                        enabled -> MaterialTheme.colorScheme.onSurface
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
        if (expanded) {
            Text(
                text = contentDescription,
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}
