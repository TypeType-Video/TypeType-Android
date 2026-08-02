package dev.typetype.player

import android.os.Handler
import androidx.media3.common.C
import androidx.media3.common.TrackGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.TransferListener
import androidx.media3.exoplayer.LoadingInfo
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.drm.DrmSessionEventListener
import androidx.media3.exoplayer.drm.DrmSessionManager
import androidx.media3.exoplayer.source.DefaultCompositeSequenceableLoaderFactory
import androidx.media3.exoplayer.source.MediaPeriod
import androidx.media3.exoplayer.source.MediaSourceEventListener
import androidx.media3.exoplayer.source.SampleStream
import androidx.media3.exoplayer.source.SequenceableLoader
import androidx.media3.exoplayer.source.TrackGroupArray
import androidx.media3.exoplayer.source.chunk.ChunkSampleStream
import androidx.media3.exoplayer.trackselection.ExoTrackSelection
import androidx.media3.exoplayer.upstream.Allocator
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import java.io.IOException
import java.util.IdentityHashMap

@UnstableApi
internal class TypeTypeMediaPeriod(
    initialWindow: PlaybackWindow,
    private val coordinator: PlaybackWindowCoordinator,
    private val playbackHandler: Handler,
    private val playbackPositionUs: () -> Long,
    private val dataSourceFactory: DataSource.Factory,
    private val transferListener: TransferListener?,
    private val allocator: Allocator,
    private val loadErrorHandlingPolicy: LoadErrorHandlingPolicy,
    private val mediaEventDispatcher: MediaSourceEventListener.EventDispatcher,
    private val drmEventDispatcher: DrmSessionEventListener.EventDispatcher,
) : MediaPeriod,
    SequenceableLoader.Callback<ChunkSampleStream<TypeTypeChunkSource>>,
    PlaybackWindowCoordinator.Listener {
    private val tracks = listOfNotNull(initialWindow.audio, initialWindow.video)
    private val trackGroups = TrackGroupArray(
        *tracks.map { TrackGroup(it.id, it.toMedia3Format()) }.toTypedArray(),
    )
    private var streams = emptyArray<ChunkSampleStream<TypeTypeChunkSource>>()
    private val ownedStreams =
        IdentityHashMap<SampleStream, ChunkSampleStream<TypeTypeChunkSource>>()
    private var loader: SequenceableLoader = DefaultCompositeSequenceableLoaderFactory().empty()
    private var callback: MediaPeriod.Callback? = null
    private var released = false
    private val refreshPlaybackWindow = object : Runnable {
        override fun run() {
            pollPlaybackWindow()
            if (!released) playbackHandler.postDelayed(this, windowPollIntervalMs())
        }
    }

    override fun prepare(callback: MediaPeriod.Callback, positionUs: Long) {
        this.callback = callback
        coordinator.addListener(this)
        playbackHandler.post(refreshPlaybackWindow)
        callback.onPrepared(this)
    }

    override fun maybeThrowPrepareError() {
        coordinator.maybeThrowError()
    }

    override fun getTrackGroups(): TrackGroupArray = trackGroups

    override fun selectTracks(
        selections: Array<out ExoTrackSelection?>,
        mayRetainStreamFlags: BooleanArray,
        sampleStreams: Array<SampleStream?>,
        streamResetFlags: BooleanArray,
        positionUs: Long,
    ): Long {
        val selectedStreams = mutableListOf<ChunkSampleStream<TypeTypeChunkSource>>()
        selections.indices.forEach { index ->
            val existing = sampleStreams[index]?.let(ownedStreams::get)
            if (existing != null && (selections[index] == null || !mayRetainStreamFlags[index])) {
                existing.release()
                ownedStreams.remove(existing)
                sampleStreams[index] = null
            }
            if (sampleStreams[index] == null && selections[index] != null) {
                val stream = buildStream(requireNotNull(selections[index]), positionUs)
                ownedStreams[stream] = stream
                sampleStreams[index] = stream
                streamResetFlags[index] = true
            }
            sampleStreams[index]?.let(ownedStreams::get)?.let(selectedStreams::add)
        }
        streams = selectedStreams.toTypedArray()
        loader = DefaultCompositeSequenceableLoaderFactory().create(
            selectedStreams,
            selectedStreams.map { listOf(it.primaryTrackType) },
        )
        return positionUs
    }

    override fun discardBuffer(positionUs: Long, toKeyframe: Boolean) {
        streams.forEach { it.discardBuffer(positionUs, toKeyframe) }
    }

    override fun readDiscontinuity(): Long = C.TIME_UNSET

    override fun seekToUs(positionUs: Long): Long {
        val bufferedStartUs = retainedPlaybackStartUs(
            streams.map { it.chunkSource.bufferedStartPositionUs },
        )
        val bufferedEndUs = loader.bufferedPositionUs
        if (!canSeekWithinPlaybackBuffer(bufferedStartUs, positionUs, bufferedEndUs)) {
            coordinator.seek(positionUs)
        }
        streams.forEach { it.seekToUs(positionUs) }
        return positionUs
    }

    override fun getAdjustedSeekPositionUs(
        positionUs: Long,
        seekParameters: SeekParameters,
    ): Long = streams.firstOrNull { it.primaryTrackType == C.TRACK_TYPE_VIDEO }
        ?.getAdjustedSeekPositionUs(positionUs, seekParameters)
        ?: positionUs

    override fun getBufferedPositionUs(): Long = loader.bufferedPositionUs

    override fun getNextLoadPositionUs(): Long = loader.nextLoadPositionUs

    override fun continueLoading(loadingInfo: LoadingInfo): Boolean =
        loader.continueLoading(loadingInfo)

    override fun isLoading(): Boolean = loader.isLoading

    override fun reevaluateBuffer(positionUs: Long) {
        loader.reevaluateBuffer(positionUs)
    }

    override fun onContinueLoadingRequested(
        source: ChunkSampleStream<TypeTypeChunkSource>,
    ) {
        callback?.onContinueLoadingRequested(this)
    }

    override fun onWindowAvailable(window: PlaybackWindow) {
        try {
            streams.forEach { it.chunkSource.update(window) }
            callback?.onContinueLoadingRequested(this)
        } catch (failure: IOException) {
            onWindowFailure()
        }
    }

    override fun onWindowFailure() {
        callback?.onContinueLoadingRequested(this)
    }

    fun release() {
        released = true
        playbackHandler.removeCallbacks(refreshPlaybackWindow)
        coordinator.removeListener(this)
        streams.forEach(ChunkSampleStream<TypeTypeChunkSource>::release)
        ownedStreams.clear()
        streams = emptyArray()
    }

    private fun buildStream(
        selection: ExoTrackSelection,
        positionUs: Long,
    ): ChunkSampleStream<TypeTypeChunkSource> {
        val trackIndex = trackGroups.indexOf(selection.trackGroup)
        require(trackIndex in tracks.indices)
        val chunkSource = TypeTypeChunkSource(
            initialTrack = tracks[trackIndex],
            dataSourceFactory = dataSourceFactory,
            transferListener = transferListener,
            coordinator = coordinator,
        )
        return ChunkSampleStream(
            tracks[trackIndex].trackType(),
            null,
            null,
            chunkSource,
            this,
            allocator,
            positionUs,
            DrmSessionManager.DRM_UNSUPPORTED,
            drmEventDispatcher,
            loadErrorHandlingPolicy,
            mediaEventDispatcher,
            false,
            C.TIME_UNSET,
            null,
        )
    }

    private fun pollPlaybackWindow() {
        val window = coordinator.window ?: return
        if (window.endOfStream || streams.isEmpty()) return
        val positionUs = playbackPositionUs().coerceAtLeast(0L)
        val bufferedEndUs = loader.bufferedPositionUs
            .takeUnless { it == C.TIME_END_OF_SOURCE || it == C.TIME_UNSET }
            ?: return
        val availableEndUs = listOfNotNull(
            window.audio.endPositionUs,
            window.video?.endPositionUs,
        ).min()
        if (
            shouldRefreshPlaybackWindow(
                playbackPositionUs = positionUs,
                bufferedEndUs = bufferedEndUs,
                availableEndUs = availableEndUs,
                endOfStream = window.endOfStream,
                activeLive = window.live?.active == true,
            )
        ) {
            requestWindow(positionUs, bufferedEndUs)
        }
    }

    private fun windowPollIntervalMs(): Long =
        if (coordinator.window?.live?.active == true) {
            LIVE_WINDOW_POLL_INTERVAL_MS
        } else {
            WINDOW_POLL_INTERVAL_MS
        }

    private fun requestWindow(playbackPositionUs: Long, bufferedEndUs: Long) {
        val ranges = streams.mapNotNull {
            val stream = it
            val endUs = it.bufferedPositionUs.takeIf { value -> value != C.TIME_END_OF_SOURCE }
                ?: bufferedEndUs
            endUs.takeIf { value -> value > playbackPositionUs }?.let { value ->
                val track = tracks.first { candidate ->
                    candidate.trackType() == stream.primaryTrackType
                }
                PlaybackBufferedRange(
                    trackId = track.id,
                    startPositionUs = playbackBufferStartUs(
                        playbackPositionUs = playbackPositionUs,
                        retainedStartPositionUs = stream.chunkSource.bufferedStartPositionUs,
                    ),
                    endPositionUs = value,
                )
            }
        }
        coordinator.load(playbackPositionUs, ranges)
    }

    private companion object {
        const val WINDOW_POLL_INTERVAL_MS = 500L
    }
}

