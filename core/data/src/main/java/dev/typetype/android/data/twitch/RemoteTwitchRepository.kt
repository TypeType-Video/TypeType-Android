package dev.typetype.android.data.twitch

import dev.typetype.android.core.error.suspendRunCatching
import dev.typetype.android.data.account.ActiveAccountScope
import dev.typetype.android.data.network.TypeTypeApi
import dev.typetype.android.data.network.TypeTypeApiHolder
import dev.typetype.android.data.network.dto.TwitchPageDto
import dev.typetype.android.data.network.requireSuccessfulResponse
import dev.typetype.android.domain.feed.Video
import dev.typetype.android.domain.stream.Chapter
import dev.typetype.android.domain.server.ServerRepository
import dev.typetype.android.domain.twitch.TwitchCategory
import dev.typetype.android.domain.twitch.TwitchChannel
import dev.typetype.android.domain.twitch.TwitchChatHighlights
import dev.typetype.android.domain.twitch.TwitchClipPlayback
import dev.typetype.android.domain.twitch.TwitchPage
import dev.typetype.android.domain.twitch.TwitchPlayback
import dev.typetype.android.domain.twitch.TwitchRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

@Singleton
class RemoteTwitchRepository @Inject constructor(
    private val apiHolder: TypeTypeApiHolder,
    private val activeAccountScope: ActiveAccountScope,
    private val serverRepository: ServerRepository,
) : TwitchRepository {
    override suspend fun popular(cursor: String?): Result<TwitchPage<Video>> =
        call { twitchStreams(cursor) }.map { page -> page.map { it.toVideo() } }

    override suspend fun categories(cursor: String?): Result<TwitchPage<TwitchCategory>> =
        call { twitchGames(cursor) }.map { page -> page.map { it.toCategory() } }

    override suspend fun categoryLives(slug: String, cursor: String?): Result<TwitchPage<Video>> =
        call { twitchGameStreams(segment(slug), cursor) }.map { body -> body.streams.map { it.toVideo() } }

    override suspend fun channel(login: String): Result<TwitchChannel> =
        call { twitchChannel(segment(login)) }.map { it.toChannel() }

    override suspend fun channelVods(login: String, cursor: String?): Result<TwitchPage<Video>> =
        call { twitchChannelVideos(segment(login), cursor) }.map { page -> page.map { it.toVideo() } }

    override suspend fun channelClips(login: String, cursor: String?): Result<TwitchPage<Video>> =
        call { twitchChannelClips(segment(login), cursor) }.map { page -> page.map { it.toVideo() } }

    override suspend fun live(login: String): Result<TwitchPlayback?> = suspendRunCatching {
        val (base, response) = request { twitchLive(segment(login)) }
        if (response.code() == HTTP_NOT_FOUND) return@suspendRunCatching null
        response.requireSuccessfulResponse()
        val body = response.body() ?: error("Empty Twitch live body")
        val channel = body.channel.toChannel()
        val video = channel.live ?: error("Twitch live without a stream")
        TwitchPlayback(
            video = video,
            playlistUrl = twitchPlaylistUrl(base, body.hlsUrl) ?: error("Twitch live without a playlist"),
            audioPlaylistUrl = twitchPlaylistUrl(base, body.audioUrl),
            live = true,
        )
    }

    override suspend fun vod(id: String): Result<TwitchPlayback> = suspendRunCatching {
        val (base, response) = request { twitchVideo(segment(id)) }
        response.requireSuccessfulResponse()
        val body = response.body() ?: error("Empty Twitch video body")
        TwitchPlayback(
            video = body.video.toVideo(),
            playlistUrl = twitchPlaylistUrl(base, body.hlsUrl) ?: error("Twitch video without a playlist"),
            audioPlaylistUrl = twitchPlaylistUrl(base, body.audioUrl),
            live = body.video.recording,
        )
    }

    override suspend fun clip(slug: String): Result<TwitchClipPlayback> = suspendRunCatching {
        val (base, response) = request { twitchClip(segment(slug)) }
        response.requireSuccessfulResponse()
        val body = response.body() ?: error("Empty Twitch clip body")
        // Clip files are served by Live under the instance origin, like the playlists.
        val qualities = body.qualities.mapNotNull { quality ->
            quality.toQuality()?.let { it.copy(url = twitchPlaylistUrl(base, it.url) ?: return@mapNotNull null) }
        }.sortedByDescending { it.height }
        require(qualities.isNotEmpty()) { "Twitch clip without a playable quality" }
        TwitchClipPlayback(body.clip.toVideo(), qualities)
    }

    override suspend fun searchChannels(query: String, cursor: String?): Result<TwitchPage<TwitchChannel>> =
        call { twitchSearchChannels(query.trim(), after = cursor) }.map { page -> page.map { it.toChannel() } }

    override suspend fun searchLives(query: String, cursor: String?): Result<TwitchPage<Video>> =
        call { twitchSearchStreams(query.trim(), after = cursor) }.map { page -> page.map { it.toVideo() } }

    override suspend fun chapters(id: String): Result<List<Chapter>> = call { twitchVideoChapters(id) }.map { chapters ->
        chapters.map { Chapter(title = it.title.ifBlank { it.game?.name.orEmpty() }, startMs = it.start * 1_000, previewUrl = null) }
    }

    override suspend fun chatHighlights(login: String): Result<TwitchChatHighlights> =
        call { twitchChatHighlights(login) }.map { it.toDomain() }

    private suspend fun <T : Any> call(block: suspend TypeTypeApi.() -> Response<T>): Result<T> = suspendRunCatching {
        val (_, response) = request(block)
        response.requireSuccessfulResponse()
        response.body() ?: error("Empty Twitch response")
    }

    private suspend fun <T> request(block: suspend TypeTypeApi.() -> Response<T>): Pair<String, Response<T>> {
        val scope = activeAccountScope.require()
        val base = serverRepository.getServer(scope.serverId)?.baseUrl ?: error("Instance not found")
        val response = withContext(Dispatchers.IO) { apiHolder.require(scope).block() }
        activeAccountScope.verify(scope)
        return base to response
    }

    private fun <T, R> TwitchPageDto<T>.map(transform: (T) -> R): TwitchPage<R> =
        TwitchPage(items.map(transform), cursor?.takeIf { hasMore && it.isNotBlank() })

    private fun segment(value: String): String = value.trim().filter { it.isLetterOrDigit() || it in "_-" }

    private companion object {
        const val HTTP_NOT_FOUND = 404
    }
}
