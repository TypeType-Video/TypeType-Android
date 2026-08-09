package dev.typetype.android.data.setup

import dev.typetype.android.domain.setup.ServerAddress
import java.net.Inet6Address
import java.net.InetAddress
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class LocalNetworkTargetResolver @Inject constructor() {
    suspend fun requiresPermission(rawValue: String): Boolean = localNetworkTarget(
        rawValue = rawValue,
        resolve = { host ->
            withContext(Dispatchers.IO) { InetAddress.getAllByName(host).toList() }
        },
    )
}

internal suspend fun localNetworkTarget(
    rawValue: String,
    resolve: suspend (String) -> List<InetAddress>,
): Boolean {
    if (ServerAddress.requiresLocalNetworkAccess(rawValue)) return true
    val host = ServerAddress.host(rawValue) ?: return false
    if (ServerAddress.isAddressLiteral(host)) return false
    return runCatching { resolve(host).any(InetAddress::isLocalNetworkAddress) }
        .getOrDefault(false)
}

private fun InetAddress.isLocalNetworkAddress(): Boolean =
    isAnyLocalAddress ||
        isLoopbackAddress ||
        isLinkLocalAddress ||
        isSiteLocalAddress ||
        this is Inet6Address && address.firstOrNull()?.toInt()?.and(0xfe) == 0xfc
