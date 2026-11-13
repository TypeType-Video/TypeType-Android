package dev.typetype.android.data.comments

import dev.typetype.android.core.error.suspendRunCatching
import dev.typetype.android.data.account.ActiveAccountScope
import dev.typetype.android.data.network.TypeTypeApiHolder
import dev.typetype.android.data.network.dto.CommentImageDto
import dev.typetype.android.data.network.dto.CommentItem
import dev.typetype.android.data.network.dto.CommentVoiceReplyDto
import dev.typetype.android.data.network.requireSuccessfulResponse
import dev.typetype.android.domain.comments.Comment
import dev.typetype.android.domain.comments.CommentImage
import dev.typetype.android.domain.comments.CommentSearchPage
import dev.typetype.android.domain.comments.CommentVoiceReply
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
        suspendRunCatching {
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
        suspendRunCatching {
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

    override suspend fun searchComments(
        videoUrl: String,
        query: String,
        cursor: String?,
    ): Result<CommentSearchPage> = suspendRunCatching {
        val scope = activeAccountScope.require()
        val api = apiHolder.require(scope)
        val response = withContext(Dispatchers.IO) {
            api.searchComments(videoUrl = videoUrl, query = query, cursor = cursor)
        }
        response.requireSuccessfulResponse()
        val body = response.body() ?: error("Empty comment search body")
        activeAccountScope.verify(scope)
        CommentSearchPage(
            comments = body.comments.map(CommentItem::toDomainComment),
            nextCursor = body.nextpage,
            scannedPages = body.scannedPages,
            commentsDisabled = body.commentsDisabled,
        )
    }

    override suspend fun loadVoiceReplyAudio(voiceReplyUrl: String): Result<ByteArray> =
        suspendRunCatching {
            val scope = activeAccountScope.require()
            val api = apiHolder.require(scope)
            val bytes = withContext(Dispatchers.IO) {
                val response = api.commentVoiceAudio(voiceReplyUrl)
                response.requireSuccessfulResponse()
                val body = response.body() ?: error("Empty voice reply body")
                body.use {
                    check(it.contentLength() <= MAX_VOICE_REPLY_BYTES) { "Voice reply is too large" }
                    it.bytes()
                }
            }
            activeAccountScope.verify(scope)
            check(bytes.isNotEmpty() && bytes.size <= MAX_VOICE_REPLY_BYTES) { "Invalid voice reply audio" }
            bytes
        }
}

private const val MAX_VOICE_REPLY_BYTES = 8L * 1024 * 1024

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
    image = image?.toDomainImage(),
    voiceReply = voiceReply?.toDomainVoiceReply(),
)

private fun CommentImageDto.toDomainImage(): CommentImage? {
    val imageUrl = url?.takeIf(String::isNotBlank)
    val missing = unavailableText?.takeIf(String::isNotBlank)
    if (imageUrl == null && missing == null) return null
    return CommentImage(url = imageUrl, animated = animated, unavailableText = missing)
}

private fun CommentVoiceReplyDto.toDomainVoiceReply(): CommentVoiceReply? {
    if (url.isBlank() && transcript.isBlank()) return null
    return CommentVoiceReply(
        url = url,
        durationMs = durationMs.coerceAtLeast(0),
        transcript = transcript,
        playable = playable && url.isNotBlank(),
        unavailableText = unavailableText?.takeIf(String::isNotBlank),
    )
}
