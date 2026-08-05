package dev.typetype.android.data.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ServerRelativeUrlTest {
    @Test
    fun `resolves a relative media path against the server API base`() {
        assertEquals(
            "https://instance.example/api/sabr/playback/session/manifest",
            resolveServerUrl(
                "https://instance.example/api/",
                "sabr/playback/session/manifest",
            ),
        )
    }

    @Test
    fun `keeps a root relative media path inside the server API base`() {
        assertEquals(
            "https://instance.example/api/streams/audio-only/source?token=value",
            resolveServerUrl(
                "https://instance.example/api/",
                "/streams/audio-only/source?token=value",
            ),
        )
    }

    @Test
    fun `does not duplicate an API prefix returned by the server`() {
        assertEquals(
            "https://instance.example/api/sabr/manifest/video",
            resolveServerUrl(
                "https://instance.example/api/",
                "/api/sabr/manifest/video",
            ),
        )
    }

    @Test
    fun `accepts an absolute URL on the exact server origin`() {
        assertEquals(
            "https://instance.example/media/manifest.mpd",
            resolveServerUrl(
                "https://instance.example/api/",
                "https://instance.example/media/manifest.mpd",
            ),
        )
    }

    @Test
    fun `rejects an absolute URL on another origin`() {
        assertNull(
            resolveServerUrl(
                "https://instance.example/api/",
                "https://media.example/manifest.mpd",
            ),
        )
    }

    @Test
    fun `rejects an absolute URL on another server port`() {
        assertNull(
            resolveServerUrl(
                "https://instance.example:8443/api/",
                "https://instance.example/manifest.mpd",
            ),
        )
    }

    @Test
    fun `accepts only the playback manifest bound to the requested session`() {
        assertEquals(
            "https://instance.example/api/sabr/playback/session/manifest",
            resolveSabrPlaybackManifestUrl(
                "https://instance.example/api/",
                "/sabr/playback/session/manifest",
                "session",
            ),
        )
        assertNull(
            resolveSabrPlaybackManifestUrl(
                "https://instance.example/api/",
                "/sabr/playback/another-session/manifest",
                "session",
            ),
        )
        assertEquals(
            "https://instance.example/api/sabr/playback/session/manifest",
            resolveSabrPlaybackManifestUrl(
                "https://instance.example/api/",
                "/api/sabr/playback/session/manifest",
                "session",
            ),
        )
    }
}
