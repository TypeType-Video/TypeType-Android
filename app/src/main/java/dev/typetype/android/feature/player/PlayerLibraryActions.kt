package dev.typetype.android.feature.player

import dev.typetype.android.domain.library.LibraryRepository
import dev.typetype.android.domain.stream.Stream
import javax.inject.Inject

class PlayerLibraryActions @Inject constructor(
    private val libraryRepository: LibraryRepository,
) {
    suspend fun addToPlaylist(playlistId: String, videoUrl: String, stream: Stream): Result<Unit> =
        libraryRepository.addVideoToPlaylist(
            playlistId = playlistId,
            videoUrl = videoUrl,
            title = stream.title,
            thumbnail = stream.thumbnailUrl,
            duration = stream.durationSeconds,
            channelName = stream.uploaderName,
            channelUrl = stream.uploaderUrl,
            channelAvatarUrl = stream.uploaderAvatarUrl,
            viewCount = stream.viewCount,
        )

    suspend fun createPlaylistAndAdd(name: String, videoUrl: String, stream: Stream): Result<Unit> =
        libraryRepository.createPlaylist(name).mapCatching { playlistId ->
            addToPlaylist(playlistId, videoUrl, stream).getOrThrow()
        }

    suspend fun toggleFavorite(videoUrl: String, stream: Stream?, isFavorite: Boolean): Result<Unit> =
        if (isFavorite) {
            libraryRepository.removeFavorite(videoUrl)
        } else {
            libraryRepository.addFavorite(
                videoUrl = videoUrl,
                title = stream?.title.orEmpty(),
                thumbnail = stream?.thumbnailUrl.orEmpty(),
                duration = stream?.durationSeconds ?: 0L,
                channelName = stream?.uploaderName.orEmpty(),
                channelUrl = stream?.uploaderUrl.orEmpty(),
                channelAvatarUrl = stream?.uploaderAvatarUrl.orEmpty(),
                viewCount = stream?.viewCount ?: 0L,
            )
        }

    suspend fun toggleWatchLater(videoUrl: String, stream: Stream, isInWatchLater: Boolean): Result<Unit> =
        if (isInWatchLater) {
            libraryRepository.removeWatchLater(videoUrl)
        } else {
            libraryRepository.addWatchLater(
                url = videoUrl,
                title = stream.title,
                thumbnail = stream.thumbnailUrl,
                duration = stream.durationSeconds,
                channelName = stream.uploaderName,
                channelUrl = stream.uploaderUrl,
                channelAvatarUrl = stream.uploaderAvatarUrl,
                viewCount = stream.viewCount,
            )
        }
}
