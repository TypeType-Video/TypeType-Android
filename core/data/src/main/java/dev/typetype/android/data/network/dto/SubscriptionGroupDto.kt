package dev.typetype.android.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class SubscriptionGroupDto(
    val id: String,
    val name: String,
    val channelCount: Int = 0,
)

@Serializable
data class SubscriptionGroupNameRequest(val name: String)

@Serializable
data class SubscriptionGroupMembershipRequest(val channelUrl: String)

@Serializable
data class SubscriptionGroupMembershipDto(
    val channelUrl: String,
    val groupIds: List<String> = emptyList(),
)
