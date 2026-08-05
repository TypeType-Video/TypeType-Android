package dev.typetype.android.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class DeArrowDto(
    val videoId: String,
    val title: String? = null,
    val thumbnailUrl: String? = null,
    val titles: List<DeArrowTitleCandidateDto>? = null,
    val thumbnails: List<DeArrowThumbnailCandidateDto>? = null,
    val randomTime: Double? = null,
    val videoDuration: Double? = null,
)

@Serializable
data class DeArrowTitleCandidateDto(
    val title: String,
    val original: Boolean,
    val votes: Int,
    val locked: Boolean,
)

@Serializable
data class DeArrowThumbnailCandidateDto(
    val thumbnailUrl: String? = null,
    val original: Boolean,
    val votes: Int,
    val locked: Boolean,
)
