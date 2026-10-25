package dev.typetype.android.feature.home

import dev.typetype.android.domain.feed.Video
import dev.typetype.android.domain.recommendations.PageRefusalReason
import dev.typetype.android.domain.recommendations.VideoRefusalReason

sealed interface HomeAction {
    data object OnRefresh : HomeAction
    data object OnLoadMore : HomeAction
    data object EnableRecommendations : HomeAction
    data object DismissWelcome : HomeAction
    data class NotForMe(val video: Video) : HomeAction
    data class ExplainRefusal(val video: Video, val reason: VideoRefusalReason) : HomeAction
    data class UndoRefusal(val videoUrl: String) : HomeAction
    data class RatePage(val good: Boolean) : HomeAction
    data class ChoosePageReason(val reason: PageRefusalReason) : HomeAction
    data class SendPageVerdict(val comment: String) : HomeAction
}
