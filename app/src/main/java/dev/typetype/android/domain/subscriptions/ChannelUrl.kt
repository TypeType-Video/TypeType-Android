package dev.typetype.android.domain.subscriptions

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

fun canonicalChannelUrl(value: String): String {
    val trimmed = value.trim().trimEnd('/')
    val parsed = trimmed.toHttpUrlOrNull() ?: return trimmed
    val scheme = if (parsed.scheme == "http" || parsed.scheme == "https") "https" else parsed.scheme
    val path = parsed.encodedPath.trimEnd('/').ifEmpty { "/" }
    return parsed.newBuilder()
        .scheme(scheme)
        .encodedPath(path)
        .query(null)
        .fragment(null)
        .build()
        .toString()
        .trimEnd('/')
}
