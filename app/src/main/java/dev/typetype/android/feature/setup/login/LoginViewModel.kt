package dev.typetype.android.feature.setup.login

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.typetype.android.R
import dev.typetype.android.core.ui.error.UserErrorMapper
import dev.typetype.android.core.ui.navigation.LoginRoute
import dev.typetype.android.domain.auth.AuthRepository
import dev.typetype.android.domain.server.ServerRepository
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
    @ApplicationContext private val context: Context,
    private val errorMapper: UserErrorMapper,
    private val authRepository: AuthRepository,
    private val serverRepository: ServerRepository,
) : ViewModel() {

    private val route: LoginRoute = savedStateHandle.toRoute<LoginRoute>()
    private val serverId: String = route.serverId

    private val _state = MutableStateFlow(LoginState())
    val state = _state.asStateFlow()

    private val eventsChannel = Channel<LoginEvent>(Channel.BUFFERED)
    val events = eventsChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            val server = serverRepository.getServer(serverId)
            _state.update { current ->
                if (server == null) {
                    current.copy(
                        isLoadingMethods = false,
                        errorMessage = context.getString(R.string.login_instance_not_found),
                    )
                } else {
                    current.copy(
                        isLoadingMethods = false,
                        instanceName = server.displayName,
                        localLoginEnabled = server.localLoginEnabled,
                        guestAllowed = server.guestAllowed,
                        registrationAllowed = server.registrationAllowed,
                        oidcEnabled = server.oidcEnabled,
                        oidcProviderName = server.oidcProviderName,
                        oidcAutoRedirect = server.oidcAutoRedirect,
                    )
                }
            }
        }
    }

    fun onAction(action: LoginAction) {
        when (action) {
            is LoginAction.OnIdentifierChange -> _state.update {
                it.copy(identifier = action.value, errorMessage = null, errorRequestId = null)
            }
            is LoginAction.OnPasswordChange -> _state.update {
                it.copy(password = action.value, errorMessage = null, errorRequestId = null)
            }
            LoginAction.OnLoginClick -> performLogin()
            LoginAction.OnOidcClick -> startOidc()
            is LoginAction.OnOidcCallback -> finishOidc(action.callbackUrl)
            LoginAction.OnOidcBrowserUnavailable -> _state.update {
                it.copy(
                    isSubmitting = false,
                    errorMessage = context.getString(R.string.login_browser_required),
                    errorRequestId = null,
                )
            }
            LoginAction.OnOidcCancelled -> {
                _state.update {
                    it.copy(isSubmitting = false, errorMessage = null, errorRequestId = null)
                }
                viewModelScope.launch { authRepository.cancelOidc(serverId) }
            }
            LoginAction.OnContinueAsGuestClick -> performGuest()
            LoginAction.OnResetPasswordClick -> emit(
                LoginEvent.NavigateToResetPassword(serverId),
            )
            LoginAction.OnBackClick -> emit(LoginEvent.NavigateBack)
        }
    }

    private fun performLogin() {
        val current = _state.value
        if (!current.localLoginEnabled) return
        if (current.identifier.isBlank() || current.password.isBlank()) {
            _state.update {
                it.copy(
                    errorMessage = context.getString(R.string.login_fields_required),
                    errorRequestId = null,
                )
            }
            return
        }
        _state.update {
            it.copy(isSubmitting = true, errorMessage = null, errorRequestId = null)
        }
        viewModelScope.launch {
            authRepository
                .loginWithCredentials(serverId, current.identifier, current.password)
                .fold(
                    onSuccess = {
                        _state.update { it.copy(isSubmitting = false) }
                        emit(LoginEvent.NavigateToHome)
                    },
                    onFailure = { throwable ->
                        val details = errorMapper.authenticationDetails(
                            failure = throwable,
                            fallbackRes = R.string.login_failed,
                            rejectedRes = R.string.login_credentials_rejected,
                        )
                        _state.update {
                            it.copy(
                                isSubmitting = false,
                                errorMessage = details.message,
                                errorRequestId = details.requestId,
                            )
                        }
                    },
                )
        }
    }

    private fun performGuest() {
        _state.update {
            it.copy(isSubmitting = true, errorMessage = null, errorRequestId = null)
        }
        viewModelScope.launch {
            authRepository.loginAsGuest(serverId).fold(
                onSuccess = {
                    _state.update { it.copy(isSubmitting = false) }
                    emit(LoginEvent.NavigateToHome)
                },
                onFailure = { throwable ->
                    val details = errorMapper.authenticationDetails(
                        throwable,
                        R.string.login_guest_failed,
                    )
                    _state.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = details.message,
                            errorRequestId = details.requestId,
                        )
                    }
                },
            )
        }
    }

    private fun startOidc() {
        if (!_state.value.oidcEnabled) return
        _state.update {
            it.copy(isSubmitting = true, errorMessage = null, errorRequestId = null)
        }
        viewModelScope.launch {
            authRepository.startOidc(serverId).fold(
                onSuccess = { authorization ->
                    emit(
                        LoginEvent.LaunchOidc(
                            authorizationUrl = authorization.authorizationUrl,
                            redirectScheme = authorization.redirectScheme,
                        ),
                    )
                },
                onFailure = { throwable ->
                    val details = errorMapper.authenticationDetails(
                        throwable,
                        R.string.login_oidc_start_failed,
                    )
                    _state.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = details.message,
                            errorRequestId = details.requestId,
                        )
                    }
                },
            )
        }
    }

    private fun finishOidc(callbackUrl: String) {
        viewModelScope.launch {
            authRepository.finishOidc(serverId, callbackUrl).fold(
                onSuccess = {
                    _state.update { it.copy(isSubmitting = false) }
                    emit(LoginEvent.NavigateToHome)
                },
                onFailure = { throwable ->
                    val details = errorMapper.authenticationDetails(
                        throwable,
                        R.string.login_oidc_finish_failed,
                    )
                    _state.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = details.message,
                            errorRequestId = details.requestId,
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
