package dev.typetype.android.feature.settings.youtubesession

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.R
import dev.typetype.android.core.ui.error.UserErrorMapper
import dev.typetype.android.data.account.AccountScope
import dev.typetype.android.data.account.AccountScopeProvider
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
import kotlinx.coroutines.flow.combine
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
    private val accountScopeProvider: AccountScopeProvider,
    private val errorMapper: UserErrorMapper,
) : ViewModel() {
    private val mutableState = MutableStateFlow(YoutubeSessionState())
    val state = mutableState.asStateFlow()

    private var serverId: String? = null
    private var activeScope: AccountScope? = null
    private var connection: YoutubeRemoteBrowserConnection? = null
    private var frameJob: Job? = null
    private var transportJob: Job? = null

    init {
        viewModelScope.launch {
            combine(
                serverRepository.observeCurrentServer(),
                accountScopeProvider.observe(),
            ) { server, scope ->
                YoutubeSessionContext(server, scope?.takeIf { it.serverId == server?.id })
            }
                .distinctUntilChangedBy { context ->
                    listOf(
                        context.server?.id,
                        context.server?.youtubeRemoteLoginSupported,
                        context.server?.youtubeRemoteLoginEnabled,
                        context.server?.youtubeRemoteLoginReady,
                        context.server?.youtubeRemoteLoginUnavailableReason,
                        context.scope,
                    )
                }
                .collect(::useContext)
        }
    }

    fun refreshStatus() {
        if (mutableState.value.isStatusLoading || activeScope == null) return
        mutableState.update {
            it.copy(availability = YoutubeSessionAvailability.Checking, isStatusLoading = true)
        }
        viewModelScope.launch { refreshCapabilitiesAndStatus() }
    }

    fun startRemoteBrowser() {
        if (!mutableState.value.canStart) return
        val operationScope = activeScope ?: return
        viewModelScope.launch {
            clearFailure()
            mutableState.update { it.copy(isStarting = true, notice = null) }
            val session = youtubeSessionRepository.startRemoteBrowser().getOrElse { failure ->
                if (activeScope != operationScope) return@launch
                showFailure(failure, R.string.youtube_session_start_failed)
                mutableState.update { it.copy(isStarting = false) }
                return@launch
            }
            if (activeScope != operationScope) return@launch
            val nextConnection = remoteBrowserConnector.connect(session).getOrElse { failure ->
                if (activeScope != operationScope) return@launch
                youtubeSessionRepository.cancelRemoteBrowser(session.sessionId)
                showFailure(failure, R.string.youtube_session_start_failed)
                mutableState.update { it.copy(isStarting = false) }
                return@launch
            }
            if (activeScope != operationScope) {
                nextConnection.close()
                return@launch
            }
            attachConnection(session.sessionId, session.expiresAt, operationScope, nextConnection)
            mutableState.update { it.copy(isStarting = false) }
        }
    }

    fun send(input: YoutubeRemoteBrowserInput): Boolean = connection?.send(input) == true

    fun cancelRemoteBrowser() {
        val remoteSessionId = mutableState.value.remoteSessionId ?: return
        if (mutableState.value.isCancelling) return
        val operationScope = activeScope ?: return
        connection?.send(YoutubeRemoteBrowserInput.Cancel)
        closeConnection(clearSession = false)
        viewModelScope.launch {
            mutableState.update { it.copy(isCancelling = true) }
            youtubeSessionRepository.cancelRemoteBrowser(remoteSessionId)
                .onSuccess {
                    if (activeScope != operationScope) return@onSuccess
                    clearRemoteSession(YoutubeSessionNotice.SignInCancelled)
                }
                .onFailure { failure ->
                    if (activeScope != operationScope) return@onFailure
                    clearRemoteSession()
                    showFailure(failure, R.string.youtube_session_cancel_failed)
                }
        }
    }

    fun disconnect() {
        if (!mutableState.value.canDisconnect) return
        val operationScope = activeScope ?: return
        viewModelScope.launch {
            clearFailure()
            mutableState.update { it.copy(isDisconnecting = true, notice = null) }
            youtubeSessionRepository.disconnect()
                .onSuccess {
                    if (activeScope != operationScope) return@onSuccess
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
                    if (activeScope != operationScope) return@onFailure
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

    private suspend fun useContext(context: YoutubeSessionContext) {
        val server = context.server
        if (serverId != server?.id || activeScope != context.scope) {
            closeConnection(clearSession = true)
            mutableState.value = mutableState.value.clearedForAccountChange()
            serverId = server?.id
            activeScope = context.scope
        }
        if (server == null || context.scope == null) {
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
        val expectedScope = activeScope ?: return
        val expectedServerId = expectedScope.serverId
        val fallback = cached ?: serverRepository.getServer(expectedServerId) ?: return
        val server = capabilitiesRepository.refresh(expectedServerId).getOrDefault(fallback)
        if (activeScope != expectedScope) return
        mutableState.update {
            it.copy(
                availability = server.youtubeSessionAvailability(),
                unavailableReason = server.youtubeRemoteLoginUnavailableReason,
            )
        }
        if (server.youtubeRemoteLoginEnabled && server.youtubeRemoteLoginReady) {
            loadStatus(expectedScope)
        } else {
            mutableState.update { it.copy(isStatusLoading = false, session = null) }
        }
    }

    private suspend fun loadStatus(expectedScope: AccountScope) {
        mutableState.update { it.copy(isStatusLoading = true) }
        youtubeSessionRepository.getStatus()
            .onSuccess { session ->
                if (activeScope != expectedScope) return@onSuccess
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
                if (activeScope != expectedScope) return@onFailure
                mutableState.update { it.copy(isStatusLoading = false) }
                showFailure(failure, R.string.youtube_session_status_failed)
            }
    }

    private fun attachConnection(
        sessionId: String,
        expiresAt: Long,
        operationScope: AccountScope,
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
                if (connection === nextConnection && activeScope == operationScope) {
                    mutableState.update { it.copy(frameBytes = bytes) }
                }
            }
        }
        transportJob = viewModelScope.launch {
            val terminal = nextConnection.state
                .onEach(::showRemoteState)
                .first { it.phase in TERMINAL_PHASES }
            if (connection !== nextConnection || activeScope != operationScope) return@launch
            if (terminal.phase == YoutubeRemoteBrowserPhase.Connected) {
                nextConnection.close()
                connection = null
                frameJob?.cancel()
                clearRemoteSession(YoutubeSessionNotice.Connected)
                loadStatus(operationScope)
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

    private companion object {
        val TERMINAL_PHASES = setOf(
            YoutubeRemoteBrowserPhase.Connected,
            YoutubeRemoteBrowserPhase.Closed,
            YoutubeRemoteBrowserPhase.Error,
        )
    }
}

private data class YoutubeSessionContext(
    val server: Server?,
    val scope: AccountScope?,
)
