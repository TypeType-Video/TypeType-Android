package dev.typetype.android.domain.channel

import dev.typetype.android.domain.feed.Video

data class Channel(
    val name: String,
    val description: String,
    val avatarUrl: String,
    val bannerUrl: String?,
    val subscriberCount: Long,
    val verified: Boolean,
    val videos: List<Video>,
)
