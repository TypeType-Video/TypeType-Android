package dev.typetype.android.feature.setup.addserver

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.typetype.android.R
import dev.typetype.android.core.ui.error.UserErrorMapper
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
    @ApplicationContext private val context: Context,
    private val errorMapper: UserErrorMapper,
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
                    errorRequestId = null,
                    localNetworkPermissionDenied = false,
                    resolvedName = null,
                    resolvedTagline = null,
                    resolvedVersion = null,
                )
            }
            AddServerAction.OnConnectClick -> connect()
            AddServerAction.OnLocalNetworkPermissionDenied -> _state.update {
                it.copy(
                    localNetworkPermissionDenied = true,
                    errorMessage = null,
                    errorRequestId = null,
                )
            }
            AddServerAction.OnBackClick -> emit(AddServerEvent.NavigateBack)
        }
    }

    private fun connect() {
        val typedUrl = _state.value.url
        if (typedUrl.isBlank()) {
            _state.update {
                it.copy(
                    errorMessage = context.getString(R.string.setup_enter_server_url),
                    errorRequestId = null,
                )
            }
            return
        }
        _state.update {
            it.copy(
                isConnecting = true,
                errorMessage = null,
                errorRequestId = null,
                localNetworkPermissionDenied = false,
            )
        }
        viewModelScope.launch {
            setupRepository.probeServer(typedUrl).fold(
                onSuccess = { probe ->
                    val server = Server(
                        id = UUID.randomUUID().toString(),
                        baseUrl = probe.normalizedUrl,
                        displayName = probe.name,
                        addedAt = System.currentTimeMillis(),
                        tagline = probe.tagline,
                        version = probe.version,
                        revision = probe.revision,
                        apiVersion = probe.apiVersion,
                        logoUrl = probe.logoUrl,
                        bannerUrl = probe.bannerUrl,
                        supportedServices = probe.supportedServices,
                        minAndroidClientVersion = probe.minAndroidClientVersion,
                        registrationAllowed = probe.registrationAllowed,
                        guestAllowed = probe.guestAllowed,
                        localLoginEnabled = probe.localLoginEnabled,
                        oidcEnabled = probe.oidcEnabled,
                        oidcProviderName = probe.oidcProviderName,
                        oidcAutoRedirect = probe.oidcAutoRedirect,
                        youtubeRemoteLoginEnabled = probe.youtubeRemoteLoginEnabled,
                        youtubeRemoteLoginReady = probe.youtubeRemoteLoginReady,
                        youtubeRemoteLoginUnavailableReason = probe.youtubeRemoteLoginUnavailableReason,
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
                        ),
                    )
                },
                onFailure = { throwable ->
                    val details = errorMapper.details(throwable, R.string.setup_server_unreachable)
                    _state.update {
                        it.copy(
                            isConnecting = false,
                            errorMessage = details.message,
                            errorRequestId = details.requestId,
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
