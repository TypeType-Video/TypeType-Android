package dev.typetype.android.feature.settings.recommender

import dev.typetype.android.domain.recommendations.RecommenderPreferences
import dev.typetype.android.domain.recommendations.RecommenderRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class RecommenderEditor(
    private val scope: CoroutineScope,
    private val state: MutableStateFlow<RecommenderSettingsState>,
    private val repository: RecommenderRepository,
    private val onSaved: () -> Unit,
) {
    private var saveJob: Job? = null

    fun edit(transform: (RecommenderPreferences) -> RecommenderPreferences?) {
        val updated = transform(state.value.preferences) ?: return
        state.update { it.copy(preferences = updated, isSaving = true, saveFailed = false) }
        saveJob?.cancel()
        saveJob = scope.launch {
            repository.save(updated).fold(
                onSuccess = { saved ->
                    state.update { it.copy(preferences = saved, isSaving = false) }
                    onSaved()
                },
                onFailure = {
                    val kept = repository.preferences().getOrNull()
                    state.update { it.copy(preferences = kept ?: it.preferences, isSaving = false, saveFailed = true) }
                },
            )
        }
    }
}
