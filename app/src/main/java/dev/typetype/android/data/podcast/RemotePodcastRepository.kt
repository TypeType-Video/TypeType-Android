package dev.typetype.android.data.podcast

import dev.typetype.android.data.account.ActiveAccountScope
import dev.typetype.android.data.network.TypeTypeApiHolder
import dev.typetype.android.data.network.dto.PodcastItemDto
import dev.typetype.android.data.network.dto.toDomainVideo
import dev.typetype.android.data.network.requireSuccessfulResponse
import dev.typetype.android.domain.podcast.ChannelPodcastPage
import dev.typetype.android.domain.podcast.Podcast
import dev.typetype.android.domain.podcast.PodcastEpisodesPage
import dev.typetype.android.domain.podcast.PodcastRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class RemotePodcastRepository @Inject constructor(
    private val apiHolder: TypeTypeApiHolder,
    private val activeAccountScope: ActiveAccountScope,
) : PodcastRepository {
    override suspend fun channelPodcasts(
        channelUrl: String,
        nextPage: String?,
    ): Result<ChannelPodcastPage> = runCatching {
        val scope = activeAccountScope.require()
        val api = apiHolder.require(scope)
        val response = withContext(Dispatchers.IO) { api.podcasts(channelUrl, nextPage) }
        response.requireSuccessfulResponse()
        val body = response.body() ?: error("Empty podcast page body")
        activeAccountScope.verify(scope)
        ChannelPodcastPage(
            channelName = body.channelName,
            channelUrl = body.channelUrl,
            podcasts = body.podcasts.map { it.toDomain() },
            episodes = body.episodes.map { it.toDomainVideo() },
            nextPage = body.nextpage,
        )
    }

    override suspend fun episodes(
        podcastUrl: String,
        nextPage: String?,
    ): Result<PodcastEpisodesPage> = runCatching {
        val scope = activeAccountScope.require()
        val api = apiHolder.require(scope)
        val response = withContext(Dispatchers.IO) { api.podcastEpisodes(podcastUrl, nextPage) }
        response.requireSuccessfulResponse()
        val body = response.body() ?: error("Empty podcast episodes body")
        activeAccountScope.verify(scope)
        PodcastEpisodesPage(
            podcast = body.podcast.toDomain(),
            episodes = body.episodes.map { it.toDomainVideo() },
            nextPage = body.nextpage,
        )
    }

    private fun PodcastItemDto.toDomain() = Podcast(
        id = id,
        title = title,
        url = url,
        thumbnailUrl = thumbnailUrl,
        uploaderName = uploaderName,
        episodeCount = streamCount,
        type = playlistType,
    )

}
