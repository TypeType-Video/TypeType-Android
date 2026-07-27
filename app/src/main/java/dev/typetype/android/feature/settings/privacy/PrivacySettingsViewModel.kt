package dev.typetype.android.feature.settings.privacy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.R
import dev.typetype.android.core.ui.error.UserErrorMapper
import dev.typetype.android.domain.library.LibraryRepository
import dev.typetype.android.data.account.ActiveAccountScope
import dev.typetype.android.domain.playback.PlaybackResumeRepository
import dev.typetype.android.domain.searchhistory.SearchHistoryRepository
import dev.typetype.android.domain.session.ActiveSessionRepository
import dev.typetype.android.domain.subscriptions.SubscriptionsRepository
import dev.typetype.android.domain.usersettings.UserSettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PrivacyState(
    val historyCount: Int = 0,
    val searchHistoryCount: Int = 0,
    val subscriptionsCount: Int = 0,
    val watchHistoryTrackingEnabled: Boolean = true,
    val watchHistoryTrackingControlEnabled: Boolean = false,
    val errorMessage: String? = null,
    val errorRequestId: String? = null,
    val failureAction: PrivacyFailureAction? = null,
    val deviceName: String = "",
)

enum class PrivacyFailureAction {
    LoadSearchHistory,
    LoadSubscriptions,
    LoadTrackingPreference,
    UpdateTrackingPreference,
    ClearWatchHistory,
    ClearSearchHistory,
    UnsubscribeAll,
}

