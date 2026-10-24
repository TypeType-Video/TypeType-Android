package dev.typetype.android.services

import android.os.SystemClock
import androidx.media3.common.Player
import dev.typetype.android.domain.playback.PlaybackQueueAutoplayCountdown
import dev.typetype.android.domain.playback.PlaybackQueueState
import dev.typetype.android.domain.preferences.PreferencesRepository
import dev.typetype.android.domain.usersettings.UserSettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Singleton
internal class PlaybackQueueAutoplayController @Inject constructor(
    preferencesRepository: PreferencesRepository,
    userSettingsRepository: UserSettingsRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutableCountdown = MutableStateFlow<PlaybackQueueAutoplayCountdown?>(null)
    val countdown: StateFlow<PlaybackQueueAutoplayCountdown?> = mutableCountdown.asStateFlow()

    private val advanceChannel = Channel<Unit>(Channel.CONFLATED)
    val advanceRequests: Flow<Unit> = advanceChannel.receiveAsFlow()

    private var policy = PlaybackQueueAutoplayPolicy()
    private var playbackState = Player.STATE_IDLE
    private var playWhenReady = false
    private var currentMediaId: String? = null
    private var nextVideoUrl: String? = null
    private var dismissedMediaId: String? = null
    private var advanceRequestedMediaId: String? = null
    private var countdownJob: Job? = null

    init {
        scope.launch {
            combine(
                preferencesRepository.observe(),
                userSettingsRepository.observe(),
            ) { preferences, settings ->
                PlaybackQueueAutoplayPolicy(
                    enabled = settings.autoplay,
                    countdownSeconds = preferences.playerAutoplayCountdownSeconds,
                    skipCountdown = settings.skipPlaylistAutoplayScreen,
                )
            }
                .distinctUntilChanged()
                .collect {
                    policy = it
                    evaluate()
                }
        }
    }

    fun updatePlaybackContext(
        playbackState: Int,
        playWhenReady: Boolean,
        currentMediaId: String?,
        nextVideoUrl: String?,
    ) {
        this.playbackState = playbackState
        this.playWhenReady = playWhenReady
        this.currentMediaId = currentMediaId
        this.nextVideoUrl = nextVideoUrl
        if (playbackState != Player.STATE_ENDED) {
            dismissedMediaId = null
            advanceRequestedMediaId = null
        }
        evaluate()
    }

    fun onMediaTransition() {
        dismissedMediaId = null
        advanceRequestedMediaId = null
        clearCountdown()
    }

    fun playNow() {
        requestAdvance()
    }

    fun cancel() {
        dismissedMediaId = currentMediaId
        clearCountdown()
    }

    fun togglePause() {
        val current = mutableCountdown.value ?: return
        mutableCountdown.value = current.copy(paused = !current.paused)
    }

    private fun evaluate() {
        val currentId = currentMediaId
        val targetUrl = nextVideoUrl
        val decision = decideQueueAutoplay(
            playbackState = playbackState,
            playWhenReady = playWhenReady,
            enabled = policy.enabled,
            countdownSeconds = policy.countdownSeconds,
            skipCountdown = policy.skipCountdown,
            currentMediaId = currentId,
            nextVideoUrl = targetUrl,
            dismissedMediaId = dismissedMediaId,
        )
        when (decision) {
            QueueAutoplayDecision.None -> {
                clearCountdown()
                return
            }
            QueueAutoplayDecision.AdvanceImmediately -> {
                requestAdvance()
                return
            }
            QueueAutoplayDecision.StartCountdown -> Unit
        }
        if (advanceRequestedMediaId == currentId) return
        val totalMillis = policy.countdownSeconds * 1_000L
        val current = mutableCountdown.value
        if (
            current != null &&
            current.targetVideoUrl == targetUrl &&
            current.totalMillis == totalMillis
        ) {
            return
        }
        startCountdown(requireNotNull(targetUrl), totalMillis)
    }

    private fun startCountdown(targetVideoUrl: String, totalMillis: Long) {
        clearCountdown()
        mutableCountdown.value = PlaybackQueueAutoplayCountdown(
            targetVideoUrl = targetVideoUrl,
            totalMillis = totalMillis,
            remainingMillis = totalMillis,
            paused = false,
        )
        countdownJob = scope.launch {
            var previousTick = SystemClock.elapsedRealtime()
            while (isActive) {
                delay(COUNTDOWN_TICK_MILLIS)
                val current = mutableCountdown.value ?: return@launch
                val now = SystemClock.elapsedRealtime()
                if (current.paused) {
                    previousTick = now
                    continue
                }
                val remaining = (current.remainingMillis - (now - previousTick)).coerceAtLeast(0L)
                previousTick = now
                if (remaining == 0L) {
                    requestAdvance()
                    return@launch
                }
                mutableCountdown.value = current.copy(remainingMillis = remaining)
            }
        }
    }

    private fun requestAdvance() {
        val currentId = currentMediaId ?: return
        if (nextVideoUrl.isNullOrBlank() || advanceRequestedMediaId == currentId) return
        advanceRequestedMediaId = currentId
        clearCountdown()
        advanceChannel.trySend(Unit)
    }

    private fun clearCountdown() {
        countdownJob?.cancel()
        countdownJob = null
        mutableCountdown.value = null
    }
}

internal data class PlaybackQueueAutoplayPolicy(
    val enabled: Boolean = true,
    val countdownSeconds: Int = 10,
    val skipCountdown: Boolean = false,
)

internal enum class QueueAutoplayDecision {
    None,
    StartCountdown,
    AdvanceImmediately,
}

internal fun decideQueueAutoplay(
    playbackState: Int,
    playWhenReady: Boolean,
    enabled: Boolean,
    countdownSeconds: Int,
    skipCountdown: Boolean,
    currentMediaId: String?,
    nextVideoUrl: String?,
    dismissedMediaId: String?,
): QueueAutoplayDecision {
    val eligible = playbackState == Player.STATE_ENDED &&
        playWhenReady &&
        enabled &&
        !currentMediaId.isNullOrBlank() &&
        !nextVideoUrl.isNullOrBlank() &&
        currentMediaId != dismissedMediaId
    if (!eligible) return QueueAutoplayDecision.None
    return if (skipCountdown || countdownSeconds <= 0) {
        QueueAutoplayDecision.AdvanceImmediately
    } else {
        QueueAutoplayDecision.StartCountdown
    }
}

internal fun PlaybackQueueAutoplayController.updateFrom(
    player: Player?,
    queue: PlaybackQueueState,
    playbackState: Int = player?.playbackState ?: Player.STATE_IDLE,
) {
    updatePlaybackContext(
        playbackState = playbackState,
        playWhenReady = player?.playWhenReady == true,
        currentMediaId = player?.currentMediaItem?.mediaId,
        nextVideoUrl = queue.next?.videoUrl,
    )
}

private const val COUNTDOWN_TICK_MILLIS = 100L
