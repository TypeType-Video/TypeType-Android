package dev.typetype.android.domain.setup

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerAddressTest {
    @Test
    fun buildsRootAndApiCandidatesForPublicInstance() {
        assertEquals(
            listOf("https://video.example/", "https://video.example/api/"),
            ServerAddress.candidateBaseUrls("video.example"),
        )
    }

    @Test
    fun keepsExplicitApiPathOnly() {
        assertEquals(
            listOf("https://video.example/prefix/api/"),
            ServerAddress.candidateBaseUrls("https://video.example/prefix/api/"),
        )
    }

    @Test
    fun allowsExplicitHttpForLocalInstance() {
        assertEquals(
            listOf("http://192.168.1.10:8080/", "http://192.168.1.10:8080/api/"),
            ServerAddress.candidateBaseUrls("http://192.168.1.10:8080"),
        )
    }

    @Test
    fun keepsBracketedIpv6AndPort() {
        assertEquals(
            listOf("http://[fd12:3456::1]:8080/", "http://[fd12:3456::1]:8080/api/"),
            ServerAddress.candidateBaseUrls("http://[fd12:3456::1]:8080"),
        )
    }

    @Test
    fun allowsCleartextDomainAfterPrivateDnsResolution() {
        assertEquals(
            listOf("http://video.home.arpa:8080/", "http://video.home.arpa:8080/api/"),
            ServerAddress.candidateBaseUrls(
                "http://video.home.arpa:8080",
                allowLocalCleartext = true,
            ),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsPublicCleartextInstance() {
        ServerAddress.candidateBaseUrls("http://video.example")
    }

    @Test
    fun `detects local hostnames`() {
        assertTrue(ServerAddress.requiresLocalNetworkAccess("typetype.local:8080"))
        assertTrue(ServerAddress.requiresLocalNetworkAccess("http://localhost:8080/api"))
        assertTrue(ServerAddress.requiresLocalNetworkAccess("https://typetype-server/api"))
    }

    @Test
    fun `detects private and overlay IPv4 addresses`() {
        assertTrue(ServerAddress.requiresLocalNetworkAccess("192.168.1.20:8080"))
        assertTrue(ServerAddress.requiresLocalNetworkAccess("http://10.0.0.4"))
        assertTrue(ServerAddress.requiresLocalNetworkAccess("https://172.31.4.2/api"))
        assertTrue(ServerAddress.requiresLocalNetworkAccess("http://100.96.2.1"))
        assertTrue(ServerAddress.requiresLocalNetworkAccess("http://169.254.2.3"))
    }

    @Test
    fun `detects local IPv6 addresses`() {
        assertTrue(ServerAddress.requiresLocalNetworkAccess("http://[::1]:8080"))
        assertTrue(ServerAddress.requiresLocalNetworkAccess("https://[fd12:3456::1]/api"))
        assertTrue(ServerAddress.requiresLocalNetworkAccess("https://[fe80::1234]/api"))
    }

    @Test
    fun `leaves public addresses alone`() {
        assertFalse(ServerAddress.requiresLocalNetworkAccess("https://typetype.example.com"))
        assertFalse(ServerAddress.requiresLocalNetworkAccess("https://8.8.8.8"))
        assertFalse(ServerAddress.requiresLocalNetworkAccess("not a url"))
        assertFalse(ServerAddress.requiresLocalNetworkAccess(""))
    }
}
