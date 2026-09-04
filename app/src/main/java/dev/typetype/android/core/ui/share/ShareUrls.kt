package dev.typetype.android.core.ui.share

import androidx.compose.runtime.compositionLocalOf
import dev.typetype.android.domain.navigation.resolveIncomingVideoUrl
import dev.typetype.android.domain.navigation.toPublicWatchParameter
import java.net.URI
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
    val encoded = URLEncoder.encode(toPublicWatchParameter(videoUrl), StandardCharsets.UTF_8.toString())
    return "$origin/watch?v=$encoded"
}

fun buildSourceShareUrl(videoUrl: String): String =
    resolveIncomingVideoUrl(videoUrl) ?: videoUrl.trim()

internal enum class ShareTarget {
    TypeType,
    Source,
}

internal data class ShareChoice(
    val target: ShareTarget,
    val url: String,
    val providerName: String? = null,
)

internal fun buildShareChoices(
    serverBaseUrl: String?,
    videoUrl: String,
): List<ShareChoice> {
    val sourceUrl = buildSourceShareUrl(videoUrl)
    val typeTypeUrl = buildShareUrl(serverBaseUrl, videoUrl)
    return buildList {
        add(ShareChoice(target = ShareTarget.TypeType, url = typeTypeUrl))
        sourceProvider(sourceUrl)?.takeIf { sourceUrl != typeTypeUrl }?.let { provider ->
            add(
                ShareChoice(
                    target = ShareTarget.Source,
                    url = sourceUrl,
                    providerName = provider,
                ),
            )
        }
    }
}

private fun sourceProvider(sourceUrl: String): String? {
    val provider = sourceUrl.lowercase()
    return when {
        "youtube.com" in provider || "youtu.be" in provider -> "YouTube"
        "nicovideo.jp" in provider || "nico.ms" in provider -> "NicoNico"
        "bilibili.com" in provider || "b23.tv" in provider -> "BiliBili"
        else -> null
    }
}

fun buildImageUrl(serverBaseUrl: String?, imageUrl: String): String {
    val source = imageUrl.trim().let { value ->
        if (value.startsWith("httpss://")) "https://${value.removePrefix("httpss://")}" else value
    }
    val base = serverBaseUrl?.trim()?.trimEnd('/')
    if (source.isBlank() || base.isNullOrBlank()) return source

    val origin = base.removeSuffix("/api")
    if (source.startsWith('/')) return "$origin$source"
    if (source.startsWith("$origin/")) return source
    if (!source.startsWith("http://") && !source.startsWith("https://")) return source
    if (!needsImageProxy(source)) return source

    val encoded = URLEncoder.encode(source, StandardCharsets.UTF_8.toString())
    return "$base/proxy?url=$encoded"
}

private fun needsImageProxy(source: String): Boolean {
    val host = runCatching { URI(source).host?.lowercase() }.getOrNull() ?: return false
    return host.endsWith("ggpht.com") ||
        host.endsWith("googleusercontent.com") ||
        host.endsWith("hdslb.com") ||
        host.endsWith("ytimg.com")
}
