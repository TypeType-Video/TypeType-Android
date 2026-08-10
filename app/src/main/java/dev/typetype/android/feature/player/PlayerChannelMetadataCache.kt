package dev.typetype.android.feature.player

import dev.typetype.android.domain.stream.StreamRequestScope
import dev.typetype.android.domain.subscriptions.canonicalChannelUrl

internal data class PlayerChannelMetadata(
    val name: String,
    val avatarUrl: String,
    val subscriberCount: Long,
    val verified: Boolean,
)

internal class PlayerChannelMetadataCache {
    private val entries = LinkedHashMap<Key, PlayerChannelMetadata>(MAX_ENTRIES, 0.75f, true)

    @Synchronized
    fun get(scope: StreamRequestScope?, channelUrl: String): PlayerChannelMetadata? {
        val key = scope?.cacheKey(channelUrl) ?: return null
        return entries[key]
    }

    @Synchronized
    fun put(scope: StreamRequestScope?, channelUrl: String, metadata: PlayerChannelMetadata) {
        val key = scope?.cacheKey(channelUrl) ?: return
        entries[key] = metadata
        while (entries.size > MAX_ENTRIES) entries.remove(entries.keys.first())
    }

    private fun StreamRequestScope.cacheKey(channelUrl: String): Key? {
        val canonicalUrl = canonicalChannelUrl(channelUrl)
        if (canonicalUrl.isBlank()) return null
        return Key(
            serverId = serverId,
            accountId = accountId,
            baseUrl = baseUrl,
            channelUrl = canonicalUrl,
        )
    }

    private data class Key(
        val serverId: String,
        val accountId: String,
        val baseUrl: String,
        val channelUrl: String,
    )

    private companion object {
        const val MAX_ENTRIES = 32
    }
}
