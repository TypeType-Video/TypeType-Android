package dev.typetype.android.domain.setup

import java.net.URI

object ServerAddress {
    fun candidateBaseUrls(
        rawValue: String,
        allowLocalCleartext: Boolean = false,
    ): List<String> {
        val value = rawValue.trim().trimEnd('/')
        require(value.isNotEmpty()) { "Enter an instance address" }
        val explicitScheme = SCHEME_PATTERN.containsMatchIn(value)
        val parsed = parseUri(if (explicitScheme) value else "https://$value")
        val scheme = parsed.scheme?.lowercase()
        require(scheme == "https" || scheme == "http") { "Only HTTP and HTTPS addresses are supported" }
        require(parsed.rawUserInfo == null) { "Credentials do not belong in the instance address" }
        require(parsed.rawQuery == null && parsed.rawFragment == null) {
            "Remove the query or fragment from the instance address"
        }
        val host = parsed.host?.trim('[', ']')?.lowercase()
        require(!host.isNullOrBlank()) { "Enter a valid instance address" }
        require(scheme != "http" || isLocalHost(host) || allowLocalCleartext) {
            "Plain HTTP is allowed only for an instance on your local network"
        }
        val path = parsed.rawPath.orEmpty().trimEnd('/')
        val normalized = URI(scheme, null, host, parsed.port, path.ifEmpty { "/" }, null, null)
            .toASCIIString()
            .trimEnd('/')
        val direct = "$normalized/"
        val lastSegment = path.substringAfterLast('/', missingDelimiterValue = path)
        return if (lastSegment == "api") listOf(direct) else listOf(direct, "${normalized}/api/")
    }

    fun requiresLocalNetworkAccess(rawValue: String): Boolean {
        val host = host(rawValue) ?: return false
        return isLocalHost(host)
    }

    internal fun host(rawValue: String): String? = parseHost(rawValue)

    internal fun isAddressLiteral(host: String): Boolean =
        isIpv4Literal(host) || ':' in host

    fun usesCleartextHttp(rawValue: String): Boolean {
        val value = rawValue.trim()
        if (!SCHEME_PATTERN.containsMatchIn(value)) return false
        return runCatching { URI(value).scheme.equals("http", ignoreCase = true) }.getOrDefault(false)
    }

    private fun isLocalHost(host: String): Boolean =
        host == "localhost" ||
            host.endsWith(".localhost") ||
            host.endsWith(".local") ||
            isSingleLabelHost(host) ||
            isPrivateIpv4(host) ||
            isLocalIpv6(host)

    private fun parseHost(rawValue: String): String? {
        val value = rawValue.trim()
        if (value.isEmpty()) return null
        val candidate = if (SCHEME_PATTERN.containsMatchIn(value)) value else "https://$value"
        return runCatching { parseUri(candidate).host }
            .getOrNull()
            ?.trim('[', ']')
            ?.lowercase()
    }

    private fun isSingleLabelHost(host: String): Boolean = '.' !in host && ':' !in host

    private fun isPrivateIpv4(host: String): Boolean {
        val octets = host.split('.').mapNotNull(String::toIntOrNull)
        if (octets.size != 4 || octets.any { it !in 0..255 }) return false
        return when {
            octets[0] == 10 -> true
            octets[0] == 100 && octets[1] in 64..127 -> true
            octets[0] == 127 -> true
            octets[0] == 169 && octets[1] == 254 -> true
            octets[0] == 172 && octets[1] in 16..31 -> true
            octets[0] == 192 && octets[1] == 168 -> true
            else -> false
        }
    }

    private fun isIpv4Literal(host: String): Boolean {
        val octets = host.split('.').mapNotNull(String::toIntOrNull)
        return octets.size == 4 && octets.all { it in 0..255 }
    }

    private fun isLocalIpv6(host: String): Boolean = ':' in host && (
        host == "::1" ||
            host.startsWith("fc") ||
            host.startsWith("fd") ||
            host.startsWith("fe8") ||
            host.startsWith("fe9") ||
            host.startsWith("fea") ||
            host.startsWith("feb")
        )

    private fun parseUri(value: String): URI =
        runCatching { URI(value) }.getOrElse { throw IllegalArgumentException("Enter a valid instance address") }

    private val SCHEME_PATTERN = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://")
}
