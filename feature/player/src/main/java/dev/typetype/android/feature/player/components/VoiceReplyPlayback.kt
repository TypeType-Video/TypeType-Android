package dev.typetype.android.feature.player.components

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.ByteArrayDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal enum class VoiceReplyState { Idle, Loading, Playing, Paused, Error }

internal val LocalVoiceReplyPlayback = staticCompositionLocalOf<VoiceReplyPlayback?> { null }

@Stable
internal class VoiceReplyPlayback(
    private val context: Context,
    private val scope: CoroutineScope,
    private val loadAudio: suspend (String) -> Result<ByteArray>,
) {
    private var activeUrl by mutableStateOf<String?>(null)
    private var activeState by mutableStateOf(VoiceReplyState.Idle)
    var failure by mutableStateOf<Throwable?>(null)
        private set
    private var activeProgress by mutableFloatStateOf(0f)
    private var player: ExoPlayer? = null
    private var loadJob: Job? = null
    private var progressJob: Job? = null

    fun stateOf(url: String): VoiceReplyState = if (url == activeUrl) activeState else VoiceReplyState.Idle

    fun progressOf(url: String): Float = if (url == activeUrl) activeProgress else 0f

    fun toggle(url: String) {
        val current = player
        if (url != activeUrl || current == null || activeState == VoiceReplyState.Error) {
            start(url)
            return
        }
        when (activeState) {
            VoiceReplyState.Playing -> current.pause()
            VoiceReplyState.Paused -> current.play()
            VoiceReplyState.Idle, VoiceReplyState.Loading, VoiceReplyState.Error -> Unit
        }
    }

    fun pause() {
        player?.pause()
    }

    fun release() {
        loadJob?.cancel()
        progressJob?.cancel()
        player?.release()
        player = null
        activeUrl = null
        activeState = VoiceReplyState.Idle
    }

    private fun start(url: String) {
        loadJob?.cancel()
        player?.stop()
        activeUrl = url
        activeState = VoiceReplyState.Loading
        failure = null
        activeProgress = 0f
        loadJob = scope.launch {
            loadAudio(url).fold(
                onSuccess = { bytes -> if (activeUrl == url) play(bytes) },
                onFailure = {
                    if (activeUrl == url) {
                        failure = it
                        activeState = VoiceReplyState.Error
                    }
                },
            )
        }
    }

    @OptIn(UnstableApi::class)
    private fun play(bytes: ByteArray) {
        val exoPlayer = player ?: createPlayer().also { player = it }
        val source = ProgressiveMediaSource.Factory(DataSource.Factory { ByteArrayDataSource(bytes) })
            .createMediaSource(MediaItem.fromUri(VOICE_REPLY_URI))
        exoPlayer.setMediaSource(source)
        exoPlayer.prepare()
        exoPlayer.play()
    }

    // Like the web client, a voice reply plays over the video instead of taking audio focus from it.
    private fun createPlayer(): ExoPlayer = ExoPlayer.Builder(context)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                .build(),
            false,
        )
        .build()
        .also { it.addListener(listener) }

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (activeState == VoiceReplyState.Error) return
            activeState = if (isPlaying) VoiceReplyState.Playing else VoiceReplyState.Paused
            if (isPlaying) trackProgress() else progressJob?.cancel()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState != Player.STATE_ENDED) return
            player?.run {
                pause()
                seekTo(0)
            }
            activeProgress = 0f
        }

        override fun onPlayerError(error: PlaybackException) {
            progressJob?.cancel()
            activeState = VoiceReplyState.Error
        }
    }

    private fun trackProgress() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                val current = player ?: break
                val duration = current.duration
                activeProgress = if (duration > 0) {
                    (current.currentPosition.toFloat() / duration).coerceIn(0f, 1f)
                } else {
                    0f
                }
                delay(PROGRESS_INTERVAL_MS)
            }
        }
    }
}

@Composable
internal fun rememberVoiceReplyPlayback(loadAudio: suspend (String) -> Result<ByteArray>): VoiceReplyPlayback {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val currentLoadAudio by rememberUpdatedState(loadAudio)
    val playback = remember(context, scope) {
        VoiceReplyPlayback(context, scope) { url -> currentLoadAudio(url) }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(playback, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) playback.pause()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            playback.release()
        }
    }
    return playback
}

private val VOICE_REPLY_URI: Uri = "typetype://comment-voice-reply".toUri()
private const val PROGRESS_INTERVAL_MS = 200L
