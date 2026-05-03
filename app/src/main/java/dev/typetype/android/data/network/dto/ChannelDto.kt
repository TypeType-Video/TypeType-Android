package dev.typetype.android.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChannelResponse(
    val name: String,
    val description: String = "",
    val avatarUrl: String = "",
    val bannerUrl: String? = null,
    val subscriberCount: Long = 0L,
    val isVerified: Boolean = false,
    val videos: List<VideoItem> = emptyList(),
    val nextpage: String? = null,
)
