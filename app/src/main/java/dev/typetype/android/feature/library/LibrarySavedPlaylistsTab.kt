package dev.typetype.android.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkRemove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.typetype.android.R
import dev.typetype.android.domain.publicplaylist.SavedPublicPlaylist
import dev.typetype.android.domain.search.SearchPlaylist
import dev.typetype.android.feature.search.SearchPlaylistCard

@Composable
fun SavedPlaylistsTab(
    playlists: List<SavedPublicPlaylist>,
    filter: String,
    canSave: Boolean,
    isMutationInFlight: Boolean,
    onOpenPlaylist: (String) -> Unit,
    onRemovePlaylist: (String) -> Unit,
) {
    var pendingRemoval by remember { mutableStateOf<SavedPublicPlaylist?>(null) }
    if (playlists.isEmpty()) {
        EmptyTab(
            emptyMessageFor(
                filter,
                stringResource(
                    if (canSave) R.string.library_empty_saved_playlists
                    else R.string.library_saved_playlists_sign_in,
                ),
            ),
        )
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 280.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        ) {
            items(playlists, key = { it.id }) { playlist ->
                Box(modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
                    SearchPlaylistCard(
                        playlist = playlist.toSearchPlaylist(),
                        onClick = { onOpenPlaylist(playlist.url) },
                    )
                    IconButton(
                        onClick = { pendingRemoval = playlist },
                        enabled = !isMutationInFlight,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .background(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                CircleShape,
                            ),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.BookmarkRemove,
                            contentDescription = stringResource(
                                R.string.library_remove_saved_playlist_accessibility,
                                playlist.title,
                            ),
                        )
                    }
                }
            }
        }
    }

    pendingRemoval?.let { playlist ->
        AlertDialog(
            onDismissRequest = { pendingRemoval = null },
            title = { Text(stringResource(R.string.library_remove_saved_playlist_title)) },
            text = { Text(stringResource(R.string.library_remove_saved_playlist_message, playlist.title)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingRemoval = null
                        onRemovePlaylist(playlist.id)
                    },
                ) {
                    Text(stringResource(R.string.library_remove_saved_playlist_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemoval = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

private fun SavedPublicPlaylist.toSearchPlaylist() = SearchPlaylist(
    id = publicPlaylistId,
    title = title,
    url = url,
    thumbnailUrl = thumbnailUrl,
    uploaderName = uploaderName,
    streamCount = streamCount,
    playlistType = playlistType,
)
