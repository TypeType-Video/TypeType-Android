package dev.typetype.android.services

import androidx.media3.common.MimeTypes
import dev.typetype.android.domain.stream.AudioOnlyStream
import dev.typetype.android.domain.stream.AudioOnlyStreamKind
import org.junit.Assert.assertEquals
import org.junit.Test

class AudioOnlyMimeTypeTest {
    private fun stream(kind: AudioOnlyStreamKind, mimeType: String) = AudioOnlyStream(
        url = "https://watch.example/api/streams/hls-manifest?token=t",
        kind = kind,
        mimeType = mimeType,
        codec = null,
        bitrate = 192,
        contentLength = null,
        durationMillis = 745_000,
    )

    @Test
    fun `server manifests play with the Media3 type the player recognizes`() {
        assertEquals(
            MimeTypes.APPLICATION_M3U8,
            stream(AudioOnlyStreamKind.Hls, "application/vnd.apple.mpegurl").playerMimeType(),
        )
        assertEquals(
            MimeTypes.APPLICATION_MPD,
            stream(AudioOnlyStreamKind.Dash, "application/dash+xml").playerMimeType(),
        )
        assertEquals("audio/mp4", stream(AudioOnlyStreamKind.Progressive, "audio/mp4").playerMimeType())
    }
}
