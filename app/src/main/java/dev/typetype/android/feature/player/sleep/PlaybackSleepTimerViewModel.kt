package dev.typetype.android.feature.player.sleep

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.services.PlaybackSleepTimer
import javax.inject.Inject

@HiltViewModel
class PlaybackSleepTimerViewModel @Inject constructor(
    private val timer: PlaybackSleepTimer,
) : ViewModel() {
    val state = timer.state

    fun start(minutes: Int) = timer.start(minutes * 60_000L)

    fun stopAtEndOfVideo() = timer.stopAtEndOfVideo()

    fun cancel() = timer.cancel()
}
