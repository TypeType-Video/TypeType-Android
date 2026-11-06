package dev.typetype.android.playback

import androidx.media3.common.MimeTypes
import dev.typetype.android.domain.preferences.RECOMMENDED_CODEC_KEY
import dev.typetype.android.domain.stream.Stream
import dev.typetype.android.domain.stream.StreamAudioSource
import dev.typetype.android.domain.stream.StreamVideoSource
import java.util.Base64

// NicoNico comes as one HLS playlist per quality and one for the audio, with no master playlist, and the
// Server announces the audio playlist as a progressive file. The master playlist built here plays them
// together and lets the player adapt the quality, as the web client does.
internal fun Stream.pickHlsProviderSource(
    selectedQuality: String,
    selectedAudioKey: String?,
    defaultAudioLanguage: String,
    preferOriginalLanguage: Boolean,
    codecSupport: PlaybackCodecSupport,
    selectedCodec: String = RECOMMENDED_CODEC_KEY,
): PlayableSource? {
    val playlists = videoOnlyStreams.filter { it.isHlsPlaylist() && it.url.isNotBlank() }
    if (playlists.isEmpty()) return null
    val videos = if (selectedQuality.isExplicitVideoSelection()) {
        listOfNotNull(playlists.pickVideo(selectedQuality, codecSupport, selectedCodec))
    } else {
        playlists.sortedByDescending(StreamVideoSource::height)
    }
    val first = videos.firstOrNull() ?: return null
    val audio = audioStreams.pickAudio(
        selectedAudioKey = selectedAudioKey,
        defaultAudioLanguage = defaultAudioLanguage,
        preferOriginalLanguage = preferOriginalLanguage,
        codecSupport = codecSupport,
        preferredDefaultAudioTrackId = preferredDefaultAudioTrackId,
        originalAudioTrackId = originalAudioTrackId,
    )
    val manifest = hlsMasterPlaylist(videos, audio)
    val encoded = Base64.getEncoder().encodeToString(manifest.toByteArray(Charsets.UTF_8))
    return PlayableSource(
        url = "data:$HLS_PLAYLIST_MIME_TYPE;base64,$encoded",
        mimeType = MimeTypes.APPLICATION_M3U8,
        sourceKey = if (videos.size == 1) first.videoSelectionKey() else first.url,
    )
}

internal fun hlsMasterPlaylist(videos: List<StreamVideoSource>, audio: StreamAudioSource?): String = buildString {
    appendLine("#EXTM3U")
    appendLine("#EXT-X-VERSION:6")
    appendLine("#EXT-X-INDEPENDENT-SEGMENTS")
    if (audio != null) {
        appendLine(
            "#EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID=\"$AUDIO_GROUP\",NAME=\"${audio.quality.quoted()}\"," +
                "DEFAULT=YES,AUTOSELECT=YES,URI=\"${audio.url.quoted()}\"",
        )
    }
    for (video in videos) {
        val bandwidth = video.bandwidth() + (audio?.bandwidth() ?: 0)
        val resolution = video.height.takeIf { it > 0 }?.let { height ->
            ",RESOLUTION=${video.width.takeIf { it > 0 } ?: (height * 16 / 9)}x$height"
        }.orEmpty()
        val codecs = if (audio != null) "$VIDEO_CODEC,$AUDIO_CODEC" else VIDEO_CODEC
        val audioGroup = if (audio != null) ",AUDIO=\"$AUDIO_GROUP\"" else ""
        appendLine("#EXT-X-STREAM-INF:BANDWIDTH=$bandwidth$resolution,CODECS=\"$codecs\"$audioGroup")
        appendLine(video.url)
    }
}

private fun StreamVideoSource.isHlsPlaylist(): Boolean =
    playbackMimeType?.contains("mpegurl", ignoreCase = true) == true

private fun StreamVideoSource.bandwidth(): Int = bitrate?.takeIf { it > 0 }?.let { if (it < 10_000) it * 1000 else it }
    ?: when {
        height >= 1080 -> 5_000_000
        height >= 720 -> 2_500_000
        height >= 480 -> 1_200_000
        height >= 360 -> 800_000
        else -> 500_000
    }

private fun StreamAudioSource.bandwidth(): Int = bitrate?.takeIf { it > 0 }?.let { if (it < 10_000) it * 1000 else it }
    ?: 128_000

private fun String?.quoted(): String = orEmpty().ifBlank { "Audio" }
    .replace("\\", "\\\\").replace("\"", "\\\"").replace('\r', ' ').replace('\n', ' ')

private const val HLS_PLAYLIST_MIME_TYPE = "application/vnd.apple.mpegurl"
private const val AUDIO_GROUP = "audio"
private const val VIDEO_CODEC = "avc1.4d4020"
private const val AUDIO_CODEC = "mp4a.40.2"
