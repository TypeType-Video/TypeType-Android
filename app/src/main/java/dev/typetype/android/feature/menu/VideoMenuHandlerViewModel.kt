package dev.typetype.android.feature.menu

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.typetype.android.R
import dev.typetype.android.toPlaybackQueueEntry
import dev.typetype.android.core.ui.error.UserErrorMapper
import dev.typetype.android.domain.actions.VideoActionsRepository
import dev.typetype.android.domain.download.DownloadProgress
import dev.typetype.android.domain.download.DownloadRepository
import dev.typetype.android.domain.download.DownloadSelection
import dev.typetype.android.domain.feed.Video
import dev.typetype.android.domain.library.LibraryRepository
import dev.typetype.android.domain.navigation.canonicalVideoIdentity
import dev.typetype.android.domain.playback.PlaybackQueueEntry
import dev.typetype.android.domain.playback.PlaybackQueueMutationResult
import dev.typetype.android.feature.player.host.PlayerHostController
import dev.typetype.android.services.PlaybackQueueCoordinator
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
    @param:ApplicationContext internal val context: Context,
    internal val libraryRepository: LibraryRepository,
    internal val videoActionsRepository: VideoActionsRepository,
    private val downloadRepository: DownloadRepository,
    private val errorMapper: UserErrorMapper,
    private val playbackQueueCoordinator: PlaybackQueueCoordinator,
    private val playerHostController: PlayerHostController,
) : ViewModel() {

    private val playlistState = libraryRepository.observePlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val playlists = playlistState

    private val favoriteTargets = libraryRepository.observeFavorites()
        .map { items -> items.associate { canonicalVideoIdentity(it.videoUrl) to it.videoUrl } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val favoriteUrls = favoriteTargets
        .map { it.keys }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    private val watchLaterTargets = libraryRepository.observeWatchLater()
        .map { items -> items.associate { canonicalVideoIdentity(it.url) to it.url } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val watchLaterUrls = watchLaterTargets
        .map { it.keys }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val watchedUrls = libraryRepository.observeWatchedUrls()
        .map { urls -> urls.mapTo(mutableSetOf(), ::canonicalVideoIdentity) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val blockedVideoUrls = videoActionsRepository.observeBlockedVideoUrls()
        .map { urls -> urls.mapTo(mutableSetOf(), ::canonicalVideoIdentity) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val blockedChannelUrls = videoActionsRepository.observeBlockedChannelUrls()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val blockedKeywords = videoActionsRepository.observeBlockedKeywords()
        .map { items -> items.map { it.keyword }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    private val _events = Channel<VideoMenuEvent>(Channel.BUFFERED)
    val events: Flow<VideoMenuEvent> = _events.receiveAsFlow()

    fun toggleFavorite(video: Video, isCurrentlyFavorite: Boolean) {
        viewModelScope.launch {
            val result = if (isCurrentlyFavorite) {
                val target = favoriteTargets.value[canonicalVideoIdentity(video.url)] ?: video.url
                libraryRepository.removeFavorite(target)
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
                val target = watchLaterTargets.value[canonicalVideoIdentity(video.url)] ?: video.url
                libraryRepository.removeWatchLater(target)
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

    fun playNext(video: Video) = updateQueue(video.toPlaybackQueueEntry(), playNext = true)

    fun addToQueue(video: Video) = updateQueue(video.toPlaybackQueueEntry(), playNext = false)

    fun playNext(entry: PlaybackQueueEntry) = updateQueue(entry, playNext = true)

    fun addToQueue(entry: PlaybackQueueEntry) = updateQueue(entry, playNext = false)

    fun download(video: Video, selection: DownloadSelection) {
        viewModelScope.launch {
            var queuedSent = false
            runCatching {
                downloadRepository.downloadVideo(
                    videoUrl = video.url,
                    title = video.title,
                    selection = selection,
                ).collect { progress ->
                    when (progress) {
                        is DownloadProgress.Queued -> {
                            if (!queuedSent) {
                                queuedSent = true
                                val message = if (progress.cached) {
                                    R.string.snackbar_download_cached
                                } else {
                                    R.string.snackbar_download_queued
                                }
                                _events.send(VideoMenuEvent.Snackbar(context.getString(message)))
                            }
                        }
                        is DownloadProgress.Running -> Unit
                        is DownloadProgress.Enqueued ->
                            _events.send(VideoMenuEvent.Snackbar(context.getString(R.string.snackbar_download_enqueued)))
                    }
                }
            }.onFailure { emitFailure(it) }
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

    internal suspend fun emitResult(result: Result<Unit>, successRes: Int) {
        result.fold(
            onSuccess = { _events.send(VideoMenuEvent.Snackbar(context.getString(successRes))) },
            onFailure = { emitFailure(it) },
        )
    }

    internal suspend fun emitFailure(throwable: Throwable) {
        _events.send(
            VideoMenuEvent.Snackbar(
                errorMapper.message(throwable, R.string.snackbar_action_failed),
            ),
        )
    }

    internal suspend fun emitMessage(message: String) {
        _events.send(VideoMenuEvent.Snackbar(message))
    }

    private fun updateQueue(entry: PlaybackQueueEntry, playNext: Boolean) {
        val result = playbackQueueCoordinator.enqueue(entry, playNext)
        val message = when (result) {
            PlaybackQueueMutationResult.Added -> if (playNext) {
                R.string.snackbar_queue_play_next
            } else {
                R.string.snackbar_added_to_queue
            }
            PlaybackQueueMutationResult.Moved -> R.string.snackbar_queue_moved_next
            PlaybackQueueMutationResult.AlreadyQueued -> R.string.snackbar_already_in_queue
            PlaybackQueueMutationResult.AlreadyPlaying -> R.string.snackbar_already_playing
            PlaybackQueueMutationResult.NoActivePlayback -> {
                playerHostController.openQueue(
                    context.getString(R.string.playback_queue_title),
                    listOf(entry),
                    shuffle = false,
                )
                R.string.snackbar_queue_started
            }
        }
        viewModelScope.launch { _events.send(VideoMenuEvent.Snackbar(context.getString(message))) }
    }
}
