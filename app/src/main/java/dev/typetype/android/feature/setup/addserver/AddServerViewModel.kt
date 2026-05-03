package dev.typetype.android.feature.setup.addserver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.domain.server.Server
import dev.typetype.android.domain.setup.SetupRepository
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AddServerViewModel @Inject constructor(
    private val setupRepository: SetupRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AddServerState())
    val state = _state.asStateFlow()

    private val eventsChannel = Channel<AddServerEvent>(Channel.BUFFERED)
    val events = eventsChannel.receiveAsFlow()

    fun onAction(action: AddServerAction) {
        when (action) {
            is AddServerAction.OnUrlChange -> _state.update {
                it.copy(
                    url = action.url,
                    errorMessage = null,
                    resolvedName = null,
                    resolvedTagline = null,
                    resolvedVersion = null,
                )
            }
            AddServerAction.OnConnectClick -> connect()
            AddServerAction.OnBackClick -> emit(AddServerEvent.NavigateBack)
        }
    }

    private fun connect() {
        val typedUrl = _state.value.url
        if (typedUrl.isBlank()) {
            _state.update { it.copy(errorMessage = "Enter a server URL") }
            return
        }
        _state.update { it.copy(isConnecting = true, errorMessage = null) }
        viewModelScope.launch {
            setupRepository.probeServer(typedUrl).fold(
                onSuccess = { probe ->
                    val server = Server(
                        id = UUID.randomUUID().toString(),
                        baseUrl = probe.normalizedUrl,
                        displayName = probe.name,
                        addedAt = System.currentTimeMillis(),
                    )
                    setupRepository.persistServer(server)
                    _state.update {
                        it.copy(
                            isConnecting = false,
                            resolvedName = probe.name,
                            resolvedTagline = probe.tagline,
                            resolvedVersion = probe.version,
                        )
                    }
                    emit(
                        AddServerEvent.NavigateToLogin(
                            serverId = server.id,
                            guestAllowed = probe.guestAllowed,
                            registrationAllowed = probe.registrationAllowed,
                        ),
                    )
                },
                onFailure = { throwable ->
                    _state.update {
                        it.copy(
                            isConnecting = false,
                            errorMessage = throwable.message ?: "Could not reach server",
                        )
                    }
                },
            )
        }
    }

    private fun emit(event: AddServerEvent) {
        viewModelScope.launch { eventsChannel.send(event) }
    }
}
