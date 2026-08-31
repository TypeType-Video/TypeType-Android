package video.typetype.tv.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

internal class AutoplayCountdownController(
    private val playNext: () -> Unit,
) {
    var active: Boolean by mutableStateOf(false)
        private set
    var paused: Boolean by mutableStateOf(false)
        private set
    var remainingSeconds: Int by mutableIntStateOf(0)
        private set
    var totalSeconds: Int by mutableIntStateOf(DEFAULT_AUTOPLAY_SECONDS)
        private set
    private var enabled: Boolean = false

    fun configure(enabled: Boolean, seconds: Int) {
        this.enabled = enabled
        totalSeconds = seconds.coerceIn(0, MAX_AUTOPLAY_SECONDS)
        if (!enabled) cancel()
    }

    fun onPlaybackEnded() {
        if (!enabled) return
        if (totalSeconds == 0) {
            playNow()
            return
        }
        remainingSeconds = totalSeconds
        paused = false
        active = true
    }

    fun tick() {
        if (!active || paused) return
        remainingSeconds = (remainingSeconds - 1).coerceAtLeast(0)
        if (remainingSeconds == 0) playNow()
    }

    fun togglePause() {
        if (active) paused = !paused
    }

    fun cancel() {
        active = false
        paused = false
        remainingSeconds = totalSeconds
    }

    fun playNow() {
        if (!enabled) return
        active = false
        paused = false
        playNext()
    }
}

@Composable
internal fun rememberAutoplayCountdown(
    playbackKey: String,
    enabled: Boolean,
    seconds: Int,
    onPlayNext: () -> Unit,
): AutoplayCountdownController {
    val latestPlayNext by rememberUpdatedState(onPlayNext)
    val controller = remember(playbackKey) { AutoplayCountdownController { latestPlayNext() } }
    LaunchedEffect(controller, enabled, seconds) {
        controller.configure(enabled, seconds)
    }
    LaunchedEffect(controller.active, controller.paused, controller.remainingSeconds) {
        if (controller.active && !controller.paused && controller.remainingSeconds > 0) {
            delay(1_000L)
            controller.tick()
        }
    }
    return controller
}

private const val DEFAULT_AUTOPLAY_SECONDS = 10
private const val MAX_AUTOPLAY_SECONDS = 60
