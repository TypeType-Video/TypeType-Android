package video.typetype.tv.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

public class AutoplayCountdownControllerTest {
    @Test
    public fun countdownAdvancesOnlyAfterTheConfiguredDelay(): Unit {
        var advances = 0
        val controller = AutoplayCountdownController { advances += 1 }
        controller.configure(enabled = true, seconds = 3)

        controller.onPlaybackEnded()
        assertTrue(controller.active)
        assertEquals(3, controller.remainingSeconds)

        controller.tick()
        controller.tick()
        assertEquals(0, advances)
        controller.tick()

        assertEquals(1, advances)
        assertFalse(controller.active)
    }

    @Test
    public fun pauseAndCancelKeepAutoplayUnderViewerControl(): Unit {
        var advances = 0
        val controller = AutoplayCountdownController { advances += 1 }
        controller.configure(enabled = true, seconds = 5)
        controller.onPlaybackEnded()

        controller.togglePause()
        controller.tick()
        assertEquals(5, controller.remainingSeconds)
        controller.togglePause()
        controller.tick()
        controller.cancel()

        assertFalse(controller.active)
        assertEquals(0, advances)
    }

    @Test
    public fun zeroDelayAdvancesImmediatelyAndDisabledAutoplayDoesNothing(): Unit {
        var advances = 0
        val controller = AutoplayCountdownController { advances += 1 }
        controller.configure(enabled = true, seconds = 0)
        controller.onPlaybackEnded()
        assertEquals(1, advances)

        controller.configure(enabled = false, seconds = 10)
        controller.onPlaybackEnded()
        assertEquals(1, advances)
    }
}
