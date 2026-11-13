package dev.typetype.android.domain.channel

data class CommunityPost(
    val id: String,
    val text: String,
    val authorName: String,
    val authorAvatarUrl: String,
    val publishedTime: String,
    val publishedAtMillis: Long?,
    val likeLabel: String,
    val commentLabel: String,
    val images: List<String>,
    val poll: CommunityPostPoll?,
    val video: CommunityPostVideo?,
    val sharedPost: CommunityPost?,
)

data class CommunityPostPoll(
    val choices: List<CommunityPostPollChoice>,
    val voteLabel: String,
)

data class CommunityPostPollChoice(
    val text: String,
    val imageUrl: String?,
)

data class CommunityPostVideo(
    val url: String,
    val title: String,
    val thumbnailUrl: String,
)

data class ChannelPostsPage(
    val posts: List<CommunityPost>,
    val nextPage: String?,
)

interface ChannelPostsRepository {
    suspend fun loadPosts(channelUrl: String, nextPage: String?): Result<ChannelPostsPage>
}
