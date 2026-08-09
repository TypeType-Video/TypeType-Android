package dev.typetype.android.data.setup

import java.net.InetAddress
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalNetworkTargetResolverTest {
    @Test
    fun publicDomainDoesNotRequestLocalNetworkPermission() = runBlocking {
        assertFalse(
            localNetworkTarget("https://video.example") {
                listOf(address(8, 8, 8, 8))
            },
        )
    }

    @Test
    fun domainResolvingToRfc1918RequestsPermission() = runBlocking {
        assertTrue(
            localNetworkTarget("https://video.example") {
                listOf(address(192, 168, 1, 20))
            },
        )
    }

    @Test
    fun mixedPublicAndPrivateAnswersRequestPermission() = runBlocking {
        assertTrue(
            localNetworkTarget("https://video.example") {
                listOf(address(8, 8, 8, 8), address(10, 0, 0, 4))
            },
        )
    }

    @Test
    fun ulaAndLinkLocalIpv6AnswersRequestPermission() = runBlocking {
        assertTrue(localNetworkTarget("https://ula.example") { listOf(ipv6(0xfd, 0x12)) })
        assertTrue(localNetworkTarget("https://link.example") { listOf(ipv6(0xfe, 0x80)) })
    }

    @Test
    fun directLocalTargetsDoNotNeedDns() = runBlocking {
        assertTrue(
            localNetworkTarget("http://[fd12:3456::1]:8080") {
                error("DNS must not run for a literal address")
            },
        )
    }

    @Test
    fun failedDnsDoesNotRequestPermissionForAnUnknownPublicName() = runBlocking {
        assertFalse(
            localNetworkTarget("https://unavailable.example") {
                error("DNS unavailable")
            },
        )
    }

    @Test
    fun targetIsResolvedAgainAfterChangingNetworks() = runBlocking {
        val privateWifi = localNetworkTarget("https://video.example") {
            listOf(address(192, 168, 1, 20))
        }
        val publicMobile = localNetworkTarget("https://video.example") {
            listOf(address(8, 8, 8, 8))
        }

        assertTrue(privateWifi)
        assertFalse(publicMobile)
    }

    private fun address(vararg bytes: Int): InetAddress =
        InetAddress.getByAddress(bytes.map { it.toByte() }.toByteArray())

    private fun ipv6(first: Int, second: Int): InetAddress = InetAddress.getByAddress(
        ByteArray(16).also {
            it[0] = first.toByte()
            it[1] = second.toByte()
            it[15] = 1
        },
    )
}
