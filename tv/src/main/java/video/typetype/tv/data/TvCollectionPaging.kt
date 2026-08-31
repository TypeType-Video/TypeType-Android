package video.typetype.tv.data

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import video.typetype.sdk.core.ChannelRequest
import video.typetype.sdk.core.PlaylistRequest
import video.typetype.sdk.core.Podcast
import video.typetype.sdk.core.TypeTypeResult

public fun TvViewModel.openPodcast(podcast: Podcast) {
    if (mutableState.value.isLoadingDetails) return
    viewModelScope.launch {
        mutableState.value = mutableState.value.copy(
            selectedVideo = null,
            selectedChannel = null,
            selectedPlaylist = null,
            selectedUserPlaylist = null,
            selectedPodcast = null,
            stream = null,
            playback = null,
            audioOnlyStream = null,
            channelPodcasts = null,
            channelPlaylists = null,
            isLoadingDetails = true,
            errorMessage = null,
        )
        when (val result = client.catalog.podcastEpisodes(podcast.url)) {
            is TypeTypeResult.Success -> mutableState.value = mutableState.value.copy(
                selectedPodcast = result.value,
                isLoadingDetails = false,
            )
            is TypeTypeResult.Failure -> mutableState.value = mutableState.value.copy(
                isLoadingDetails = false,
                errorMessage = result.error.toUserMessage(),
            )
        }
    }
}

public fun TvViewModel.closePodcast() {
    mutableState.value = mutableState.value.copy(
        selectedPodcast = null,
        isLoadingMoreCollection = false,
        errorMessage = null,
    )
}

public fun TvViewModel.loadMoreChannel() {
    val current = mutableState.value.selectedChannel ?: return
    val playlists = mutableState.value.channelPlaylists
    val nextPage = current.nextPage
    val playlistNextPage = playlists?.nextPage
    if (nextPage == null && playlistNextPage == null) return
    if (!beginCollectionPage()) return
    viewModelScope.launch {
        val channelDeferred = async {
            nextPage?.let { client.catalog.channelPage(ChannelRequest(current.url, it)) }
        }
        val playlistsDeferred = async {
            playlistNextPage?.let { client.catalog.channelPlaylists(ChannelRequest(current.url, it)) }
        }
        val channelResult = channelDeferred.await()
        val playlistsResult = playlistsDeferred.await()
        val error = listOfNotNull(channelResult, playlistsResult)
            .mapNotNull { (it as? TypeTypeResult.Failure)?.error }
            .firstOrNull()
        if (error != null) {
            finishCollectionPage(error.toUserMessage())
        } else {
            val channelPage = (channelResult as? TypeTypeResult.Success)?.value
            val playlistPage = (playlistsResult as? TypeTypeResult.Success)?.value
            mutableState.value = mutableState.value.copy(
                selectedChannel = channelPage?.let {
                    current.copy(
                        videos = (current.videos + it.videos).distinctBy { video -> video.id.value },
                        nextPage = it.nextPage,
                    )
                } ?: current,
                channelPlaylists = playlistPage?.let {
                    playlists?.copy(
                        playlists = (playlists.playlists + it.playlists).distinctBy { item -> item.id },
                        nextPage = it.nextPage,
                    )
                } ?: playlists,
                isLoadingMoreCollection = false,
                errorMessage = null,
            )
        }
    }
}

public fun TvViewModel.loadMorePlaylist() {
    val current = mutableState.value.selectedPlaylist ?: return
    val nextPage = current.nextPage ?: return
    if (!beginCollectionPage()) return
    viewModelScope.launch {
        when (val result = client.catalog.playlist(PlaylistRequest(current.playlist.url, nextPage))) {
            is TypeTypeResult.Success -> mutableState.value = mutableState.value.copy(
                selectedPlaylist = current.copy(
                    videos = (current.videos + result.value.videos).distinctBy { it.id.value },
                    nextPage = result.value.nextPage,
                ),
                isLoadingMoreCollection = false,
                errorMessage = null,
            )
            is TypeTypeResult.Failure -> finishCollectionPage(result.error.toUserMessage())
        }
    }
}

public fun TvViewModel.loadMorePodcast() {
    val current = mutableState.value.selectedPodcast ?: return
    val nextPage = current.nextPage ?: return
    if (!beginCollectionPage()) return
    viewModelScope.launch {
        when (val result = client.catalog.podcastEpisodes(current.podcast.url, nextPage)) {
            is TypeTypeResult.Success -> mutableState.value = mutableState.value.copy(
                selectedPodcast = current.copy(
                    episodes = (current.episodes + result.value.episodes).distinctBy { it.id.value },
                    nextPage = result.value.nextPage,
                ),
                isLoadingMoreCollection = false,
                errorMessage = null,
            )
            is TypeTypeResult.Failure -> finishCollectionPage(result.error.toUserMessage())
        }
    }
}

private fun TvViewModel.beginCollectionPage(): Boolean {
    if (mutableState.value.isLoadingMoreCollection) return false
    mutableState.value = mutableState.value.copy(isLoadingMoreCollection = true, errorMessage = null)
    return true
}

private fun TvViewModel.finishCollectionPage(error: String) {
    mutableState.value = mutableState.value.copy(
        isLoadingMoreCollection = false,
        errorMessage = error,
    )
}
