package dev.typetype.android.feature.player.queue

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.services.PlaybackQueueCoordinator
import javax.inject.Inject

@HiltViewModel
class PlaybackQueueViewModel @Inject constructor(
    private val coordinator: PlaybackQueueCoordinator,
) : ViewModel() {
    fun play(index: Int) = coordinator.play(index)

    fun retryNext() = coordinator.retryNext()

    fun playNext(index: Int) = coordinator.playNext(index)

    fun remove(index: Int) = coordinator.remove(index)

    fun shuffleUpcoming() = coordinator.shuffleUpcoming()

    fun cycleRepeatMode() = coordinator.cycleRepeatMode()
}
