package dev.typetype.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.core.ui.navigation.HomeRoute
import dev.typetype.android.core.ui.navigation.WelcomeRoute
import dev.typetype.android.data.network.AccessTokenStore
import dev.typetype.android.domain.preferences.AppPreferences
import dev.typetype.android.domain.preferences.PreferencesRepository
import dev.typetype.android.domain.server.ServerRepository
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MainState(
    val isLoading: Boolean = true,
    val startRoute: Any? = null,
)

sealed interface MainEvent {
    data object NavigateToWelcome : MainEvent
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val serverRepository: ServerRepository,
    private val tokenStore: AccessTokenStore,
    preferencesRepository: PreferencesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(MainState())
    val state: StateFlow<MainState> = _state.asStateFlow()

    val preferences: StateFlow<AppPreferences> = preferencesRepository.observe()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = AppPreferences(),
        )

    private val eventsChannel = Channel<MainEvent>(Channel.BUFFERED)
    val events = eventsChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            val initial = serverRepository.observeCurrentServer().first()
            _state.value = MainState(
                isLoading = false,
                startRoute = if (initial != null) HomeRoute else WelcomeRoute,
            )
        }
    }

    fun signOut() {
        viewModelScope.launch {
            tokenStore.setAccessToken(null)
            serverRepository.clearCurrentServer()
            eventsChannel.send(MainEvent.NavigateToWelcome)
        }
    }
}
