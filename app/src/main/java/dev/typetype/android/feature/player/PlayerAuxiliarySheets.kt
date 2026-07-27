package dev.typetype.android.feature.player

import androidx.compose.runtime.Composable
import androidx.media3.session.MediaController
import androidx.paging.PagingData
import dev.typetype.android.domain.comments.Comment
import dev.typetype.android.domain.comments.CommentsRepository
import dev.typetype.android.domain.library.Playlist
import dev.typetype.android.feature.download.DownloadSelectionSheet
import dev.typetype.android.feature.player.components.CommentsSheet
import dev.typetype.android.feature.player.components.PlaylistPickerSheet
import kotlinx.coroutines.flow.Flow

@Composable
internal fun PlayerAuxiliarySheets(
    commentsVisible: Boolean,
    onDismissComments: () -> Unit,
    commentsFlow: Flow<PagingData<Comment>>,
    commentsRepository: CommentsRepository?,
    videoUrl: String,
    controller: MediaController?,
    playlistPickerVisible: Boolean,
    playlists: List<Playlist>,
    playlistActionInFlight: Boolean,
    downloadPickerVisible: Boolean,
    downloadInFlight: Boolean,
    onDismissDownload: () -> Unit,
    onAction: (PlayerAction) -> Unit,
) {
    if (commentsVisible && commentsRepository != null) {
        CommentsSheet(
            pagingFlow = commentsFlow,
            videoUrl = videoUrl,
            commentsRepository = commentsRepository,
            onDismiss = onDismissComments,
            onTimestampClick = {
                controller?.seekTo(it)
                onDismissComments()
            },
        )
    }
    if (playlistPickerVisible) {
        PlaylistPickerSheet(
            playlists = playlists,
            isInFlight = playlistActionInFlight,
            onAddToPlaylist = { onAction(PlayerAction.OnAddToPlaylist(it)) },
            onCreatePlaylist = { onAction(PlayerAction.OnCreatePlaylistAndAdd(it)) },
            onDismiss = { onAction(PlayerAction.OnDismissPlaylistPicker) },
        )
    }
    if (downloadPickerVisible) {
        DownloadSelectionSheet(
            isInFlight = downloadInFlight,
            onSelect = {
                onAction(PlayerAction.OnDownload(it))
                onDismissDownload()
            },
            onDismiss = onDismissDownload,
        )
    }
}
