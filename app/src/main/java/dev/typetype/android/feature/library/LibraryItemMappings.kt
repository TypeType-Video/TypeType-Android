package dev.typetype.android.feature.library

import dev.typetype.android.domain.library.FavoriteItem
import dev.typetype.android.domain.library.PlaylistVideo
import dev.typetype.android.domain.library.Playlist
import dev.typetype.android.domain.library.WatchLaterItem
import dev.typetype.android.domain.publicplaylist.SavedPublicPlaylist

internal data class LibraryContent(
    val historyCount: Int,
    val favorites: List<FavoriteItem>,
    val watchLater: List<WatchLaterItem>,
    val playlists: List<Playlist>,
    val savedPlaylists: List<SavedPublicPlaylist>,
)

internal fun FavoriteItem.asPlaylistVideo() = PlaylistVideo(
    id = videoUrl,
    url = videoUrl,
    title = title,
    thumbnailUrl = thumbnailUrl,
    durationSeconds = durationSeconds,
    position = 0,
    channelName = channelName,
    channelUrl = channelUrl,
    channelAvatarUrl = channelAvatarUrl,
    viewCount = viewCount,
)

internal fun WatchLaterItem.asPlaylistVideo() = PlaylistVideo(
    id = url,
    url = url,
    title = title,
    thumbnailUrl = thumbnailUrl,
    durationSeconds = durationSeconds,
    position = 0,
    channelName = channelName,
    channelUrl = channelUrl,
    channelAvatarUrl = channelAvatarUrl,
    viewCount = viewCount,
)
