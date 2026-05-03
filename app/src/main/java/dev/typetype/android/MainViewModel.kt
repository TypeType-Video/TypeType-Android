package dev.typetype.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.core.ui.navigation.HomeRoute
import dev.typetype.android.core.ui.navigation.WelcomeRoute
import dev.typetype.android.domain.server.ServerRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class MainState(
    val isLoading: Boolean = true,
    val startRoute: Any? = null,
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val serverRepository: ServerRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(MainState())
    val state: StateFlow<MainState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val initial = serverRepository.observeCurrentServer().first()
            _state.value = MainState(
                isLoading = false,
                startRoute = if (initial != null) HomeRoute else WelcomeRoute,
            )
        }
    }
}
