package dev.typetype.android.data.stream

import dev.typetype.android.domain.feed.Video
import dev.typetype.android.domain.twitch.TwitchCategory
import dev.typetype.android.domain.twitch.TwitchChannel
import dev.typetype.android.domain.twitch.TwitchChatHighlights
import dev.typetype.android.domain.twitch.TwitchClipPlayback
import dev.typetype.android.domain.twitch.TwitchPage
import dev.typetype.android.domain.twitch.TwitchPlayback
import dev.typetype.android.domain.twitch.TwitchRepository

internal class FakeTwitchRepository(
    private val livePlayback: TwitchPlayback? = null,
    private val vodPlayback: TwitchPlayback? = null,
    private val clipPlayback: TwitchClipPlayback? = null,
) : TwitchRepository {
    override suspend fun popular(cursor: String?): Result<TwitchPage<Video>> = Result.success(TwitchPage(emptyList(), null))
    override suspend fun categories(cursor: String?): Result<TwitchPage<TwitchCategory>> = Result.success(TwitchPage(emptyList(), null))
    override suspend fun categoryLives(slug: String, cursor: String?): Result<TwitchPage<Video>> = Result.success(TwitchPage(emptyList(), null))
    override suspend fun channel(login: String): Result<TwitchChannel> = Result.failure(UnsupportedOperationException())
    override suspend fun channelVods(login: String, cursor: String?): Result<TwitchPage<Video>> = Result.success(TwitchPage(emptyList(), null))
    override suspend fun channelClips(login: String, cursor: String?): Result<TwitchPage<Video>> = Result.success(TwitchPage(emptyList(), null))
    override suspend fun live(login: String): Result<TwitchPlayback?> = Result.success(livePlayback)
    override suspend fun vod(id: String): Result<TwitchPlayback> = vodPlayback?.let { Result.success(it) } ?: Result.failure(IllegalStateException())
    override suspend fun clip(slug: String): Result<TwitchClipPlayback> = clipPlayback?.let { Result.success(it) } ?: Result.failure(IllegalStateException())
    override suspend fun searchChannels(query: String, cursor: String?): Result<TwitchPage<TwitchChannel>> = Result.success(TwitchPage(emptyList(), null))
    override suspend fun searchLives(query: String, cursor: String?): Result<TwitchPage<Video>> = Result.success(TwitchPage(emptyList(), null))
    override suspend fun chatHighlights(login: String): Result<TwitchChatHighlights> = Result.success(TwitchChatHighlights(null, null))
}
