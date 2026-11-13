package dev.typetype.android.feature.settings.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.R
import dev.typetype.android.core.ui.error.UserErrorMapper
import dev.typetype.android.domain.devicelink.DeviceLinkRepository
import dev.typetype.android.domain.devicelink.DeviceLinkRequest
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class DeviceLinkOutcome { Approved, Denied }

data class DeviceLinkState(
    val request: DeviceLinkRequest? = null,
    val outcome: DeviceLinkOutcome? = null,
    val busy: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class DeviceLinkViewModel @Inject constructor(
    private val repository: DeviceLinkRepository,
    private val errorMapper: UserErrorMapper,
) : ViewModel() {
    private val _state = MutableStateFlow(DeviceLinkState())
    val state: StateFlow<DeviceLinkState> = _state.asStateFlow()

    fun lookup(userCode: String) = run {
        val request = repository.lookup(userCode).getOrThrow()
        _state.update { it.copy(request = request, outcome = null) }
    }

    fun approve() = decide(DeviceLinkOutcome.Approved) { repository.approve(it) }

    fun deny() = decide(DeviceLinkOutcome.Denied) { repository.deny(it) }

    fun reset() = _state.update { DeviceLinkState() }

    private fun decide(outcome: DeviceLinkOutcome, call: suspend (String) -> Result<Unit>) {
        val code = _state.value.request?.userCode ?: return
        run {
            call(code).getOrThrow()
            _state.update { it.copy(request = null, outcome = outcome) }
        }
    }

    private fun run(block: suspend () -> Unit) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching { block() }.onFailure { failure ->
                if (failure is CancellationException) throw failure
                _state.update { it.copy(errorMessage = errorMapper.details(failure, R.string.device_link_failed).message) }
            }
            _state.update { it.copy(busy = false) }
        }
    }
}
