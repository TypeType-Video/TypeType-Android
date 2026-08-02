package dev.typetype.android.data.search

import dev.typetype.android.data.account.ActiveAccountScope
import dev.typetype.android.data.network.TypeTypeApiHolder
import dev.typetype.android.data.network.dto.SearchChannelDto
import dev.typetype.android.data.network.dto.SearchFilterOptionDto
import dev.typetype.android.data.network.dto.SearchFilterGroupDto
import dev.typetype.android.data.network.dto.SearchPlaylistDto
import dev.typetype.android.data.network.dto.toDomainVideo
import dev.typetype.android.data.network.requireSuccessfulResponse
import dev.typetype.android.domain.search.SearchChannel
import dev.typetype.android.domain.search.SearchFilterOption
import dev.typetype.android.domain.search.SearchFilterGroup
import dev.typetype.android.domain.search.SearchFilters
import dev.typetype.android.domain.search.SearchPage
import dev.typetype.android.domain.search.SearchPlaylist
import dev.typetype.android.domain.search.SearchRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class SearchRepositoryImpl @Inject constructor(
    private val apiHolder: TypeTypeApiHolder,
    private val activeAccountScope: ActiveAccountScope,
) : SearchRepository {

    override suspend fun search(
        query: String,
        service: Int,
        nextPage: String?,
        contentFilter: String?,
        filters: List<String>,
    ): Result<SearchPage> = catching {
        val scope = activeAccountScope.require()
        val api = apiHolder.require(scope)
        val response = withContext(Dispatchers.IO) {
            api.search(
                query = query,
                service = service,
                nextpage = nextPage,
                contentFilter = contentFilter,
                filters = filters,
            )
        }
        response.requireSuccessfulResponse()
        val body = response.body() ?: error("Empty search body")
        activeAccountScope.verify(scope)
        SearchPage(
            videos = body.items.map { it.toDomainVideo() },
            channels = body.channels.map { it.toDomain() },
            playlists = body.playlists.map { it.toDomain() },
            nextPage = body.nextpage,
            suggestion = body.searchSuggestion,
            isCorrected = body.isCorrectedSearch,
        )
    }

    override suspend fun filters(service: Int, contentFilter: String?): Result<SearchFilters> = catching {
        val scope = activeAccountScope.require()
        val api = apiHolder.require(scope)
        val response = withContext(Dispatchers.IO) { api.searchFilters(service, contentFilter) }
        response.requireSuccessfulResponse()
        val body = response.body() ?: error("Empty search filters body")
        activeAccountScope.verify(scope)
        SearchFilters(
            content = body.contentFilters.map { it.toDomain() },
            sort = body.sortFilters.map { it.toDomain() },
            groups = body.filterGroups.map { it.toDomain() },
        )
    }

    override suspend fun suggestions(query: String, service: Int): Result<List<String>> = catching {
        val scope = activeAccountScope.require()
        val api = apiHolder.require(scope)
        val response = withContext(Dispatchers.IO) {
            api.searchSuggestions(query = query, service = service)
        }
        response.requireSuccessfulResponse()
        activeAccountScope.verify(scope)
        response.body() ?: emptyList()
    }

    private fun SearchChannelDto.toDomain() = SearchChannel(
        id = id,
        name = name,
        url = url,
        thumbnailUrl = thumbnailUrl,
        description = description,
        subscriberCount = subscriberCount,
        streamCount = streamCount,
        isVerified = isVerified,
    )

    private fun SearchPlaylistDto.toDomain() = SearchPlaylist(
        id = id,
        title = title,
        url = url,
        thumbnailUrl = thumbnailUrl,
        uploaderName = uploaderName,
        streamCount = streamCount,
        playlistType = playlistType,
    )

    private fun SearchFilterOptionDto.toDomain() = SearchFilterOption(value, label, isDefault)

    private fun SearchFilterGroupDto.toDomain() = SearchFilterGroup(
        key = key,
        label = label,
        multiSelect = multiSelect,
        options = options.map { it.toDomain() },
    )
}

private inline fun <T> catching(block: () -> T): Result<T> = runCatching(block).onFailure {
    if (it is CancellationException) throw it
}
