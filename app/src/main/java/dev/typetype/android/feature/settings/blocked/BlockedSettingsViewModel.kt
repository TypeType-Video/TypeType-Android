package dev.typetype.android.feature.settings.blocked

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.domain.actions.BlockedItem
import dev.typetype.android.domain.actions.VideoActionsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BlockedState(
    val channels: List<BlockedItem> = emptyList(),
    val videos: List<BlockedItem> = emptyList(),
)

@HiltViewModel
class BlockedSettingsViewModel @Inject constructor(
    private val videoActionsRepository: VideoActionsRepository,
) : ViewModel() {

    val state = combine(
        videoActionsRepository.observeBlockedChannels(),
        videoActionsRepository.observeBlockedVideos(),
    ) { channels, videos ->
        BlockedState(channels = channels, videos = videos)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BlockedState())

    init {
        viewModelScope.launch { videoActionsRepository.refreshBlocked() }
    }

    fun unblockChannel(url: String) {
        viewModelScope.launch { videoActionsRepository.unblockChannel(url) }
    }

    fun unblockVideo(url: String) {
        viewModelScope.launch { videoActionsRepository.unblockVideo(url) }
    }
}
