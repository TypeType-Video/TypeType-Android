package dev.typetype.android.feature.settings.recommender

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.domain.recommendations.RecommenderPreferences
import dev.typetype.android.domain.recommendations.RecommenderRepository
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class RecommenderSettingsViewModel @Inject constructor(
    private val repository: RecommenderRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(RecommenderSettingsState())
    val state = _state.asStateFlow()
    private var saveJob: Job? = null

    init {
        load()
    }

    fun onAction(action: RecommenderSettingsAction) {
        when (action) {
            RecommenderSettingsAction.Retry -> load()
            is RecommenderSettingsAction.SetEnabled -> edit { it.copy(enabled = action.enabled) }
            is RecommenderSettingsAction.ChooseAlgorithm -> edit { it.copy(algorithmId = action.id, enabled = true) }
            is RecommenderSettingsAction.SetParameter -> edit { it.copy(parameters = it.parameters + (action.id to action.value)) }
            RecommenderSettingsAction.ResetParameters -> {
                val ids = _state.value.selectedAlgorithm?.parameters?.map { it.id }.orEmpty().toSet()
                edit { it.copy(parameters = it.parameters - ids) }
            }
            is RecommenderSettingsAction.TogglePinned -> edit {
                it.copy(pinnedGroups = it.pinnedGroups.toggle(action.groupId), mutedGroups = it.mutedGroups - action.groupId)
            }
            is RecommenderSettingsAction.ToggleMuted -> edit {
                it.copy(mutedGroups = it.mutedGroups.toggle(action.groupId), pinnedGroups = it.pinnedGroups - action.groupId)
            }
            is RecommenderSettingsAction.AddInterest -> {
                val text = action.text.trim().take(MAX_INTEREST_LENGTH)
                if (text.isEmpty() || !_state.value.canAddInterest) return
                edit { it.copy(customInterests = (it.customInterests + text).distinct()) }
            }
            is RecommenderSettingsAction.RemoveInterest -> edit { it.copy(customInterests = it.customInterests - action.text) }
            is RecommenderSettingsAction.SetFamilyUsed -> {
                edit {
                    it.copy(unusedData = if (action.used) it.unusedData - action.id else (it.unusedData + action.id).distinct())
                }
                _state.update { state ->
                    state.copy(families = state.families.map { family -> if (family.id == action.id) family.copy(used = action.used) else family })
                }
            }
        }
    }

    private fun load() {
        _state.update { it.copy(isLoading = true, loadFailed = false) }
        viewModelScope.launch {
            val locale = Locale.getDefault().toLanguageTag()
            val catalog = async { repository.catalog(locale) }
            val preferences = async { repository.preferences() }
            val interests = async { repository.interests(locale) }
            val families = async { repository.dataFamilies() }
            val loadedCatalog = catalog.await().getOrNull()
            val loadedPreferences = preferences.await().getOrNull()
            _state.update {
                it.copy(
                    isLoading = false,
                    loadFailed = loadedCatalog == null || loadedPreferences == null,
                    catalog = loadedCatalog,
                    preferences = loadedPreferences ?: it.preferences,
                    interests = interests.await().getOrDefault(emptyList()),
                    families = families.await().getOrDefault(emptyList()),
                )
            }
        }
    }

    private fun edit(transform: (RecommenderPreferences) -> RecommenderPreferences) {
        val updated = transform(_state.value.preferences)
        _state.update { it.copy(preferences = updated, isSaving = true, saveFailed = false) }
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            repository.save(updated).fold(
                onSuccess = { saved -> _state.update { it.copy(preferences = saved, isSaving = false) } },
                onFailure = { _state.update { it.copy(isSaving = false, saveFailed = true) } },
            )
        }
    }
}

private fun List<String>.toggle(value: String): List<String> = if (value in this) this - value else this + value
