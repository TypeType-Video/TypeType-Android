package dev.typetype.android.data.youtubesession

import dev.typetype.android.domain.youtubesession.KeyEvent
import dev.typetype.android.domain.youtubesession.PointerEvent
import dev.typetype.android.domain.youtubesession.YoutubeRemoteBrowserInput
import dev.typetype.android.domain.youtubesession.YoutubeRemoteBrowserPhase
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class YoutubeRemoteBrowserMessagesTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun parsesStatusAndErrorMessages() {
        val status = parseYoutubeRemoteBrowserMessage(
            json,
            """{"type":"status","phase":"awaiting_login"}""",
        )
        val error = parseYoutubeRemoteBrowserMessage(
            json,
            """{"type":"error","message":"Remote browser expired"}""",
        )

        assertEquals(YoutubeRemoteBrowserPhase.AwaitingLogin, status?.phase)
        assertEquals(YoutubeRemoteBrowserPhase.Error, error?.phase)
        assertEquals("Remote browser expired", error?.errorMessage)
    }

    @Test
    fun ignoresMalformedAndUnknownMessages() {
        assertNull(parseYoutubeRemoteBrowserMessage(json, "not-json"))
        assertNull(parseYoutubeRemoteBrowserMessage(json, """{"type":"status","phase":"future"}"""))
        assertNull(parseYoutubeRemoteBrowserMessage(json, """{"type":"other"}"""))
    }

    @Test
    fun encodesPointerAndKeyboardInput() {
        assertEquals(
            """{"type":"pointer","event":"down","x":120,"y":80,"button":"left"}""",
            encodeYoutubeRemoteBrowserInput(
                YoutubeRemoteBrowserInput.Pointer(PointerEvent.Down, 120, 80),
            ),
        )
        assertEquals(
            """{"type":"key","event":"up","key":"Enter","code":"Enter","modifiers":["Shift"]}""",
            encodeYoutubeRemoteBrowserInput(
                YoutubeRemoteBrowserInput.Key(
                    event = KeyEvent.Up,
                    key = "Enter",
                    code = "Enter",
                    modifiers = listOf("Shift"),
                ),
            ),
        )
    }

    @Test
    fun encodesTextResizeWheelAndCancelInput() {
        assertEquals(
            """{"type":"text","value":"hello"}""",
            encodeYoutubeRemoteBrowserInput(YoutubeRemoteBrowserInput.Text("hello")),
        )
        assertEquals(
            """{"type":"resize","width":800,"height":450}""",
            encodeYoutubeRemoteBrowserInput(YoutubeRemoteBrowserInput.Resize(800, 450)),
        )
        assertEquals(
            """{"type":"wheel","deltaX":1.5,"deltaY":-2.0}""",
            encodeYoutubeRemoteBrowserInput(YoutubeRemoteBrowserInput.Wheel(1.5f, -2f)),
        )
        assertEquals(
            """{"type":"cancel"}""",
            encodeYoutubeRemoteBrowserInput(YoutubeRemoteBrowserInput.Cancel),
        )
    }
}
