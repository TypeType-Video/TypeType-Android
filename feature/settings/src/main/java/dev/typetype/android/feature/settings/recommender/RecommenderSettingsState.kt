package dev.typetype.android.feature.settings.recommender

import dev.typetype.android.domain.feed.HomeRecommendationRow
import dev.typetype.android.domain.recommendations.AlgorithmCatalog
import dev.typetype.android.domain.recommendations.AlgorithmInfo
import dev.typetype.android.domain.recommendations.AlgorithmParameter
import dev.typetype.android.domain.recommendations.DataCategory
import dev.typetype.android.domain.recommendations.FeedbackEntry
import dev.typetype.android.domain.recommendations.PageTrace
import dev.typetype.android.domain.recommendations.RecommenderPreferences
import dev.typetype.android.domain.recommendations.RecommenderProfile

enum class RecommenderAvailability { Loading, Ready, Guest, NotInstalled, NotOffered, Unreachable }

sealed interface Loadable<out T> {
    data object Idle : Loadable<Nothing>
    data object Loading : Loadable<Nothing>
    data object Failed : Loadable<Nothing>
    data class Loaded<T>(val value: T) : Loadable<T>
}

internal val <T> Loadable<T>.value: T?
    get() = (this as? Loadable.Loaded<T>)?.value

data class HomeComposition(val algorithm: String?, val rows: List<HomeRecommendationRow>)

data class RecommenderSettingsState(
    val availability: RecommenderAvailability = RecommenderAvailability.Loading,
    val section: RecommenderSection? = null,
    val catalog: AlgorithmCatalog? = null,
    val preferences: RecommenderPreferences = RecommenderPreferences(),
    val isSaving: Boolean = false,
    val saveFailed: Boolean = false,
    val home: Loadable<HomeComposition> = Loadable.Idle,
    val profile: Loadable<RecommenderProfile> = Loadable.Idle,
    val data: Loadable<List<DataCategory>> = Loadable.Idle,
    val lastHome: Loadable<PageTrace?> = Loadable.Idle,
    val feedback: Loadable<List<FeedbackEntry>> = Loadable.Idle,
    val openCategory: String? = null,
    val collectHistory: Boolean = true,
    val highlight: String? = null,
    val chosenAlgorithm: String? = null,
) {
    val algorithms: List<AlgorithmInfo>
        get() = catalog?.algorithms.orEmpty()

    val selectedAlgorithm: AlgorithmInfo?
        get() {
            val id = preferences.algorithmId ?: catalog?.defaultId
            return algorithms.firstOrNull { it.id == id } ?: algorithms.firstOrNull()
        }

    val canAddInterest: Boolean
        get() = preferences.customInterests.size < MAX_CUSTOM_INTERESTS

    fun valueOf(parameter: AlgorithmParameter): Any = preferences.parameters[parameter.id] ?: parameter.defaultValue

    fun algorithmName(id: String): String = algorithms.firstOrNull { it.id == id }?.name ?: id
}

internal val AlgorithmParameter.defaultValue: Any
    get() = when (this) {
        is AlgorithmParameter.Number -> default
        is AlgorithmParameter.Toggle -> default
    }

internal fun AlgorithmParameter.isDefault(value: Any): Boolean = when (this) {
    is AlgorithmParameter.Number -> (value as? Number)?.toDouble() == default
    is AlgorithmParameter.Toggle -> value == default
}

sealed interface RecommenderSettingsAction {
    data object Retry : RecommenderSettingsAction
    data class Open(val section: RecommenderSection?) : RecommenderSettingsAction
    data object Back : RecommenderSettingsAction
    data class OpenInterest(val groupId: String) : RecommenderSettingsAction
    data class SetEnabled(val enabled: Boolean) : RecommenderSettingsAction
    data class ChooseAlgorithm(val id: String, val announce: Boolean = false) : RecommenderSettingsAction
    data object ChosenShown : RecommenderSettingsAction
    data class SetParameter(val id: String, val value: Any) : RecommenderSettingsAction
    data class RestoreParameter(val id: String) : RecommenderSettingsAction
    data object ResetParameters : RecommenderSettingsAction
    data class TogglePinned(val groupId: String) : RecommenderSettingsAction
    data class ToggleMuted(val groupId: String) : RecommenderSettingsAction
    data class ToggleIgnored(val id: String) : RecommenderSettingsAction
    data object ForgetRecent : RecommenderSettingsAction
    data class AddInterest(val text: String) : RecommenderSettingsAction
    data class RemoveInterest(val text: String) : RecommenderSettingsAction
    data class OpenCategory(val id: String) : RecommenderSettingsAction
    data class ToggleUse(val categoryId: String) : RecommenderSettingsAction
    data class SetRetention(val categoryId: String, val days: Int?) : RecommenderSettingsAction
    data class ToggleExcluded(val itemId: String) : RecommenderSettingsAction
    data class SetCollect(val collect: Boolean) : RecommenderSettingsAction
    data class RemoveFeedback(val id: String) : RecommenderSettingsAction
}

internal const val MAX_CUSTOM_INTERESTS = 10
internal const val MAX_INTEREST_LENGTH = 60
