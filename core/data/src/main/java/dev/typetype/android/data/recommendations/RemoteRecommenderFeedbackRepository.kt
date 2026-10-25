package dev.typetype.android.data.recommendations

import dev.typetype.android.core.error.suspendRunCatching
import dev.typetype.android.data.account.ActiveAccountScope
import dev.typetype.android.data.network.TypeTypeApiHolder
import dev.typetype.android.data.network.dto.RecommenderFeedbackRequestDto
import dev.typetype.android.data.network.requireSuccessfulResponse
import dev.typetype.android.domain.feed.Video
import dev.typetype.android.domain.recommendations.PageRefusalReason
import dev.typetype.android.domain.recommendations.RecommenderFeedbackRepository
import dev.typetype.android.domain.recommendations.VideoRefusalReason
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class RemoteRecommenderFeedbackRepository @Inject constructor(
    private val apiHolder: TypeTypeApiHolder,
    private val activeAccountScope: ActiveAccountScope,
) : RecommenderFeedbackRepository {
    override suspend fun notForMe(video: Video, reason: VideoRefusalReason?): Result<String> = send(
        RecommenderFeedbackRequestDto(
            kind = "video",
            verdict = "not_for_me",
            videoUrl = video.url,
            reason = reason?.wireName,
            title = video.title.take(MAX_TEXT),
            channelName = video.uploaderName.take(MAX_TEXT),
            thumbnailUrl = video.thumbnailUrl.takeIf { it.length <= MAX_URL },
        ),
    )

    override suspend fun ratePage(good: Boolean, reason: PageRefusalReason?, comment: String?): Result<String> = send(
        RecommenderFeedbackRequestDto(
            kind = "page",
            verdict = if (good) "good" else "bad",
            reason = reason?.wireName,
            comment = comment?.trim()?.takeIf { it.isNotEmpty() }?.take(MAX_COMMENT),
        ),
    )

    override suspend fun withdraw(feedbackId: String): Result<Unit> = suspendRunCatching {
        val scope = activeAccountScope.require()
        val response = withContext(Dispatchers.IO) {
            apiHolder.require(scope).deleteRecommenderFeedback(feedbackId)
        }
        response.requireSuccessfulResponse()
    }

    private suspend fun send(request: RecommenderFeedbackRequestDto): Result<String> = suspendRunCatching {
        val scope = activeAccountScope.require()
        val response = withContext(Dispatchers.IO) {
            apiHolder.require(scope).sendRecommenderFeedback(request)
        }
        response.requireSuccessfulResponse()
        response.body()?.id.orEmpty()
    }

    private companion object {
        const val MAX_TEXT = 300
        const val MAX_URL = 500
        const val MAX_COMMENT = 1000
    }
}
