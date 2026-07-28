package dev.typetype.player

import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackWindowCoordinatorTest {
    @Test
    fun `first seek starts immediately`() {
        val loader = RecordingLoader()
        val coordinator = PlaybackWindowCoordinator(loader, RecordingDispatcher())

        coordinator.seek(10_000L)

        assertEquals(listOf(10_000L), loader.seekPositions)
    }

    @Test
    fun `repeated seeks retain only the latest deferred position`() {
        val loader = RecordingLoader()
        val dispatcher = RecordingDispatcher()
        val coordinator = PlaybackWindowCoordinator(loader, dispatcher)

        coordinator.seek(10_000L)
        coordinator.seek(20_000L)
        coordinator.seek(30_000L)

        assertEquals(listOf(10_000L), loader.seekPositions)
        assertEquals(listOf(250L, 250L), dispatcher.delays)

        dispatcher.runDelayed()

        assertEquals(listOf(10_000L, 30_000L), loader.seekPositions)
        assertTrue(loader.cancellations.first().cancelled)
    }

    @Test
    fun `cancelled seek result cannot replace a deferred seek`() {
        val loader = RecordingLoader()
        val dispatcher = RecordingDispatcher()
        val coordinator = PlaybackWindowCoordinator(loader, dispatcher)

        coordinator.seek(10_000L)
        coordinator.seek(20_000L)
        loader.callbacks.first()(Result.failure(IOException("cancelled")))
        dispatcher.runPosted()

        coordinator.maybeThrowError()
        dispatcher.runDelayed()

        assertEquals(listOf(10_000L, 20_000L), loader.seekPositions)
    }

    @Test
    fun `window loading cannot overtake a deferred seek`() {
        val loader = RecordingLoader()
        val dispatcher = RecordingDispatcher()
        val coordinator = PlaybackWindowCoordinator(loader, dispatcher)

        coordinator.seek(10_000L)
        coordinator.seek(20_000L)
        coordinator.load(15_000L)

        assertEquals(0, loader.loadCount)

        dispatcher.runDelayed()

        assertEquals(listOf(10_000L, 20_000L), loader.seekPositions)
    }

    @Test
    fun `release cancels deferred seek`() {
        val loader = RecordingLoader()
        val dispatcher = RecordingDispatcher()
        val coordinator = PlaybackWindowCoordinator(loader, dispatcher)

        coordinator.seek(10_000L)
        coordinator.seek(20_000L)
        coordinator.release()
        dispatcher.runDelayed()

        assertEquals(listOf(10_000L), loader.seekPositions)
        assertTrue(loader.released)
    }

    @Test
    fun `source and media period both receive sliding window updates`() {
        val loader = RecordingLoader()
        val dispatcher = RecordingDispatcher()
        val coordinator = PlaybackWindowCoordinator(loader, dispatcher)
        val source = RecordingWindowListener()
        val period = RecordingWindowListener()
        coordinator.addListener(source)
        coordinator.addListener(period)

        coordinator.seek(10_000L)
        loader.callbacks.single()(Result.success(playbackWindow()))
        dispatcher.runPosted()

        assertEquals(1, source.available)
        assertEquals(1, period.available)
    }
}

private fun playbackWindow() = PlaybackWindow(
    generation = 0L,
    durationUs = 60_000_000L,
    startPositionUs = 10_000_000L,
    endOfStream = false,
    audio = PlaybackTrack(
        kind = PlaybackTrackKind.Audio,
        id = "140",
        mimeType = "audio/mp4",
        initializationUrl = "https://example.test/140/init",
        segments = listOf(
            PlaybackSegment(
                url = "https://example.test/140/segment/1",
                startPositionUs = 10_000_000L,
                durationUs = 10_000_000L,
            ),
        ),
    ),
    video = null,
)

private class RecordingWindowListener : PlaybackWindowCoordinator.Listener {
    var available = 0

    override fun onWindowAvailable(window: PlaybackWindow) {
        available++
    }

    override fun onWindowFailure() = Unit
}

private class RecordingLoader : PlaybackWindowLoader {
    val seekPositions = mutableListOf<Long>()
    val callbacks = mutableListOf<(Result<PlaybackWindow>) -> Unit>()
    val cancellations = mutableListOf<RecordingCancellation>()
    var loadCount = 0
    var released = false

    override fun load(
        request: () -> PlaybackWindowRequest,
        callback: (Result<PlaybackWindow>) -> Unit,
    ): PlaybackLoadCancellation {
        loadCount++
        return RecordingCancellation().also(cancellations::add)
    }

    override fun seek(
        positionUs: Long,
        callback: (Result<PlaybackWindow>) -> Unit,
    ): PlaybackLoadCancellation {
        seekPositions += positionUs
        callbacks += callback
        return RecordingCancellation().also(cancellations::add)
    }

    override fun release() {
        released = true
    }
}

private class RecordingCancellation : PlaybackLoadCancellation {
    var cancelled = false

    override fun cancel() {
        cancelled = true
    }
}

private class RecordingDispatcher : PlaybackTaskDispatcher {
    private val posted = mutableListOf<Runnable>()
    private val delayed = mutableListOf<Runnable>()
    private val recordedDelays = mutableListOf<Long>()

    val delays: List<Long>
        get() = recordedDelays.toList()

    override fun post(task: Runnable) {
        posted += task
    }

    override fun postDelayed(task: Runnable, delayMs: Long) {
        delayed += task
        recordedDelays += delayMs
    }

    override fun remove(task: Runnable) {
        posted -= task
        delayed -= task
    }

    fun runPosted() {
        posted.toList().also(posted::removeAll).forEach(Runnable::run)
    }

    fun runDelayed() {
        delayed.toList().also(delayed::removeAll).forEach(Runnable::run)
    }
}
