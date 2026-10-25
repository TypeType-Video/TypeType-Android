package dev.typetype.android.data.channel

import dev.typetype.android.data.account.ActiveAccountScope
import dev.typetype.android.data.network.TypeTypeApiHolder
import dev.typetype.android.data.network.dto.ChannelPageRequest
import dev.typetype.android.data.network.dto.SearchPlaylistDto
import dev.typetype.android.data.network.dto.toDomainVideo
import dev.typetype.android.data.network.requireSuccessfulResponse
import dev.typetype.android.domain.channel.Channel
import dev.typetype.android.domain.channel.ChannelPage
import dev.typetype.android.domain.channel.ChannelPlaylistsPage
import dev.typetype.android.domain.channel.ChannelQuery
import dev.typetype.android.domain.channel.ChannelRepository
import dev.typetype.android.domain.channel.ChannelSort
import dev.typetype.android.domain.search.SearchPlaylist
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChannelRepositoryImpl @Inject constructor(
    private val apiHolder: TypeTypeApiHolder,
    private val activeAccountScope: ActiveAccountScope,
) : ChannelRepository {

    override suspend fun loadChannel(query: ChannelQuery, nextPage: String?): Result<ChannelPage> = catching {
        val scope = activeAccountScope.require()
        val api = apiHolder.require(scope)
        val response = withContext(Dispatchers.IO) {
            api.channel(
                ChannelPageRequest(
                    url = buildChannelRequestUrl(query.channelUrl, query.searchQuery, query.live),
                    nextpage = nextPage,
                    sort = query.sort.apiValue(),
                ),
            )
        }
        response.requireSuccessfulResponse()
        val body = response.body() ?: error("Empty channel body")
        activeAccountScope.verify(scope)
        ChannelPage(
            channel = Channel(
                name = body.name,
                description = body.description,
                avatarUrl = body.avatarUrl,
                bannerUrl = body.bannerUrl,
                subscriberCount = body.subscriberCount,
                verified = body.isVerified,
                videos = body.videos.map { item ->
                    item.toDomainVideo().let { video ->
                        if (video.uploaderAvatarUrl.isBlank()) {
                            video.copy(uploaderAvatarUrl = body.avatarUrl)
                        } else {
                            video
                        }
                    }
                },
            ),
            nextPage = body.nextpage,
        )
    }

    override suspend fun loadPlaylists(
        channelUrl: String,
        nextPage: String?,
    ): Result<ChannelPlaylistsPage> = catching {
        val scope = activeAccountScope.require()
        val api = apiHolder.require(scope)
        val response = withContext(Dispatchers.IO) {
            api.channelPlaylists(channelUrl, nextPage)
        }
        response.requireSuccessfulResponse()
        val body = response.body() ?: error("Empty channel playlists body")
        activeAccountScope.verify(scope)
        ChannelPlaylistsPage(
            playlists = body.playlists.map(SearchPlaylistDto::toDomain),
            nextPage = body.nextpage,
        )
    }

}

private inline fun <T> catching(block: () -> T): Result<T> = runCatching(block).onFailure {
    if (it is CancellationException) throw it
}

private fun ChannelSort.apiValue(): String = when (this) {
    ChannelSort.Latest -> "latest"
    ChannelSort.Popular -> "popular"
    ChannelSort.Oldest -> "oldest"
}

private fun SearchPlaylistDto.toDomain() = SearchPlaylist(
    id = id,
    title = title,
    url = url,
    thumbnailUrl = thumbnailUrl,
    uploaderName = uploaderName,
    streamCount = streamCount,
    playlistType = playlistType,
)
