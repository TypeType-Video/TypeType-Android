package dev.typetype.android.data.actions

import dev.typetype.android.data.network.TypeTypeApiHolder
import dev.typetype.android.data.network.dto.BlockChannelRequest
import dev.typetype.android.data.network.dto.BlockVideoRequest
import dev.typetype.android.data.network.extractServerErrorMessage
import dev.typetype.android.domain.actions.VideoActionsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

@Singleton
class VideoActionsRepositoryImpl @Inject constructor(
    private val apiHolder: TypeTypeApiHolder,
) : VideoActionsRepository {

    private val blockedVideos = MutableStateFlow<Set<String>>(emptySet())
    private val blockedChannels = MutableStateFlow<Set<String>>(emptySet())

    override fun observeBlockedVideoUrls(): Flow<Set<String>> = blockedVideos.asStateFlow()

    override fun observeBlockedChannelUrls(): Flow<Set<String>> = blockedChannels.asStateFlow()

    override suspend fun refreshBlocked(): Result<Unit> = runCatching {
        val api = apiHolder.require()
        val (videos, channels) = withContext(Dispatchers.IO) {
            val v = api.blockedVideos()
            val c = api.blockedChannels()
            if (!v.isSuccessful) error(extractServerErrorMessage(v))
            if (!c.isSuccessful) error(extractServerErrorMessage(c))
            (v.body().orEmpty().mapNotNull { it.url }.toSet()) to
                (c.body().orEmpty().mapNotNull { it.url }.toSet())
        }
        blockedVideos.value = videos
        blockedChannels.value = channels
    }

    override suspend fun blockVideo(videoUrl: String): Result<Unit> = runCatching {
        val response = withContext(Dispatchers.IO) {
            apiHolder.require().blockVideo(BlockVideoRequest(url = videoUrl))
        }
        if (!response.isSuccessful) error(extractServerErrorMessage(response))
        blockedVideos.update { it + videoUrl }
    }

    override suspend fun unblockVideo(videoUrl: String): Result<Unit> = runCatching {
        val response = withContext(Dispatchers.IO) {
            apiHolder.require().unblockVideo(videoUrl)
        }
        if (!response.isSuccessful) error(extractServerErrorMessage(response))
        blockedVideos.update { it - videoUrl }
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
        blockedChannels.update { it + channelUrl }
    }

    override suspend fun unblockChannel(channelUrl: String): Result<Unit> = runCatching {
        val response = withContext(Dispatchers.IO) {
            apiHolder.require().unblockChannel(channelUrl)
        }
        if (!response.isSuccessful) error(extractServerErrorMessage(response))
        blockedChannels.update { it - channelUrl }
    }
}
