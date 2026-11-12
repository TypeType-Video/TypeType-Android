package dev.typetype.android.data.livechat

import dev.typetype.android.domain.livechat.LiveChatEvent
import dev.typetype.android.domain.livechat.LiveChatMessage
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal data class LiveChatMessageDto(
    val id: String,
    val text: String,
    val receivedAtMs: Long,
    val authorName: String? = null,
    val authorAvatarUrl: String? = null,
    val moderator: Boolean = false,
)

@Serializable
internal data class LiveChatErrorDto(val error: String = "Live chat retrieval failed")

internal class LiveChatStreamException(message: String) : Exception(message)

// Mirrors the web client parser: comment lines are keep-alives, an "error" event ends the stream with a failure.
internal class LiveChatSseParser(private val json: Json) {
    private var eventName = DEFAULT_EVENT
    private val data = StringBuilder()
    private var hasData = false

    fun accept(line: String): LiveChatEvent? {
        if (line.isEmpty()) return dispatch()
        if (line.startsWith(":")) return null
        val separator = line.indexOf(':')
        val field = if (separator < 0) line else line.substring(0, separator)
        val value = if (separator < 0) "" else line.substring(separator + 1).removePrefix(" ")
        when (field) {
            "event" -> eventName = value
            "data" -> {
                if (hasData) data.append('\n')
                data.append(value)
                hasData = true
            }
        }
        return null
    }

    fun finish(): LiveChatEvent? = dispatch()

    private fun dispatch(): LiveChatEvent? {
        val name = eventName
        val payload = data.toString()
        val present = hasData
        eventName = DEFAULT_EVENT
        data.setLength(0)
        hasData = false
        if (!present) return null
        return when (name) {
            "ended" -> LiveChatEvent.Ended
            "error" -> throw LiveChatStreamException(
                runCatching { json.decodeFromString<LiveChatErrorDto>(payload).error }
                    .getOrDefault("Live chat retrieval failed"),
            )
            DEFAULT_EVENT -> runCatching { json.decodeFromString<LiveChatMessageDto>(payload) }
                .getOrNull()
                ?.let { LiveChatEvent.Message(it.toDomain()) }
            else -> null
        }
    }

    private companion object {
        const val DEFAULT_EVENT = "message"
    }
}

private fun LiveChatMessageDto.toDomain() = LiveChatMessage(
    id = id,
    text = text,
    authorName = authorName?.takeIf(String::isNotBlank),
    authorAvatarUrl = authorAvatarUrl?.takeIf(String::isNotBlank),
    moderator = moderator,
)
