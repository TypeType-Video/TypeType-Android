package video.typetype.tv.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import video.typetype.sdk.core.Video
import video.typetype.sdk.core.SavedPlaylist
import video.typetype.sdk.core.UserPlaylist
import video.typetype.tv.data.TvAppState
import video.typetype.tv.data.TvPlaylistActions
import video.typetype.tv.data.TvSubscriptionGroupActions

@Composable
public fun LibraryScreen(
    state: TvAppState,
    isActive: Boolean,
    onOpenVideo: (Video) -> Unit,
    onOpenUserPlaylist: (UserPlaylist) -> Unit,
    onOpenSavedPlaylist: (SavedPlaylist) -> Unit,
    onClearHistory: () -> Unit,
    playlistActions: TvPlaylistActions?,
    subscriptionGroupActions: TvSubscriptionGroupActions?,
    initialFocus: FocusRequester,
    topNavigationFocus: FocusRequester,
) {
    var creatingPlaylist by remember { mutableStateOf(false) }
    var managingSubscriptions by remember { mutableStateOf(false) }
    var confirmingHistoryClear by remember { mutableStateOf(false) }
    if (state.isLoadingLibrary && state.history.isEmpty() && state.watchLater.isEmpty() && state.favorites.isEmpty()) {
        LoadingScreen()
        return
    }
    val isEmpty = state.history.isEmpty() && state.watchLater.isEmpty() && state.favorites.isEmpty() &&
        state.playlists.isEmpty() && state.savedPlaylists.isEmpty()
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().restoreFocusWhen(isActive),
            contentPadding = PaddingValues(top = 82.dp, bottom = 56.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            playlistActions?.let {
                item {
                    Row(
                        modifier = Modifier.padding(horizontal = 58.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        androidx.tv.material3.Button(
                            modifier = Modifier.focusRequester(initialFocus).focusProperties { up = topNavigationFocus },
                            onClick = { creatingPlaylist = true },
                        ) {
                            androidx.tv.material3.Text("New playlist")
                        }
                        if (subscriptionGroupActions != null) {
                            androidx.tv.material3.Button(onClick = { managingSubscriptions = true }) {
                                androidx.tv.material3.Text("Manage subscriptions")
                            }
                        }
                        if (state.history.isNotEmpty()) {
                            androidx.tv.material3.Button(onClick = { confirmingHistoryClear = true }) {
                                androidx.tv.material3.Text("Clear history")
                            }
                        }
                    }
                }
            }
            if (state.history.isNotEmpty()) item {
                VideoRow(
                    "History",
                    state.history.map { it.video },
                    onOpenVideo,
                    progressByVideoId = state.history.associate { it.video.id.value to it.progressMilliseconds },
                    cinematic = false,
                )
            }
            if (state.watchLater.isNotEmpty()) item {
                VideoRow("Watch later", state.watchLater.map { it.video }, onOpenVideo, cinematic = false)
            }
            if (state.favorites.isNotEmpty()) item {
                VideoRow("Favorites", state.favorites.map { it.video }, onOpenVideo, cinematic = false)
            }
            if (state.playlists.isNotEmpty()) item { UserPlaylistRow(state.playlists, onOpenUserPlaylist) }
            if (state.savedPlaylists.isNotEmpty()) item { SavedPlaylistRow(state.savedPlaylists, onOpenSavedPlaylist) }
        }
        if (isEmpty) {
            EmptyScreen("Your library is empty", "Saved content from the TypeType instance will appear here.")
        }
    }
    if (creatingPlaylist) {
        TvTextPrompt(
            title = "Create playlist",
            actionLabel = "Create",
            onDismiss = { creatingPlaylist = false },
            onSubmit = { name ->
                playlistActions?.create?.invoke(name)
                creatingPlaylist = false
            },
        )
    }
    if (managingSubscriptions && subscriptionGroupActions != null) {
        SubscriptionGroupManager(
            state = state,
            actions = subscriptionGroupActions,
            onDismiss = { managingSubscriptions = false },
        )
    }
    if (confirmingHistoryClear) {
        TvConfirmDialog(
            title = "Clear watch history?",
            message = "This removes the complete watch history from your TypeType account.",
            confirmLabel = "Clear history",
            onDismiss = { confirmingHistoryClear = false },
            onConfirm = {
                confirmingHistoryClear = false
                onClearHistory()
            },
        )
    }
}
