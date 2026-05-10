package dev.typetype.android.feature.channel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.core.ui.navigation.ChannelRoute
import dev.typetype.android.domain.channel.ChannelRepository
import dev.typetype.android.domain.library.VideoMetaRepository
import dev.typetype.android.domain.library.cacheVideos
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChannelViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val channelRepository: ChannelRepository,
    private val videoMetaRepository: VideoMetaRepository,
) : ViewModel() {

    private val channelUrl = savedStateHandle.toRoute<ChannelRoute>().channelUrl

    private val _state = MutableStateFlow(ChannelState())
    val state = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    fun onAction(action: ChannelAction) {
        when (action) {
            ChannelAction.OnRefresh -> load()
        }
    }

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            channelRepository.loadChannel(channelUrl).fold(
                onSuccess = { channel ->
                    videoMetaRepository.cacheVideos(channel.videos)
                    _state.update { it.copy(isLoading = false, channel = channel) }
                },
                onFailure = { error ->
                    _state.update { it.copy(isLoading = false, errorMessage = error.message) }
                },
            )
        }
    }
}
