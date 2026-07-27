package dev.typetype.android.data.actions

import dev.typetype.android.data.account.AccountScopedValue
import dev.typetype.android.data.account.ActiveAccountScope
import dev.typetype.android.data.network.TypeTypeApiHolder
import dev.typetype.android.data.network.dto.BlockChannelRequest
import dev.typetype.android.data.network.dto.BlockVideoRequest
import dev.typetype.android.data.network.dto.BlockedItemDto
import dev.typetype.android.data.network.requireSuccessfulResponse
import dev.typetype.android.domain.actions.BlockedItem
import dev.typetype.android.domain.actions.VideoActionsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

@Singleton
class VideoActionsRepositoryImpl @Inject constructor(
    private val apiHolder: TypeTypeApiHolder,
    private val activeAccountScope: ActiveAccountScope,
) : VideoActionsRepository {

    private val blockedVideosState = MutableStateFlow<AccountScopedValue<List<BlockedItem>>?>(null)
    private val blockedChannelsState = MutableStateFlow<AccountScopedValue<List<BlockedItem>>?>(null)

    override fun observeBlockedVideoUrls(): Flow<Set<String>> =
        observeBlockedVideos().map { items -> items.map { it.url }.toSet() }

    override fun observeBlockedChannelUrls(): Flow<Set<String>> =
        observeBlockedChannels().map { items -> items.map { it.url }.toSet() }

    override fun observeBlockedVideos(): Flow<List<BlockedItem>> = visible(blockedVideosState)

    override fun observeBlockedChannels(): Flow<List<BlockedItem>> = visible(blockedChannelsState)

    override suspend fun refreshBlocked(): Result<Unit> = runCatching {
        val scope = activeAccountScope.require()
        val api = apiHolder.require(scope)
        withContext(Dispatchers.IO) {
            val v = api.blockedVideos()
            val c = api.blockedChannels()
            v.requireSuccessfulResponse()
            c.requireSuccessfulResponse()
            activeAccountScope.verify(scope)
            blockedVideosState.value = AccountScopedValue(scope, v.body().orEmpty().map { it.toDomain() })
            blockedChannelsState.value = AccountScopedValue(scope, c.body().orEmpty().map { it.toDomain() })
        }
    }

    override suspend fun blockVideo(videoUrl: String): Result<Unit> = runCatching {
        val scope = activeAccountScope.require()
        val response = withContext(Dispatchers.IO) {
            apiHolder.require(scope).blockVideo(BlockVideoRequest(url = videoUrl))
        }
        response.requireSuccessfulResponse()
        activeAccountScope.verify(scope)
        blockedVideosState.update { cached ->
            val current = cached?.takeIf { it.scope == scope }?.value.orEmpty()
            val next = if (current.any { it.url == videoUrl }) current
            else current + BlockedItem(videoUrl, "", "", System.currentTimeMillis())
            AccountScopedValue(scope, next)
        }
    }

    override suspend fun unblockVideo(videoUrl: String): Result<Unit> = runCatching {
        val scope = activeAccountScope.require()
        val response = withContext(Dispatchers.IO) {
            apiHolder.require(scope).unblockVideo(videoUrl)
        }
        response.requireSuccessfulResponse()
        activeAccountScope.verify(scope)
        blockedVideosState.update { cached ->
            AccountScopedValue(scope, cached.current(scope).filterNot { it.url == videoUrl })
        }
    }

    override suspend fun blockChannel(
        channelUrl: String,
        channelName: String?,
        avatarUrl: String?,
    ): Result<Unit> = runCatching {
        val scope = activeAccountScope.require()
        val response = withContext(Dispatchers.IO) {
            apiHolder.require(scope).blockChannel(
                BlockChannelRequest(
                    url = channelUrl,
                    name = channelName,
                    thumbnailUrl = avatarUrl,
                ),
            )
        }
        response.requireSuccessfulResponse()
        activeAccountScope.verify(scope)
        blockedChannelsState.update { cached ->
            val current = cached.current(scope)
            val next = if (current.any { it.url == channelUrl }) current else current + BlockedItem(
                url = channelUrl,
                name = channelName.orEmpty(),
                thumbnailUrl = avatarUrl.orEmpty(),
                blockedAt = System.currentTimeMillis(),
            )
            AccountScopedValue(scope, next)
        }
    }

    override suspend fun unblockChannel(channelUrl: String): Result<Unit> = runCatching {
        val scope = activeAccountScope.require()
        val response = withContext(Dispatchers.IO) {
            apiHolder.require(scope).unblockChannel(channelUrl)
        }
        response.requireSuccessfulResponse()
        activeAccountScope.verify(scope)
        blockedChannelsState.update { cached ->
            AccountScopedValue(scope, cached.current(scope).filterNot { it.url == channelUrl })
        }
    }

    private fun BlockedItemDto.toDomain(): BlockedItem = BlockedItem(
        url = url,
        name = name.orEmpty(),
        thumbnailUrl = thumbnailUrl.orEmpty(),
        blockedAt = blockedAt,
    )

    private fun visible(
        source: Flow<AccountScopedValue<List<BlockedItem>>?>,
    ): Flow<List<BlockedItem>> = combine(activeAccountScope.observe(), source) { scope, cached ->
        cached?.value?.takeIf { scope == cached.scope }.orEmpty()
    }

    private fun AccountScopedValue<List<BlockedItem>>?.current(scope: dev.typetype.android.data.account.AccountScope) =
        this?.takeIf { it.scope == scope }?.value.orEmpty()
}
