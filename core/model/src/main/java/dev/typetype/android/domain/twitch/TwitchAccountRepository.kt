package dev.typetype.android.domain.twitch

import dev.typetype.android.domain.feed.Video

data class TwitchAccountStatus(val linked: Boolean, val name: String?)

data class TwitchLinkCode(
    val deviceCode: String,
    val userCode: String,
    val activationUrl: String,
    val intervalSeconds: Int,
    val expiresInSeconds: Int,
)

sealed interface TwitchLinkPoll {
    data object Pending : TwitchLinkPoll

    data object SlowDown : TwitchLinkPoll

    data class Linked(val name: String) : TwitchLinkPoll
}

// Twitch device code login: the app shows a code, the viewer confirms it on twitch.tv/activate.
interface TwitchAccountRepository {
    suspend fun status(): Result<TwitchAccountStatus>

    suspend fun start(): Result<TwitchLinkCode>

    suspend fun poll(deviceCode: String): Result<TwitchLinkPoll>

    suspend fun unlink(): Result<Unit>

    suspend fun followedLives(cursor: String? = null): Result<TwitchPage<Video>>

    suspend fun isFollowing(login: String): Result<Boolean>

    suspend fun setFollowing(login: String, follow: Boolean): Result<Unit>
}
