package dev.typetype.android.data.twitch

import dev.typetype.android.core.error.suspendRunCatching
import dev.typetype.android.data.account.ActiveAccountScope
import dev.typetype.android.data.network.TypeTypeApi
import dev.typetype.android.data.network.TypeTypeApiHolder
import dev.typetype.android.data.network.dto.TwitchChatSendRequest
import dev.typetype.android.data.network.dto.TwitchDevicePollRequest
import dev.typetype.android.data.network.dto.TwitchPollVoteRequest
import dev.typetype.android.data.network.requireSuccessfulResponse
import dev.typetype.android.domain.feed.Video
import dev.typetype.android.domain.twitch.TwitchAccountRepository
import dev.typetype.android.domain.twitch.TwitchAccountStatus
import dev.typetype.android.domain.twitch.TwitchLinkCode
import dev.typetype.android.domain.twitch.TwitchLinkPoll
import dev.typetype.android.domain.twitch.TwitchPage
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

@Singleton
class RemoteTwitchAccountRepository @Inject constructor(
    private val apiHolder: TypeTypeApiHolder,
    private val activeAccountScope: ActiveAccountScope,
) : TwitchAccountRepository {
    override suspend fun status(): Result<TwitchAccountStatus> = call { twitchAccount() }.map {
        TwitchAccountStatus(linked = it.linked, name = it.displayName?.ifBlank { null } ?: it.login)
    }

    override suspend fun start(): Result<TwitchLinkCode> = call { twitchStartDevice() }.map { dto ->
        check(dto.deviceCode.isNotBlank() && dto.userCode.isNotBlank()) { "Twitch gave no device code" }
        TwitchLinkCode(
            deviceCode = dto.deviceCode,
            userCode = dto.userCode,
            activationUrl = "${dto.verificationUri.ifBlank { ACTIVATE }}?device-code=${dto.userCode}",
            intervalSeconds = dto.interval.coerceAtLeast(MIN_INTERVAL),
            expiresInSeconds = dto.expiresIn,
        )
    }

    override suspend fun poll(deviceCode: String): Result<TwitchLinkPoll> =
        call { twitchPollDevice(TwitchDevicePollRequest(deviceCode)) }.map { dto ->
            when (dto.status) {
                "ok" -> TwitchLinkPoll.Linked(dto.account?.displayName ?: dto.account?.login.orEmpty())
                "slow_down" -> TwitchLinkPoll.SlowDown
                else -> TwitchLinkPoll.Pending
            }
        }

    override suspend fun unlink(): Result<Unit> = write { twitchUnlink() }

    override suspend fun followedLives(cursor: String?): Result<TwitchPage<Video>> = call { twitchFollowedLives(cursor) }
        .map { page -> TwitchPage(page.items.map { it.toVideo() }, page.cursor?.takeIf { page.hasMore && it.isNotBlank() }) }

    override suspend fun isFollowing(login: String): Result<Boolean> = call { twitchFollowState(login) }.map { it.following }

    override suspend fun setFollowing(login: String, follow: Boolean): Result<Unit> =
        write { if (follow) twitchFollow(login) else twitchUnfollow(login) }

    override suspend fun sendChat(login: String, message: String): Result<Unit> =
        write { twitchSendChat(login, TwitchChatSendRequest(message)) }

    override suspend fun votePoll(pollId: String, choiceId: String): Result<Unit> =
        write { twitchVotePoll(TwitchPollVoteRequest(pollId, choiceId)) }

    private suspend fun write(block: suspend TypeTypeApi.() -> Response<Unit>): Result<Unit> = suspendRunCatching {
        val scope = activeAccountScope.require()
        val response = withContext(Dispatchers.IO) { apiHolder.require(scope).block() }
        activeAccountScope.verify(scope)
        response.requireSuccessfulResponse()
    }

    private suspend fun <T : Any> call(block: suspend TypeTypeApi.() -> Response<T>): Result<T> = suspendRunCatching {
        val scope = activeAccountScope.require()
        val response = withContext(Dispatchers.IO) { apiHolder.require(scope).block() }
        activeAccountScope.verify(scope)
        response.requireSuccessfulResponse()
        response.body() ?: error("Empty Twitch response")
    }

    private companion object {
        const val ACTIVATE = "https://www.twitch.tv/activate"
        const val MIN_INTERVAL = 5
    }
}
