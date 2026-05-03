package dev.typetype.android.feature.setup.login

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.core.ui.navigation.LoginRoute
import dev.typetype.android.domain.auth.AuthRepository
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class LoginViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val serverId: String = savedStateHandle.toRoute<LoginRoute>().serverId

    private val _state = MutableStateFlow(LoginState())
    val state = _state.asStateFlow()

    private val eventsChannel = Channel<LoginEvent>(Channel.BUFFERED)
    val events = eventsChannel.receiveAsFlow()

    fun onAction(action: LoginAction) {
        when (action) {
            is LoginAction.OnIdentifierChange -> _state.update {
                it.copy(identifier = action.value, errorMessage = null)
            }
            is LoginAction.OnPasswordChange -> _state.update {
                it.copy(password = action.value, errorMessage = null)
            }
            LoginAction.OnLoginClick -> performLogin()
            LoginAction.OnContinueAsGuestClick -> performGuest()
            LoginAction.OnBackClick -> emit(LoginEvent.NavigateBack)
        }
    }

    private fun performLogin() {
        val current = _state.value
        if (current.identifier.isBlank() || current.password.isBlank()) {
            _state.update { it.copy(errorMessage = "Both fields are required") }
            return
        }
        _state.update { it.copy(isSubmitting = true, errorMessage = null) }
        viewModelScope.launch {
            authRepository
                .loginWithCredentials(serverId, current.identifier, current.password)
                .fold(
                    onSuccess = {
                        _state.update { it.copy(isSubmitting = false) }
                        emit(LoginEvent.NavigateToHome)
                    },
                    onFailure = { throwable ->
                        _state.update {
                            it.copy(
                                isSubmitting = false,
                                errorMessage = throwable.message ?: "Login failed",
                            )
                        }
                    },
                )
        }
    }

    private fun performGuest() {
        _state.update { it.copy(isSubmitting = true, errorMessage = null) }
        viewModelScope.launch {
            authRepository.loginAsGuest(serverId).fold(
                onSuccess = {
                    _state.update { it.copy(isSubmitting = false) }
                    emit(LoginEvent.NavigateToHome)
                },
                onFailure = { throwable ->
                    _state.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = throwable.message ?: "Guest login failed",
                        )
                    }
                },
            )
        }
    }

    private fun emit(event: LoginEvent) {
        viewModelScope.launch { eventsChannel.send(event) }
    }
}
