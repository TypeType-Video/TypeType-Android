package dev.typetype.android.feature.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil
import dev.typetype.android.domain.stream.StreamAudioSource
import dev.typetype.android.domain.stream.StreamVideoSource
import java.util.concurrent.ConcurrentHashMap

internal enum class DecoderSupport(val rank: Int) {
    Unsupported(0),
    Software(1),
    Hardware(2),
}

internal interface PlaybackCodecSupport {
    fun video(source: StreamVideoSource): DecoderSupport
    fun audio(source: StreamAudioSource): DecoderSupport
}

@OptIn(markerClass = [UnstableApi::class])
internal class DevicePlaybackCodecSupport(
    context: Context,
) : PlaybackCodecSupport {
    private val applicationContext = context.applicationContext
    private val videoCache = ConcurrentHashMap<VideoCodecKey, DecoderSupport>()
    private val audioCache = ConcurrentHashMap<AudioCodecKey, DecoderSupport>()

    override fun video(source: StreamVideoSource): DecoderSupport {
        val key = VideoCodecKey(
            mimeType = source.mimeType,
            codec = source.codec,
            width = source.width,
            height = source.height,
            fps = source.fps,
        )
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
    val mimeType: String,
    val codec: String?,
    val width: Int,
    val height: Int,
    val fps: Int,
)

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
