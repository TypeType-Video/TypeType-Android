package dev.typetype.android.feature.player.queue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.domain.usersettings.UserSettingsRepository
import dev.typetype.android.services.PlaybackQueueCoordinator
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class PlaybackQueueViewModel @Inject constructor(
    private val coordinator: PlaybackQueueCoordinator,
    userSettingsRepository: UserSettingsRepository,
) : ViewModel() {
    val hideUpcomingByDefault: StateFlow<Boolean> = userSettingsRepository.observe()
        .map { it.hideUpcomingPlaylistVideos }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun play(index: Int) = coordinator.play(index)

    fun retryNext() = coordinator.retryNext()

    fun playNext(index: Int) = coordinator.playNext(index)

    fun remove(index: Int) = coordinator.remove(index)

    fun shuffleUpcoming() = coordinator.shuffleUpcoming()

    fun cycleRepeatMode() = coordinator.cycleRepeatMode()
}
