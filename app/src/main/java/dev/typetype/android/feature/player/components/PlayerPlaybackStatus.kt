package dev.typetype.android.feature.player.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import dev.typetype.android.services.isRecoverableSabrSessionFailure
import kotlinx.coroutines.delay

internal data class PlayerPlaybackStatus(
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

@Composable
internal fun rememberPlayerPlaybackStatus(player: Player): PlayerPlaybackStatus {
    var playbackState by remember(player) { mutableIntStateOf(player.playbackState) }
    var isPlaying by remember(player) { mutableStateOf(player.isPlaying) }
    var isLoading by remember(player) { mutableStateOf(player.isLoading) }
    var observedError by remember(player) { mutableStateOf(player.playerError) }
    var visibleError by remember(player) { mutableStateOf<PlaybackException?>(null) }
    var isRecovering by remember(player) { mutableStateOf(false) }

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
                observedError = errorValue
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    LaunchedEffect(player, observedError) {
        val candidate = observedError
        visibleError = null
        isRecovering = candidate?.isRecoverableSabrSessionFailure() == true
        if (isRecovering) delay(RECOVERABLE_FAILURE_GRACE_PERIOD_MS)
        if (player.playerError === candidate) {
            visibleError = candidate
            isRecovering = false
        }
    }

    return PlayerPlaybackStatus(
        playbackState = playbackState,
        isPlaying = isPlaying,
        isLoading = isLoading,
        error = visibleError,
        isRecovering = isRecovering,
    )
}

private const val RECOVERABLE_FAILURE_GRACE_PERIOD_MS = 10_000L
