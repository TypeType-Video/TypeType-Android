package video.typetype.tv.data

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import video.typetype.sdk.core.TypeTypeResult

internal suspend fun TvViewModel.loadLibraryContent(showLoading: Boolean): Unit = coroutineScope {
    if (showLoading) mutableState.value = mutableState.value.copy(isLoadingLibrary = true, errorMessage = null)
    val historyDeferred = async { client.library.history() }
    val watchLaterDeferred = async { client.library.watchLater() }
    val favoritesDeferred = async { client.library.favorites() }
    val playlistsDeferred = async { client.library.playlists() }
    val savedPlaylistsDeferred = async { client.library.savedPlaylists() }
    val history = historyDeferred.await()
    val watchLater = watchLaterDeferred.await()
    val favorites = favoritesDeferred.await()
    val playlists = playlistsDeferred.await()
    val savedPlaylists = savedPlaylistsDeferred.await()
    val errors = listOf(history, watchLater, favorites, playlists, savedPlaylists)
        .mapNotNull { (it as? TypeTypeResult.Failure)?.error }
    val current = mutableState.value
    mutableState.value = current.copy(
        history = (history as? TypeTypeResult.Success)?.value ?: current.history,
        watchLater = (watchLater as? TypeTypeResult.Success)?.value ?: current.watchLater,
        favorites = (favorites as? TypeTypeResult.Success)?.value ?: current.favorites,
        playlists = (playlists as? TypeTypeResult.Success)?.value ?: current.playlists,
        savedPlaylists = (savedPlaylists as? TypeTypeResult.Success)?.value ?: current.savedPlaylists,
        isLoadingLibrary = false,
        errorMessage = errors.firstOrNull()?.toUserMessage() ?: current.errorMessage,
    )
}
