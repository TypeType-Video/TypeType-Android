package dev.typetype.android.playback

import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import dev.typetype.android.domain.stream.Stream
import dev.typetype.android.domain.stream.StreamSubtitleSource
import dev.typetype.android.services.toSubtitleConfigurations

fun buildResolvedMediaItem(
    stream: Stream,
    videoUrl: String,
    source: PlayableSource,
    subtitles: List<StreamSubtitleSource>,
    startPositionMillis: Long,
    preferOriginalAudio: Boolean = false,
    preferredAudioLocale: String = "",
): MediaItem {
    val metadata = MediaMetadata.Builder()
        .setTitle(stream.title)
        .setArtist(stream.uploaderName)
        .setArtworkUri(stream.thumbnailUrl.toUri())
        .build()
    return MediaItem.Builder()
        .setUri(source.url)
        .setMediaId(videoUrl)
        .setMediaMetadata(metadata)
        .setRequestMetadata(
            source.toRequestMetadata(
                scope = stream.requestScope,
                resumePositionMillis = startPositionMillis.coerceAtLeast(0L),
                isLiveContent = stream.isLiveContent || stream.isLive,
                stream = stream,
                preferOriginalAudio = preferOriginalAudio,
                preferredAudioLocale = preferredAudioLocale,
            ),
        )
        .apply { source.mimeType?.let(::setMimeType) }
        .apply { stream.liveTargetOffsetMillis?.let { setLiveConfiguration(lowLatencyLive(it)) } }
        .setSubtitleConfigurations(subtitles.toSubtitleConfigurations())
        .build()
}

// Twitch announces a 6 s target duration for 2 s segments, so the offset is set in milliseconds; the player may speed
// up a little to settle there, like the web player's low latency mode.
private fun lowLatencyLive(targetMillis: Long): MediaItem.LiveConfiguration = MediaItem.LiveConfiguration.Builder()
    .setTargetOffsetMs(targetMillis)
    .setMaxOffsetMs(targetMillis + LOW_LATENCY_MAX_EXTRA_MS)
    .setMinPlaybackSpeed(1f)
    .setMaxPlaybackSpeed(LOW_LATENCY_MAX_SPEED)
    .build()

private const val LOW_LATENCY_MAX_EXTRA_MS = 6_000L
private const val LOW_LATENCY_MAX_SPEED = 1.1f
