package dev.typetype.android.data.twitch

import dev.typetype.android.domain.livechat.ChatEmoteRange
import dev.typetype.android.data.network.dto.TwitchChatAssetsDto
import dev.typetype.android.domain.livechat.ChatEmote
import dev.typetype.android.domain.livechat.ChatImage
import dev.typetype.android.domain.twitch.TwitchChatAssets
import dev.typetype.android.domain.twitch.TwitchLiveInfo
import dev.typetype.android.data.network.dto.TwitchChannelDto
import dev.typetype.android.data.network.dto.TwitchChatHighlightsDto
import dev.typetype.android.data.network.dto.TwitchClipDto
import dev.typetype.android.data.network.dto.TwitchClipQualityDto
import dev.typetype.android.data.network.dto.TwitchGameDto
import dev.typetype.android.data.network.dto.TwitchStreamDto
import dev.typetype.android.data.network.dto.TwitchUserDto
import dev.typetype.android.data.network.dto.TwitchVideoCommentDto
import dev.typetype.android.data.network.dto.TwitchVideoDto
import dev.typetype.android.domain.feed.Video
import dev.typetype.android.domain.twitch.TwitchCategory
import dev.typetype.android.domain.twitch.TwitchChannel
import dev.typetype.android.domain.twitch.TwitchChatHighlights
import dev.typetype.android.domain.twitch.TwitchClipQuality
import dev.typetype.android.domain.twitch.TwitchPinnedMessage
import dev.typetype.android.domain.twitch.TwitchPoll
import dev.typetype.android.domain.twitch.TwitchPollChoice
import dev.typetype.android.domain.twitch.TwitchUrls
import dev.typetype.android.domain.twitch.TwitchVideoComment
import java.net.URI
import java.util.Calendar
import java.util.TimeZone

internal fun TwitchStreamDto.toVideo(): Video = video(
    url = TwitchUrls.live(user.login),
    title = title,
    user = user,
    thumbnailUrl = previewUrl,
    durationSeconds = 0,
    isLive = true,
    count = viewers,
    at = startedAt,
)

internal fun TwitchVideoDto.toVideo(): Video = video(
    url = TwitchUrls.vod(id),
    title = title,
    user = owner,
    thumbnailUrl = previewUrl,
    durationSeconds = lengthSeconds,
    isLive = recording,
    count = views,
    at = createdAt,
)

internal fun TwitchClipDto.toVideo(): Video = video(
    url = TwitchUrls.clip(slug),
    title = title,
    user = broadcaster,
    thumbnailUrl = thumbnailUrl,
    durationSeconds = durationSeconds.toLong(),
    isLive = false,
    count = views,
    at = createdAt,
)

internal fun TwitchChannelDto.toChannel(): TwitchChannel = TwitchChannel(
    login = login,
    displayName = displayName.ifBlank { login },
    avatarUrl = avatarUrl.ifBlank { null },
    description = description.trim(),
    bannerUrl = bannerUrl?.ifBlank { null },
    followers = followers,
    live = live?.toVideo(),
    liveInfo = live?.let {
        TwitchLiveInfo(it.title, it.viewers, twitchMillis(it.startedAt), it.game?.name?.ifBlank { null }, it.tags)
    },
)

internal fun mergeChatAssets(channel: TwitchChatAssetsDto?, global: TwitchChatAssetsDto?): TwitchChatAssets {
    val emotes = LinkedHashMap<String, ChatEmote>()
    for (emote in channel?.emotes.orEmpty() + global?.emotes.orEmpty()) {
        if (emote.name.isBlank() || emote.url.isBlank()) continue
        emotes.putIfAbsent(emote.name, ChatEmote(emote.name, emote.url2x ?: emote.url, emote.zeroWidth))
    }
    val badges = HashMap<String, ChatImage>()
    for ((id, badge) in global?.badges.orEmpty() + channel?.badges.orEmpty()) {
        if (badge.url.isNotBlank()) badges[id] = ChatImage(badge.title, badge.url2x ?: badge.url)
    }
    return TwitchChatAssets(emotes, badges)
}

internal fun TwitchGameDto.toCategory(): TwitchCategory =
    TwitchCategory(id = id, name = name, slug = slug, boxArtUrl = boxArtUrl?.ifBlank { null }, viewers = viewers)

internal fun TwitchClipQualityDto.toQuality(): TwitchClipQuality? {
    val height = quality.takeWhile(Char::isDigit).toIntOrNull() ?: return null
    return TwitchClipQuality(height, frameRate.toInt(), url).takeIf { url.isNotBlank() }
}

// Live serves its playlists under the instance origin (/live/hls/...), next to /api, not inside it.
internal fun twitchPlaylistUrl(apiBaseUrl: String, reference: String?): String? {
    val trimmed = reference?.trim().orEmpty()
    if (trimmed.isEmpty()) return null
    return runCatching { URI(apiBaseUrl).resolve(trimmed).toString() }.getOrNull()
}

// java.time is not there before API 26 and the app supports API 23: Twitch sends UTC times like 2026-11-17T18:06:44Z.
internal fun twitchMillis(value: String): Long {
    val match = UTC_TIME.matchEntire(value.trim()) ?: return 0L
    val (year, month, day, hour, minute, second) = match.destructured
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).run {
        clear()
        set(year.toInt(), month.toInt() - 1, day.toInt(), hour.toInt(), minute.toInt(), second.toInt())
        timeInMillis
    }
}

private fun video(
    url: String,
    title: String,
    user: TwitchUserDto,
    thumbnailUrl: String,
    durationSeconds: Long,
    isLive: Boolean,
    count: Long,
    at: String,
): Video {
    val millis = twitchMillis(at)
    return Video(
        id = url,
        url = url,
        title = title,
        thumbnailUrl = thumbnailUrl,
        uploaderName = user.displayName.ifBlank { user.login },
        uploaderUrl = TwitchUrls.live(user.login),
        uploaderAvatarUrl = user.avatarUrl,
        uploaderVerified = false,
        durationSeconds = durationSeconds.coerceAtLeast(0),
        isLive = isLive,
        viewCount = count,
        uploadedAtMillis = millis,
        isShortFormContent = false,
        shortDescription = null,
        publishedAtMillis = millis.takeIf { it > 0 },
        isLiveContent = isLive,
    )
}

private val UTC_TIME = Regex("""(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2})(?:\.\d+)?Z""")

internal fun TwitchChatHighlightsDto.toDomain() = TwitchChatHighlights(
    poll = poll?.takeIf { it.id.isNotBlank() && it.choices.isNotEmpty() }?.let { dto ->
        TwitchPoll(
            id = dto.id,
            title = dto.title,
            votes = dto.votes,
            choices = dto.choices.map { TwitchPollChoice(it.id, it.title, it.votes) },
        )
    },
    pinned = pinned?.takeIf { it.id.isNotBlank() && it.text.isNotBlank() }?.let { dto ->
        TwitchPinnedMessage(dto.id, dto.text, dto.displayName.ifBlank { dto.login })
    },
)

internal fun TwitchVideoCommentDto.toDomain() = TwitchVideoComment(
    id = id,
    offsetSeconds = offset,
    author = displayName?.ifBlank { null } ?: login,
    text = text,
    moderator = badges.any { it.startsWith("moderator/") || it.startsWith("broadcaster/") },
    login = login?.ifBlank { null },
    color = color,
    emotes = emotes.map { ChatEmoteRange(it.id, it.start, it.end) },
    badges = badges,
)
