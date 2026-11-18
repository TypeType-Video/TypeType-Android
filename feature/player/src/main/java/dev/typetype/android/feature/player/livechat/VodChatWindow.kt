package dev.typetype.android.feature.player.livechat

import dev.typetype.android.domain.twitch.TwitchPage
import dev.typetype.android.domain.twitch.TwitchVideoComment

internal sealed interface VodChatRequest {
    data class From(val seconds: Long) : VodChatRequest

    data class Next(val cursor: String) : VodChatRequest
}

// The web's useTwitchVodChat: one page from where the viewer is, the next page a little before it runs out, and a
// fresh page from the new position after a seek outside what is loaded.
internal class VodChatWindow {
    private var comments: List<TwitchVideoComment> = emptyList()
    private var cursor: String? = null
    private var from = 0L
    private var until = 0L
    var loaded = false
        private set

    fun request(seconds: Long): VodChatRequest? {
        if (!loaded || seconds < from - 1 || seconds > until + SEEK_GAP_SECONDS) return VodChatRequest.From(seconds)
        val next = cursor ?: return null
        return if (seconds > until - PREFETCH_SECONDS) VodChatRequest.Next(next) else null
    }

    fun accept(request: VodChatRequest, page: TwitchPage<TwitchVideoComment>, seconds: Long) {
        val last = page.items.lastOrNull()?.offsetSeconds
        if (request is VodChatRequest.From || !loaded) {
            comments = page.items
            from = seconds
            until = last ?: seconds
        } else {
            val known = comments.mapTo(HashSet()) { it.id }
            val merged = comments + page.items.filterNot { it.id in known }
            comments = merged.filter { it.offsetSeconds <= seconds }.takeLast(VISIBLE) + merged.filter { it.offsetSeconds > seconds }
            from = comments.firstOrNull()?.offsetSeconds ?: from
            until = last ?: until
        }
        cursor = page.cursor
        loaded = true
    }

    fun visible(seconds: Long): List<TwitchVideoComment> = comments.filter { it.offsetSeconds <= seconds }.takeLast(VISIBLE)

    private companion object {
        const val VISIBLE = 150
        const val PREFETCH_SECONDS = 20L
        const val SEEK_GAP_SECONDS = 60L
    }
}
