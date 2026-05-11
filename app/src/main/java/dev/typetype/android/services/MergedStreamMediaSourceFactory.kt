package dev.typetype.android.services

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy

@UnstableApi
class MergedStreamMediaSourceFactory(
    context: Context,
) : MediaSource.Factory {

    private val delegate = DefaultMediaSourceFactory(context)

    override fun setDrmSessionManagerProvider(
        drmSessionManagerProvider: DrmSessionManagerProvider,
    ): MediaSource.Factory {
        delegate.setDrmSessionManagerProvider(drmSessionManagerProvider)
        return this
    }

    override fun setLoadErrorHandlingPolicy(
        loadErrorHandlingPolicy: LoadErrorHandlingPolicy,
    ): MediaSource.Factory {
        delegate.setLoadErrorHandlingPolicy(loadErrorHandlingPolicy)
        return this
    }

    override fun getSupportedTypes(): IntArray =
        delegate.supportedTypes

    override fun createMediaSource(mediaItem: MediaItem): MediaSource {
        val extras = mediaItem.requestMetadata.extras
        val audioUrl = extras?.getString(MergedStreamMediaKeys.EXTRA_AUDIO_URL)?.takeIf { it.isNotBlank() }
        if (audioUrl == null) return delegate.createMediaSource(mediaItem)
        val videoMimeType = extras.getString(MergedStreamMediaKeys.EXTRA_VIDEO_MIME_TYPE)
        val audioMimeType = extras.getString(MergedStreamMediaKeys.EXTRA_AUDIO_MIME_TYPE)
        val videoItem = mediaItem.buildUpon()
            .setRequestMetadata(MediaItem.RequestMetadata.EMPTY)
            .apply { videoMimeType?.let { setMimeType(it) } }
            .build()
        val audioItem = MediaItem.Builder()
            .setUri(audioUrl)
            .apply { audioMimeType?.let { setMimeType(it) } }
            .build()
        return MergingMediaSource(
            delegate.createMediaSource(videoItem),
            delegate.createMediaSource(audioItem),
        )
    }

}
