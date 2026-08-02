package dev.typetype.android.domain.channel

interface ChannelRepository {
    suspend fun loadChannel(query: ChannelQuery, nextPage: String? = null): Result<ChannelPage>

    suspend fun loadPlaylists(channelUrl: String, nextPage: String? = null): Result<ChannelPlaylistsPage>
}
