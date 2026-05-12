package dev.typetype.android.feature.player

import dev.typetype.android.domain.library.Playlist
import dev.typetype.android.domain.stream.Stream
import dev.typetype.android.feature.player.components.PlayerGestureConfig

data class PlayerState(
    val isLoading: Boolean = true,
    val stream: Stream? = null,
    val videoUrl: String = "",
    val resumeAtMillis: Long = 0L,
    val errorMessage: String? = null,
    val isFavorited: Boolean = false,
    val isInWatchLater: Boolean = false,
    val gestureConfig: PlayerGestureConfig = PlayerGestureConfig(),
    val autoplayEnabled: Boolean = true,
    val defaultQuality: String = "1080p",
    val defaultAudioLanguage: String = "",
    val subtitlesEnabled: Boolean = false,
    val defaultSubtitleLanguage: String = "",
    val preferOriginalLanguage: Boolean = false,
    val playlistPickerVisible: Boolean = false,
    val playlists: List<Playlist> = emptyList(),
    val playlistActionInFlight: Boolean = false,
    val downloadInFlight: Boolean = false,
)
