package dev.typetype.android.feature.home

import dev.typetype.android.domain.feed.HomeRecommendationRow
import dev.typetype.android.domain.recommendations.PageRefusalReason
import dev.typetype.android.domain.recommendations.VideoRefusalReason

data class VideoRefusal(
    val feedbackId: String? = null,
    val reason: VideoRefusalReason? = null,
)

sealed interface PageVerdict {
    data object Unasked : PageVerdict
    data object AskingWhy : PageVerdict
    data object Thanked : PageVerdict
}

data class HomeRecommenderState(
    val available: Boolean = false,
    val enabled: Boolean? = null,
    val welcomeDismissed: Boolean = false,
    val rows: List<HomeRecommendationRow> = emptyList(),
    val algorithm: String? = null,
    val preparing: Boolean = false,
    val refused: Map<String, VideoRefusal> = emptyMap(),
    val pageVerdict: PageVerdict = PageVerdict.Unasked,
    val pageReason: PageRefusalReason? = null,
) {
    val showWelcome: Boolean
        get() = available && enabled == false && !welcomeDismissed

    val personalized: Boolean
        get() = algorithm != null && rows.isNotEmpty()
}

internal fun mergeRows(
    current: List<HomeRecommendationRow>,
    next: List<HomeRecommendationRow>,
): List<HomeRecommendationRow> {
    val merged = LinkedHashMap<String, HomeRecommendationRow>()
    (current + next).forEach { row ->
        val known = merged[row.id]
        merged[row.id] = known?.copy(videoUrls = (known.videoUrls + row.videoUrls).distinct()) ?: row
    }
    return merged.values.toList()
}
