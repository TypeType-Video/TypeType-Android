@file:androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])

package video.typetype.tv.player

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer

internal fun createTvExoPlayer(context: Context): ExoPlayer {
    val audioAttributes = AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
        .build()
    return ExoPlayer.Builder(
        context,
        DefaultRenderersFactory(context).setEnableDecoderFallback(true),
    )
        .setAudioAttributes(audioAttributes, true)
        .setHandleAudioBecomingNoisy(true)
        .setWakeMode(C.WAKE_MODE_NETWORK)
        .setLoadControl(
            DefaultLoadControl.Builder()
                .setBufferDurationsMs(15_000, 30_000, 2_000, 3_000)
                .setBackBuffer(15_000, true)
                .build(),
        )
        .build()
}
