package dev.typetype.android.domain.channel

interface ChannelRepository {
    suspend fun loadChannel(channelUrl: String): Result<Channel>
}
