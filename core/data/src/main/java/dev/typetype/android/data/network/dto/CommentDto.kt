package dev.typetype.android.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class CommentItem(
    val id: String,
    val text: String,
    val author: String,
    val authorUrl: String,
    val authorAvatarUrl: String,
    val likeCount: Long,
    val textualLikeCount: String,
    val publishedTime: String,
    val publishedAt: Long? = null,
    val isHeartedByUploader: Boolean,
    val isPinned: Boolean,
    val uploaderVerified: Boolean,
    val replyCount: Int,
    val repliesPage: String? = null,
)

@Serializable
data class CommentsPageResponse(
    val comments: List<CommentItem> = emptyList(),
    val nextpage: String? = null,
    val commentsDisabled: Boolean = false,
)
