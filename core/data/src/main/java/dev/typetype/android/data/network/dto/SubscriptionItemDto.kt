package dev.typetype.android.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class SubscriptionItemDto(
    val channelUrl: String,
    val name: String,
    val avatarUrl: String = "",
    val subscribedAt: Long = 0L,
)
