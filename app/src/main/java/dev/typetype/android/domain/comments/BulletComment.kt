package dev.typetype.android.domain.comments

data class BulletComment(
    val text: String,
    val rgbColor: Int,
    val position: BulletCommentPosition,
    val relativeFontSize: Float,
    val presentationTimeMillis: Long,
    val isLive: Boolean,
)

enum class BulletCommentPosition {
    Regular,
    Top,
    Bottom,
    SuperChat,
}
