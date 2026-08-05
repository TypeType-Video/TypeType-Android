package dev.typetype.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.core.ui.navigation.HomeRoute
import dev.typetype.android.core.ui.navigation.LoginRoute
import dev.typetype.android.core.ui.navigation.WelcomeRoute
import dev.typetype.android.data.network.AccessTokenStore
import dev.typetype.android.data.account.ActiveAccountScope
import dev.typetype.android.domain.actions.VideoActionsRepository
import dev.typetype.android.domain.auth.AuthRepository
import dev.typetype.android.domain.auth.SessionStatus
import dev.typetype.android.domain.diagnostics.CrashReport
import dev.typetype.android.domain.diagnostics.CrashReportRepository
import dev.typetype.android.domain.preferences.AppPreferences
import dev.typetype.android.domain.library.LibraryRepository
import dev.typetype.android.domain.navigation.PendingVideoRequest
import dev.typetype.android.domain.preferences.PreferencesRepository
import dev.typetype.android.domain.playback.PlaybackResumeRepository
import dev.typetype.android.domain.playback.PlaybackQueueRepository
import dev.typetype.android.domain.playback.PlaybackQueueSnapshot
import dev.typetype.android.domain.playback.PlaybackResume
import dev.typetype.android.domain.profile.ProfileRepository
import dev.typetype.android.domain.server.ServerRepository
import dev.typetype.android.domain.subscriptions.SubscriptionsRepository
import dev.typetype.android.domain.usersettings.UserSettingsRepository
import dev.typetype.android.feature.player.host.PlayerHostController
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

