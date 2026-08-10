package dev.typetype.android.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class RssFeedRequestDto(
    val name: String,
    val scope: String,
    val channelUrls: List<String>,
    val serviceIds: List<Int>,
    val includeVideos: Boolean,
    val includeShorts: Boolean,
    val includeLive: Boolean,
    val includeUpcoming: Boolean,
)

@Serializable
data class RssFeedItemDto(
    val id: String,
    val name: String,
    val scope: String,
    val channelUrls: List<String>,
    val serviceIds: List<Int>,
    val includeVideos: Boolean,
    val includeShorts: Boolean,
    val includeLive: Boolean,
    val includeUpcoming: Boolean,
    val enabled: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val lastUsedAt: Long? = null,
)

@Serializable
data class RssFeedSecretItemDto(
    val feed: RssFeedItemDto,
    val feedUrl: String,
)

@Serializable
data class RssFeedEnabledRequestDto(val enabled: Boolean)
