package dev.typetype.android.data.diagnostics

import dev.typetype.android.data.network.CurrentServerEndpoint
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NetworkRouteClassifierTest {
    private val endpoint = CurrentServerEndpoint(
        serverId = "server",
        baseUrl = "https://video.example/api/v1/",
    )

    @Test
    fun preservesOnlyKnownStaticSegments() {
        val url = "https://video.example/api/v1/auth/oidc/start?returnTo=private".toHttpUrl()

        assertEquals("/auth/oidc/start", NetworkRouteClassifier.classify(endpoint, url))
    }

    @Test
    fun identifiesSabrWithoutRecordingTheVideoUrl() {
        val url = "https://video.example/api/v1/streams/youtube/sabr?url=private-video".toHttpUrl()

        assertEquals("/streams/youtube/sabr", NetworkRouteClassifier.classify(endpoint, url))
    }

    @Test
    fun distinguishesSabrBootstrapWithoutRecordingTheVideoUrl() {
        val url = "https://video.example/api/v1/streams/youtube/sabr/bootstrap?url=private-video".toHttpUrl()

        assertEquals("/streams/youtube/sabr/bootstrap", NetworkRouteClassifier.classify(endpoint, url))
    }

    @Test
    fun identifiesYouTubeSubtitlesWithoutRecordingTrackIdentifiers() {
        val url = "https://video.example/api/v1/subtitles/youtube/private-video" +
            "?language=fr&sourceLanguage=en&name=private-track"

        assertEquals(
            "/subtitles/youtube",
            NetworkRouteClassifier.classify(endpoint, url.toHttpUrl()),
        )
    }

    @Test
    fun identifiesSharedSabrSeekWithoutRecordingTheSession() {
        val url = "https://video.example/api/v1/sabr/playback/private-session/seek".toHttpUrl()

        assertEquals("/sabr/playback/seek", NetworkRouteClassifier.classify(endpoint, url))
    }

    @Test
    fun identifiesSharedSabrLifecycleWithoutRecordingTheSession() {
        val position = "https://video.example/api/v1/sabr/playback/private-session/position".toHttpUrl()
        val prefetch = "https://video.example/api/v1/sabr/playback/private-session/prefetch".toHttpUrl()

        assertEquals("/sabr/playback/position", NetworkRouteClassifier.classify(endpoint, position))
        assertEquals("/sabr/playback/prefetch", NetworkRouteClassifier.classify(endpoint, prefetch))
    }

    @Test
    fun identifiesSabrMediaWithoutRecordingItagOrSequence() {
        val url = "https://video.example/api/v1/sabr/playback/private-session/137/segment/42".toHttpUrl()
        val initUrl = "https://video.example/api/v1/sabr/playback/private-session/137/init".toHttpUrl()

        assertEquals("/sabr/playback/segment", NetworkRouteClassifier.classify(endpoint, url))
        assertEquals("/sabr/playback/init", NetworkRouteClassifier.classify(endpoint, initUrl))
    }

    @Test
    fun identifiesSabrCreationWithoutRecordingTheVideoId() {
        val url = "https://video.example/api/v1/sabr/playback/private-video".toHttpUrl()

        assertEquals("/sabr/playback/create", NetworkRouteClassifier.classify(endpoint, url))
    }

    @Test
    fun removesDynamicPathAndQueryValues() {
        val url = "https://video.example/api/v1/playlists/private-id/videos/encoded-video".toHttpUrl()

        assertEquals("/playlists", NetworkRouteClassifier.classify(endpoint, url))
    }

    @Test
    fun labelsUnknownRoutesWithoutCopyingTheirValue() {
        val url = "https://video.example/api/v1/private-value?token=secret".toHttpUrl()

        assertEquals("/other", NetworkRouteClassifier.classify(endpoint, url))
    }

    @Test
    fun ignoresOtherOrigins() {
        val url = "https://other.example/api/v1/auth/me".toHttpUrl()

        assertNull(NetworkRouteClassifier.classify(endpoint, url))
    }
}
