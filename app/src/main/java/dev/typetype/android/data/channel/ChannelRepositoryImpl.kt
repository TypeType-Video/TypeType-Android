package dev.typetype.android.data.channel

import dev.typetype.android.data.network.TypeTypeApiHolder
import dev.typetype.android.data.network.dto.VideoItem
import dev.typetype.android.domain.channel.Channel
import dev.typetype.android.domain.channel.ChannelRepository
import dev.typetype.android.domain.feed.Video
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChannelRepositoryImpl @Inject constructor(
    private val apiHolder: TypeTypeApiHolder,
) : ChannelRepository {

    override suspend fun loadChannel(channelUrl: String): Result<Channel> = runCatching {
        val api = apiHolder.require()
        val response = withContext(Dispatchers.IO) { api.channel(url = channelUrl) }
        if (!response.isSuccessful) error("Channel failed (HTTP ${response.code()})")
        val body = response.body() ?: error("Empty channel body")
        Channel(
            name = body.name,
            description = body.description,
            avatarUrl = body.avatarUrl,
            bannerUrl = body.bannerUrl,
            subscriberCount = body.subscriberCount,
            verified = body.isVerified,
            videos = body.videos.map { it.toDomain() },
        )
    }

    private fun VideoItem.toDomain() = Video(
        id = id,
        url = url,
        title = title,
        thumbnailUrl = thumbnailUrl,
        uploaderName = uploaderName,
        uploaderUrl = uploaderUrl,
        uploaderAvatarUrl = uploaderAvatarUrl,
        uploaderVerified = uploaderVerified,
        durationSeconds = duration,
        viewCount = viewCount,
        uploadedAtMillis = uploaded,
        isShortFormContent = isShortFormContent,
        shortDescription = shortDescription,
    )
}
