package dev.typetype.android.feature.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.core.ui.navigation.PlayerRoute
import dev.typetype.android.domain.stream.StreamRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val streamRepository: StreamRepository,
) : ViewModel() {

    private val route: PlayerRoute = savedStateHandle.toRoute<PlayerRoute>()

    private val _state = MutableStateFlow(PlayerState())
    val state = _state.asStateFlow()

    init {
        loadStream()
    }

    private fun loadStream() {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            streamRepository.loadStream(route.videoUrl).fold(
                onSuccess = { stream ->
                    _state.update { it.copy(isLoading = false, stream = stream, errorMessage = null) }
                },
                onFailure = { throwable ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: "Could not load stream",
                        )
                    }
                },
            )
        }
    }
}
