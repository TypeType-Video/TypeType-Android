package dev.typetype.android.core.ui.share

import androidx.compose.runtime.compositionLocalOf
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

val LocalServerBaseUrl = compositionLocalOf<String?> { null }

fun buildShareUrl(serverBaseUrl: String?, videoUrl: String): String {
    if (serverBaseUrl.isNullOrBlank()) return videoUrl
    val origin = serverBaseUrl
        .trimEnd('/')
        .removeSuffix("/api")
        .trimEnd('/')
    if (origin.isBlank()) return videoUrl
    val encoded = URLEncoder.encode(videoUrl, StandardCharsets.UTF_8.toString())
    return "$origin/watch?v=$encoded"
}
