package dev.typetype.player

import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.TransferListener
import androidx.media3.exoplayer.drm.DrmSessionEventListener
import androidx.media3.exoplayer.source.BaseMediaSource
import androidx.media3.exoplayer.source.MediaPeriod
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.SinglePeriodTimeline
import androidx.media3.exoplayer.upstream.Allocator
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import java.io.IOException

@UnstableApi
class TypeTypeMediaSource(
    private val mediaItem: MediaItem,
    private val initialPositionUs: Long,
    private val initialWindow: PlaybackWindow?,
    private val playbackPositionUs: () -> Long,
    private val windowLoader: PlaybackWindowLoader,
    private val dataSourceFactory: DataSource.Factory,
    private val loadErrorHandlingPolicy: LoadErrorHandlingPolicy,
) : BaseMediaSource(), PlaybackWindowCoordinator.Listener {
    private var transferListener: TransferListener? = null
    private var coordinator: PlaybackWindowCoordinator? = null
    private var window: PlaybackWindow? = null

    override fun getMediaItem(): MediaItem = mediaItem

    override fun prepareSourceInternal(mediaTransferListener: TransferListener?) {
        transferListener = mediaTransferListener
        val handler = Handler(requireNotNull(Looper.myLooper()))
        val playbackCoordinator = PlaybackWindowCoordinator(windowLoader, handler)
        coordinator = playbackCoordinator
        playbackCoordinator.setListener(this)
        val seededWindow = initialWindow
        if (seededWindow == null) {
            playbackCoordinator.load(initialPositionUs)
        } else {
            playbackCoordinator.seed(seededWindow)
            onWindowAvailable(seededWindow)
        }
    }

    override fun maybeThrowSourceInfoRefreshError() {
        coordinator?.maybeThrowError()
    }

    override fun createPeriod(
        id: MediaSource.MediaPeriodId,
        allocator: Allocator,
        startPositionUs: Long,
    ): MediaPeriod {
        val currentWindow = requireNotNull(window)
        val currentCoordinator = requireNotNull(coordinator)
        return TypeTypeMediaPeriod(
            initialWindow = currentWindow,
            coordinator = currentCoordinator,
            playbackHandler = Handler(requireNotNull(Looper.myLooper())),
            playbackPositionUs = playbackPositionUs,
            dataSourceFactory = dataSourceFactory,
            transferListener = transferListener,
            allocator = allocator,
            loadErrorHandlingPolicy = loadErrorHandlingPolicy,
            mediaEventDispatcher = createEventDispatcher(id),
            drmEventDispatcher = createDrmEventDispatcher(id),
        )
    }

    override fun releasePeriod(mediaPeriod: MediaPeriod) {
        (mediaPeriod as TypeTypeMediaPeriod).release()
    }

    override fun releaseSourceInternal() {
        coordinator?.release()
        coordinator = null
        transferListener = null
        window = null
    }

    override fun onWindowAvailable(window: PlaybackWindow) {
        this.window = window
        refreshSourceInfo(
            SinglePeriodTimeline(
                window.durationUs,
                true,
                false,
                false,
                window,
                mediaItem,
            ),
        )
    }

    override fun onWindowFailure() = Unit
}
