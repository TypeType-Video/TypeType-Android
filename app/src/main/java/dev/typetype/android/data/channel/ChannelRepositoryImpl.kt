package dev.typetype.android.data.channel

import dev.typetype.android.data.account.ActiveAccountScope
import dev.typetype.android.data.network.TypeTypeApiHolder
import dev.typetype.android.data.network.dto.toDomainVideo
import dev.typetype.android.data.network.requireSuccessfulResponse
import dev.typetype.android.domain.channel.Channel
import dev.typetype.android.domain.channel.ChannelRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChannelRepositoryImpl @Inject constructor(
    private val apiHolder: TypeTypeApiHolder,
    private val activeAccountScope: ActiveAccountScope,
) : ChannelRepository {

    override suspend fun loadChannel(channelUrl: String): Result<Channel> = runCatching {
        val scope = activeAccountScope.require()
        val api = apiHolder.require(scope)
        val response = withContext(Dispatchers.IO) { api.channel(url = channelUrl) }
        response.requireSuccessfulResponse()
        val body = response.body() ?: error("Empty channel body")
        activeAccountScope.verify(scope)
        Channel(
            name = body.name,
            description = body.description,
            avatarUrl = body.avatarUrl,
            bannerUrl = body.bannerUrl,
            subscriberCount = body.subscriberCount,
            verified = body.isVerified,
            videos = body.videos.map { it.toDomainVideo() },
        )
    }

}
