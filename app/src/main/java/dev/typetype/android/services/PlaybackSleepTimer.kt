package dev.typetype.android.services

import android.os.SystemClock
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import dev.typetype.android.domain.playback.PlaybackSleepTimerMode
import dev.typetype.android.domain.playback.PlaybackSleepTimerState
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Singleton
@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
class PlaybackSleepTimer @Inject constructor() : Player.Listener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutableState = MutableStateFlow(PlaybackSleepTimerState())
    val state: StateFlow<PlaybackSleepTimerState> = mutableState.asStateFlow()

    private var player: ExoPlayer? = null
    private var timerJob: Job? = null

    fun attach(player: ExoPlayer) {
        if (this.player === player) return
        this.player?.let {
            it.setPauseAtEndOfMediaItems(false)
            it.removeListener(this@PlaybackSleepTimer)
        }
        this.player = player
        player.addListener(this)
    }

    fun detach(player: ExoPlayer) {
        if (this.player !== player) return
        player.setPauseAtEndOfMediaItems(false)
        player.removeListener(this)
        this.player = null
        cancel()
    }

    fun start(durationMillis: Long) {
        if (player == null || durationMillis !in 1L..MAX_DURATION_MILLIS) return
        timerJob?.cancel()
        player?.setPauseAtEndOfMediaItems(false)
        val deadline = SystemClock.elapsedRealtime() + durationMillis
        mutableState.value = PlaybackSleepTimerState(
            mode = PlaybackSleepTimerMode.Timed,
            durationMillis = durationMillis,
            remainingMillis = durationMillis,
        )
        timerJob = scope.launch {
            while (true) {
                val remaining = (deadline - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
                mutableState.value = mutableState.value.copy(remainingMillis = remaining)
                if (remaining == 0L) {
                    player?.pause()
                    finish()
                    break
                }
                delay(minOf(TICK_MILLIS, remaining))
            }
        }
    }

    fun stopAtEndOfVideo() {
        val currentPlayer = player?.takeIf { it.currentMediaItem != null } ?: return
        timerJob?.cancel()
        currentPlayer.setPauseAtEndOfMediaItems(true)
        mutableState.value = PlaybackSleepTimerState(mode = PlaybackSleepTimerMode.EndOfVideo)
        if (currentPlayer.playbackState == Player.STATE_ENDED) {
            currentPlayer.pause()
            finish()
        }
    }

    fun cancel() {
        timerJob?.cancel()
        player?.setPauseAtEndOfMediaItems(false)
        mutableState.value = PlaybackSleepTimerState()
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        if (
            !playWhenReady &&
            reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM &&
            state.value.mode == PlaybackSleepTimerMode.EndOfVideo
        ) {
            finish()
        }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        if (
            reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO &&
            state.value.mode == PlaybackSleepTimerMode.EndOfVideo
        ) {
            player?.pause()
            finish()
        }
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        if (playbackState == Player.STATE_ENDED && state.value.mode == PlaybackSleepTimerMode.EndOfVideo) {
            player?.pause()
            finish()
        }
    }

    private fun finish() {
        timerJob?.cancel()
        player?.setPauseAtEndOfMediaItems(false)
        mutableState.value = PlaybackSleepTimerState()
    }

    private companion object {
        const val TICK_MILLIS = 1_000L
        const val MAX_DURATION_MILLIS = 24L * 60L * 60L * 1_000L
    }
}
