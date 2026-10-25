package dev.typetype.android.playback

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.typetype.android.domain.stream.StreamAudioSource
import dev.typetype.android.domain.stream.StreamVideoSource
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

enum class DecoderSupport(val rank: Int) {
    Unsupported(0),
    Software(1),
    Hardware(2),
}

interface PlaybackCodecSupport {
    fun video(source: StreamVideoSource): DecoderSupport
    fun audio(source: StreamAudioSource): DecoderSupport
    fun rejectVideo(format: Format): Boolean = false
}

// One instance for the whole app: the player, Shorts, prewarming and the queue share what each decoder can play
// and which formats already failed.
@OptIn(markerClass = [UnstableApi::class])
@Singleton
class DevicePlaybackCodecSupport @Inject constructor(
    @ApplicationContext context: Context,
    private val rejectedStore: RejectedVideoFormatStore,
) : PlaybackCodecSupport {
    private val applicationContext = context.applicationContext
    private val videoCache = ConcurrentHashMap<VideoCodecKey, DecoderSupport>()
    private val audioCache = ConcurrentHashMap<AudioCodecKey, DecoderSupport>()
    private val rejectedVideoFormats = ConcurrentHashMap.newKeySet<RejectedVideoCodecKey>().apply {
        for (stored in rejectedStore.load()) parseRejectedVideoCodecKey(stored)?.let { add(it) }
    }

    override fun video(source: StreamVideoSource): DecoderSupport {
        val key = source.videoCodecKey()
        if (source.rejectedVideoCodecKey() in rejectedVideoFormats) {
            return DecoderSupport.Unsupported
        }
        videoCache[key]?.let { return it }
        val mediaMimeType = source.videoMediaMimeType() ?: return DecoderSupport.Unsupported
        val format = Format.Builder()
            .setSampleMimeType(mediaMimeType)
            .setCodecs(source.codec)
            .setWidth(source.width)
            .setHeight(source.height)
            .apply {
                if (source.fps > 0) setFrameRate(source.fps.toFloat())
            }
            .build()
        val support = decoderSupport(format)
        return videoCache.putIfAbsent(key, support) ?: support
    }

    override fun audio(source: StreamAudioSource): DecoderSupport {
        val key = AudioCodecKey(source.mimeType, source.codec)
        audioCache[key]?.let { return it }
        val mediaMimeType = source.audioMediaMimeType() ?: return DecoderSupport.Unsupported
        val format = Format.Builder()
            .setSampleMimeType(mediaMimeType)
            .setCodecs(source.codec)
            .build()
        val support = decoderSupport(format)
        return audioCache.putIfAbsent(key, support) ?: support
    }

    override fun rejectVideo(format: Format): Boolean {
        val key = format.rejectedVideoCodecKey() ?: return false
        if (!rejectedVideoFormats.add(key)) return false
        rejectedStore.add(key.stored())
        return true
    }

    private fun decoderSupport(format: Format): DecoderSupport = try {
        val decoders = MediaCodecUtil.getDecoderInfos(
            requireNotNull(format.sampleMimeType),
            false,
            false,
        ).filter { it.isFormatSupported(applicationContext, format) }
        when {
            decoders.any { it.hardwareAccelerated } -> DecoderSupport.Hardware
            decoders.isNotEmpty() -> DecoderSupport.Software
            else -> DecoderSupport.Unsupported
        }
    } catch (_: MediaCodecUtil.DecoderQueryException) {
        DecoderSupport.Unsupported
    }
}

private data class VideoCodecKey(
    val codec: String,
    val width: Int,
    val height: Int,
    val fps: Int,
)

private data class RejectedVideoCodecKey(
    val codec: String,
    val width: Int,
    val height: Int,
) {
    fun stored(): String = "$codec|$width|$height"
}

private fun parseRejectedVideoCodecKey(stored: String): RejectedVideoCodecKey? {
    val parts = stored.split('|')
    if (parts.size != 3) return null
    val width = parts[1].toIntOrNull() ?: return null
    val height = parts[2].toIntOrNull() ?: return null
    return RejectedVideoCodecKey(parts[0], width, height)
}

private fun StreamVideoSource.videoCodecKey() = VideoCodecKey(
    codec = codec.orEmpty().trim().lowercase(),
    width = width,
    height = height,
    fps = fps,
)

private fun StreamVideoSource.rejectedVideoCodecKey() = RejectedVideoCodecKey(
    codec = codec.orEmpty().trim().lowercase(),
    width = width,
    height = height,
)

private fun Format.rejectedVideoCodecKey(): RejectedVideoCodecKey? {
    val codec = codecs.orEmpty().trim().lowercase().takeIf(String::isNotBlank) ?: return null
    if (width <= 0 || height <= 0) return null
    return RejectedVideoCodecKey(
        codec = codec,
        width = width,
        height = height,
    )
}

private data class AudioCodecKey(
    val mimeType: String,
    val codec: String?,
)

@OptIn(markerClass = [UnstableApi::class])
private fun StreamVideoSource.videoMediaMimeType(): String? {
    return MimeTypes.getVideoMediaMimeType(codec.orEmpty())
        ?: when (mimeType.substringBefore(';').trim().lowercase()) {
            MimeTypes.VIDEO_MP4 -> MimeTypes.VIDEO_H264
            MimeTypes.VIDEO_WEBM -> MimeTypes.VIDEO_VP9
            else -> null
        }
}

@OptIn(markerClass = [UnstableApi::class])
private fun StreamAudioSource.audioMediaMimeType(): String? {
    return MimeTypes.getAudioMediaMimeType(codec.orEmpty())
        ?: when (mimeType.substringBefore(';').trim().lowercase()) {
            MimeTypes.AUDIO_MP4 -> MimeTypes.AUDIO_AAC
            MimeTypes.AUDIO_WEBM -> MimeTypes.AUDIO_OPUS
            else -> null
        }
}
