package dev.typetype.android.feature.settings.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.domain.push.PushRegistrationStatus
import dev.typetype.android.domain.push.PushRepository
import dev.typetype.android.services.push.PushRegistrationManager
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface PushSettingsAction {
    data object Retry : PushSettingsAction

    data object Toggle : PushSettingsAction
}

data class PushSettingsState(
    val isLoading: Boolean = true,
    val capabilityEnabled: Boolean = false,
    val maxDevices: Int = 0,
    val deviceCount: Int = 0,
    val status: PushRegistrationStatus = PushRegistrationStatus.Disabled,
    val inFlight: Boolean = false,
    val showError: Boolean = false,
)

@HiltViewModel
class PushNotificationsViewModel @Inject constructor(
    private val pushRepository: PushRepository,
    private val registrationManager: PushRegistrationManager,
) : ViewModel() {
    private val _state = MutableStateFlow(PushSettingsState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { registrationManager.refreshStatus() }
            load()
        }
        viewModelScope.launch {
            registrationManager.status.collect { status ->
                _state.update { it.copy(status = status) }
            }
        }
    }

    fun onAction(action: PushSettingsAction) {
        when (action) {
            PushSettingsAction.Retry -> load()
            PushSettingsAction.Toggle -> toggle()
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, showError = false) }
            val capability = runCatching { pushRepository.currentCapability() }.getOrNull()
            val devices = runCatching { pushRepository.devices() }.getOrNull()
            _state.update {
                it.copy(
                    isLoading = false,
                    capabilityEnabled = capability?.enabled == true,
                    maxDevices = capability?.maxDevicesPerAccount ?: 0,
                    deviceCount = devices?.getOrNull()?.size ?: it.deviceCount,
                )
            }
        }
    }

    private fun toggle() {
        if (_state.value.inFlight) return
        viewModelScope.launch {
            _state.update { it.copy(inFlight = true, showError = false) }
            val result = runCatching {
                if (_state.value.status == PushRegistrationStatus.Registered) {
                    registrationManager.disable()
                } else {
                    registrationManager.enable()
                }
            }
            val devices = runCatching { pushRepository.devices() }.getOrNull()
            _state.update {
                it.copy(
                    inFlight = false,
                    showError = result.isFailure,
                    deviceCount = devices?.getOrNull()?.size ?: it.deviceCount,
                )
            }
        }
    }
}
