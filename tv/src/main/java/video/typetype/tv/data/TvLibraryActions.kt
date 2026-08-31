package video.typetype.tv.data

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import video.typetype.sdk.core.Channel
import video.typetype.sdk.core.PublicPlaylist
import video.typetype.sdk.core.TypeTypeResult
import video.typetype.sdk.core.Video
import video.typetype.sdk.core.UserPlaylist
import video.typetype.sdk.core.WatchLaterItem

public fun TvViewModel.toggleFavorite(video: Video) {
    if (!beginAuthenticatedAction()) return
    val remove = mutableState.value.favorites.any { it.video.url == video.url }
    viewModelScope.launch {
        val mutation = if (remove) {
            client.library.removeFavorite(video.url)
        } else {
            client.library.addFavorite(video.url)
        }
        when (mutation) {
            is TypeTypeResult.Success -> refreshFavorites()
            is TypeTypeResult.Failure -> finishAction(mutation.error.toUserMessage())
        }
    }
}

public fun TvViewModel.toggleWatchLater(video: Video) {
    if (!beginAuthenticatedAction()) return
    val remove = mutableState.value.watchLater.any { it.video.url == video.url }
    viewModelScope.launch {
        val mutation = if (remove) {
            client.library.removeWatchLater(video.url)
        } else {
            client.library.addWatchLater(WatchLaterItem(video, System.currentTimeMillis()))
        }
        when (mutation) {
            is TypeTypeResult.Success -> refreshWatchLater()
            is TypeTypeResult.Failure -> finishAction(mutation.error.toUserMessage())
        }
    }
}

public fun TvViewModel.toggleSubscription(channel: Channel) {
    if (!beginAuthenticatedAction()) return
    val remove = mutableState.value.subscriptions.any { it.channelUrl == channel.url }
    viewModelScope.launch {
        val mutation = if (remove) {
            client.subscriptions.unsubscribe(channel.url)
        } else {
            client.subscriptions.subscribe(channel.url, channel.name, channel.avatarUrl)
        }
        when (mutation) {
            is TypeTypeResult.Success -> refreshSubscriptions()
            is TypeTypeResult.Failure -> finishAction(mutation.error.toUserMessage())
        }
    }
}

public fun TvViewModel.toggleSavedPlaylist(playlist: PublicPlaylist) {
    if (!beginAuthenticatedAction()) return
    val saved = mutableState.value.savedPlaylists.firstOrNull {
        it.publicPlaylistId == playlist.playlist.id || it.url == playlist.playlist.url
    }
    viewModelScope.launch {
        val mutation = if (saved == null) {
            client.library.savePlaylist(playlist.playlist.url)
        } else {
            client.library.deleteSavedPlaylist(saved.id)
        }
        when (mutation) {
            is TypeTypeResult.Success -> refreshSavedPlaylists()
            is TypeTypeResult.Failure -> finishAction(mutation.error.toUserMessage())
        }
    }
}

public fun TvViewModel.clearHistory() {
    if (!beginAuthenticatedAction()) return
    viewModelScope.launch {
        when (val result = client.library.clearHistory()) {
            is TypeTypeResult.Success -> mutableState.value = mutableState.value.copy(
                history = emptyList(),
                isActionInProgress = false,
                errorMessage = null,
            )
            is TypeTypeResult.Failure -> finishAction(result.error.toUserMessage())
        }
    }
}

public fun TvViewModel.togglePlaylistVideo(playlist: UserPlaylist, video: Video) {
    if (!beginAuthenticatedAction()) return
    val remove = playlist.videos.any { it.url == video.url }
    viewModelScope.launch {
        val mutation = if (remove) {
            client.library.removePlaylistVideo(playlist.id, video.url)
        } else {
            client.library.addPlaylistVideo(playlist.id, video)
        }
        when (mutation) {
            is TypeTypeResult.Success -> refreshPlaylists()
            is TypeTypeResult.Failure -> finishAction(mutation.error.toUserMessage())
        }
    }
}

public fun TvViewModel.createPlaylist(name: String) {
    if (name.isBlank() || !beginAuthenticatedAction()) return
    viewModelScope.launch {
        when (val result = client.library.createPlaylist(name.trim())) {
            is TypeTypeResult.Success -> refreshPlaylists()
            is TypeTypeResult.Failure -> finishAction(result.error.toUserMessage())
        }
    }
}

