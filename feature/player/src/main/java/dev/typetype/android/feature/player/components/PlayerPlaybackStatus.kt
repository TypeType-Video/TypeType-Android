package dev.typetype.android.feature.player.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import dev.typetype.android.services.PlaybackRecoveryState
import dev.typetype.android.services.isRecovering
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class PlayerPlaybackStatus(
    val playbackState: Int,
    val isPlaying: Boolean,
    val isLoading: Boolean,
    val error: PlaybackException?,
    val isRecovering: Boolean,
) {
    val isBuffering: Boolean
        get() = playbackState == Player.STATE_BUFFERING || isRecovering

    val acceptsInput: Boolean
        get() = error == null && !isRecovering
}

// What the playback service reports about a failed item; the app root provides the service's status.
val LocalPlaybackRecovery = staticCompositionLocalOf<StateFlow<PlaybackRecoveryState>> { NO_RECOVERY }

private val NO_RECOVERY: StateFlow<PlaybackRecoveryState> = MutableStateFlow(PlaybackRecoveryState.Idle)

// While the service retries a failed item the player shows buffering; its error appears only once the service
// gave up or the failure cannot be retried.
@Composable
fun rememberPlayerPlaybackStatus(player: Player): PlayerPlaybackStatus {
    var playbackState by remember(player) { mutableIntStateOf(player.playbackState) }
    var isPlaying by remember(player) { mutableStateOf(player.isPlaying) }
    var isLoading by remember(player) { mutableStateOf(player.isLoading) }
    var error by remember(player) { mutableStateOf(player.playerError) }
    var mediaId by remember(player) { mutableStateOf(player.currentMediaItem?.mediaId) }
    val recovery by LocalPlaybackRecovery.current.collectAsState()

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackStateValue: Int) {
                playbackState = playbackStateValue
            }

            override fun onIsPlayingChanged(isPlayingValue: Boolean) {
                isPlaying = isPlayingValue
            }

            override fun onIsLoadingChanged(isLoadingValue: Boolean) {
                isLoading = isLoadingValue
            }

            override fun onPlayerErrorChanged(errorValue: PlaybackException?) {
                error = errorValue
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                mediaId = mediaItem?.mediaId
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    val isRecovering = recovery.isRecovering(mediaId)
    return PlayerPlaybackStatus(
        playbackState = playbackState,
        isPlaying = isPlaying,
        isLoading = isLoading,
        error = error.takeUnless { isRecovering },
        isRecovering = isRecovering,
    )
}
