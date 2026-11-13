package dev.typetype.android.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChannelPostsResponse(
    val posts: List<CommunityPostDto> = emptyList(),
    val nextpage: String? = null,
)

@Serializable
data class CommunityPostDto(
    val id: String,
    val text: String = "",
    val authorName: String = "",
    val authorAvatarUrl: String = "",
    val publishedTime: String = "",
    val publishedAt: Long? = null,
    val likeCount: Long? = null,
    val textualLikeCount: String = "",
    val textualCommentCount: String = "",
    val images: List<String> = emptyList(),
    val poll: CommunityPostPollDto? = null,
    val video: CommunityPostVideoDto? = null,
    val sharedPost: CommunityPostDto? = null,
)

@Serializable
data class CommunityPostPollDto(
    val choices: List<CommunityPostPollChoiceDto> = emptyList(),
    val textualVoteCount: String = "",
)

@Serializable
data class CommunityPostPollChoiceDto(
    val text: String = "",
    val imageUrl: String? = null,
)

@Serializable
data class CommunityPostVideoDto(
    val url: String,
    val title: String = "",
    val thumbnailUrl: String = "",
)
