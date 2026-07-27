package dev.typetype.android.feature.player.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player

internal data class PlayerPlaybackStatus(
    val playbackState: Int,
    val isPlaying: Boolean,
    val isLoading: Boolean,
    val error: PlaybackException?,
) {
    val isBuffering: Boolean
        get() = playbackState == Player.STATE_BUFFERING
}

@Composable
internal fun rememberPlayerPlaybackStatus(player: Player): PlayerPlaybackStatus {
    var playbackState by remember(player) { mutableIntStateOf(player.playbackState) }
    var isPlaying by remember(player) { mutableStateOf(player.isPlaying) }
    var isLoading by remember(player) { mutableStateOf(player.isLoading) }
    var error by remember(player) { mutableStateOf(player.playerError) }

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
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    return PlayerPlaybackStatus(
        playbackState = playbackState,
        isPlaying = isPlaying,
        isLoading = isLoading,
        error = error,
    )
}
