package dev.typetype.android.feature.settings.content

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.R
import dev.typetype.android.core.ui.error.UserErrorMapper
import dev.typetype.android.data.account.AccountScope
import dev.typetype.android.data.account.ActiveAccountScope
import dev.typetype.android.data.preferences.StartupLandingStore
import dev.typetype.android.domain.account.AccountRepository
import dev.typetype.android.domain.recommendations.RecommenderSettingsRepository
import dev.typetype.android.domain.server.ServerCapabilitiesRepository
import dev.typetype.android.domain.server.ServerRepository
import dev.typetype.android.domain.usersettings.UserSettings
import dev.typetype.android.domain.usersettings.UserSettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ContentSettingsViewModel @Inject constructor(
    private val repository: UserSettingsRepository,
    private val errorMapper: UserErrorMapper,
    private val activeAccountScope: ActiveAccountScope,
    private val startupLandingStore: StartupLandingStore,
    private val accountRepository: AccountRepository,
    private val serverRepository: ServerRepository,
    private val capabilitiesRepository: ServerCapabilitiesRepository,
    private val recommenderSettingsRepository: RecommenderSettingsRepository,
) : ViewModel() {
    private val feedback = MutableStateFlow(ContentSettingsFeedback())

    val state: StateFlow<ContentSettingsState> = combine(
        repository.observe(),
        feedback,
        UserSettings::toContentSettingsState,
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ContentSettingsState(),
    )

    init {
        refresh()
        observeRecommender()
    }

    fun onAction(action: ContentSettingsAction) {
        when (action) {
            ContentSettingsAction.Retry -> refresh()
            ContentSettingsAction.RetryRecommender -> refreshRecommender()
            ContentSettingsAction.DismissFailure -> feedback.update {
                it.copy(errorMessage = null, errorRequestId = null)
            }
            ContentSettingsAction.DismissRecommenderFailure -> feedback.update {
                it.copy(recommenderErrorMessage = null, recommenderErrorRequestId = null)
            }
            is ContentSettingsAction.SetRecommenderEnabled -> updateRecommender(action.enabled)
            is ContentSettingsAction.Update -> update(action)
        }
    }

    private fun observeRecommender() {
        viewModelScope.launch {
            activeAccountScope.observe().distinctUntilChanged().collectLatest { scope ->
                feedback.update { it.withoutRecommender(scope != null) }
                loadRecommender(scope)
            }
        }
    }

    private fun refreshRecommender() {
        viewModelScope.launch {
            val scope = activeAccountScope.observe().first()
            feedback.update { it.withoutRecommender(scope != null) }
            loadRecommender(scope)
        }
    }

    private suspend fun loadRecommender(scope: AccountScope?) {
        if (scope == null) return
        try {
            val account = accountRepository.observeAccounts().first().firstOrNull {
                it.serverId == scope.serverId && it.id == scope.accountId
            }
            if (account == null || account.isGuest) {
                clearRecommender(scope)
                return
            }
            val cachedServer = serverRepository.getServer(scope.serverId)
            if (cachedServer == null) {
                clearRecommender(scope)
                return
            }
            val server = capabilitiesRepository.refresh(scope.serverId).getOrNull() ?: cachedServer
            if (!scopeIsCurrent(scope)) return
            if (!server.recommender.available) {
                feedback.update { it.withoutRecommender() }
                return
            }
            feedback.update {
                it.copy(
                    recommenderAvailable = true,
                    recommenderExperimental = server.recommender.experimental,
                )
            }
            val result = recommenderSettingsRepository.enabled()
            if (!scopeIsCurrent(scope)) return
            result.fold(
                onSuccess = { enabled ->
                    feedback.update {
                        it.copy(
                            recommenderEnabled = enabled,
                            isRecommenderLoading = false,
                            recommenderErrorMessage = null,
                            recommenderErrorRequestId = null,
                        )
                    }
                },
                onFailure = ::showRecommenderFailure,
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            if (scopeIsCurrent(scope)) showRecommenderFailure(failure)
        }
    }

    private suspend fun clearRecommender(scope: AccountScope) {
        if (scopeIsCurrent(scope)) feedback.update { it.withoutRecommender() }
    }

    private suspend fun scopeIsCurrent(expected: AccountScope): Boolean =
        activeAccountScope.observe().first() == expected

    private fun updateRecommender(enabled: Boolean) {
        val previousEnabled = feedback.value.recommenderEnabled
        if (!feedback.value.recommenderAvailable || feedback.value.isRecommenderLoading ||
            feedback.value.isRecommenderUpdating || previousEnabled == enabled
        ) return
        viewModelScope.launch {
            val scope = activeAccountScope.observe().first() ?: return@launch
            if (!feedback.value.recommenderAvailable) return@launch
            feedback.update {
                it.copy(
                    recommenderEnabled = enabled,
                    isRecommenderUpdating = true,
                    recommenderErrorMessage = null,
                    recommenderErrorRequestId = null,
                )
            }
            val result = recommenderSettingsRepository.setEnabled(enabled)
            if (!scopeIsCurrent(scope)) return@launch
            result.fold(
                onSuccess = { saved ->
                    feedback.update {
                        it.copy(recommenderEnabled = saved, isRecommenderUpdating = false)
                    }
                },
                onFailure = { failure -> showRecommenderFailure(failure, previousEnabled) },
            )
        }
    }

    private fun showRecommenderFailure(failure: Throwable, enabled: Boolean? = null) {
        val details = errorMapper.details(failure, R.string.settings_content_update_failed)
        feedback.update {
            it.copy(
                recommenderEnabled = enabled ?: it.recommenderEnabled,
                isRecommenderLoading = false,
                isRecommenderUpdating = false,
                recommenderErrorMessage = details.message,
                recommenderErrorRequestId = details.requestId,
            )
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            feedback.update { it.copy(isLoading = true, errorMessage = null, errorRequestId = null) }
            repository.refresh().fold(
                onSuccess = { feedback.update { it.copy(isLoading = false) } },
                onFailure = { failure -> showFailure(failure, isLoading = false) },
            )
        }
    }

    private fun update(action: ContentSettingsAction.Update) {
        if (feedback.value.isUpdating) return
        viewModelScope.launch {
            feedback.update { it.copy(isUpdating = true, errorMessage = null, errorRequestId = null) }
            repository.update { it.updatedBy(action) }.fold(
                onSuccess = {
                    if (action is ContentSettingsAction.SetDefaultLandingPage) {
                        val scope = activeAccountScope.require()
                        startupLandingStore.setLandingPage(scope, action.page)
                    }
                    feedback.update { it.copy(isUpdating = false) }
                },
                onFailure = { failure -> showFailure(failure, isUpdating = false) },
            )
        }
    }

    private fun showFailure(
        failure: Throwable,
        isLoading: Boolean = feedback.value.isLoading,
        isUpdating: Boolean = feedback.value.isUpdating,
    ) {
        val details = errorMapper.details(failure, R.string.settings_content_update_failed)
        feedback.update {
            it.copy(
                isLoading = isLoading,
                isUpdating = isUpdating,
                errorMessage = details.message,
                errorRequestId = details.requestId,
            )
        }
    }
}

private data class ContentSettingsFeedback(
    val isLoading: Boolean = true,
    val isUpdating: Boolean = false,
    val recommenderAvailable: Boolean = false,
    val recommenderEnabled: Boolean = false,
    val recommenderExperimental: Boolean = false,
    val isRecommenderLoading: Boolean = false,
    val isRecommenderUpdating: Boolean = false,
    val recommenderErrorMessage: String? = null,
    val recommenderErrorRequestId: String? = null,
    val errorMessage: String? = null,
    val errorRequestId: String? = null,
)

private fun ContentSettingsFeedback.withoutRecommender(loading: Boolean = false) = copy(
    recommenderAvailable = false,
    recommenderEnabled = false,
    recommenderExperimental = false,
    isRecommenderLoading = loading,
    isRecommenderUpdating = false,
    recommenderErrorMessage = null,
    recommenderErrorRequestId = null,
)

private fun UserSettings.toContentSettingsState(feedback: ContentSettingsFeedback) =
    ContentSettingsState(
        isLoading = feedback.isLoading,
        isUpdating = feedback.isUpdating,
        defaultLandingPage = defaultLandingPage,
        hideHomeRecommendations = hideHomeRecommendations,
        hideContinueWatching = hideContinueWatching,
        hideRelatedVideos = hideRelatedVideos,
        hideComments = hideComments,
        hideShorts = hideShorts,
        hideSubscriptionLiveStreams = hideSubscriptionLiveStreams,
        supportsHideSubscriptionLiveStreams = supportsHideSubscriptionLiveStreams,
        recommenderAvailable = feedback.recommenderAvailable,
        recommenderEnabled = feedback.recommenderEnabled,
        recommenderExperimental = feedback.recommenderExperimental,
        isRecommenderLoading = feedback.isRecommenderLoading,
        isRecommenderUpdating = feedback.isRecommenderUpdating,
        recommenderErrorMessage = feedback.recommenderErrorMessage,
        recommenderErrorRequestId = feedback.recommenderErrorRequestId,
        deArrowEnabled = deArrowEnabled,
        deArrowTitleMode = deArrowTitleMode,
        deArrowThumbnailMode = deArrowThumbnailMode,
        deArrowTrustMode = deArrowTrustMode,
        errorMessage = feedback.errorMessage,
        errorRequestId = feedback.errorRequestId,
    )
