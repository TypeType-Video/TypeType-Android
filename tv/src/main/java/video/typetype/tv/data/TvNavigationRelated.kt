package video.typetype.tv.data

import video.typetype.sdk.core.StreamDetails
import video.typetype.sdk.core.Video

internal fun TvAppState.navigationRelated(video: Video): List<Video> {
    val candidates = when {
        selectedPlaylist != null -> selectedPlaylist.videos
        selectedUserPlaylist != null -> selectedUserPlaylist.videos
        selectedPodcast != null -> selectedPodcast.episodes
        selectedChannel != null -> selectedChannel.videos
        destination == TvDestination.SEARCH -> searchPage?.videos.orEmpty()
        destination == TvDestination.LIBRARY -> buildList {
            addAll(history.map { it.video })
            addAll(watchLater.map { it.video })
            addAll(favorites.map { it.video })
        }
        else -> buildList {
            bigBuckBunny?.let(::add)
            addAll(home)
            addAll(trending)
            addAll(shorts)
        }
    }
    return candidates.navigationRelated(video)
}

internal fun StreamDetails.withNavigationRelated(fallback: List<Video>): StreamDetails =
    if (relatedStreams.isNotEmpty() || fallback.isEmpty()) this else copy(relatedStreams = fallback)

internal fun List<Video>.navigationRelated(video: Video): List<Video> =
    distinctBy(Video::id).filterNot { it.id == video.id }.take(MAX_NAVIGATION_RELATED)

private const val MAX_NAVIGATION_RELATED = 20
