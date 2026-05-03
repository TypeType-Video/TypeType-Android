package dev.typetype.android.domain.feed

data class Video(
    val id: String,
    val url: String,
    val title: String,
    val thumbnailUrl: String,
    val uploaderName: String,
    val uploaderUrl: String,
    val uploaderAvatarUrl: String,
    val uploaderVerified: Boolean,
    val durationSeconds: Long,
    val viewCount: Long,
    val uploadedAtMillis: Long,
    val isShortFormContent: Boolean,
    val shortDescription: String?,
)
