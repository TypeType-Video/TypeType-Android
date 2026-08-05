package dev.typetype.player

import java.io.IOException
import kotlin.random.Random
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
    fun `repeated seeks start immediately and only the latest result wins`() {
        val loader = RecordingLoader()
        val dispatcher = RecordingDispatcher()
        val coordinator = PlaybackWindowCoordinator(loader, dispatcher)

        coordinator.seek(10_000L)
        coordinator.seek(20_000L)
        coordinator.seek(30_000L)

        assertEquals(listOf(10_000L, 20_000L, 30_000L), loader.seekPositions)
        assertTrue(loader.cancellations.take(2).all { it.cancelled })

        loader.callbacks[1](Result.success(playbackWindow(generation = 2L)))
        loader.callbacks[0](Result.success(playbackWindow(generation = 1L)))
        loader.callbacks[2](Result.success(playbackWindow(generation = 3L)))
        dispatcher.runPosted()

        assertEquals(3L, coordinator.window?.generation)
    }

    @Test
    fun `cancelled seek result cannot replace a newer seek`() {
        val loader = RecordingLoader()
        val dispatcher = RecordingDispatcher()
        val coordinator = PlaybackWindowCoordinator(loader, dispatcher)

        coordinator.seek(10_000L)
        coordinator.seek(20_000L)
        loader.callbacks.first()(Result.failure(IOException("cancelled")))
        dispatcher.runPosted()

        coordinator.maybeThrowError()
        loader.callbacks.last()(Result.success(playbackWindow(generation = 2L)))
        dispatcher.runPosted()

        assertEquals(listOf(10_000L, 20_000L), loader.seekPositions)
        assertEquals(2L, coordinator.window?.generation)
    }

    @Test
    fun `window loading cannot overtake an active seek`() {
        val loader = RecordingLoader()
        val dispatcher = RecordingDispatcher()
        val coordinator = PlaybackWindowCoordinator(loader, dispatcher)

        coordinator.seek(10_000L)
        coordinator.seek(20_000L)
        coordinator.load(15_000L)

        assertEquals(listOf(10_000L, 20_000L), loader.seekPositions)
        assertEquals(0, loader.loadCount)
    }

    @Test
    fun `release cancels active seek`() {
        val loader = RecordingLoader()
        val dispatcher = RecordingDispatcher()
        val coordinator = PlaybackWindowCoordinator(loader, dispatcher)

        coordinator.seek(10_000L)
        coordinator.seek(20_000L)
        coordinator.release()
        assertEquals(listOf(10_000L, 20_000L), loader.seekPositions)
        assertTrue(loader.cancellations.all { it.cancelled })
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

    @Test
    fun `rapid seek results cannot overtake the final request`() {
        val loader = RecordingLoader()
        val dispatcher = RecordingDispatcher()
        val coordinator = PlaybackWindowCoordinator(loader, dispatcher)

        repeat(5_000) { index -> coordinator.seek(index * 1_000L) }
        (0 until 5_000).shuffled(Random(15)).forEach { index ->
            loader.callbacks[index](
                Result.success(playbackWindow(generation = index.toLong())),
            )
        }
        dispatcher.runPosted()

        assertEquals(5_000, loader.seekPositions.size)
        assertTrue(loader.cancellations.dropLast(1).all { it.cancelled })
        assertEquals(4_999L, coordinator.window?.generation)
    }
}

private fun playbackWindow(generation: Long = 0L) = PlaybackWindow(
    generation = generation,
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

    override fun post(task: Runnable) {
        posted += task
    }

    fun runPosted() {
        posted.toList().also(posted::removeAll).forEach(Runnable::run)
    }
}
