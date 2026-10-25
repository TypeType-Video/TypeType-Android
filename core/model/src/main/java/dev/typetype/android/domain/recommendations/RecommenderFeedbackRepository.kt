package dev.typetype.android.domain.recommendations

import dev.typetype.android.domain.feed.Video

enum class VideoRefusalReason(val wireName: String) {
    Seen("seen"),
    Channel("channel"),
    Topic("topic"),
    Other("other"),
}

enum class PageRefusalReason(val wireName: String) {
    Repetitive("repetitive"),
    Unrelated("unrelated"),
    Old("old"),
    Missing("missing"),
}

interface RecommenderFeedbackRepository {
    suspend fun notForMe(video: Video, reason: VideoRefusalReason? = null): Result<String>
    suspend fun withdraw(feedbackId: String): Result<Unit>
    suspend fun ratePage(good: Boolean, reason: PageRefusalReason? = null, comment: String? = null): Result<String>
}
