package dev.typetype.android.data.publicplaylist

import dev.typetype.android.data.account.ActiveAccountScope
import dev.typetype.android.data.network.TypeTypeApiHolder
import dev.typetype.android.data.network.dto.SearchPlaylistDto
import dev.typetype.android.data.network.dto.toDomainVideo
import dev.typetype.android.data.network.requireSuccessfulResponse
import dev.typetype.android.domain.publicplaylist.PublicPlaylistPage
import dev.typetype.android.domain.publicplaylist.PublicPlaylistRepository
import dev.typetype.android.domain.search.SearchPlaylist
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class RemotePublicPlaylistRepository @Inject constructor(
    private val apiHolder: TypeTypeApiHolder,
    private val activeAccountScope: ActiveAccountScope,
) : PublicPlaylistRepository {
    override suspend fun load(url: String, nextPage: String?): Result<PublicPlaylistPage> = runCatching {
        val scope = activeAccountScope.require()
        val api = apiHolder.require(scope)
        val response = withContext(Dispatchers.IO) { api.publicPlaylist(url, nextPage) }
        response.requireSuccessfulResponse()
        val body = response.body() ?: error("Empty public playlist body")
        activeAccountScope.verify(scope)
        PublicPlaylistPage(
            playlist = body.playlist.toDomain(),
            videos = body.videos.map { it.toDomainVideo() },
            nextPage = body.nextpage,
        )
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

}
