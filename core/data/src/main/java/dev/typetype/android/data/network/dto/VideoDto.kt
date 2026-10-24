package dev.typetype.android.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class VideoItem(
    val id: String,
    val title: String,
    val url: String,
    val thumbnailUrl: String,
    val uploaderName: String,
    val uploaderUrl: String,
    val uploaderAvatarUrl: String,
    val duration: Long,
    val viewCount: Long,
    val uploadDate: String,
    val uploaded: Long,
    val streamType: String,
    val isShortFormContent: Boolean,
    val uploaderVerified: Boolean,
    val shortDescription: String? = null,
    val publishedAt: Long? = null,
    val isLive: Boolean = false,
    val isPostLive: Boolean = false,
    val isLiveContent: Boolean = false,
    val requiresMembership: Boolean = false,
)

@Serializable
data class HomeRecommendationsResponse(
    val items: List<VideoItem> = emptyList(),
    val nextCursor: String? = null,
    val hasMore: Boolean = false,
)

@Serializable
data class SubscriptionFeedResponse(
    val videos: List<VideoItem> = emptyList(),
    val nextpage: String? = null,
    val generation: Long? = null,
    val generatedAt: Long? = null,
    val refreshing: Boolean? = null,
    val code: String? = null,
    val retryAfterMs: Long? = null,
)
