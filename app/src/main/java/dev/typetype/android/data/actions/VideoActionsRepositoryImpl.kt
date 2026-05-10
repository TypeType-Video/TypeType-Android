package dev.typetype.android.data.actions

import dev.typetype.android.data.network.TypeTypeApiHolder
import dev.typetype.android.data.network.dto.BlockChannelRequest
import dev.typetype.android.data.network.dto.BlockVideoRequest
import dev.typetype.android.data.network.dto.BlockedItemDto
import dev.typetype.android.data.network.extractServerErrorMessage
import dev.typetype.android.domain.actions.BlockedItem
import dev.typetype.android.domain.actions.VideoActionsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

@Singleton
class VideoActionsRepositoryImpl @Inject constructor(
    private val apiHolder: TypeTypeApiHolder,
) : VideoActionsRepository {

    private val blockedVideosState = MutableStateFlow<List<BlockedItem>>(emptyList())
    private val blockedChannelsState = MutableStateFlow<List<BlockedItem>>(emptyList())

    override fun observeBlockedVideoUrls(): Flow<Set<String>> =
        blockedVideosState.map { items -> items.map { it.url }.toSet() }

    override fun observeBlockedChannelUrls(): Flow<Set<String>> =
        blockedChannelsState.map { items -> items.map { it.url }.toSet() }

    override fun observeBlockedVideos(): Flow<List<BlockedItem>> = blockedVideosState.asStateFlow()

    override fun observeBlockedChannels(): Flow<List<BlockedItem>> = blockedChannelsState.asStateFlow()

    override suspend fun refreshBlocked(): Result<Unit> = runCatching {
        val api = apiHolder.require()
        withContext(Dispatchers.IO) {
            val v = api.blockedVideos()
            val c = api.blockedChannels()
            if (!v.isSuccessful) error(extractServerErrorMessage(v))
            if (!c.isSuccessful) error(extractServerErrorMessage(c))
            blockedVideosState.value = v.body().orEmpty().map { it.toDomain() }
            blockedChannelsState.value = c.body().orEmpty().map { it.toDomain() }
        }
    }

    override suspend fun blockVideo(videoUrl: String): Result<Unit> = runCatching {
        val response = withContext(Dispatchers.IO) {
            apiHolder.require().blockVideo(BlockVideoRequest(url = videoUrl))
        }
        if (!response.isSuccessful) error(extractServerErrorMessage(response))
        blockedVideosState.update { current ->
            if (current.any { it.url == videoUrl }) current
            else current + BlockedItem(videoUrl, "", "", System.currentTimeMillis())
        }
    }

    override suspend fun unblockVideo(videoUrl: String): Result<Unit> = runCatching {
        val response = withContext(Dispatchers.IO) {
            apiHolder.require().unblockVideo(videoUrl)
        }
        if (!response.isSuccessful) error(extractServerErrorMessage(response))
        blockedVideosState.update { it.filterNot { item -> item.url == videoUrl } }
    }

    override suspend fun blockChannel(
        channelUrl: String,
        channelName: String?,
        avatarUrl: String?,
    ): Result<Unit> = runCatching {
        val response = withContext(Dispatchers.IO) {
            apiHolder.require().blockChannel(
                BlockChannelRequest(
                    url = channelUrl,
                    name = channelName,
                    thumbnailUrl = avatarUrl,
                ),
            )
        }
        if (!response.isSuccessful) error(extractServerErrorMessage(response))
        blockedChannelsState.update { current ->
            if (current.any { it.url == channelUrl }) current
            else current + BlockedItem(
                url = channelUrl,
                name = channelName.orEmpty(),
                thumbnailUrl = avatarUrl.orEmpty(),
                blockedAt = System.currentTimeMillis(),
            )
        }
    }

    override suspend fun unblockChannel(channelUrl: String): Result<Unit> = runCatching {
        val response = withContext(Dispatchers.IO) {
            apiHolder.require().unblockChannel(channelUrl)
        }
        if (!response.isSuccessful) error(extractServerErrorMessage(response))
        blockedChannelsState.update { it.filterNot { item -> item.url == channelUrl } }
    }

    private fun BlockedItemDto.toDomain(): BlockedItem = BlockedItem(
        url = url,
        name = name.orEmpty(),
        thumbnailUrl = thumbnailUrl.orEmpty(),
        blockedAt = blockedAt,
    )
}
