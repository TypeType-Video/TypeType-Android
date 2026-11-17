package dev.typetype.android.data.twitch

import dev.typetype.android.data.network.dto.TwitchChannelDto
import dev.typetype.android.data.network.dto.TwitchClipDto
import dev.typetype.android.data.network.dto.TwitchClipQualityDto
import dev.typetype.android.data.network.dto.TwitchGameDto
import dev.typetype.android.data.network.dto.TwitchStreamDto
import dev.typetype.android.data.network.dto.TwitchUserDto
import dev.typetype.android.data.network.dto.TwitchVideoDto
import dev.typetype.android.domain.feed.Video
import dev.typetype.android.domain.twitch.TwitchCategory
import dev.typetype.android.domain.twitch.TwitchChannel
import dev.typetype.android.domain.twitch.TwitchClipQuality
import dev.typetype.android.domain.twitch.TwitchUrls
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
)

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
