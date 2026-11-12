package dev.typetype.android.data.channel

import dev.typetype.android.core.error.suspendRunCatching
import dev.typetype.android.data.account.ActiveAccountScope
import dev.typetype.android.data.network.TypeTypeApiHolder
import dev.typetype.android.data.network.dto.CommunityPostDto
import dev.typetype.android.data.network.requireSuccessfulResponse
import dev.typetype.android.domain.channel.ChannelPostsPage
import dev.typetype.android.domain.channel.ChannelPostsRepository
import dev.typetype.android.domain.channel.CommunityPost
import dev.typetype.android.domain.channel.CommunityPostPoll
import dev.typetype.android.domain.channel.CommunityPostPollChoice
import dev.typetype.android.domain.channel.CommunityPostVideo
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class ChannelPostsRepositoryImpl @Inject constructor(
    private val apiHolder: TypeTypeApiHolder,
    private val activeAccountScope: ActiveAccountScope,
) : ChannelPostsRepository {
    override suspend fun loadPosts(channelUrl: String, nextPage: String?): Result<ChannelPostsPage> =
        suspendRunCatching {
            val scope = activeAccountScope.require()
            val api = apiHolder.require(scope)
            val response = withContext(Dispatchers.IO) { api.channelPosts(channelUrl, nextPage) }
            response.requireSuccessfulResponse()
            val body = response.body() ?: error("Empty channel posts body")
            activeAccountScope.verify(scope)
            ChannelPostsPage(posts = body.posts.map { it.toDomain() }, nextPage = body.nextpage)
        }
}

internal fun CommunityPostDto.toDomain(): CommunityPost = CommunityPost(
    id = id,
    text = text,
    authorName = authorName,
    authorAvatarUrl = authorAvatarUrl,
    publishedTime = publishedTime,
    publishedAtMillis = publishedAt,
    likeLabel = textualLikeCount.trim().ifEmpty { likeCount?.takeIf { it > 0 }?.toString().orEmpty() },
    commentLabel = textualCommentCount.trim(),
    images = images.filter(String::isNotBlank),
    poll = poll?.takeIf { it.choices.isNotEmpty() }?.let { poll ->
        CommunityPostPoll(
            choices = poll.choices.map { CommunityPostPollChoice(it.text, it.imageUrl?.takeIf(String::isNotBlank)) },
            voteLabel = poll.textualVoteCount.trim(),
        )
    },
    video = video?.takeIf { it.url.isNotBlank() }?.let {
        CommunityPostVideo(url = it.url, title = it.title, thumbnailUrl = it.thumbnailUrl)
    },
    sharedPost = sharedPost?.toDomain(),
)
