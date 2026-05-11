package dev.typetype.android.feature.menu

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.typetype.android.R
import dev.typetype.android.domain.actions.VideoActionsRepository
import dev.typetype.android.domain.feed.Video
import dev.typetype.android.domain.library.LibraryRepository
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface VideoMenuEvent {
    data class Snackbar(val message: String) : VideoMenuEvent
}

@HiltViewModel
class VideoMenuHandlerViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val libraryRepository: LibraryRepository,
    private val videoActionsRepository: VideoActionsRepository,
) : ViewModel() {

    val playlists = libraryRepository.observePlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val favoriteUrls = libraryRepository.observePlaylists()
        .map { lists ->
            lists.firstOrNull { it.name.equals("Favorites", ignoreCase = true) }
                ?.videos?.map { it.url }?.toSet() ?: emptySet()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val watchLaterUrls = libraryRepository.observePlaylists()
        .map { lists ->
            lists.firstOrNull { it.name.equals("Watch Later", ignoreCase = true) }
                ?.videos?.map { it.url }?.toSet() ?: emptySet()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val watchedUrls = libraryRepository.observeHistory()
        .map { items ->
            items.asSequence()
                .filter { it.durationSeconds > 0 && it.progressSeconds >= it.durationSeconds * 0.9 }
                .map { it.url }
                .toSet()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val blockedVideoUrls = videoActionsRepository.observeBlockedVideoUrls()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val blockedChannelUrls = videoActionsRepository.observeBlockedChannelUrls()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    private val _events = Channel<VideoMenuEvent>(Channel.BUFFERED)
    val events: Flow<VideoMenuEvent> = _events.receiveAsFlow()

    fun toggleFavorite(video: Video, isCurrentlyFavorite: Boolean) {
        viewModelScope.launch {
            val result = if (isCurrentlyFavorite) {
                libraryRepository.removeFavorite(video.url)
            } else {
                libraryRepository.addFavorite(
                    videoUrl = video.url,
                    title = video.title,
                    thumbnail = video.thumbnailUrl,
                    duration = video.durationSeconds,
                    channelName = video.uploaderName,
                    channelUrl = video.uploaderUrl,
                    channelAvatarUrl = video.uploaderAvatarUrl,
                    viewCount = video.viewCount,
                )
            }
            val successRes = if (isCurrentlyFavorite) {
                R.string.snackbar_removed_from_favorites
            } else {
                R.string.snackbar_added_to_favorites
            }
            emitResult(result, successRes)
        }
    }

    fun toggleWatchLater(video: Video, isCurrentlyInWatchLater: Boolean) {
        viewModelScope.launch {
            val result = if (isCurrentlyInWatchLater) {
                libraryRepository.removeWatchLater(video.url)
            } else {
                libraryRepository.addWatchLater(
                    url = video.url,
                    title = video.title,
                    thumbnail = video.thumbnailUrl,
                    duration = video.durationSeconds,
                    channelName = video.uploaderName,
                    channelUrl = video.uploaderUrl,
                    channelAvatarUrl = video.uploaderAvatarUrl,
                    viewCount = video.viewCount,
                )
            }
            val successRes = if (isCurrentlyInWatchLater) {
                R.string.snackbar_removed_from_watch_later
            } else {
                R.string.snackbar_added_to_watch_later
            }
            emitResult(result, successRes)
        }
    }

    fun addToPlaylist(playlistId: String, video: Video) {
        viewModelScope.launch {
            val playlistName = playlists.value.firstOrNull { it.id == playlistId }?.name
            libraryRepository.addVideoToPlaylist(
                playlistId = playlistId,
                videoUrl = video.url,
                title = video.title,
                thumbnail = video.thumbnailUrl,
                duration = video.durationSeconds,
                channelName = video.uploaderName,
                channelUrl = video.uploaderUrl,
                channelAvatarUrl = video.uploaderAvatarUrl,
                viewCount = video.viewCount,
            ).fold(
                onSuccess = {
                    val msg = playlistName?.let {
                        context.getString(R.string.snackbar_added_to_playlist, it)
                    } ?: context.getString(R.string.snackbar_added_to_playlist_default)
                    _events.send(VideoMenuEvent.Snackbar(msg))
                },
                onFailure = { emitFailure(it) },
            )
        }
    }

    fun createPlaylistAndAdd(name: String, video: Video) {
        val cleaned = name.trim()
        if (cleaned.isEmpty()) return
        viewModelScope.launch {
            libraryRepository.createPlaylist(cleaned).fold(
                onSuccess = { newId ->
                    libraryRepository.addVideoToPlaylist(
                        playlistId = newId,
                        videoUrl = video.url,
                        title = video.title,
                        thumbnail = video.thumbnailUrl,
                        duration = video.durationSeconds,
                        channelName = video.uploaderName,
                        channelUrl = video.uploaderUrl,
                        channelAvatarUrl = video.uploaderAvatarUrl,
                        viewCount = video.viewCount,
                    ).fold(
                        onSuccess = {
                            _events.send(
                                VideoMenuEvent.Snackbar(
                                    context.getString(R.string.snackbar_added_to_playlist, cleaned),
                                ),
                            )
                        },
                        onFailure = { emitFailure(it) },
                    )
                },
                onFailure = { emitFailure(it) },
            )
        }
    }

    fun toggleWatched(video: Video, isCurrentlyWatched: Boolean) {
        viewModelScope.launch {
            val result = if (isCurrentlyWatched) {
                libraryRepository.removeFromHistory(video.url)
            } else {
                libraryRepository.addHistory(
                    videoUrl = video.url,
                    title = video.title,
                    thumbnail = video.thumbnailUrl,
                    duration = video.durationSeconds,
                    channelName = video.uploaderName,
                    channelUrl = video.uploaderUrl,
                    channelAvatarUrl = video.uploaderAvatarUrl,
                )
            }
            val successRes = if (isCurrentlyWatched) {
                R.string.snackbar_unmarked_as_watched
            } else {
                R.string.snackbar_marked_as_watched
            }
            emitResult(result, successRes)
        }
    }

    fun blockVideo(video: Video) {
        viewModelScope.launch {
            emitResult(
                videoActionsRepository.blockVideo(video.url),
                R.string.snackbar_video_blocked,
            )
        }
    }

    fun blockVideoUrl(videoUrl: String) {
        viewModelScope.launch {
            emitResult(
                videoActionsRepository.blockVideo(videoUrl),
                R.string.snackbar_video_blocked,
            )
        }
    }

    fun removeFromPlaylist(playlistId: String, playlistName: String, videoUrl: String) {
        viewModelScope.launch {
            libraryRepository.removeVideoFromPlaylist(playlistId, videoUrl).fold(
                onSuccess = {
                    _events.send(
                        VideoMenuEvent.Snackbar(
                            context.getString(R.string.snackbar_removed_from_playlist, playlistName),
                        ),
                    )
                },
                onFailure = { emitFailure(it) },
            )
        }
    }

    fun removeFavoriteUrl(videoUrl: String) {
        viewModelScope.launch {
            emitResult(
                libraryRepository.removeFavorite(videoUrl),
                R.string.snackbar_removed_from_favorites,
            )
        }
    }

    fun removeWatchLaterUrl(videoUrl: String) {
        viewModelScope.launch {
            emitResult(
                libraryRepository.removeWatchLater(videoUrl),
                R.string.snackbar_removed_from_watch_later,
            )
        }
    }

    fun toggleWatchedUrl(
        videoUrl: String,
        title: String,
        thumbnail: String,
        duration: Long,
        isCurrentlyWatched: Boolean,
    ) {
        viewModelScope.launch {
            val result = if (isCurrentlyWatched) {
                libraryRepository.removeFromHistory(videoUrl)
            } else {
                libraryRepository.addHistory(
                    videoUrl = videoUrl,
                    title = title,
                    thumbnail = thumbnail,
                    duration = duration,
                    channelName = "",
                    channelUrl = "",
                )
            }
            val successRes = if (isCurrentlyWatched) {
                R.string.snackbar_unmarked_as_watched
            } else {
                R.string.snackbar_marked_as_watched
            }
            emitResult(result, successRes)
        }
    }

    fun blockChannel(video: Video) {
        viewModelScope.launch {
            emitResult(
                videoActionsRepository.blockChannel(
                    channelUrl = video.uploaderUrl,
                    channelName = video.uploaderName,
                    avatarUrl = video.uploaderAvatarUrl,
                ),
                R.string.snackbar_channel_blocked,
            )
        }
    }

    private suspend fun emitResult(result: Result<Unit>, successRes: Int) {
        result.fold(
            onSuccess = { _events.send(VideoMenuEvent.Snackbar(context.getString(successRes))) },
            onFailure = { emitFailure(it) },
        )
    }

    private suspend fun emitFailure(throwable: Throwable) {
        val msg = throwable.message?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.snackbar_action_failed)
        _events.send(VideoMenuEvent.Snackbar(msg))
    }
}
