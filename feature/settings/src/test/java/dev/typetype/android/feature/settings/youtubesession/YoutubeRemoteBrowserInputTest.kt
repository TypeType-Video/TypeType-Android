package dev.typetype.android.feature.settings.youtubesession

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import dev.typetype.android.domain.youtubesession.KeyEvent
import dev.typetype.android.domain.youtubesession.YoutubeRemoteBrowserInput
import org.junit.Assert.assertEquals
import org.junit.Test

class YoutubeRemoteBrowserInputTest {
    @Test
    fun replacingSuffixProducesBackspacesAndText() {
        val inputs = remoteTextEdit("hello", "help").toInputs()

        assertEquals(5, inputs.size)
        assertEquals(KeyEvent.Down, (inputs[0] as YoutubeRemoteBrowserInput.Key).event)
        assertEquals(KeyEvent.Up, (inputs[1] as YoutubeRemoteBrowserInput.Key).event)
        assertEquals(KeyEvent.Down, (inputs[2] as YoutubeRemoteBrowserInput.Key).event)
        assertEquals(KeyEvent.Up, (inputs[3] as YoutubeRemoteBrowserInput.Key).event)
        assertEquals("p", (inputs[4] as YoutubeRemoteBrowserInput.Text).value)
    }

    @Test
    fun appendedTextUsesOneTextInput() {
        assertEquals(
            listOf(YoutubeRemoteBrowserInput.Text(" world")),
            remoteTextEdit("hello", "hello world").toInputs(),
        )
    }

    @Test
    fun pointMappingAccountsForLetterboxing() {
        assertEquals(
            IntSize(960, 540),
            remotePoint(
                position = Offset(500f, 500f),
                containerSize = IntSize(1000, 1000),
                frameSize = IntSize(1920, 1080),
            ),
        )
        assertEquals(
            IntSize(0, 0),
            remotePoint(
                position = Offset(0f, 0f),
                containerSize = IntSize(1000, 1000),
                frameSize = IntSize(1920, 1080),
            ),
        )
    }

    @Test
    fun viewportIsBoundedByServerContract() {
        assertEquals(IntSize(320, 240), remoteViewport(IntSize(100, 100)))
        assertEquals(IntSize(1920, 1080), remoteViewport(IntSize(4000, 3000)))
        assertEquals(IntSize(800, 600), remoteViewport(IntSize(800, 600)))
    }
}
