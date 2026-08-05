package dev.typetype.android.feature.player

import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import dev.typetype.android.domain.stream.Stream
import dev.typetype.android.domain.stream.StreamSubtitleSource
import dev.typetype.android.services.toSubtitleConfigurations

internal fun buildResolvedMediaItem(
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
        .setSubtitleConfigurations(subtitles.toSubtitleConfigurations())
        .build()
}
