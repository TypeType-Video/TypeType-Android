package video.typetype.tv.data

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import video.typetype.sdk.core.ServiceId
import video.typetype.sdk.core.TypeTypeResult
import video.typetype.sdk.core.Video

public fun TvViewModel.openVideo(video: Video): Unit =
    openVideo(video, video.serviceId, autoPlay = false)

public fun TvViewModel.playVideo(video: Video): Unit =
    openVideo(video, video.serviceId, autoPlay = true)

public fun TvViewModel.openVideo(video: Video, service: ServiceId): Unit =
    openVideo(video, service, autoPlay = false)

private fun TvViewModel.openVideo(video: Video, service: ServiceId, autoPlay: Boolean) {
    val previousState = mutableState.value
    val navigationRelated = previousState.navigationRelated(video)
    val navigatingFromVideo = previousState.selectedVideo != null
    viewModelScope.launch {
        mutableState.value = mutableState.value.copy(
            selectedVideo = video,
            selectedService = service,
            selectedChannel = if (navigatingFromVideo) null else previousState.selectedChannel,
            selectedPlaylist = if (navigatingFromVideo) null else previousState.selectedPlaylist,
            selectedUserPlaylist = if (navigatingFromVideo) null else previousState.selectedUserPlaylist,
            selectedPodcast = if (navigatingFromVideo) null else previousState.selectedPodcast,
            channelPodcasts = if (navigatingFromVideo) null else previousState.channelPodcasts,
            channelPlaylists = if (navigatingFromVideo) null else previousState.channelPlaylists,
            stream = null,
            playback = null,
            audioOnlyStream = null,
            selectedSubtitleLanguage = null,
            selectedSubtitleAuto = false,
            selectedSubtitleName = null,
            isLoadingDetails = true,
            comments = emptyList(),
            commentsNextPage = null,
            commentsDisabled = false,
            commentReplies = emptyMap(),
            errorMessage = null,
        )
        if (!mutableState.value.settings.hideComments) loadInitialComments(video.url)
        val result = withTimeoutOrNull(DETAILS_LOAD_TIMEOUT_MILLISECONDS) {
            loadStreamDetails(video.url, service)
        }
        if (result == null) {
            if (mutableState.value.selectedVideo?.url == video.url) {
                mutableState.value = mutableState.value.copy(
                    isLoadingDetails = false,
                    errorMessage = "The video took too long to prepare.\nTry again without leaving this screen.",
                )
            }
            return@launch
        }
        when (result) {
            is TypeTypeResult.Success -> {
                if (mutableState.value.selectedVideo?.url != video.url) return@launch
                val streamWithNavigation = result.value.withNavigationRelated(navigationRelated)
                val visibleStream = if (mutableState.value.settings.hideRelatedVideos) {
                    streamWithNavigation.copy(relatedStreams = emptyList())
                } else {
                    streamWithNavigation
                }
                val tracks = visibleStream.selectTvPlaybackTracks(
                    isVideoSupported,
                    preferredAudioTrackId = visibleStream.defaultTvAudioTrackId(mutableState.value.settings),
                    preferredQuality = mutableState.value.settings.defaultQuality,
                )
                mutableState.value = mutableState.value.copy(
                    stream = visibleStream,
                    supportedVideoItags = visibleStream.videoOnlyStreams
                        .filter(isVideoSupported).mapTo(mutableSetOf()) { it.itag },
                    selectedVideoItag = tracks?.video?.itag,
                    selectedAudioItag = tracks?.audio?.itag,
                    selectedAudioTrackId = tracks?.audio?.audioTrackId,
                    isLoadingDetails = false,
                )
                if (autoPlay) startPlayback()
                if (!mutableState.value.settings.hideRelatedVideos && streamWithNavigation.relatedStreams.isEmpty()) {
                    loadRecommendationNavigation(video, service)
                }
                if (service == ServiceId.YOUTUBE) {
                    enrichYoutubeDetails(video, streamWithNavigation)
                }
            }
            is TypeTypeResult.Failure -> {
                if (mutableState.value.selectedVideo?.url != video.url) return@launch
                mutableState.value = mutableState.value.copy(
                    isLoadingDetails = false,
                    errorMessage = result.error.toUserMessage(),
                )
            }
        }
    }
}

private fun TvViewModel.enrichYoutubeDetails(video: Video, bootstrap: video.typetype.sdk.core.StreamDetails) {
    viewModelScope.launch {
        val full = withTimeoutOrNull(FULL_DETAILS_LOAD_TIMEOUT_MILLISECONDS) {
            client.catalog.stream(video.url, ServiceId.YOUTUBE)
        } ?: return@launch
        val fullStream = (full as? TypeTypeResult.Success)?.value ?: return@launch
        val current = mutableState.value
        if (current.selectedVideo?.url != video.url) return@launch
        val currentRelated = current.stream?.relatedStreams.orEmpty()
        val relatedFallback = currentRelated.ifEmpty { bootstrap.relatedStreams }
        val enriched = fullStream.withNavigationRelated(relatedFallback)
        val visibleStream = if (current.settings.hideRelatedVideos) {
            enriched.copy(relatedStreams = emptyList())
        } else {
            enriched
        }
        val tracks = visibleStream.selectTvPlaybackTracks(
            isVideoSupported,
            preferredAudioTrackId = visibleStream.defaultTvAudioTrackId(current.settings),
            preferredQuality = current.settings.defaultQuality,
        )
        mutableState.value = current.copy(
            stream = visibleStream,
            supportedVideoItags = visibleStream.videoOnlyStreams
                .filter(isVideoSupported).mapTo(mutableSetOf()) { it.itag },
            selectedVideoItag = tracks?.video?.itag ?: current.selectedVideoItag,
            selectedAudioItag = tracks?.audio?.itag ?: current.selectedAudioItag,
            selectedAudioTrackId = tracks?.audio?.audioTrackId ?: current.selectedAudioTrackId,
        )
    }
}

private fun TvViewModel.loadRecommendationNavigation(video: Video, service: ServiceId) {
    if (mutableState.value.authStatus != TvAuthStatus.AUTHENTICATED) return
    viewModelScope.launch {
        val result = withTimeoutOrNull(RECOMMENDATIONS_LOAD_TIMEOUT_MILLISECONDS) {
            client.recommendations.home(service, limit = RECOMMENDATIONS_LIMIT)
        } ?: return@launch
        val recommendations = (result as? TypeTypeResult.Success)?.value?.items
            ?.navigationRelated(video).orEmpty()
        if (recommendations.isEmpty()) return@launch
        val current = mutableState.value
        val stream = current.stream ?: return@launch
        if (current.selectedVideo?.url != video.url ||
            current.settings.hideRelatedVideos ||
            stream.relatedStreams.isNotEmpty()
        ) return@launch
        mutableState.value = current.copy(stream = stream.copy(relatedStreams = recommendations))
    }
}

private const val DETAILS_LOAD_TIMEOUT_MILLISECONDS = 60_000L
private const val FULL_DETAILS_LOAD_TIMEOUT_MILLISECONDS = 30_000L
private const val RECOMMENDATIONS_LOAD_TIMEOUT_MILLISECONDS = 10_000L
private const val RECOMMENDATIONS_LIMIT = 18