data class MainState(
    val isLoading: Boolean = true,
    val startRoute: Any? = null,
    val pendingCrashReport: CrashReport? = null,
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
    private val videoActionsRepository: VideoActionsRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val profileRepository: ProfileRepository,
    private val subscriptionsRepository: SubscriptionsRepository,
    private val libraryRepository: LibraryRepository,
    private val activeAccountScope: ActiveAccountScope,
    private val playbackResumeRepository: PlaybackResumeRepository,
    private val playbackQueueRepository: PlaybackQueueRepository,
    private val crashReportRepository: CrashReportRepository,
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

    val currentServerBaseUrl: StateFlow<String?> = serverRepository.observeCurrentServer()
        .map { it?.baseUrl }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )

    val currentServerId: StateFlow<String?> = serverRepository.observeCurrentServer()
        .map { it?.id }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )

    val currentProfile: StateFlow<dev.typetype.android.domain.profile.Profile?> =
        profileRepository.observe()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = null,
            )

    private val eventsChannel = Channel<MainEvent>(Channel.BUFFERED)
    val events = eventsChannel.receiveAsFlow()
    private val pendingVideoRequest = PendingVideoRequest()

    init {
        viewModelScope.launch {
            val initial = withTimeoutOrNull(STARTUP_TIMEOUT_MS) {
                serverRepository.observeCurrentServer().first()
            }
            val pendingCrashReport = crashReportRepository.pendingCurrent()
            if (pendingCrashReport != null) {
                _state.update { it.copy(pendingCrashReport = pendingCrashReport) }
            }
            val sessionStatus = if (initial == null) {
                SessionStatus.Invalid
            } else {
                withTimeoutOrNull(SESSION_VALIDATION_TIMEOUT_MS) {
                    authRepository.validateSession()
                } ?: SessionStatus.Unknown
            }
            val startRoute = when {
                initial == null -> WelcomeRoute
                sessionStatus == SessionStatus.Invalid -> {
                    tokenStore.setAccessToken(initial.id, null)
                    LoginRoute(serverId = initial.id)
                }
                else -> HomeRoute
            }
            val playbackRestore = if (startRoute == HomeRoute) {
                withTimeoutOrNull(PLAYBACK_RESTORE_TIMEOUT_MS) {
                    loadPlaybackRestore()
                }
            } else null
            _state.update { it.copy(isLoading = false, startRoute = startRoute) }
            if (startRoute == HomeRoute) {
                val externalUrl = pendingVideoRequest.setReady(true)
                if (externalUrl == null) {
                    playbackRestore?.let(::applyPlaybackRestore)
                } else {
                    playerHostController.openVideo(externalUrl)
                }
                launch { videoActionsRepository.refreshBlocked() }
                launch { userSettingsRepository.refresh() }
                launch { profileRepository.refresh() }
                launch { subscriptionsRepository.refresh() }
                launch { libraryRepository.resumePendingWrites() }
            }
        }
    }

    private companion object {
        const val STARTUP_TIMEOUT_MS = 4_000L
        const val SESSION_VALIDATION_TIMEOUT_MS = 6_000L
        const val PLAYBACK_RESTORE_TIMEOUT_MS = 4_000L
    }

    fun signOut() {
        pendingVideoRequest.clear()
        viewModelScope.launch {
            val server = serverRepository.observeCurrentServer().first()
            if (server == null) {
                eventsChannel.send(MainEvent.NavigateToWelcome)
            } else {
                authRepository.logout(server.id)
                eventsChannel.send(MainEvent.NavigateToLogin(server.id))
            }
        }
    }

    fun continueAfterCrash() {
        if (_state.value.pendingCrashReport == null) return
        _state.update { it.copy(pendingCrashReport = null) }
        viewModelScope.launch { crashReportRepository.acknowledgeCurrent() }
    }

    fun onAccountActivated() {
        pendingVideoRequest.setReady(false)
        playerHostController.hide()
        viewModelScope.launch {
            launch { videoActionsRepository.refreshBlocked() }
            launch { userSettingsRepository.refresh() }
            launch { profileRepository.refresh() }
            launch { subscriptionsRepository.refresh() }
            launch { libraryRepository.resumePendingWrites() }
            launch {
                restorePlaybackUnlessExternalRequestArrives()
            }
        }
    }

    fun openExternalVideo(url: String) {
        pendingVideoRequest.submit(url)?.let(playerHostController::openVideo)
    }

    fun closePlayback() {
        playerHostController.hide()
        viewModelScope.launch {
            val scope = activeAccountScope.observe().first() ?: return@launch
            playbackResumeRepository.clear(scope.serverId, scope.accountId)
            playbackQueueRepository.clear(scope.serverId, scope.accountId)
        }
    }

    private suspend fun restorePlaybackUnlessExternalRequestArrives() {
        val restoreRevision = pendingVideoRequest.currentRevision
        val playbackRestore = loadPlaybackRestore()
        val externalUrl = pendingVideoRequest.setReady(true)
        when {
            externalUrl != null -> playerHostController.openVideo(externalUrl)
            pendingVideoRequest.isCurrent(restoreRevision) -> {
                playbackRestore?.let(::applyPlaybackRestore)
            }
        }
    }

    private suspend fun loadPlaybackRestore(): PlaybackRestore? {
        val scope = activeAccountScope.observe().first() ?: return null
        val settings = userSettingsRepository.current().getOrNull() ?: return null
        if (settings.disableWatchHistory) {
            playbackResumeRepository.clear(scope.serverId, scope.accountId)
            playbackQueueRepository.clear(scope.serverId, scope.accountId)
            return null
        }
        val resume = playbackResumeRepository.get(scope.serverId, scope.accountId)
        if (resume == null) {
            playbackQueueRepository.clear(scope.serverId, scope.accountId)
            return null
        }
        val queue = playbackQueueRepository.get(scope.serverId, scope.accountId)
            ?.takeIf { it.current?.videoUrl == resume.videoUrl }
        if (queue == null) playbackQueueRepository.clear(scope.serverId, scope.accountId)
        if (activeAccountScope.observe().first() != scope) return null
        return PlaybackRestore(resume, queue)
    }

    private fun applyPlaybackRestore(restore: PlaybackRestore) {
        val queue = restore.queue
        if (queue == null) {
            playerHostController.restoreVideo(
                restore.resume.videoUrl,
                restore.resume.positionMillis,
            )
        } else {
            playerHostController.restoreQueue(queue, restore.resume.positionMillis)
        }
    }

    private data class PlaybackRestore(
        val resume: PlaybackResume,
        val queue: PlaybackQueueSnapshot?,
    )
}
