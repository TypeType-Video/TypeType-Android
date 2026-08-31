package video.typetype.tv.data

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import video.typetype.sdk.core.Channel
import video.typetype.sdk.core.ChannelRequest
import video.typetype.sdk.core.Playlist
import video.typetype.sdk.core.PlaylistRequest
import video.typetype.sdk.core.PlaybackOpenRequest
import video.typetype.sdk.core.SavedPlaylist
import video.typetype.sdk.core.ServiceId
import video.typetype.sdk.core.TypeTypeResult
import video.typetype.sdk.core.UserPlaylist

public fun TvViewModel.playNext() {
    val next = mutableState.value.stream?.relatedStreams?.firstOrNull()
    if (next == null) {
        closePlayback()
        return
    }
    playQueuedVideo(next)
}

public fun TvViewModel.playQueuedVideo(next: video.typetype.sdk.core.Video) {
    val navigationRelated = mutableState.value.stream?.relatedStreams.orEmpty()
        .filterNot { it.id == next.id }
    viewModelScope.launch {
        mutableState.value = mutableState.value.copy(isAdvancingPlayback = true, errorMessage = null)
        val service = next.serviceId
        when (val result = loadStreamDetails(next.url, service)) {
            is TypeTypeResult.Success -> {
                val stream = result.value.withNavigationRelated(navigationRelated)
                val supportedVideoItags = stream.videoOnlyStreams
                    .filter(isVideoSupported).mapTo(mutableSetOf()) { it.itag }
                val standardSession = stream.standardPlaybackSession(service)
                if (standardSession != null) {
                    mutableState.value = mutableState.value.copy(
                        selectedVideo = next,
                        selectedService = service,
                        stream = stream,
                        supportedVideoItags = supportedVideoItags,
                        playback = standardSession,
                        audioOnlyStream = null,
                        selectedVideoItag = null,
                        selectedAudioItag = null,
                        selectedAudioTrackId = null,
                        selectedSubtitleLanguage = null,
                        selectedSubtitleAuto = false,
                        selectedSubtitleName = null,
                        isAdvancingPlayback = false,
                        errorMessage = null,
                    )
                    return@launch
                }
                if (service != ServiceId.YOUTUBE) {
                    mutableState.value = mutableState.value.copy(
                        isAdvancingPlayback = false,
                        errorMessage = "The server did not return a playable manifest for this service",
                    )
                    return@launch
                }
                val tracks = stream.selectTvPlaybackTracks(isVideoSupported)
                if (tracks == null) {
                    mutableState.value = mutableState.value.copy(
                        isAdvancingPlayback = false,
                        errorMessage = "The TypeType server did not return a playable SABR audio/video pair",
                    )
                    return@launch
                }
                when (val opened = client.playback.open(
                    PlaybackOpenRequest(
                        videoUrl = next.url,
                        videoItag = tracks.video.itag,
                        audioItag = tracks.audio.itag,
                        audioTrackId = tracks.audio.audioTrackId,
                        startTimeMilliseconds = stream.startPositionMilliseconds,
                        isLive = stream.isLive,
                    ),
                )) {
                    is TypeTypeResult.Success -> mutableState.value = mutableState.value.copy(
                        selectedVideo = next,
                        selectedService = service,
                        stream = stream,
                        supportedVideoItags = supportedVideoItags,
                        playback = opened.value,
                        audioOnlyStream = null,
                        selectedVideoItag = tracks.video.itag,
                        selectedAudioItag = tracks.audio.itag,
                        selectedAudioTrackId = tracks.audio.audioTrackId,
                        selectedSubtitleLanguage = null,
                        selectedSubtitleAuto = false,
                        selectedSubtitleName = null,
                        isAdvancingPlayback = false,
                        errorMessage = null,
                    )
                    is TypeTypeResult.Failure -> mutableState.value = mutableState.value.copy(
                        isAdvancingPlayback = false,
                        errorMessage = "The next playback session could not start. ${opened.error.toUserMessage()}",
                    )
                }
            }
            is TypeTypeResult.Failure -> mutableState.value = mutableState.value.copy(
                isAdvancingPlayback = false,
                errorMessage = "The next video could not be loaded. ${result.error.toUserMessage()}",
            )
        }
    }
}