internal fun shouldRefreshPlaybackWindow(
    playbackPositionUs: Long,
    bufferedEndUs: Long,
    availableEndUs: Long,
    endOfStream: Boolean,
    activeLive: Boolean = false,
): Boolean {
    if (endOfStream) return false
    val refreshThresholdUs = if (activeLive) {
        LIVE_WINDOW_REFRESH_THRESHOLD_US
    } else {
        WINDOW_REFRESH_THRESHOLD_US
    }
    val bufferedAheadUs = bufferedEndUs - playbackPositionUs
    val availableAheadUs = availableEndUs - playbackPositionUs
    return bufferedAheadUs < refreshThresholdUs &&
        availableAheadUs < refreshThresholdUs
}

internal fun playbackBufferStartUs(
    playbackPositionUs: Long,
    retainedStartPositionUs: Long,
): Long {
    val trimmedStartUs = (playbackPositionUs - BACK_BUFFER_US).coerceAtLeast(0L)
    return retainedStartPositionUs
        .takeUnless { it == C.TIME_UNSET }
        ?.coerceAtLeast(trimmedStartUs)
        ?: trimmedStartUs
}

internal fun canSeekWithinPlaybackBuffer(
    bufferedStartUs: Long,
    targetPositionUs: Long,
    bufferedEndUs: Long,
): Boolean {
    if (bufferedStartUs == C.TIME_UNSET) return false
    if (bufferedEndUs == C.TIME_END_OF_SOURCE || bufferedEndUs == C.TIME_UNSET) return false
    return targetPositionUs >= bufferedStartUs &&
        targetPositionUs <= bufferedEndUs - LOCAL_SEEK_END_MARGIN_US
}

internal fun retainedPlaybackStartUs(trackStartsUs: List<Long>): Long =
    trackStartsUs
        .takeIf { starts -> starts.isNotEmpty() && starts.none { it == C.TIME_UNSET } }
        ?.maxOrNull()
        ?: C.TIME_UNSET

private const val LOCAL_SEEK_END_MARGIN_US = 250_000L
private const val BACK_BUFFER_US = 30_000_000L
private const val WINDOW_REFRESH_THRESHOLD_US = 20_000_000L
private const val LIVE_WINDOW_REFRESH_THRESHOLD_US = 5_000_000L
private const val LIVE_WINDOW_POLL_INTERVAL_MS = 250L
