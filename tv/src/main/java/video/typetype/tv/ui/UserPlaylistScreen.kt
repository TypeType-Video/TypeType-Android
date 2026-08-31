package video.typetype.tv.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import video.typetype.sdk.core.UserPlaylist
import video.typetype.sdk.core.Video
import video.typetype.tv.data.TvPlaylistActions

@Composable
public fun UserPlaylistScreen(
    playlist: UserPlaylist,
    isActionInProgress: Boolean,
    errorMessage: String?,
    actions: TvPlaylistActions,
    onOpenVideo: (Video) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    var focusedItem by rememberSaveable(playlist.id) {
        mutableStateOf(playlist.videos.firstOrNull()?.let { videoFocusKey("Videos", it) })
    }
    var managing by rememberSaveable(playlist.id) { mutableStateOf(false) }
    var renaming by rememberSaveable(playlist.id) { mutableStateOf(false) }
    var confirmingDelete by rememberSaveable(playlist.id) { mutableStateOf(false) }
    val focusedVideo = playlist.videos.firstOrNull { videoFocusKey("Videos", it) == focusedItem }
    val focusedIndex = focusedVideo?.let(playlist.videos::indexOf) ?: -1
    CollectionBackdrop(playlist.videos.firstOrNull()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().restoreFocusWhen(true),
            contentPadding = PaddingValues(bottom = 64.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            item {
                CollectionHero(
                    eyebrow = "MY PLAYLIST",
                    title = playlist.name,
                    metadata = "${playlist.videoCount} videos",
                    description = playlist.description,
                    action = {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(onClick = { managing = !managing }, enabled = !isActionInProgress) {
                                Text(if (managing) "Done" else "Manage")
                            }
                            Button(onClick = { renaming = true }, enabled = !isActionInProgress) { Text("Rename") }
                            Button(onClick = { confirmingDelete = true }, enabled = !isActionInProgress) { Text("Delete") }
                        }
                    },
                )
            }
            if (managing && focusedVideo != null) item {
                Box(Modifier.padding(horizontal = 58.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { actions.moveVideo(playlist, focusedVideo, -1) },
                            enabled = focusedIndex > 0 && !isActionInProgress,
                        ) { Text("Move earlier") }
                        Button(
                            onClick = { actions.moveVideo(playlist, focusedVideo, 1) },
                            enabled = focusedIndex in 0 until playlist.videos.lastIndex && !isActionInProgress,
                        ) { Text("Move later") }
                        Button(
                            onClick = { actions.removeVideo(playlist, focusedVideo) },
                            enabled = !isActionInProgress,
                        ) { Text("Remove") }
                    }
                }
            }
            if (playlist.videos.isNotEmpty()) item {
                VideoRow(
                    "Videos",
                    playlist.videos,
                    onOpenVideo,
                    restoreFocusKey = focusedItem,
                    focusActive = true,
                    onFocused = { focusedItem = it },
                )
            } else item { EmptyScreen("This playlist is empty", "Add videos from their details page.") }
            errorMessage?.let { item { CollectionError(it) } }
        }
    }
    if (renaming) {
        TvTextPrompt(
            title = "Rename playlist",
            initialValue = playlist.name,
            actionLabel = "Rename",
            onDismiss = { renaming = false },
            onSubmit = { name ->
                actions.rename(playlist, name)
                renaming = false
            },
        )
    }
    if (confirmingDelete) {
        TvConfirmDialog(
            title = "Delete ${playlist.name}?",
            message = "This removes the playlist from your TypeType account.",
            onDismiss = { confirmingDelete = false },
            onConfirm = {
                actions.delete(playlist)
                confirmingDelete = false
            },
        )
    }
}
