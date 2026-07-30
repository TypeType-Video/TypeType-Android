package dev.typetype.android.feature.player

import dev.typetype.android.domain.library.Playlist
import dev.typetype.android.domain.playback.PlaybackQueueState
import dev.typetype.android.domain.stream.Stream
import dev.typetype.android.feature.player.components.PlayerGestureConfig
import dev.typetype.android.feature.player.error.StreamErrorClass

data class PlayerState(
    val isLoading: Boolean = true,
    val stream: Stream? = null,
    val videoUrl: String = "",
    val resumeAtMillis: Long = 0L,
    val initialPlayWhenReady: Boolean = true,
    val playbackBindGeneration: Long = 0L,
    val error: StreamErrorClass? = null,
    val isFavorited: Boolean = false,
    val isInWatchLater: Boolean = false,
    val gestureConfig: PlayerGestureConfig = PlayerGestureConfig(),
    val autoplayEnabled: Boolean = true,
    val autoplayCountdownSeconds: Int = 10,
    val defaultQuality: String = "1080p",
    val defaultAudioLanguage: String = "",
    val subtitlesEnabled: Boolean = false,
    val defaultSubtitleLanguage: String = "",
    val preferOriginalLanguage: Boolean = false,
    val playlistPickerVisible: Boolean = false,
    val playlists: List<Playlist> = emptyList(),
    val playlistActionInFlight: Boolean = false,
    val downloadInFlight: Boolean = false,
    val playbackQueue: PlaybackQueueState = PlaybackQueueState(),
)

internal fun PlayerState.retryPlayback(): PlayerState =
    copy(
        playbackBindGeneration = playbackBindGeneration + 1L,
        error = null,
    )