public fun TvViewModel.renamePlaylist(playlist: UserPlaylist, name: String) {
    if (name.isBlank() || !beginAuthenticatedAction()) return
    viewModelScope.launch {
        when (val result = client.library.updatePlaylist(playlist.id, name.trim(), playlist.description)) {
            is TypeTypeResult.Success -> refreshPlaylists()
            is TypeTypeResult.Failure -> finishAction(result.error.toUserMessage())
        }
    }
}

public fun TvViewModel.deletePlaylist(playlist: UserPlaylist) {
    if (!beginAuthenticatedAction()) return
    viewModelScope.launch {
        when (val result = client.library.deletePlaylist(playlist.id)) {
            is TypeTypeResult.Success -> {
                mutableState.value = mutableState.value.copy(selectedUserPlaylist = null)
                refreshPlaylists()
            }
            is TypeTypeResult.Failure -> finishAction(result.error.toUserMessage())
        }
    }
}

public fun TvViewModel.removePlaylistVideo(playlist: UserPlaylist, video: Video) {
    if (!beginAuthenticatedAction()) return
    viewModelScope.launch {
        when (val result = client.library.removePlaylistVideo(playlist.id, video.url)) {
            is TypeTypeResult.Success -> refreshPlaylists()
            is TypeTypeResult.Failure -> finishAction(result.error.toUserMessage())
        }
    }
}

public fun TvViewModel.movePlaylistVideo(playlist: UserPlaylist, video: Video, offset: Int) {
    val reordered = reorderedVideoIds(
        playlist.videos.map { it.id.value },
        video.id.value,
        offset,
    ) ?: return
    if (!beginAuthenticatedAction()) return
    viewModelScope.launch {
        when (val result = client.library.reorderPlaylist(playlist.id, reordered)) {
            is TypeTypeResult.Success -> refreshPlaylists()
            is TypeTypeResult.Failure -> finishAction(result.error.toUserMessage())
        }
    }
}

internal fun TvViewModel.beginAuthenticatedAction(): Boolean {
    if (mutableState.value.isActionInProgress) return false
    if (mutableState.value.authStatus != TvAuthStatus.AUTHENTICATED) {
        mutableState.value = mutableState.value.copy(errorMessage = "Sign in to change your library")
        return false
    }
    mutableState.value = mutableState.value.copy(isActionInProgress = true, errorMessage = null)
    return true
}

private suspend fun TvViewModel.refreshFavorites() {
    when (val result = client.library.favorites()) {
        is TypeTypeResult.Success -> mutableState.value = mutableState.value.copy(
            favorites = result.value,
            isActionInProgress = false,
            errorMessage = null,
        )
        is TypeTypeResult.Failure -> finishAction(result.error.toUserMessage())
    }
}

private suspend fun TvViewModel.refreshWatchLater() {
    when (val result = client.library.watchLater()) {
        is TypeTypeResult.Success -> mutableState.value = mutableState.value.copy(
            watchLater = result.value,
            isActionInProgress = false,
            errorMessage = null,
        )
        is TypeTypeResult.Failure -> finishAction(result.error.toUserMessage())
    }
}

internal suspend fun TvViewModel.refreshSubscriptions() {
    when (val result = client.subscriptions.list()) {
        is TypeTypeResult.Success -> mutableState.value = mutableState.value.copy(
            subscriptions = result.value,
            isActionInProgress = false,
            errorMessage = null,
        )
        is TypeTypeResult.Failure -> finishAction(result.error.toUserMessage())
    }
}

private suspend fun TvViewModel.refreshPlaylists() {
    when (val result = client.library.playlists()) {
        is TypeTypeResult.Success -> mutableState.value = mutableState.value.copy(
            playlists = result.value,
            selectedUserPlaylist = mutableState.value.selectedUserPlaylist?.let { selected ->
                result.value.firstOrNull { it.id == selected.id }
            },
            isActionInProgress = false,
            errorMessage = null,
        )
        is TypeTypeResult.Failure -> finishAction(result.error.toUserMessage())
    }
}

private suspend fun TvViewModel.refreshSavedPlaylists() {
    when (val result = client.library.savedPlaylists()) {
        is TypeTypeResult.Success -> mutableState.value = mutableState.value.copy(
            savedPlaylists = result.value,
            isActionInProgress = false,
            errorMessage = null,
        )
        is TypeTypeResult.Failure -> finishAction(result.error.toUserMessage())
    }
}

internal fun TvViewModel.finishAction(error: String) {
    mutableState.value = mutableState.value.copy(isActionInProgress = false, errorMessage = error)
}