@HiltViewModel
class PrivacySettingsViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val searchHistoryRepository: SearchHistoryRepository,
    private val subscriptionsRepository: SubscriptionsRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val activeAccountScope: ActiveAccountScope,
    private val playbackResumeRepository: PlaybackResumeRepository,
    private val errorMapper: UserErrorMapper,
    private val activeSessionRepository: ActiveSessionRepository,
) : ViewModel() {

    private val subscriptionsCount = MutableStateFlow(0)
    private var trackingRetryTarget: Boolean? = null

    val state = combine(
        libraryRepository.observeHistoryCount(),
        subscriptionsCount,
        userSettingsRepository.observe(),
        activeSessionRepository.observeDeviceName(),
    ) { historyCount, subsCount, settings, deviceName ->
        PrivacyState(
            historyCount = historyCount,
            subscriptionsCount = subsCount,
            watchHistoryTrackingEnabled = !settings.disableWatchHistory,
            deviceName = deviceName,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PrivacyState())

    private val _localState = MutableStateFlow(PrivacyState())
    val combinedState = combine(state, _localState) { server, local ->
        server.copy(
            searchHistoryCount = local.searchHistoryCount,
            watchHistoryTrackingControlEnabled = local.watchHistoryTrackingControlEnabled,
            errorMessage = local.errorMessage,
            errorRequestId = local.errorRequestId,
            failureAction = local.failureAction,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PrivacyState())

    init {
        refreshSearchHistory()
        refreshSubscriptions()
        refreshWatchHistoryTracking()
    }

    fun setWatchHistoryTracking(enabled: Boolean) {
        trackingRetryTarget = enabled
        viewModelScope.launch {
            clearFailure()
            setTrackingControlEnabled(false)
            userSettingsRepository.update { it.copy(disableWatchHistory = !enabled) }
                .onSuccess {
                    if (!enabled) {
                        libraryRepository.discardPendingProgress().onFailure {
                            showFailure(
                                it,
                                PrivacyFailureAction.UpdateTrackingPreference,
                                R.string.settings_privacy_tracking_cleanup_failed,
                            )
                        }
                        discardPlaybackResume().onFailure {
                            showFailure(
                                it,
                                PrivacyFailureAction.UpdateTrackingPreference,
                                R.string.settings_privacy_tracking_cleanup_failed,
                            )
                        }
                    }
                    setTrackingControlEnabled(true)
                }
                .onFailure {
                    setTrackingControlEnabled(true)
                    showFailure(
                        it,
                        PrivacyFailureAction.UpdateTrackingPreference,
                        R.string.settings_privacy_tracking_failed,
                    )
                }
        }
    }

    fun clearWatchHistory() {
        viewModelScope.launch {
            clearFailure()
            libraryRepository.clearHistory().onFailure {
                showFailure(
                    it,
                    PrivacyFailureAction.ClearWatchHistory,
                    R.string.settings_privacy_clear_watch_failed,
                )
            }
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            clearFailure()
            searchHistoryRepository.clearHistory()
                .onSuccess { _localState.update { it.copy(searchHistoryCount = 0) } }
                .onFailure {
                    showFailure(
                        it,
                        PrivacyFailureAction.ClearSearchHistory,
                        R.string.settings_privacy_clear_search_failed,
                    )
                }
        }
    }

    fun unsubscribeAll() {
        viewModelScope.launch {
            clearFailure()
            subscriptionsRepository.unsubscribeAll()
                .onSuccess { subscriptionsCount.value = 0 }
                .onFailure {
                    showFailure(
                        it,
                        PrivacyFailureAction.UnsubscribeAll,
                        R.string.settings_privacy_unsubscribe_failed,
                    )
                }
        }
    }

    fun setDeviceName(name: String) {
        viewModelScope.launch {
            activeSessionRepository.setDeviceName(name.take(MAX_DEVICE_NAME_LENGTH))
        }
    }

    fun retryFailure() {
        val action = _localState.value.failureAction
        clearFailure()
        when (action) {
            PrivacyFailureAction.LoadSearchHistory -> refreshSearchHistory()
            PrivacyFailureAction.LoadSubscriptions -> refreshSubscriptions()
            PrivacyFailureAction.LoadTrackingPreference -> refreshWatchHistoryTracking()
            PrivacyFailureAction.UpdateTrackingPreference ->
                trackingRetryTarget?.let(::setWatchHistoryTracking)
            PrivacyFailureAction.ClearWatchHistory -> clearWatchHistory()
            PrivacyFailureAction.ClearSearchHistory -> clearSearchHistory()
            PrivacyFailureAction.UnsubscribeAll -> unsubscribeAll()
            null -> Unit
        }
    }

    fun dismissFailure() {
        trackingRetryTarget = null
        clearFailure()
    }

    private fun refreshSearchHistory() {
        viewModelScope.launch {
            searchHistoryRepository.loadHistory().onSuccess { history ->
                _localState.update { it.copy(searchHistoryCount = history.size) }
            }.onFailure {
                showFailure(
                    it,
                    PrivacyFailureAction.LoadSearchHistory,
                    R.string.settings_privacy_load_search_failed,
                )
            }
        }
    }

    private fun refreshSubscriptions() {
        viewModelScope.launch {
            subscriptionsRepository.listSubscriptions()
                .onSuccess { subscriptionsCount.value = it.size }
                .onFailure {
                    showFailure(
                        it,
                        PrivacyFailureAction.LoadSubscriptions,
                        R.string.settings_privacy_load_subscriptions_failed,
                    )
                }
        }
    }

    private fun refreshWatchHistoryTracking() {
        viewModelScope.launch {
            userSettingsRepository.refresh()
                .onSuccess { setTrackingControlEnabled(true) }
                .onFailure {
                    showFailure(
                        it,
                        PrivacyFailureAction.LoadTrackingPreference,
                        R.string.settings_privacy_load_tracking_failed,
                    )
                }
        }
    }

    private suspend fun discardPlaybackResume(): Result<Unit> = runCatching {
        val scope = activeAccountScope.observe().first() ?: return@runCatching
        playbackResumeRepository.clear(scope.serverId, scope.accountId)
    }

    private fun clearFailure() {
        _localState.update {
            it.copy(errorMessage = null, errorRequestId = null, failureAction = null)
        }
    }

    private fun setTrackingControlEnabled(enabled: Boolean) {
        _localState.update { it.copy(watchHistoryTrackingControlEnabled = enabled) }
    }

    private fun showFailure(
        failure: Throwable,
        action: PrivacyFailureAction,
        fallbackMessage: Int,
    ) {
        val details = errorMapper.details(failure, fallbackMessage)
        _localState.update {
            it.copy(
                errorMessage = details.message,
                errorRequestId = details.requestId,
                failureAction = action,
            )
        }
    }

    private companion object {
        const val MAX_DEVICE_NAME_LENGTH = 120
    }
}
