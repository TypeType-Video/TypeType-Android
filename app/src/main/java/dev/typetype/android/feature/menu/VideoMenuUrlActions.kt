package dev.typetype.android.feature.menu

import androidx.lifecycle.viewModelScope
import dev.typetype.android.R
import kotlinx.coroutines.launch

fun VideoMenuHandlerViewModel.blockVideoUrl(videoUrl: String) {
    viewModelScope.launch {
        emitResult(
            videoActionsRepository.blockVideo(videoUrl),
            R.string.snackbar_video_blocked,
        )
    }
}

fun VideoMenuHandlerViewModel.removeFromPlaylist(
    playlistId: String,
    playlistName: String,
    videoUrl: String,
) {
    viewModelScope.launch {
        libraryRepository.removeVideoFromPlaylist(playlistId, videoUrl).fold(
            onSuccess = {
                emitMessage(context.getString(R.string.snackbar_removed_from_playlist, playlistName))
            },
            onFailure = { emitFailure(it) },
        )
    }
}

fun VideoMenuHandlerViewModel.removeFavoriteUrl(videoUrl: String) {
    viewModelScope.launch {
        emitResult(
            libraryRepository.removeFavorite(videoUrl),
            R.string.snackbar_removed_from_favorites,
        )
    }
}

fun VideoMenuHandlerViewModel.removeWatchLaterUrl(videoUrl: String) {
    viewModelScope.launch {
        emitResult(
            libraryRepository.removeWatchLater(videoUrl),
            R.string.snackbar_removed_from_watch_later,
        )
    }
}

fun VideoMenuHandlerViewModel.toggleWatchedUrl(
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
