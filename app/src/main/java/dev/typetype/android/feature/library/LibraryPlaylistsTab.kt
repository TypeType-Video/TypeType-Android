package dev.typetype.android.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.typetype.android.R
import dev.typetype.android.domain.library.Playlist

@Composable
fun PlaylistsTab(
    playlists: List<Playlist>,
    filter: String,
    isMutationInFlight: Boolean,
    onOpenPlaylist: (playlistId: String) -> Unit,
    onCreatePlaylist: (name: String) -> Unit,
    onRenamePlaylist: (playlistId: String, name: String) -> Unit,
    onDeletePlaylist: (playlistId: String) -> Unit,
) {
    var dialog by remember { mutableStateOf<PlaylistDialog?>(null) }
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            FilledTonalButton(
                onClick = { dialog = PlaylistDialog.Create },
                enabled = !isMutationInFlight,
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                Text(
                    text = stringResource(R.string.library_playlist_create),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
        if (playlists.isEmpty()) {
            Box(modifier = Modifier.weight(1f)) {
                EmptyTab(emptyMessageFor(filter, stringResource(R.string.library_empty_playlists)))
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 280.dp),
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(playlists, key = { it.id }) { playlist ->
                    PlaylistListCard(
                        playlist = playlist,
                        actionsEnabled = !isMutationInFlight,
                        onClick = { onOpenPlaylist(playlist.id) },
                        onRename = { dialog = PlaylistDialog.Rename(playlist) },
                        onDelete = { dialog = PlaylistDialog.Delete(playlist) },
                    )
                }
            }
        }
    }

    when (val pending = dialog) {
        PlaylistDialog.Create -> PlaylistNameDialog(
            title = stringResource(R.string.playlist_picker_new_title),
            initialName = "",
            confirmLabel = stringResource(R.string.library_playlist_create_action),
            onDismiss = { dialog = null },
            onConfirm = {
                dialog = null
                onCreatePlaylist(it)
            },
        )
        is PlaylistDialog.Rename -> PlaylistNameDialog(
            title = stringResource(R.string.library_playlist_rename_title),
            initialName = pending.playlist.name,
            confirmLabel = stringResource(R.string.action_save),
            onDismiss = { dialog = null },
            onConfirm = {
                dialog = null
                onRenamePlaylist(pending.playlist.id, it)
            },
        )
        is PlaylistDialog.Delete -> PlaylistDeleteDialog(
            playlistName = pending.playlist.name,
            onDismiss = { dialog = null },
            onConfirm = {
                dialog = null
                onDeletePlaylist(pending.playlist.id)
            },
        )
        null -> Unit
    }
}

@Composable
private fun PlaylistListCard(
    playlist: Playlist,
    actionsEnabled: Boolean,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val countLabel = pluralStringResource(
        R.plurals.playlist_card_video_count,
        playlist.videoCount,
        playlist.videoCount,
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            playlist.videos.firstOrNull()?.thumbnailUrl?.let { cover ->
                AsyncImage(
                    model = cover,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            PlaylistActionsMenu(
                expanded = menuExpanded,
                enabled = actionsEnabled,
                onExpand = { menuExpanded = true },
                onDismiss = { menuExpanded = false },
                onRename = onRename,
                onDelete = onDelete,
                modifier = Modifier.align(Alignment.TopEnd),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.85f))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(
                    text = countLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = playlist.name,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = countLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PlaylistActionsMenu(
    expanded: Boolean,
    enabled: Boolean,
    onExpand: () -> Unit,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.padding(4.dp)) {
        IconButton(
            onClick = onExpand,
            enabled = enabled,
            modifier = Modifier
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.85f)),
        ) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = stringResource(R.string.library_playlist_more_actions),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.library_playlist_rename)) },
                onClick = {
                    onDismiss()
                    onRename()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.library_playlist_delete)) },
                onClick = {
                    onDismiss()
                    onDelete()
                },
            )
        }
    }
}

private sealed interface PlaylistDialog {
    data object Create : PlaylistDialog
    data class Rename(val playlist: Playlist) : PlaylistDialog
    data class Delete(val playlist: Playlist) : PlaylistDialog
}
