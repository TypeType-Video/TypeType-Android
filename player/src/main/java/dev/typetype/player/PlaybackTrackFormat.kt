package dev.typetype.player

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi

@OptIn(markerClass = [UnstableApi::class])
internal fun PlaybackTrack.toMedia3Format(): Format {
    val containerMimeType = mimeType.substringBefore(';').trim()
    val codecs = CODECS_PATTERN.find(mimeType)?.groupValues?.get(1)?.trim()
    val sampleMimeType = codecs
        ?.split(',')
        ?.firstNotNullOfOrNull { MimeTypes.getMediaMimeType(it.trim()) }
        ?: defaultSampleMimeType(containerMimeType)
    return Format.Builder()
        .setId(id)
        .setContainerMimeType(containerMimeType)
        .setSampleMimeType(sampleMimeType)
        .setCodecs(codecs)
        .build()
}

internal fun PlaybackTrack.trackType(): Int = when (kind) {
    PlaybackTrackKind.Audio -> C.TRACK_TYPE_AUDIO
    PlaybackTrackKind.Video -> C.TRACK_TYPE_VIDEO
}

@OptIn(markerClass = [UnstableApi::class])
private fun PlaybackTrack.defaultSampleMimeType(containerMimeType: String): String? = when (kind) {
    PlaybackTrackKind.Audio -> when (containerMimeType) {
        MimeTypes.AUDIO_MP4 -> MimeTypes.AUDIO_AAC
        MimeTypes.AUDIO_WEBM -> MimeTypes.AUDIO_OPUS
        else -> null
    }
    PlaybackTrackKind.Video -> when (containerMimeType) {
        MimeTypes.VIDEO_MP4 -> MimeTypes.VIDEO_H264
        MimeTypes.VIDEO_WEBM -> MimeTypes.VIDEO_VP9
        else -> null
    }
}

private val CODECS_PATTERN = Regex("""codecs\s*=\s*"?([^";]+)"?""", RegexOption.IGNORE_CASE)
