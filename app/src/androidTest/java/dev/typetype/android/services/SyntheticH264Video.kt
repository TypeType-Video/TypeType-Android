package dev.typetype.android.services

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File
import java.nio.ByteBuffer

internal fun createSyntheticH264Video(
    context: Context,
    durationSeconds: Int = DEFAULT_DURATION_SECONDS,
    frameRate: Int = DEFAULT_FRAME_RATE,
): File {
    require(durationSeconds > 0)
    require(frameRate > 0)
    val output = File.createTempFile("playback-smoke-", ".mp4", context.cacheDir)
    val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
    val muxer = MediaMuxer(output.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    var codecStarted = false
    var muxerStarted = false
    var trackIndex = -1
    val bufferInfo = MediaCodec.BufferInfo()
    val frameCount = frameRate * durationSeconds
    var nextFrame = 0
    var outputEnded = false

    try {
        codec.configure(
            videoFormat(codec.codecInfo, frameRate),
            null,
            null,
            MediaCodec.CONFIGURE_FLAG_ENCODE,
        )
        codec.start()
        codecStarted = true
        while (!outputEnded) {
            if (nextFrame <= frameCount) {
                val inputIndex = codec.dequeueInputBuffer(CODEC_TIMEOUT_US)
                if (inputIndex >= 0) {
                    val presentationTimeUs = nextFrame * MICROS_PER_SECOND / frameRate
                    if (nextFrame == frameCount) {
                        codec.queueInputBuffer(
                            inputIndex,
                            0,
                            0,
                            presentationTimeUs,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                        )
                    } else {
                        val buffer = requireNotNull(codec.getInputBuffer(inputIndex))
                        val size = writeFrame(buffer, nextFrame)
                        codec.queueInputBuffer(inputIndex, 0, size, presentationTimeUs, 0)
                    }
                    nextFrame += 1
                }
            }

            var draining = true
            while (draining) {
                val outputIndex = codec.dequeueOutputBuffer(bufferInfo, CODEC_TIMEOUT_US)
                when {
                    outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> draining = false
                    outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        check(!muxerStarted)
                        trackIndex = muxer.addTrack(codec.outputFormat)
                        muxer.start()
                        muxerStarted = true
                    }
                    outputIndex >= 0 -> {
                        val buffer = requireNotNull(codec.getOutputBuffer(outputIndex))
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                            bufferInfo.size = 0
                        }
                        if (bufferInfo.size > 0) {
                            check(muxerStarted)
                            buffer.position(bufferInfo.offset)
                            buffer.limit(bufferInfo.offset + bufferInfo.size)
                            muxer.writeSampleData(trackIndex, buffer, bufferInfo)
                        }
                        outputEnded = bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                        codec.releaseOutputBuffer(outputIndex, false)
                    }
                }
            }
        }
    } finally {
        try {
            if (codecStarted) codec.stop()
        } finally {
            codec.release()
            try {
                if (muxerStarted) muxer.stop()
            } finally {
                muxer.release()
            }
        }
    }
    check(output.length() > 0L)
    return output
}

@Suppress("DEPRECATION")
private fun videoFormat(
    codecInfo: MediaCodecInfo,
    frameRate: Int,
): MediaFormat =
    MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, WIDTH, HEIGHT).apply {
        setInteger(
            MediaFormat.KEY_COLOR_FORMAT,
            codecInfo.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_AVC)
                .colorFormats
                .firstOrNull { it == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar }
                ?: MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible,
        )
        setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
        setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
        setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
    }

private fun writeFrame(buffer: ByteBuffer, frameNumber: Int): Int {
    val lumaSize = WIDTH * HEIGHT
    val luma = (48 + frameNumber * 3 % 160).toByte()
    buffer.clear()
    repeat(lumaSize) { buffer.put(luma) }
    repeat(lumaSize / 2) { buffer.put(NEUTRAL_CHROMA) }
    return lumaSize * 3 / 2
}

private const val WIDTH = 320
private const val HEIGHT = 180
private const val DEFAULT_FRAME_RATE = 15
private const val DEFAULT_DURATION_SECONDS = 3
private const val BIT_RATE = 250_000
private const val CODEC_TIMEOUT_US = 10_000L
private const val MICROS_PER_SECOND = 1_000_000L
private const val NEUTRAL_CHROMA: Byte = -128
