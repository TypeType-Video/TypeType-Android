package dev.typetype.android.services

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.SingleSampleMediaSource
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import dev.typetype.android.domain.stream.SabrPlaybackBinding
import dev.typetype.android.domain.stream.SabrPlaybackRepository
import dev.typetype.android.domain.stream.StreamRequestScope
import dev.typetype.player.TypeTypeMediaSource

@UnstableApi
internal class MergedStreamMediaSourceFactory(
    context: Context,
    private val mediaClientFactory: ScopedMediaClientFactory,
    private val sabrPlaybackRepository: SabrPlaybackRepository,
    private val sabrPlaybackWindowCache: SabrPlaybackWindowCache,
    private val playbackPositionUs: () -> Long,
    private val recoveryDispatcher: SabrPlaybackRecoveryDispatcher,
) : MediaSource.Factory {

    private val applicationContext = context.applicationContext
    private var drmSessionManagerProvider: DrmSessionManagerProvider? = null
    private var loadErrorHandlingPolicy: LoadErrorHandlingPolicy? = null

    override fun setDrmSessionManagerProvider(
        drmSessionManagerProvider: DrmSessionManagerProvider,
    ): MediaSource.Factory {
        this.drmSessionManagerProvider = drmSessionManagerProvider
        return this
    }

    override fun setLoadErrorHandlingPolicy(
        loadErrorHandlingPolicy: LoadErrorHandlingPolicy,
    ): MediaSource.Factory {
        this.loadErrorHandlingPolicy = loadErrorHandlingPolicy
        return this
    }

    override fun getSupportedTypes(): IntArray =
        DefaultMediaSourceFactory(applicationContext).supportedTypes

    override fun createMediaSource(mediaItem: MediaItem): MediaSource {
        if (mediaItem.isSabrPlayback()) return createSabrMediaSource(mediaItem)
        val delegate = delegateFor(mediaItem)
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

    private fun createSabrMediaSource(mediaItem: MediaItem): MediaSource {
        val scope = mediaItem.requestScope()
        val binding = mediaItem.requireSabrTransportScope(scope)
        val target = requireNotNull(mediaItem.requestMetadata.extras?.sabrPlaybackTarget()) {
            "Missing SABR playback target"
        }
        val client = mediaClientFactory.create(requireNotNull(scope))
            .newBuilder()
            .followRedirects(false)
            .followSslRedirects(false)
            .build()
        val http = OkHttpDataSource.Factory(client)
        val transportState = SabrPlaybackTransportState(binding)
        val mediaDataSource = SabrPlaybackContractDataSource.Factory(
            upstream = http,
            expectedBinding = transportState::currentBinding,
        )
        val policy = SabrLoadErrorHandlingPolicy(
            loadErrorHandlingPolicy ?: DefaultLoadErrorHandlingPolicy(),
        )
        val baseItem = mediaItem.buildUpon()
            .setSubtitleConfigurations(emptyList())
            .build()
        val videoSource = TypeTypeMediaSource(
            mediaItem = baseItem,
            initialPositionUs = mediaItem.initialSabrPositionUs(),
            initialWindow = sabrPlaybackWindowCache.take(binding)?.toPlayerWindow(),
            playbackPositionUs = playbackPositionUs,
            windowLoader = SabrPlaybackWindowLoader(
                repository = sabrPlaybackRepository,
                target = target,
                transportState = transportState,
                recoveryDispatcher = recoveryDispatcher,
            ),
            dataSourceFactory = mediaDataSource,
            loadErrorHandlingPolicy = policy,
        )
        val subtitleSources = mediaItem.localConfiguration?.subtitleConfigurations.orEmpty().map {
            createSubtitleMediaSource(it, mediaDataSource, loadErrorHandlingPolicy)
        }
        return if (subtitleSources.isEmpty()) {
            videoSource
        } else {
            MergingMediaSource(videoSource, *subtitleSources.toTypedArray())
        }
    }

    private fun delegateFor(mediaItem: MediaItem): DefaultMediaSourceFactory {
        val scope = mediaItem.requestScope()
        val client = scope?.let(mediaClientFactory::create)
        val delegate = if (client == null) {
            DefaultMediaSourceFactory(applicationContext)
        } else {
            val http = OkHttpDataSource.Factory(client)
            DefaultMediaSourceFactory(DefaultDataSource.Factory(applicationContext, http))
        }
        drmSessionManagerProvider?.let(delegate::setDrmSessionManagerProvider)
        loadErrorHandlingPolicy?.let(delegate::setLoadErrorHandlingPolicy)
        return delegate
    }

    private fun MediaItem.isSabrPlayback(): Boolean =
        requestMetadata.extras?.getString(MergedStreamMediaKeys.EXTRA_SABR_REQUEST_KEY)
            ?.isNotBlank() == true

    private fun MediaItem.requestScope(): StreamRequestScope? {
        return requestMetadata.extras?.streamRequestScope()
    }

}

@Suppress("DEPRECATION")
@UnstableApi
private fun createSubtitleMediaSource(
    subtitle: MediaItem.SubtitleConfiguration,
    dataSourceFactory: DataSource.Factory,
    loadErrorHandlingPolicy: LoadErrorHandlingPolicy?,
): MediaSource {
    val factory = SingleSampleMediaSource.Factory(dataSourceFactory)
        .setTreatLoadErrorsAsEndOfStream(false)
    loadErrorHandlingPolicy?.let(factory::setLoadErrorHandlingPolicy)
    return factory.createMediaSource(subtitle, C.TIME_UNSET)
}

internal fun MediaItem.requireSabrTransportScope(scope: StreamRequestScope?): SabrPlaybackBinding {
    requireNotNull(scope) { "Missing SABR media request scope" }
    val binding = requireNotNull(requestMetadata.extras?.sabrPlaybackBinding()) {
        "Missing SABR playback binding"
    }
    return binding
}

private fun MediaItem.initialSabrPositionUs(): Long {
    val positionMs = requestMetadata.extras
        ?.resumePositionMillis()
        ?.coerceAtLeast(0L)
        ?: 0L
    return Math.multiplyExact(positionMs, 1_000L)
}
