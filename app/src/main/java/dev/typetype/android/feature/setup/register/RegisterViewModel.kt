package dev.typetype.android.feature.setup.register

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.typetype.android.R
import dev.typetype.android.core.ui.error.UserErrorMapper
import dev.typetype.android.core.ui.navigation.RegisterRoute
import dev.typetype.android.domain.auth.AuthRepository
import dev.typetype.android.domain.auth.LoginMethods
import dev.typetype.android.domain.auth.OidcCallbackRelay
import dev.typetype.android.domain.auth.RegistrationStatus
import dev.typetype.android.domain.server.ServerRepository
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class RegisterViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val errorMapper: UserErrorMapper,
    private val authRepository: AuthRepository,
    private val oidcCallbackRelay: OidcCallbackRelay,
    private val serverRepository: ServerRepository,
) : ViewModel() {
    private val serverId = savedStateHandle.toRoute<RegisterRoute>().serverId
    private val _state = MutableStateFlow(RegisterState())
    val state = _state.asStateFlow()
    private val eventsChannel = Channel<RegisterEvent>(Channel.BUFFERED)
    val events = eventsChannel.receiveAsFlow()
    private var oidcCallbackInProgress = false

    init {
        viewModelScope.launch {
            oidcCallbackRelay.callbacks.collect { callbackUrl ->
                onAction(RegisterAction.OnOidcCallback(callbackUrl))
                oidcCallbackRelay.markConsumed(callbackUrl)
            }
        }
        loadOptions()
    }

    fun onAction(action: RegisterAction) {
        when (action) {
            is RegisterAction.OnNameChange -> updateInput { copy(name = action.value) }
            is RegisterAction.OnEmailChange -> updateInput { copy(email = action.value) }
            is RegisterAction.OnPasswordChange -> updateInput { copy(password = action.value) }
            RegisterAction.OnRegisterClick -> register()
            RegisterAction.OnOidcClick -> startOidc()
            is RegisterAction.OnOidcCallback -> finishOidc(action.callbackUrl)
            RegisterAction.OnOidcBrowserUnavailable -> _state.update {
                it.copy(
                    isSubmitting = false,
                    errorMessage = context.getString(R.string.login_browser_required),
                    errorRequestId = null,
                )
            }
            RegisterAction.OnOidcCancelled -> cancelOidc()
            RegisterAction.OnRetryClick -> loadOptions()
            RegisterAction.OnBackClick -> emit(RegisterEvent.NavigateBack)
        }
    }

    private fun loadOptions() {
        _state.update {
            it.copy(isLoading = true, errorMessage = null, errorRequestId = null)
        }
        viewModelScope.launch {
            val server = serverRepository.getServer(serverId)
            if (server == null) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = context.getString(R.string.login_instance_not_found),
                    )
                }
                return@launch
            }
            _state.update {
                it.copy(
                    instanceName = server.displayName,
                    localLoginEnabled = server.localLoginEnabled,
                    oidcEnabled = server.oidcEnabled,
                    oidcProviderName = server.oidcProviderName,
                )
            }
            val registration = async { authRepository.getRegistrationStatus(serverId) }
            val methods = async { authRepository.getLoginMethods(serverId) }
            methods.await().onSuccess(::applyLoginMethods)
            registration.await().fold(
                onSuccess = ::applyRegistrationStatus,
                onFailure = { failure ->
                    val details = errorMapper.details(failure, R.string.register_status_failed)
                    _state.update {
                        it.copy(
                            isLoading = false,
                            allowRegistration = false,
                            bootstrapAvailable = false,
                            localLoginEnabled = false,
                            errorMessage = details.message,
                            errorRequestId = details.requestId,
                        )
                    }
                },
            )
        }
    }

    private fun register() {
        val current = _state.value
        if (!current.isRegistrationOpen || current.isSubmitting) return
        if (current.name.isBlank() || current.email.isBlank() || current.password.isBlank()) {
            _state.update {
                it.copy(
                    errorMessage = context.getString(R.string.register_fields_required),
                    errorRequestId = null,
                )
            }
            return
        }
        _state.update {
            it.copy(isSubmitting = true, errorMessage = null, errorRequestId = null)
        }
        viewModelScope.launch {
            authRepository.registerWithCredentials(
                serverId = serverId,
                name = current.name.trim(),
                email = current.email.trim(),
                password = current.password,
            ).fold(
                onSuccess = {
                    _state.update { it.copy(isSubmitting = false) }
                    emit(RegisterEvent.NavigateToHome)
                },
                onFailure = { failure ->
                    val details = errorMapper.authenticationDetails(
                        failure = failure,
                        fallbackRes = R.string.register_failed,
                        rejectedRes = R.string.register_rejected,
                    )
                    _state.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = details.message,
                            errorRequestId = details.requestId,
                        )
                    }
                    authRepository.getRegistrationStatus(serverId)
                        .onSuccess(::applyRegistrationStatus)
                },
            )
        }
    }

    private fun startOidc() {
        if (!_state.value.oidcEnabled || _state.value.isSubmitting) return
        _state.update {
            it.copy(isSubmitting = true, errorMessage = null, errorRequestId = null)
        }
        viewModelScope.launch {
            authRepository.startOidc(serverId).fold(
                onSuccess = { authorization ->
                    emit(
                        RegisterEvent.LaunchOidc(
                            authorizationUrl = authorization.authorizationUrl,
                            redirectScheme = authorization.redirectScheme,
                        ),
                    )
                },
                onFailure = { failure ->
                    val details = errorMapper.authenticationDetails(
                        failure,
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
        if (oidcCallbackInProgress) return
        oidcCallbackInProgress = true
        _state.update {
            it.copy(isSubmitting = true, errorMessage = null, errorRequestId = null)
        }
        viewModelScope.launch {
            authRepository.finishOidc(serverId, callbackUrl).fold(
                onSuccess = {
                    _state.update { it.copy(isSubmitting = false) }
                    emit(RegisterEvent.NavigateToHome)
                },
                onFailure = { failure ->
                    oidcCallbackInProgress = false
                    val details = errorMapper.authenticationDetails(
                        failure,
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

    private fun cancelOidc() {
        if (oidcCallbackInProgress || oidcCallbackRelay.hasPendingCallback) return
        _state.update {
            it.copy(isSubmitting = false, errorMessage = null, errorRequestId = null)
        }
        viewModelScope.launch { authRepository.cancelOidc(serverId) }
    }

    private fun applyRegistrationStatus(status: RegistrationStatus) {
        _state.update {
            it.copy(
                isLoading = false,
                allowRegistration = status.allowRegistration,
                bootstrapAvailable = status.bootstrapAvailable,
                localLoginEnabled = status.localLoginEnabled,
            )
        }
    }

    private fun applyLoginMethods(methods: LoginMethods) {
        _state.update {
            it.copy(
                oidcEnabled = methods.oidcEnabled,
                oidcProviderName = methods.oidcProviderName,
            )
        }
    }

    private fun updateInput(transform: RegisterState.() -> RegisterState) {
        _state.update {
            it.transform().copy(errorMessage = null, errorRequestId = null)
        }
    }

    private fun emit(event: RegisterEvent) {
        viewModelScope.launch { eventsChannel.send(event) }
    }
}
