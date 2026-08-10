package dev.typetype.android.feature.settings.youtubesession

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.R
import dev.typetype.android.core.ui.error.UserErrorMapper
import dev.typetype.android.domain.server.Server
import dev.typetype.android.domain.server.ServerCapabilitiesRepository
import dev.typetype.android.domain.server.ServerRepository
import dev.typetype.android.domain.youtubesession.YoutubeRemoteBrowserConnection
import dev.typetype.android.domain.youtubesession.YoutubeRemoteBrowserConnector
import dev.typetype.android.domain.youtubesession.YoutubeRemoteBrowserInput
import dev.typetype.android.domain.youtubesession.YoutubeRemoteBrowserPhase
import dev.typetype.android.domain.youtubesession.YoutubeRemoteBrowserState
import dev.typetype.android.domain.youtubesession.YoutubeSessionRepository
import dev.typetype.android.domain.youtubesession.YoutubeSessionStatus
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class YoutubeSessionViewModel @Inject constructor(
    private val youtubeSessionRepository: YoutubeSessionRepository,
    private val remoteBrowserConnector: YoutubeRemoteBrowserConnector,
    private val serverRepository: ServerRepository,
    private val capabilitiesRepository: ServerCapabilitiesRepository,
    private val errorMapper: UserErrorMapper,
) : ViewModel() {
    private val mutableState = MutableStateFlow(YoutubeSessionState())
    val state = mutableState.asStateFlow()

    private var serverId: String? = null
    private var connection: YoutubeRemoteBrowserConnection? = null
    private var frameJob: Job? = null
    private var transportJob: Job? = null

    init {
        viewModelScope.launch {
            serverRepository.observeCurrentServer()
                .distinctUntilChangedBy { server ->
                    listOf(
                        server?.id,
                        server?.youtubeRemoteLoginEnabled,
                        server?.youtubeRemoteLoginReady,
                        server?.youtubeRemoteLoginUnavailableReason,
                    )
                }
                .collect { server -> useServer(server) }
        }
    }

    fun refreshStatus() {
        if (mutableState.value.isStatusLoading) return
        mutableState.update {
            it.copy(availability = YoutubeSessionAvailability.Checking, isStatusLoading = true)
        }
        viewModelScope.launch { refreshCapabilitiesAndStatus() }
    }

    fun startRemoteBrowser() {
        if (!mutableState.value.canStart) return
        viewModelScope.launch {
            clearFailure()
            mutableState.update { it.copy(isStarting = true, notice = null) }
            val session = youtubeSessionRepository.startRemoteBrowser().getOrElse { failure ->
                showFailure(failure, R.string.youtube_session_start_failed)
                mutableState.update { it.copy(isStarting = false) }
                return@launch
            }
            val nextConnection = remoteBrowserConnector.connect(session).getOrElse { failure ->
                youtubeSessionRepository.cancelRemoteBrowser(session.sessionId)
                showFailure(failure, R.string.youtube_session_start_failed)
                mutableState.update { it.copy(isStarting = false) }
                return@launch
            }
            attachConnection(session.sessionId, session.expiresAt, nextConnection)
            mutableState.update { it.copy(isStarting = false) }
        }
    }

    fun send(input: YoutubeRemoteBrowserInput): Boolean = connection?.send(input) == true

    fun cancelRemoteBrowser() {
        val remoteSessionId = mutableState.value.remoteSessionId ?: return
        if (mutableState.value.isCancelling) return
        connection?.send(YoutubeRemoteBrowserInput.Cancel)
        closeConnection(clearSession = false)
        viewModelScope.launch {
            mutableState.update { it.copy(isCancelling = true) }
            youtubeSessionRepository.cancelRemoteBrowser(remoteSessionId)
                .onSuccess {
                    clearRemoteSession(YoutubeSessionNotice.SignInCancelled)
                }
                .onFailure { failure ->
                    clearRemoteSession()
                    showFailure(failure, R.string.youtube_session_cancel_failed)
                }
        }
    }

    fun disconnect() {
        if (!mutableState.value.canDisconnect) return
        viewModelScope.launch {
            clearFailure()
            mutableState.update { it.copy(isDisconnecting = true, notice = null) }
            youtubeSessionRepository.disconnect()
                .onSuccess {
                    mutableState.update {
                        it.copy(
                            isDisconnecting = false,
                            session = it.session?.copy(
                                status = YoutubeSessionStatus.Disconnected,
                            ),
                            notice = YoutubeSessionNotice.Disconnected,
                        )
                    }
                }
                .onFailure { failure ->
                    mutableState.update { it.copy(isDisconnecting = false) }
                    showFailure(failure, R.string.youtube_session_disconnect_failed)
                }
        }
    }

    fun dismissNotice() {
        mutableState.update { it.copy(notice = null) }
    }

    fun leave() {
        connection?.send(YoutubeRemoteBrowserInput.Cancel)
        closeConnection(clearSession = true)
    }

    override fun onCleared() {
        leave()
        super.onCleared()
    }

    private suspend fun useServer(server: Server?) {
        if (serverId != server?.id) {
            closeConnection(clearSession = true)
            serverId = server?.id
        }
        if (server == null) {
            mutableState.update {
                it.copy(availability = YoutubeSessionAvailability.Checking, isStatusLoading = false)
            }
            return
        }
        mutableState.update {
            it.copy(availability = YoutubeSessionAvailability.Checking, isStatusLoading = true)
        }
        refreshCapabilitiesAndStatus(server)
    }

    private suspend fun refreshCapabilitiesAndStatus(cached: Server? = null) {
        val expectedServerId = serverId ?: return
        val fallback = cached ?: serverRepository.getServer(expectedServerId) ?: return
        val server = capabilitiesRepository.refresh(expectedServerId).getOrDefault(fallback)
        if (serverId != expectedServerId) return
        mutableState.update {
            it.copy(
                availability = server.availability,
                unavailableReason = server.youtubeRemoteLoginUnavailableReason,
            )
        }
        if (server.youtubeRemoteLoginEnabled && server.youtubeRemoteLoginReady) {
            loadStatus()
        } else {
            mutableState.update { it.copy(isStatusLoading = false, session = null) }
        }
    }

    private suspend fun loadStatus() {
        mutableState.update { it.copy(isStatusLoading = true) }
        youtubeSessionRepository.getStatus()
            .onSuccess { session ->
                mutableState.update {
                    it.copy(
                        session = session,
                        isStatusLoading = false,
                        errorMessage = null,
                        errorRequestId = null,
                    )
                }
            }
            .onFailure { failure ->
                mutableState.update { it.copy(isStatusLoading = false) }
                showFailure(failure, R.string.youtube_session_status_failed)
            }
    }

    private fun attachConnection(
        sessionId: String,
        expiresAt: Long,
        nextConnection: YoutubeRemoteBrowserConnection,
    ) {
        closeConnection(clearSession = true)
        connection = nextConnection
        mutableState.update {
            it.copy(
                remoteSessionId = sessionId,
                remoteSessionExpiresAt = expiresAt,
                remotePhase = nextConnection.state.value.phase,
                remoteErrorMessage = null,
                frameBytes = null,
            )
        }
        frameJob = viewModelScope.launch {
            nextConnection.frames.collect { bytes ->
                if (connection === nextConnection) mutableState.update { it.copy(frameBytes = bytes) }
            }
        }
        transportJob = viewModelScope.launch {
            val terminal = nextConnection.state
                .onEach(::showRemoteState)
                .first { it.phase in TERMINAL_PHASES }
            if (connection !== nextConnection) return@launch
            if (terminal.phase == YoutubeRemoteBrowserPhase.Connected) {
                nextConnection.close()
                connection = null
                frameJob?.cancel()
                clearRemoteSession(YoutubeSessionNotice.Connected)
                loadStatus()
            }
        }
    }

    private fun showRemoteState(next: YoutubeRemoteBrowserState) {
        mutableState.update {
            it.copy(remotePhase = next.phase, remoteErrorMessage = next.errorMessage)
        }
    }

    private fun closeConnection(clearSession: Boolean) {
        transportJob?.cancel()
        frameJob?.cancel()
        transportJob = null
        frameJob = null
        connection?.close()
        connection = null
        if (clearSession) clearRemoteSession()
    }

    private fun clearRemoteSession(notice: YoutubeSessionNotice? = null) {
        mutableState.update {
            it.copy(
                isCancelling = false,
                remoteSessionId = null,
                remoteSessionExpiresAt = null,
                remotePhase = YoutubeRemoteBrowserPhase.Idle,
                remoteErrorMessage = null,
                frameBytes = null,
                notice = notice,
            )
        }
    }

    private fun showFailure(failure: Throwable, fallbackRes: Int) {
        val details = errorMapper.details(failure, fallbackRes)
        mutableState.update {
            it.copy(errorMessage = details.message, errorRequestId = details.requestId)
        }
    }

    private fun clearFailure() {
        mutableState.update { it.copy(errorMessage = null, errorRequestId = null) }
    }

    private val Server?.availability: YoutubeSessionAvailability
        get() = when {
            this == null -> YoutubeSessionAvailability.Checking
            !youtubeRemoteLoginSupported -> YoutubeSessionAvailability.Disabled
            !youtubeRemoteLoginEnabled -> YoutubeSessionAvailability.Disabled
            !youtubeRemoteLoginReady -> YoutubeSessionAvailability.Unavailable
            else -> YoutubeSessionAvailability.Available
        }

    private companion object {
        val TERMINAL_PHASES = setOf(
            YoutubeRemoteBrowserPhase.Connected,
            YoutubeRemoteBrowserPhase.Closed,
            YoutubeRemoteBrowserPhase.Error,
        )
    }
}
