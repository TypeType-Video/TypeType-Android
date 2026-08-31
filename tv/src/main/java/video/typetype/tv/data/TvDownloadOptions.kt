package video.typetype.tv.data

import video.typetype.sdk.core.CreateDownloadJobRequest
import video.typetype.sdk.core.DownloadMode
import video.typetype.sdk.core.DownloadOptions
import video.typetype.sdk.core.StreamAudio
import video.typetype.sdk.core.StreamDetails
import video.typetype.sdk.core.StreamVideo

public enum class TvDownloadKind {
    VIDEO,
    AUDIO,
}

public data class TvDownloadOption(
    val id: String,
    val kind: TvDownloadKind,
    val label: String,
    val detail: String,
    val size: String,
    val recommended: Boolean,
    val request: CreateDownloadJobRequest,
)

internal fun buildTvDownloadOptions(stream: StreamDetails, videoUrl: String): List<TvDownloadOption> {
    val preferredAudio = stream.audioStreams.preferredDownloadAudio()
    val videos = stream.videoOnlyStreams.ifEmpty { stream.videoStreams }
        .distinctBy(StreamVideo::itag)
        .sortedWith(compareByDescending<StreamVideo> { it.height }.thenByDescending { it.frameRate })
        .map { video -> video.toDownloadOption(videoUrl, preferredAudio, recommended = false) }
        .toMutableList()
    videos.recommendedVideoIndex()?.let { index -> videos[index] = videos[index].copy(recommended = true) }
    val audios = stream.audioStreams.distinctBy(StreamAudio::itag)
        .sortedByDescending { it.bitrate ?: 0L }
        .map { audio -> audio.toDownloadOption(videoUrl, recommended = audio.itag == preferredAudio?.itag) }
    return videos + audios
}

private fun StreamVideo.toDownloadOption(
    videoUrl: String,
    audio: StreamAudio?,
    recommended: Boolean,
): TvDownloadOption {
    val container = mediaContainer(mimeType, format, "mp4")
    val quality = when {
        height >= 1080 -> "best"
        height >= 720 -> "balanced"
        else -> "small"
    }
    val fpsLabel = frameRate.takeIf { it > 30 }?.let { " ${it}fps" }.orEmpty()
    val estimatedBytes = contentLength.coerceAtLeast(0L) + (audio?.contentLength ?: 0L).coerceAtLeast(0L)
    return TvDownloadOption(
        id = "video-$itag",
        kind = TvDownloadKind.VIDEO,
        label = "${resolution.ifBlank { "${height}p" }}$fpsLabel",
        detail = listOfNotNull(codec, container.uppercase(), "itag $itag").joinToString(" · "),
        size = formatBytes(estimatedBytes),
        recommended = recommended,
        request = CreateDownloadJobRequest(
            videoUrl,
            DownloadOptions(
                mode = DownloadMode.Video,
                quality = quality,
                format = container,
                videoItag = itag.toString(),
                audioItag = audio?.itag?.toString(),
                height = height.takeIf { it > 0 },
                fps = frameRate.takeIf { it > 0 },
                videoCodec = codec,
                audioCodec = audio?.codec,
                allowQualityFallback = false,
            ),
        ),
    )
}

private fun StreamAudio.toDownloadOption(videoUrl: String, recommended: Boolean): TvDownloadOption {
    val container = mediaContainer(mimeType, format, "m4a")
    val bitrateValue = bitrate?.coerceAtMost(Int.MAX_VALUE.toLong())?.toInt()
    val displayedBitrate = bitrateValue?.let { if (it >= 10_000) it / 1_000 else it }
    val quality = when {
        (displayedBitrate ?: 0) >= 192 -> "best"
        (displayedBitrate ?: 0) >= 128 -> "balanced"
        else -> "small"
    }
    val language = audioTrackName ?: audioLocale ?: quality
    return TvDownloadOption(
        id = "audio-$itag",
        kind = TvDownloadKind.AUDIO,
        label = displayedBitrate?.let { "$it kbps" } ?: "Audio",
        detail = listOfNotNull(language, codec, container.uppercase(), "itag $itag").joinToString(" · "),
        size = formatBytes(contentLength),
        recommended = recommended,
        request = CreateDownloadJobRequest(
            videoUrl,
            DownloadOptions(
                mode = DownloadMode.Audio,
                quality = quality,
                format = container,
                audioItag = itag.toString(),
                audioCodec = codec,
                bitrate = bitrateValue,
                allowQualityFallback = false,
            ),
        ),
    )
}

private fun List<StreamAudio>.preferredDownloadAudio(): StreamAudio? =
    firstOrNull(StreamAudio::isOriginal) ?: maxByOrNull { it.bitrate ?: 0L }

private fun List<TvDownloadOption>.recommendedVideoIndex(): Int? {
    if (isEmpty()) return null
    val fullHd = indexOfFirst { it.request.options.height == 1080 }
    if (fullHd >= 0) return fullHd
    val hd = indexOfFirst { it.request.options.height == 720 }
    return if (hd >= 0) hd else lastIndex
}

private fun mediaContainer(mimeType: String, format: String, fallback: String): String =
    format.trim().lowercase().takeIf(String::isNotBlank)
        ?: mimeType.substringBefore(';').substringAfter('/', "").lowercase().takeIf(String::isNotBlank)
        ?: fallback

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "Size unavailable"
    val units = arrayOf("B", "KB", "MB", "GB")
    var value = bytes.toDouble()
    var unit = 0
    while (value >= 1024.0 && unit < units.lastIndex) {
        value /= 1024.0
        unit += 1
    }
    val decimals = if (value >= 100) 0 else if (value >= 10) 1 else 2
    return "%.${decimals}f %s".format(value, units[unit])
}
