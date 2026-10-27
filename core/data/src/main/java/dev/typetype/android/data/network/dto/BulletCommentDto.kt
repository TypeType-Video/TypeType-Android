package dev.typetype.android.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class BulletCommentItem(
    val text: String,
    val argbColor: Int,
    val position: String,
    val relativeFontSize: Double,
    val durationMs: Long,
    val isLive: Boolean,
)

@Serializable
data class BulletCommentsPageResponse(
    val comments: List<BulletCommentItem> = emptyList(),
    val nextpage: String? = null,
)
