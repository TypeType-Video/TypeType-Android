package dev.typetype.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.core.ui.navigation.HomeRoute
import dev.typetype.android.core.ui.navigation.WelcomeRoute
import dev.typetype.android.domain.server.ServerRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class MainState(
    val isLoading: Boolean = true,
    val startRoute: Any? = null,
)

@HiltViewModel
class MainViewModel @Inject constructor(
    serverRepository: ServerRepository,
) : ViewModel() {

    val state: StateFlow<MainState> = serverRepository.observeCurrentServer()
        .map { current ->
            MainState(
                isLoading = false,
                startRoute = if (current != null) HomeRoute else WelcomeRoute,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MainState(),
        )
}
