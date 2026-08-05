package dev.typetype.android.feature.player

import java.net.URI

internal fun supportsServerBulletComments(videoUrl: String): Boolean {
    val host = runCatching { URI(videoUrl).host?.lowercase() }.getOrNull() ?: return false
    return host == "nicovideo.jp" || host.endsWith(".nicovideo.jp") || host == "nico.ms"
}
