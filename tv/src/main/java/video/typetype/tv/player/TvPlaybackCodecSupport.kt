@file:androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])

package video.typetype.tv.player

import android.content.Context
import android.view.WindowManager
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil
import java.util.concurrent.ConcurrentHashMap
import video.typetype.sdk.core.StreamVideo

internal class TvPlaybackCodecSupport(context: Context) {
    private val applicationContext = context.applicationContext
    private val cache = ConcurrentHashMap<CodecKey, Boolean>()
    @Suppress("DEPRECATION")
    private val displaySize = applicationContext.getSystemService(WindowManager::class.java)
        ?.defaultDisplay?.mode?.let { it.physicalWidth to it.physicalHeight }
        ?: applicationContext.resources.displayMetrics.let { it.widthPixels to it.heightPixels }

    fun isVideoSupported(source: StreamVideo): Boolean {
        if (source.width > displaySize.first || source.height > displaySize.second) return false
        val key = CodecKey(source.codec.orEmpty(), source.width, source.height, source.frameRate)
        return cache.getOrPut(key) {
            val mimeType = source.videoMimeType() ?: return@getOrPut false
            val format = Format.Builder()
                .setSampleMimeType(mimeType)
                .setCodecs(source.codec)
                .setWidth(source.width)
                .setHeight(source.height)
                .apply { if (source.frameRate > 0) setFrameRate(source.frameRate.toFloat()) }
                .build()
            try {
                val supportedDecoders = MediaCodecUtil.getDecoderInfos(mimeType, false, false)
                    .filter { it.isFormatSupported(applicationContext, format) }
                supportedDecoders.isNotEmpty() &&
                    (!source.isHighDefinitionAv1() || supportedDecoders.any { it.hardwareAccelerated })
            } catch (_: MediaCodecUtil.DecoderQueryException) {
                false
            }
        }
    }

    private data class CodecKey(
        val codec: String,
        val width: Int,
        val height: Int,
        val frameRate: Int,
    )
}

private fun StreamVideo.isHighDefinitionAv1(): Boolean =
    codec.orEmpty().startsWith("av01", ignoreCase = true) && height > 720

private fun StreamVideo.videoMimeType(): String? =
    MimeTypes.getVideoMediaMimeType(codec.orEmpty())
        ?: when (mimeType.substringBefore(';').trim().lowercase()) {
            MimeTypes.VIDEO_MP4 -> MimeTypes.VIDEO_H264
            MimeTypes.VIDEO_WEBM -> MimeTypes.VIDEO_VP9
            else -> null
        }
