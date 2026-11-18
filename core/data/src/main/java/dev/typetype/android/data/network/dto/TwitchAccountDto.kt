package dev.typetype.android.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class TwitchAccountStatusDto(
    val linked: Boolean = false,
    val login: String? = null,
    val displayName: String? = null,
    val avatarUrl: String? = null,
)

@Serializable
data class TwitchDeviceCodeDto(
    val deviceCode: String = "",
    val userCode: String = "",
    val verificationUri: String = "",
    val interval: Int = 5,
    val expiresIn: Int = 0,
)

@Serializable
data class TwitchDevicePollRequest(val deviceCode: String)

@Serializable
data class TwitchDevicePollDto(val status: String = "", val account: TwitchAccountStatusDto? = null)

@Serializable
data class TwitchFollowStateDto(
    val channelId: String = "",
    val following: Boolean = false,
    val followedAt: String? = null,
)

@Serializable
data class TwitchChatSendRequest(val message: String)

@Serializable
data class TwitchPollVoteRequest(val pollId: String, val choiceId: String)
