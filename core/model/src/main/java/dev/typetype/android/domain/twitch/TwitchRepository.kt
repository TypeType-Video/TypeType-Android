package dev.typetype.android.domain.twitch

import dev.typetype.android.domain.feed.Video
import dev.typetype.android.domain.stream.Chapter

interface TwitchRepository {
    suspend fun popular(cursor: String? = null): Result<TwitchPage<Video>>

    suspend fun categories(cursor: String? = null): Result<TwitchPage<TwitchCategory>>

    suspend fun categoryLives(slug: String, cursor: String? = null): Result<TwitchPage<Video>>

    suspend fun channel(login: String): Result<TwitchChannel>

    suspend fun channelVods(login: String, cursor: String? = null): Result<TwitchPage<Video>>

    suspend fun channelClips(login: String, cursor: String? = null): Result<TwitchPage<Video>>

    suspend fun live(login: String): Result<TwitchPlayback?>

    suspend fun vod(id: String): Result<TwitchPlayback>

    suspend fun chapters(id: String): Result<List<Chapter>>

    suspend fun clip(slug: String): Result<TwitchClipPlayback>

    suspend fun searchChannels(query: String, cursor: String? = null): Result<TwitchPage<TwitchChannel>>

    suspend fun searchLives(query: String, cursor: String? = null): Result<TwitchPage<Video>>

    suspend fun chatHighlights(login: String): Result<TwitchChatHighlights>
}
