package dev.typetype.android.feature.player.components

import android.os.Handler
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.MoreExecutors
import dev.typetype.android.domain.stream.Stream
import dev.typetype.android.services.MergedStreamMediaKeys
import dev.typetype.android.services.PlaybackAudioOnlyCommand

@Stable
@OptIn(markerClass = [UnstableApi::class])
internal class AudioOnlyPlaybackState(
    private val controller: MediaController,
    val available: Boolean,
) {
    var active by mutableStateOf(controller.currentAudioOnlyMode())
        private set
    var changing by mutableStateOf(false)
        private set
    var failure by mutableStateOf<AudioOnlyPlaybackFailure?>(null)
        private set

    fun setEnabled(enabled: Boolean) {
        if (!available || changing || active == enabled) return
        changing = true
        failure = null
        val future = controller.sendCustomCommand(
            PlaybackAudioOnlyCommand.command,
            PlaybackAudioOnlyCommand.arguments(enabled),
        )
        future.addListener(
            {
                val result = runCatching { future.get() }.getOrNull()
                Handler(controller.applicationLooper).post {
                    changing = false
                    if (result?.resultCode == SessionResult.RESULT_SUCCESS) {
                        synchronize()
                    } else {
                        failure = when (result?.resultCode) {
                            SessionError.ERROR_IO -> AudioOnlyPlaybackFailure.Network
                            else -> AudioOnlyPlaybackFailure.Unavailable
                        }
                    }
                }
            },
            MoreExecutors.directExecutor(),
        )
    }

    fun synchronize() {
        active = controller.currentAudioOnlyMode()
    }

    fun consumeFailure() {
        failure = null
    }
}

internal enum class AudioOnlyPlaybackFailure {
    Network,
    Unavailable,
}

@Composable
internal fun rememberAudioOnlyPlaybackState(
    controller: MediaController,
    stream: Stream,
): AudioOnlyPlaybackState {
    val available = !stream.isLive && !stream.isLiveContent &&
        controller.isSessionCommandAvailable(PlaybackAudioOnlyCommand.command)
    val state = remember(controller, stream.id, available) {
        AudioOnlyPlaybackState(controller, available)
    }
    DisposableEffect(controller, state) {
        val listener = object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                state.synchronize()
            }
        }
        controller.addListener(listener)
        state.synchronize()
        onDispose { controller.removeListener(listener) }
    }
    return state
}

private fun MediaController.currentAudioOnlyMode(): Boolean =
    currentMediaItem?.requestMetadata?.extras?.let {
        it.getBoolean(MergedStreamMediaKeys.EXTRA_AUDIO_ONLY_ACTIVE) ||
            it.getBoolean(MergedStreamMediaKeys.EXTRA_SABR_AUDIO_ONLY)
    } == true
