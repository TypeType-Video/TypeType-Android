package dev.typetype.android.data.comments

import dev.typetype.android.data.account.ActiveAccountScope
import dev.typetype.android.data.network.TypeTypeApiHolder
import dev.typetype.android.data.network.dto.CommentItem
import dev.typetype.android.data.network.requireSuccessfulResponse
import dev.typetype.android.domain.comments.Comment
import dev.typetype.android.domain.comments.CommentsPage
import dev.typetype.android.domain.comments.CommentsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class CommentsRepositoryImpl @Inject constructor(
    private val apiHolder: TypeTypeApiHolder,
    private val activeAccountScope: ActiveAccountScope,
) : CommentsRepository {

    override suspend fun loadPage(videoUrl: String, nextpage: String?): Result<CommentsPage> =
        runCatching {
            val scope = activeAccountScope.require()
            val api = apiHolder.require(scope)
            val response = withContext(Dispatchers.IO) {
                api.comments(videoUrl = videoUrl, nextpage = nextpage)
            }
            response.requireSuccessfulResponse()
            val body = response.body() ?: error("Empty comments body")
            activeAccountScope.verify(scope)
            CommentsPage(
                comments = body.comments.map(CommentItem::toDomainComment),
                nextpage = body.nextpage,
                commentsDisabled = body.commentsDisabled,
            )
        }

    override suspend fun loadReplies(videoUrl: String, repliesPage: String): Result<CommentsPage> =
        runCatching {
            val scope = activeAccountScope.require()
            val api = apiHolder.require(scope)
            val response = withContext(Dispatchers.IO) {
                api.commentReplies(videoUrl = videoUrl, repliesPage = repliesPage)
            }
            response.requireSuccessfulResponse()
            val body = response.body() ?: error("Empty replies body")
            activeAccountScope.verify(scope)
            CommentsPage(
                comments = body.comments.map(CommentItem::toDomainComment),
                nextpage = body.nextpage,
                commentsDisabled = body.commentsDisabled,
            )
        }

}

internal fun CommentItem.toDomainComment(): Comment = Comment(
    id = id,
    text = text,
    authorName = author,
    authorAvatarUrl = authorAvatarUrl,
    likeCount = likeCount,
    textualLikeCount = textualLikeCount,
    publishedTime = publishedTime,
    isHeartedByUploader = isHeartedByUploader,
    isPinned = isPinned,
    uploaderVerified = uploaderVerified,
    replyCount = replyCount,
    publishedAtMillis = publishedAt,
    repliesPage = repliesPage,
)
