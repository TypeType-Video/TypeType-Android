package dev.typetype.android.data.stream

import dev.typetype.android.data.network.dto.AudioOnlyStreamResponse
import dev.typetype.android.domain.stream.AudioOnlyStreamKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AudioOnlyStreamResponseTest {
    @Test
    fun `resolves server-owned sources and converts duration to milliseconds`() {
        val stream = response().toDomain("https://instance.example/api/")

        assertEquals("https://instance.example/api/streams/audio-only/source?token=value", stream.url)
        assertEquals(AudioOnlyStreamKind.Progressive, stream.kind)
        assertEquals(42_000L, stream.durationMillis)
    }

    @Test
    fun `rejects an audio-only source on another origin`() {
        assertThrows(IllegalStateException::class.java) {
            response(src = "https://media.example/audio.m4a").toDomain(
                "https://instance.example/api/",
            )
        }
    }

    @Test
    fun `rejects a stream kind outside the server contract`() {
        assertThrows(IllegalStateException::class.java) {
            response(kind = "other").toDomain("https://instance.example/api/")
        }
    }

    private fun response(
        src: String = "/streams/audio-only/source?token=value",
        kind: String = "progressive",
    ) = AudioOnlyStreamResponse(
        src = src,
        kind = kind,
        mimeType = "audio/mp4",
        duration = 42,
    )
}
