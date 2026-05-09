package dev.typetype.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.core.ui.navigation.HomeRoute
import dev.typetype.android.core.ui.navigation.WelcomeRoute
import dev.typetype.android.data.network.AccessTokenStore
import dev.typetype.android.domain.auth.AuthRepository
import dev.typetype.android.domain.auth.SessionStatus
import dev.typetype.android.domain.preferences.AppPreferences
import dev.typetype.android.domain.preferences.PreferencesRepository
import dev.typetype.android.domain.server.ServerRepository
import dev.typetype.android.feature.player.host.PlayerHostController
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
    data class NavigateToLogin(val serverId: String) : MainEvent
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val serverRepository: ServerRepository,
    private val tokenStore: AccessTokenStore,
    private val authRepository: AuthRepository,
    val playerHostController: PlayerHostController,
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
            val startRoute = when {
                initial == null -> WelcomeRoute
                authRepository.validateSession() == SessionStatus.Invalid -> {
                    tokenStore.setAccessToken(null)
                    serverRepository.clearCurrentServer()
                    WelcomeRoute
                }
                else -> HomeRoute
            }
            _state.value = MainState(isLoading = false, startRoute = startRoute)
        }
    }

    fun signOut() {
        viewModelScope.launch {
            tokenStore.setAccessToken(null)
            val server = serverRepository.observeCurrentServer().first()
            if (server == null) {
                eventsChannel.send(MainEvent.NavigateToWelcome)
            } else {
                eventsChannel.send(MainEvent.NavigateToLogin(server.id))
            }
        }
    }
}
