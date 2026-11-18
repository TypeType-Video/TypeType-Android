package dev.typetype.android.data.livechat

import dev.typetype.android.data.network.dto.TwitchChatEventDto
import dev.typetype.android.domain.livechat.LiveChatEvent
import dev.typetype.android.domain.livechat.LiveChatMessage
import kotlinx.serialization.json.Json

// Live relays the Twitch chat as server-sent events whose data is one chat event; the event name is not used.
internal class TwitchChatSseParser(private val json: Json) {
    private val data = StringBuilder()

    fun accept(line: String): LiveChatEvent? {
        if (line.isEmpty()) return dispatch()
        if (line.startsWith("data:")) {
            if (data.isNotEmpty()) data.append('\n')
            data.append(line.removePrefix("data:").removePrefix(" "))
        }
        return null
    }

    private fun dispatch(): LiveChatEvent? {
        val payload = data.toString()
        data.setLength(0)
        if (payload.isEmpty()) return null
        val event = runCatching { json.decodeFromString(TwitchChatEventDto.serializer(), payload) }.getOrNull() ?: return null
        return when (event.type) {
            "message" -> event.text?.takeIf { it.isNotBlank() && !event.deleted }?.let { text ->
                LiveChatEvent.Message(
                    LiveChatMessage(
                        id = event.id.orEmpty(),
                        text = text,
                        authorName = event.displayName?.ifBlank { null } ?: event.login,
                        authorAvatarUrl = null,
                        moderator = event.badges.any { it.startsWith("moderator/") || it.startsWith("broadcaster/") },
                    ),
                )
            }
            "delete" -> event.targetId?.let(LiveChatEvent::Removed)
            "clear" -> LiveChatEvent.Cleared(event.login)
            else -> null
        }
    }
}
