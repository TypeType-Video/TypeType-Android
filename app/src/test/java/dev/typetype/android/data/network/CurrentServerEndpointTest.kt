package dev.typetype.android.data.network

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrentServerEndpointTest {
    private val endpoint = CurrentServerEndpoint(
        serverId = "instance-a",
        baseUrl = "https://video.example.com/api",
    )

    @Test
    fun `accepts every path on the same origin`() {
        assertTrue(endpoint.owns("https://video.example.com/api/auth/me".toHttpUrl()))
        assertTrue(endpoint.owns("https://video.example.com/proxy/image".toHttpUrl()))
    }

    @Test
    fun `rejects another host scheme or port`() {
        assertFalse(endpoint.owns("https://cdn.example.com/thumbnail.jpg".toHttpUrl()))
        assertFalse(endpoint.owns("http://video.example.com/api".toHttpUrl()))
        assertFalse(endpoint.owns("https://video.example.com:8443/api".toHttpUrl()))
    }
}
