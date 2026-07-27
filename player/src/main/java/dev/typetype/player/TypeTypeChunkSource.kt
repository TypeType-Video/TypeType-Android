package dev.typetype.player

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import androidx.media3.exoplayer.LoadingInfo
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.source.chunk.BundledChunkExtractor
import androidx.media3.exoplayer.source.chunk.Chunk
import androidx.media3.exoplayer.source.chunk.ChunkHolder
import androidx.media3.exoplayer.source.chunk.ChunkSource
import androidx.media3.exoplayer.source.chunk.ContainerMediaChunk
import androidx.media3.exoplayer.source.chunk.InitializationChunk
import androidx.media3.exoplayer.source.chunk.MediaChunk
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import androidx.media3.extractor.Extractor
import androidx.media3.extractor.mkv.MatroskaExtractor
import androidx.media3.extractor.mp4.FragmentedMp4Extractor
import androidx.media3.extractor.text.DefaultSubtitleParserFactory
import java.io.IOException

@UnstableApi
internal class TypeTypeChunkSource(
    initialTrack: PlaybackTrack,
    dataSourceFactory: DataSource.Factory,
    transferListener: TransferListener?,
    private val coordinator: PlaybackWindowCoordinator,
    private val requestWindow: (Long, Long) -> Unit,
) : ChunkSource {
    private val format: Format = initialTrack.toMedia3Format()
    private val dataSource = dataSourceFactory.createDataSource().apply {
        transferListener?.let(::addTransferListener)
    }
    private val extractor = BundledChunkExtractor(
        initialTrack.createExtractor(),
        initialTrack.trackType(),
        format,
    )
    private var track = initialTrack
    private var retainedStartPositionUs = C.TIME_UNSET

    val bufferedStartPositionUs: Long
        get() = retainedStartPositionUs

    fun update(window: PlaybackWindow) {
        val updated = when (track.kind) {
            PlaybackTrackKind.Audio -> window.audio
            PlaybackTrackKind.Video -> window.video
        } ?: throw IOException("Playback window removed its video track")
        if (updated.id != track.id || updated.mimeType != track.mimeType) {
            throw IOException("Playback window changed its track identity")
        }
        track = updated
    }

    override fun getAdjustedSeekPositionUs(
        positionUs: Long,
        seekParameters: SeekParameters,
    ): Long {
        val segments = track.segments
        val nextIndex = segments.indexOfFirst { it.endPositionUs > positionUs }
            .takeIf { it >= 0 } ?: return positionUs
        val first = segments[nextIndex].startPositionUs
        val second = segments.getOrNull(nextIndex + 1)?.startPositionUs ?: first
        return seekParameters.resolveSeekPositionUs(positionUs, first, second)
    }

    override fun maybeThrowError() {
        coordinator.maybeThrowError()
    }

    override fun getPreferredQueueSize(
        playbackPositionUs: Long,
        queue: List<MediaChunk>,
    ): Int {
        updateRetainedRange(queue)
        return queue.size
    }

    override fun shouldCancelLoad(
        playbackPositionUs: Long,
        loadingChunk: Chunk,
        queue: List<MediaChunk>,
    ): Boolean = false

    override fun getNextChunk(
        loadingInfo: LoadingInfo,
        loadPositionUs: Long,
        queue: List<MediaChunk>,
        out: ChunkHolder,
    ) {
        updateRetainedRange(queue)
        if (coordinator.isSeeking) return
        if (extractor.sampleFormats == null) {
            out.chunk = InitializationChunk(
                dataSource,
                DataSpec(Uri.parse(track.initializationUrl)),
                format,
                C.SELECTION_REASON_INITIAL,
                null,
                extractor,
            )
            return
        }
        val targetUs = queue.lastOrNull()?.endTimeUs ?: loadPositionUs
        val segment = track.segments.firstOrNull {
            it.endPositionUs > targetUs + TIMELINE_TOLERANCE_US
        }
        if (segment != null) {
            out.chunk = segment.toChunk(queue.isEmpty(), targetUs)
            return
        }
        val window = coordinator.window ?: return
        if (window.endOfStream && targetUs >= track.endPositionUs - TIMELINE_TOLERANCE_US) {
            out.endOfStream = true
            return
        }
        if (shouldRefreshPlaybackWindow(loadingInfo.playbackPositionUs, targetUs)) {
            requestWindow(loadingInfo.playbackPositionUs, targetUs)
        }
    }

    override fun onChunkLoadCompleted(chunk: Chunk) = Unit

    override fun onChunkLoadError(
        chunk: Chunk,
        cancelable: Boolean,
        loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo,
        loadErrorHandlingPolicy: LoadErrorHandlingPolicy,
    ): Boolean = false

    override fun release() {
        extractor.release()
    }

    private fun updateRetainedRange(queue: List<MediaChunk>) {
        retainedStartPositionUs = queue.firstOrNull()?.startTimeUs ?: C.TIME_UNSET
    }

    private fun PlaybackSegment.toChunk(first: Boolean, loadPositionUs: Long) =
        ContainerMediaChunk(
            dataSource,
            DataSpec(Uri.parse(url)),
            format,
            C.SELECTION_REASON_INITIAL,
            null,
            startPositionUs,
            endPositionUs,
            if (first) loadPositionUs else C.TIME_UNSET,
            C.TIME_UNSET,
            startPositionUs,
            1,
            0L,
            extractor,
        )

    private companion object {
        const val TIMELINE_TOLERANCE_US = 1_000L
    }
}

internal fun shouldRefreshPlaybackWindow(
    playbackPositionUs: Long,
    bufferedEndUs: Long,
): Boolean = bufferedEndUs - playbackPositionUs < WINDOW_REFRESH_THRESHOLD_US

internal fun PlaybackTrack.createExtractor(): Extractor {
    val subtitleParsers = DefaultSubtitleParserFactory()
    return when (mimeType.substringBefore(';').trim().lowercase()) {
        "audio/webm", "video/webm", "application/webm" ->
            MatroskaExtractor(subtitleParsers, 0)
        else -> FragmentedMp4Extractor(subtitleParsers, 0)
    }
}

private const val WINDOW_REFRESH_THRESHOLD_US = 20_000_000L
