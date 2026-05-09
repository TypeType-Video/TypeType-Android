package dev.typetype.android.data.comments

import dev.typetype.android.data.network.TypeTypeApiHolder
import dev.typetype.android.data.network.dto.CommentItem
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
) : CommentsRepository {

    override suspend fun loadPage(videoUrl: String, nextpage: String?): Result<CommentsPage> =
        runCatching {
            val api = apiHolder.require()
            val response = withContext(Dispatchers.IO) {
                api.comments(videoUrl = videoUrl, nextpage = nextpage)
            }
            if (!response.isSuccessful) {
                error("Comments failed (HTTP ${response.code()})")
            }
            val body = response.body() ?: error("Empty comments body")
            CommentsPage(
                comments = body.comments.map { it.toDomain() },
                nextpage = body.nextpage,
                commentsDisabled = body.commentsDisabled,
            )
        }

    override suspend fun loadReplies(videoUrl: String, repliesPage: String): Result<CommentsPage> =
        runCatching {
            val api = apiHolder.require()
            val response = withContext(Dispatchers.IO) {
                api.commentReplies(videoUrl = videoUrl, repliesPage = repliesPage)
            }
            if (!response.isSuccessful) {
                error("Replies failed (HTTP ${response.code()})")
            }
            val body = response.body() ?: error("Empty replies body")
            CommentsPage(
                comments = body.comments.map { it.toDomain() },
                nextpage = body.nextpage,
                commentsDisabled = body.commentsDisabled,
            )
        }

    private fun CommentItem.toDomain(): Comment = Comment(
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
        repliesPage = repliesPage,
    )
}
