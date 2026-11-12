package dev.typetype.android.domain.comments

data class Comment(
    val id: String,
    val text: String,
    val authorName: String,
    val authorAvatarUrl: String,
    val likeCount: Long,
    val textualLikeCount: String,
    val publishedTime: String,
    val isHeartedByUploader: Boolean,
    val isPinned: Boolean,
    val uploaderVerified: Boolean,
    val replyCount: Int,
    val publishedAtMillis: Long? = null,
    val repliesPage: String? = null,
    val image: CommentImage? = null,
    val voiceReply: CommentVoiceReply? = null,
)

data class CommentImage(
    val url: String?,
    val animated: Boolean,
    val unavailableText: String?,
)

data class CommentVoiceReply(
    val url: String,
    val durationMs: Long,
    val transcript: String,
    val playable: Boolean,
    val unavailableText: String?,
)

data class CommentsPage(
    val comments: List<Comment>,
    val nextpage: String?,
    val commentsDisabled: Boolean,
)
