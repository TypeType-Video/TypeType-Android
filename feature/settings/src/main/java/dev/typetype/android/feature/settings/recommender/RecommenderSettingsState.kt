package dev.typetype.android.feature.settings.recommender

import dev.typetype.android.domain.recommendations.AlgorithmCatalog
import dev.typetype.android.domain.recommendations.AlgorithmInfo
import dev.typetype.android.domain.recommendations.DataFamily
import dev.typetype.android.domain.recommendations.InterestGroup
import dev.typetype.android.domain.recommendations.RecommenderPreferences

data class RecommenderSettingsState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val loadFailed: Boolean = false,
    val saveFailed: Boolean = false,
    val catalog: AlgorithmCatalog? = null,
    val preferences: RecommenderPreferences = RecommenderPreferences(),
    val interests: List<InterestGroup> = emptyList(),
    val families: List<DataFamily> = emptyList(),
) {
    val selectedAlgorithm: AlgorithmInfo?
        get() {
            val algorithms = catalog?.algorithms.orEmpty()
            val id = preferences.algorithmId ?: catalog?.defaultId
            return algorithms.firstOrNull { it.id == id } ?: algorithms.firstOrNull()
        }

    val canAddInterest: Boolean
        get() = preferences.customInterests.size < MAX_CUSTOM_INTERESTS
}

sealed interface RecommenderSettingsAction {
    data object Retry : RecommenderSettingsAction
    data class SetEnabled(val enabled: Boolean) : RecommenderSettingsAction
    data class ChooseAlgorithm(val id: String) : RecommenderSettingsAction
    data class SetParameter(val id: String, val value: Any) : RecommenderSettingsAction
    data object ResetParameters : RecommenderSettingsAction
    data class TogglePinned(val groupId: String) : RecommenderSettingsAction
    data class ToggleMuted(val groupId: String) : RecommenderSettingsAction
    data class AddInterest(val text: String) : RecommenderSettingsAction
    data class RemoveInterest(val text: String) : RecommenderSettingsAction
    data class SetFamilyUsed(val id: String, val used: Boolean) : RecommenderSettingsAction
}

internal const val MAX_CUSTOM_INTERESTS = 10
internal const val MAX_INTEREST_LENGTH = 60
