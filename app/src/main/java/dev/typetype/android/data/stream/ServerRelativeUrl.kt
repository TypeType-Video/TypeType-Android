package dev.typetype.android.data.stream

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

internal fun resolveServerUrl(baseUrl: String, value: String?): String? {
    val source = value?.takeIf { it.isNotBlank() } ?: return null
    val server = baseUrl.toHttpUrlOrNull() ?: return null
    val direct = source.toHttpUrlOrNull()
    val resolved = direct ?: if (source.startsWith(server.encodedPath)) {
        server.resolve(source)
    } else {
        server.resolve(source.removePrefix("/"))
    } ?: return null
    return resolved.takeIf { it.hasSameOrigin(server) }?.toString()
}

internal fun resolveSabrPlaybackManifestUrl(
    baseUrl: String,
    value: String?,
    sessionId: String,
): String? {
    val server = baseUrl.toHttpUrlOrNull() ?: return null
    val source = value?.takeIf { it.isNotBlank() } ?: "sabr/playback/$sessionId/manifest"
    val direct = source.toHttpUrlOrNull()
    val resolved = direct ?: if (source.startsWith(server.encodedPath)) {
        server.resolve(source)
    } else {
        server.resolve(source.removePrefix("/"))
    } ?: return null
    if (!resolved.hasSameOrigin(server)) return null
    val expectedTail = listOf("sabr", "playback", sessionId, "manifest")
    return resolved.takeIf { it.pathSegments.takeLast(expectedTail.size) == expectedTail }?.toString()
}

internal fun isExpectedServerEndpoint(
    baseUrl: String,
    value: String?,
    expectedTail: List<String>,
): Boolean {
    val resolved = resolveServerUrl(baseUrl, value)?.toHttpUrlOrNull() ?: return false
    return resolved.pathSegments.takeLast(expectedTail.size) == expectedTail
}

private fun okhttp3.HttpUrl.hasSameOrigin(other: okhttp3.HttpUrl): Boolean =
    scheme == other.scheme && host == other.host && port == other.port
