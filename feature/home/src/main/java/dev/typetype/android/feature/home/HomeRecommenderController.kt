package dev.typetype.android.feature.home

import dev.typetype.android.domain.feed.Video
import dev.typetype.android.domain.recommendations.PageRefusalReason
import dev.typetype.android.domain.recommendations.RecommenderFeedbackRepository
import dev.typetype.android.domain.recommendations.RecommenderSettingsRepository
import dev.typetype.android.domain.recommendations.VideoRefusalReason
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class HomeRecommenderController(
    private val scope: CoroutineScope,
    private val state: MutableStateFlow<HomeState>,
    private val settings: RecommenderSettingsRepository,
    private val feedback: RecommenderFeedbackRepository,
    private val onEnabled: () -> Unit,
) {
    fun refreshEnabled() {
        if (!state.value.recommender.available) return
        scope.launch {
            settings.enabled().onSuccess { enabled -> update { it.copy(enabled = enabled) } }
        }
    }

    fun enable() {
        scope.launch {
            settings.setEnabled(true).onSuccess { enabled ->
                update { it.copy(enabled = enabled) }
                if (enabled) onEnabled()
            }
        }
    }

    fun dismissWelcome() = update { it.copy(welcomeDismissed = true) }

    fun notForMe(video: Video) {
        update { it.copy(refused = it.refused + (video.url to VideoRefusal())) }
        scope.launch {
            feedback.notForMe(video).fold(
                onSuccess = { id -> setRefusal(video.url) { it.copy(feedbackId = id) } },
                onFailure = { update { it.copy(refused = it.refused - video.url) } },
            )
        }
    }

    fun explain(video: Video, reason: VideoRefusalReason) {
        setRefusal(video.url) { it.copy(reason = reason) }
        scope.launch { feedback.notForMe(video, reason) }
    }

    fun undo(videoUrl: String) {
        val id = state.value.recommender.refused[videoUrl]?.feedbackId
        update { it.copy(refused = it.refused - videoUrl) }
        if (!id.isNullOrEmpty()) scope.launch { feedback.withdraw(id) }
    }

    fun ratePage(good: Boolean) {
        if (good) {
            update { it.copy(pageVerdict = PageVerdict.Thanked) }
            scope.launch { feedback.ratePage(good = true) }
        } else {
            update { it.copy(pageVerdict = PageVerdict.AskingWhy) }
        }
    }

    fun choosePageReason(reason: PageRefusalReason) = update {
        it.copy(pageReason = reason.takeUnless { selected -> selected == it.pageReason })
    }

    fun sendPageVerdict(comment: String) {
        val reason = state.value.recommender.pageReason
        update { it.copy(pageVerdict = PageVerdict.Thanked) }
        scope.launch { feedback.ratePage(good = false, reason = reason, comment = comment) }
    }

    private fun setRefusal(videoUrl: String, transform: (VideoRefusal) -> VideoRefusal) = update {
        val current = it.refused[videoUrl] ?: return@update it
        it.copy(refused = it.refused + (videoUrl to transform(current)))
    }

    private fun update(transform: (HomeRecommenderState) -> HomeRecommenderState) {
        state.update { it.copy(recommender = transform(it.recommender)) }
    }
}
