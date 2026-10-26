package dev.typetype.android.feature.settings.recommender

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.data.account.ActiveAccountScope
import dev.typetype.android.domain.account.AccountRepository
import dev.typetype.android.domain.feed.HomeFeedRepository
import dev.typetype.android.domain.recommendations.RecommenderFeedbackRepository
import dev.typetype.android.domain.recommendations.RecommenderRepository
import dev.typetype.android.domain.server.Server
import dev.typetype.android.domain.server.ServerCapabilitiesRepository
import dev.typetype.android.domain.server.ServerRepository
import dev.typetype.android.domain.usersettings.UserSettingsRepository
import dev.typetype.android.feature.settings.accountFeaturesAvailable
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class RecommenderSettingsViewModel @Inject constructor(
    private val repository: RecommenderRepository,
    private val capabilitiesRepository: ServerCapabilitiesRepository,
    private val userSettingsRepository: UserSettingsRepository,
    serverRepository: ServerRepository,
    activeAccountScope: ActiveAccountScope,
    accountRepository: AccountRepository,
    feedRepository: HomeFeedRepository,
    feedbackRepository: RecommenderFeedbackRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(RecommenderSettingsState())
    val state = _state.asStateFlow()
    private val insights = RecommenderInsights(viewModelScope, _state, repository, feedRepository, feedbackRepository)
    private val editor = RecommenderEditor(viewModelScope, _state, repository) { insights.refresh(_state.value.section) }
    private var server: Server? = null
    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            combine(
                serverRepository.observeCurrentServer(),
                activeAccountScope.observe(),
                accountRepository.observeAccounts(),
            ) { current, scope, accounts -> current to accountFeaturesAvailable(scope, accounts) }
                .distinctUntilChanged { old, new -> old.first?.id == new.first?.id && old.second == new.second }
                .collectLatest { (current, signedIn) ->
                    server = current
                    if (!signedIn || current == null) {
                        _state.update { it.copy(availability = RecommenderAvailability.Guest) }
                    } else {
                        checkAvailability(current)
                    }
                }
        }
        viewModelScope.launch {
            userSettingsRepository.observe().collect { settings ->
                _state.update { it.copy(collectHistory = !settings.disableWatchHistory) }
            }
        }
    }

    fun onAction(action: RecommenderSettingsAction) {
        when (action) {
            RecommenderSettingsAction.Retry -> retry()
            is RecommenderSettingsAction.Open -> open(action.section)
            RecommenderSettingsAction.Back -> back()
            is RecommenderSettingsAction.OpenInterest -> {
                _state.update { it.copy(section = RecommenderSection.Profile, highlight = action.groupId) }
                insights.ensure(RecommenderSection.Profile)
            }
            is RecommenderSettingsAction.SetEnabled -> editor.edit { it.copy(enabled = action.enabled) }
            is RecommenderSettingsAction.ChooseAlgorithm -> {
                editor.edit { it.choose(action.id) }
                if (action.announce) _state.update { it.copy(chosenAlgorithm = it.algorithmName(action.id)) }
            }
            RecommenderSettingsAction.ChosenShown -> _state.update { it.copy(chosenAlgorithm = null) }
            is RecommenderSettingsAction.SetParameter -> editor.edit { it.copy(parameters = it.parameters + (action.id to action.value)) }
            is RecommenderSettingsAction.RestoreParameter -> editor.edit { it.copy(parameters = it.parameters - action.id) }
            RecommenderSettingsAction.ResetParameters -> editor.edit { it.copy(parameters = emptyMap()) }
            is RecommenderSettingsAction.TogglePinned -> editor.edit { it.togglePinned(action.groupId) }
            is RecommenderSettingsAction.ToggleMuted -> editor.edit { it.toggleMuted(action.groupId) }
            is RecommenderSettingsAction.ToggleIgnored -> editor.edit { it.toggleIgnored(action.id) }
            RecommenderSettingsAction.ForgetRecent -> editor.edit { it.copy(sessionResetAt = System.currentTimeMillis()) }
            is RecommenderSettingsAction.AddInterest -> editor.edit { it.addInterest(action.text) }
            is RecommenderSettingsAction.RemoveInterest -> editor.edit { it.removeInterest(action.text) }
            is RecommenderSettingsAction.OpenCategory -> _state.update { it.copy(openCategory = action.id) }
            is RecommenderSettingsAction.ToggleUse -> {
                editor.edit { it.toggleUse(action.categoryId) }
                insights.updateData { list -> list.map { if (it.id == action.categoryId) it.copy(used = !it.used) else it } }
            }
            is RecommenderSettingsAction.SetRetention -> {
                editor.edit { it.withRetention(action.categoryId, action.days) }
                insights.updateData { list -> list.map { if (it.id == action.categoryId) it.copy(retentionDays = action.days) else it } }
            }
            is RecommenderSettingsAction.ToggleExcluded -> {
                editor.edit { it.toggleExcluded(action.itemId) }
                insights.updateData { list ->
                    list.map { category ->
                        category.copy(items = category.items.map { if (it.id == action.itemId) it.copy(excluded = !it.excluded) else it })
                    }
                }
            }
            is RecommenderSettingsAction.SetCollect -> viewModelScope.launch {
                userSettingsRepository.update { it.copy(disableWatchHistory = !action.collect) }
            }
            is RecommenderSettingsAction.RemoveFeedback -> insights.removeFeedback(action.id)
        }
    }

    private fun open(section: RecommenderSection?) {
        _state.update { it.copy(section = section, openCategory = null, highlight = null) }
        insights.ensure(section)
    }

    private fun back() = _state.update {
        when {
            it.openCategory != null -> it.copy(openCategory = null)
            else -> it.copy(section = null, highlight = null)
        }
    }

    private fun retry() {
        val current = server ?: return
        when (_state.value.availability) {
            RecommenderAvailability.Unreachable, RecommenderAvailability.NotOffered -> {
                viewModelScope.launch { checkAvailability(current) }
            }
            else -> insights.ensure(_state.value.section)
        }
    }

    private suspend fun checkAvailability(current: Server) {
        _state.update { it.copy(availability = RecommenderAvailability.Loading) }
        val fresh = capabilitiesRepository.refresh(current.id).getOrDefault(current)
        when {
            !fresh.recommender.installed -> _state.update { it.copy(availability = RecommenderAvailability.NotInstalled) }
            !fresh.recommender.enabled -> _state.update { it.copy(availability = RecommenderAvailability.NotOffered) }
            else -> load()
        }
    }

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val catalog = async { repository.catalog(Locale.getDefault().toLanguageTag()) }
            val preferences = async { repository.preferences() }
            val loadedCatalog = catalog.await().getOrNull()
            val loadedPreferences = preferences.await().getOrNull()
            if (loadedCatalog == null || loadedPreferences == null) {
                _state.update { it.copy(availability = RecommenderAvailability.Unreachable) }
                return@launch
            }
            _state.update {
                it.copy(
                    availability = RecommenderAvailability.Ready,
                    catalog = loadedCatalog,
                    preferences = loadedPreferences,
                )
            }
            insights.ensure(_state.value.section)
        }
    }
}