public fun TvViewModel.openChannel(channel: Channel) {
    viewModelScope.launch {
        mutableState.value = mutableState.value.copy(
            selectedVideo = null,
            selectedPlaylist = null,
            selectedUserPlaylist = null,
            selectedPodcast = null,
            selectedChannel = channel,
            stream = null,
            playback = null,
            audioOnlyStream = null,
            channelPodcasts = null,
            channelPlaylists = null,
            isLoadingDetails = true,
            errorMessage = null,
        )
        val channelDeferred = async { client.catalog.channel(ChannelRequest(channel.url)) }
        val podcastsDeferred = async { client.catalog.podcasts(channel.url) }
        val playlistsDeferred = async { client.catalog.channelPlaylists(ChannelRequest(channel.url)) }
        val channelResult = channelDeferred.await()
        val podcastsResult = podcastsDeferred.await()
        val playlistsResult = playlistsDeferred.await()
        val error = listOf(channelResult, podcastsResult, playlistsResult)
            .mapNotNull { (it as? TypeTypeResult.Failure)?.error }
            .firstOrNull()
        mutableState.value = mutableState.value.copy(
            selectedChannel = (channelResult as? TypeTypeResult.Success)?.value ?: channel,
            channelPodcasts = (podcastsResult as? TypeTypeResult.Success)?.value,
            channelPlaylists = (playlistsResult as? TypeTypeResult.Success)?.value,
            isLoadingDetails = false,
            errorMessage = if (channelResult is TypeTypeResult.Failure) {
                "Channel details are temporarily unavailable"
            } else {
                error?.toUserMessage()
            },
        )
    }
}

public fun TvViewModel.openPlaylist(playlist: Playlist) {
    viewModelScope.launch {
        mutableState.value = mutableState.value.copy(
            selectedVideo = null,
            selectedChannel = null,
            selectedUserPlaylist = null,
            selectedPodcast = null,
            selectedPlaylist = null,
            stream = null,
            playback = null,
            audioOnlyStream = null,
            channelPodcasts = null,
            channelPlaylists = null,
            isLoadingDetails = true,
            errorMessage = null,
        )
        when (val result = client.catalog.playlist(PlaylistRequest(playlist.url))) {
            is TypeTypeResult.Success -> mutableState.value = mutableState.value.copy(
                selectedPlaylist = result.value,
                isLoadingDetails = false,
            )
            is TypeTypeResult.Failure -> mutableState.value = mutableState.value.copy(
                isLoadingDetails = false,
                errorMessage = result.error.toUserMessage(),
            )
        }
    }
}

public fun TvViewModel.openUserPlaylist(playlist: UserPlaylist) {
    viewModelScope.launch {
        mutableState.value = mutableState.value.copy(
            selectedVideo = null,
            selectedChannel = null,
            selectedPlaylist = null,
            selectedPodcast = null,
            selectedUserPlaylist = playlist,
            stream = null,
            playback = null,
            audioOnlyStream = null,
            channelPodcasts = null,
            channelPlaylists = null,
            isLoadingDetails = true,
            errorMessage = null,
        )
        when (val result = client.library.playlist(playlist.id)) {
            is TypeTypeResult.Success -> mutableState.value = mutableState.value.copy(
                selectedUserPlaylist = result.value,
                isLoadingDetails = false,
            )
            is TypeTypeResult.Failure -> mutableState.value = mutableState.value.copy(
                isLoadingDetails = false,
                errorMessage = result.error.toUserMessage(),
            )
        }
    }
}

public fun TvViewModel.openSavedPlaylist(playlist: SavedPlaylist) {
    openPlaylist(
        Playlist(
            id = playlist.publicPlaylistId,
            title = playlist.title,
            url = playlist.url,
            thumbnailUrl = playlist.thumbnailUrl,
            uploaderName = playlist.uploaderName,
            streamCount = playlist.streamCount,
            playlistType = playlist.playlistType,
        ),
    )
}

public fun TvViewModel.closeCollection() {
    mutableState.value = mutableState.value.copy(
        selectedChannel = null,
        selectedPlaylist = null,
        selectedUserPlaylist = null,
        channelPodcasts = null,
        channelPlaylists = null,
        selectedPodcast = null,
        isLoadingMoreCollection = false,
        errorMessage = null,
    )
}
