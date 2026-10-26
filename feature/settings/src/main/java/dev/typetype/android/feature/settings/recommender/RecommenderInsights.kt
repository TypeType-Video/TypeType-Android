package dev.typetype.android.feature.settings.recommender

import dev.typetype.android.domain.feed.HomeFeedRepository
import dev.typetype.android.domain.recommendations.DataCategory
import dev.typetype.android.domain.recommendations.RecommenderFeedbackRepository
import dev.typetype.android.domain.recommendations.RecommenderRepository
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal enum class Insight { Home, Profile, Data, LastHome, Feedback }

internal class RecommenderInsights(
    private val scope: CoroutineScope,
    private val state: MutableStateFlow<RecommenderSettingsState>,
    private val repository: RecommenderRepository,
    private val feedRepository: HomeFeedRepository,
    private val feedbackRepository: RecommenderFeedbackRepository,
) {
    private val jobs = mutableMapOf<Insight, Job>()

    fun ensure(section: RecommenderSection?) {
        needs(section).forEach { insight ->
            val current = read(insight)
            if (current is Loadable.Idle || current is Loadable.Failed) load(insight, silent = false)
        }
    }

    fun refresh(section: RecommenderSection?) {
        val needed = needs(section)
        Insight.entries.forEach { insight ->
            when {
                insight == Insight.Feedback -> Unit
                insight in needed -> load(insight, silent = read(insight) is Loadable.Loaded<*>)
                else -> {
                    jobs.remove(insight)?.cancel()
                    clear(insight)
                }
            }
        }
    }

    fun updateData(transform: (List<DataCategory>) -> List<DataCategory>) = state.update { current ->
        val categories = current.data.value ?: return@update current
        current.copy(data = Loadable.Loaded(transform(categories)))
    }

    fun removeFeedback(id: String) {
        state.update { current ->
            val entries = current.feedback.value ?: return@update current
            current.copy(feedback = Loadable.Loaded(entries.filterNot { it.id == id }))
        }
        scope.launch {
            feedbackRepository.withdraw(id).onFailure { load(Insight.Feedback, silent = true) }
        }
    }

    private fun needs(section: RecommenderSection?): List<Insight> {
        val enabled = state.value.preferences.enabled
        return when (section) {
            RecommenderSection.Overview -> if (enabled) listOf(Insight.Home, Insight.Profile) else emptyList()
            RecommenderSection.Profile -> if (enabled) listOf(Insight.Profile) else emptyList()
            RecommenderSection.Data -> listOf(Insight.Data)
            RecommenderSection.Details -> listOf(Insight.LastHome)
            RecommenderSection.Feedback -> listOf(Insight.Feedback)
            else -> emptyList()
        }
    }

    private fun load(insight: Insight, silent: Boolean) {
        val locale = Locale.getDefault().toLanguageTag()
        when (insight) {
            Insight.Home -> launch(insight, silent, { s, v -> s.copy(home = v) }) {
                feedRepository.loadHomeRecommendations(limit = HOME_PAGE_SIZE).map { HomeComposition(it.algorithm, it.rows) }
            }
            Insight.Profile -> launch(insight, silent, { s, v -> s.copy(profile = v) }) { repository.profile(locale) }
            Insight.Data -> launch(insight, silent, { s, v -> s.copy(data = v) }) { repository.data() }
            Insight.LastHome -> launch(insight, silent, { s, v -> s.copy(lastHome = v) }) { repository.lastHome() }
            Insight.Feedback -> launch(insight, silent, { s, v -> s.copy(feedback = v) }) { repository.feedback() }
        }
    }

    private fun <T> launch(
        insight: Insight,
        silent: Boolean,
        put: (RecommenderSettingsState, Loadable<T>) -> RecommenderSettingsState,
        fetch: suspend () -> Result<T>,
    ) {
        jobs.remove(insight)?.cancel()
        if (!silent) state.update { put(it, Loadable.Loading) }
        jobs[insight] = scope.launch {
            val result = fetch().fold(onSuccess = { Loadable.Loaded(it) }, onFailure = { Loadable.Failed })
            state.update { put(it, result) }
        }
    }

    private fun read(insight: Insight): Loadable<*> = with(state.value) {
        when (insight) {
            Insight.Home -> home
            Insight.Profile -> profile
            Insight.Data -> data
            Insight.LastHome -> lastHome
            Insight.Feedback -> feedback
        }
    }

    private fun clear(insight: Insight) = state.update {
        when (insight) {
            Insight.Home -> it.copy(home = Loadable.Idle)
            Insight.Profile -> it.copy(profile = Loadable.Idle)
            Insight.Data -> it.copy(data = Loadable.Idle)
            Insight.LastHome -> it.copy(lastHome = Loadable.Idle)
            Insight.Feedback -> it.copy(feedback = Loadable.Idle)
        }
    }

    private companion object {
        const val HOME_PAGE_SIZE = 30
    }
}
