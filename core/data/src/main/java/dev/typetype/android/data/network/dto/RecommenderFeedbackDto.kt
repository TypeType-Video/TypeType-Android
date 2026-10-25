package dev.typetype.android.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class RecommenderFeedbackRequestDto(
    val kind: String,
    val verdict: String,
    val comment: String? = null,
    val videoUrl: String? = null,
    val reason: String? = null,
    val title: String? = null,
    val channelName: String? = null,
    val thumbnailUrl: String? = null,
)

@Serializable
data class RecommenderFeedbackDto(
    val id: String = "",
)
