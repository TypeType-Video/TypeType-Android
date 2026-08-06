package dev.typetype.android.feature.player.components

import android.os.Handler
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.delay

@Stable
@OptIn(markerClass = [UnstableApi::class])
internal class AudioOnlyPlaybackState(
    private val controller: MediaController,
    private val playbackEligible: Boolean,
    private val defaultGate: AudioOnlyDefaultGate,
) {
    var available by mutableStateOf(
        playbackEligible && controller.isSessionCommandAvailable(PlaybackAudioOnlyCommand.command),
    )
        private set
    var active by mutableStateOf(controller.currentAudioOnlyMode())
        private set
    var changing by mutableStateOf(false)
        private set
    var failure by mutableStateOf<AudioOnlyPlaybackFailure?>(null)
        private set

    fun setEnabled(enabled: Boolean, defaultRequest: Boolean = false) {
        if (!available || changing || active == enabled) return
        changing = true
        failure = null
        val future = controller.sendCustomCommand(
            PlaybackAudioOnlyCommand.command,
            PlaybackAudioOnlyCommand.arguments(enabled, defaultRequest),
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
        available = playbackEligible &&
            controller.isSessionCommandAvailable(PlaybackAudioOnlyCommand.command)
        active = controller.currentAudioOnlyMode()
        if (
            defaultGate.shouldEnable(
                mediaId = controller.currentMediaItem?.mediaId,
                available = available,
                active = active,
                ready = controller.playbackState == Player.STATE_READY,
            )
        ) {
            setEnabled(true, defaultRequest = true)
        }
    }

    fun consumeFailure() {
        failure = null
    }
}

internal enum class AudioOnlyPlaybackFailure {
    Network,
    Unavailable,
}

internal data class AudioOnlyPlaybackDefault(
    val mediaId: String,
    val enabled: Boolean?,
)

internal class AudioOnlyDefaultGate(
    private val default: AudioOnlyPlaybackDefault,
) {
    private var resolved = false

    fun shouldEnable(
        mediaId: String?,
        available: Boolean,
        active: Boolean,
        ready: Boolean,
    ): Boolean {
        val enabled = default.enabled ?: return false
        if (resolved || !available || !ready || mediaId != default.mediaId) return false
        resolved = true
        return enabled && !active
    }
}

@Composable
internal fun rememberAudioOnlyPlaybackState(
    controller: MediaController,
    stream: Stream,
    default: AudioOnlyPlaybackDefault,
): AudioOnlyPlaybackState {
    val playbackEligible = !stream.isLive && !stream.isLiveContent
    val state = remember(controller, stream.id, playbackEligible, default.mediaId, default.enabled) {
        AudioOnlyPlaybackState(controller, playbackEligible, AudioOnlyDefaultGate(default))
    }
    DisposableEffect(controller, state) {
        val listener = object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                state.synchronize()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                state.synchronize()
            }
        }
        controller.addListener(listener)
        state.synchronize()
        onDispose { controller.removeListener(listener) }
    }
    LaunchedEffect(controller, state) {
        repeat(DEFAULT_SYNCHRONIZATION_ATTEMPTS) {
            state.synchronize()
            delay(DEFAULT_SYNCHRONIZATION_INTERVAL_MS)
        }
    }
    return state
}

private fun MediaController.currentAudioOnlyMode(): Boolean =
    currentMediaItem?.requestMetadata?.extras?.let {
        it.getBoolean(MergedStreamMediaKeys.EXTRA_AUDIO_ONLY_ACTIVE) ||
            it.getBoolean(MergedStreamMediaKeys.EXTRA_SABR_AUDIO_ONLY)
    } == true

private const val DEFAULT_SYNCHRONIZATION_ATTEMPTS = 50
private const val DEFAULT_SYNCHRONIZATION_INTERVAL_MS = 100L
