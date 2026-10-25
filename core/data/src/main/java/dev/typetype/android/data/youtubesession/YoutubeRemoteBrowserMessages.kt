package dev.typetype.android.data.youtubesession

import dev.typetype.android.domain.youtubesession.KeyEvent
import dev.typetype.android.domain.youtubesession.PointerEvent
import dev.typetype.android.domain.youtubesession.YoutubeRemoteBrowserInput
import dev.typetype.android.domain.youtubesession.YoutubeRemoteBrowserPhase
import dev.typetype.android.domain.youtubesession.YoutubeRemoteBrowserState
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

internal fun parseYoutubeRemoteBrowserMessage(
    json: Json,
    value: String,
): YoutubeRemoteBrowserState? {
    val message = runCatching { json.parseToJsonElement(value).jsonObject }.getOrNull() ?: return null
    return when (message.text("type")) {
        "status" -> message.text("phase")?.toYoutubeRemoteBrowserPhase()?.let {
            YoutubeRemoteBrowserState(phase = it)
        }
        "error" -> message.text("message")?.let {
            YoutubeRemoteBrowserState(
                phase = YoutubeRemoteBrowserPhase.Error,
                errorMessage = it,
            )
        }
        else -> null
    }
}

internal fun encodeYoutubeRemoteBrowserInput(input: YoutubeRemoteBrowserInput): String =
    buildJsonObject {
        when (input) {
            is YoutubeRemoteBrowserInput.Resize -> {
                put("type", "resize")
                put("width", input.width)
                put("height", input.height)
            }
            is YoutubeRemoteBrowserInput.Pointer -> {
                put("type", "pointer")
                put("event", input.event.wireValue)
                put("x", input.x)
                put("y", input.y)
                put("button", "left")
            }
            is YoutubeRemoteBrowserInput.Wheel -> {
                put("type", "wheel")
                put("deltaX", input.deltaX)
                put("deltaY", input.deltaY)
            }
            is YoutubeRemoteBrowserInput.Key -> {
                put("type", "key")
                put("event", input.event.wireValue)
                put("key", input.key)
                put("code", input.code)
                put("modifiers", buildJsonArray {
                    input.modifiers.forEach { add(JsonPrimitive(it)) }
                })
            }
            is YoutubeRemoteBrowserInput.Text -> {
                put("type", "text")
                put("value", input.value)
            }
            YoutubeRemoteBrowserInput.Cancel -> put("type", "cancel")
        }
    }.toString()

private fun JsonObject.text(name: String): String? =
    (get(name) as? JsonPrimitive)?.content

private fun String.toYoutubeRemoteBrowserPhase(): YoutubeRemoteBrowserPhase? = when (this) {
    "idle" -> YoutubeRemoteBrowserPhase.Idle
    "connecting" -> YoutubeRemoteBrowserPhase.Connecting
    "opening" -> YoutubeRemoteBrowserPhase.Opening
    "awaiting_login" -> YoutubeRemoteBrowserPhase.AwaitingLogin
    "capturing_session" -> YoutubeRemoteBrowserPhase.CapturingSession
    "connected" -> YoutubeRemoteBrowserPhase.Connected
    "closed" -> YoutubeRemoteBrowserPhase.Closed
    "error" -> YoutubeRemoteBrowserPhase.Error
    else -> null
}

private val PointerEvent.wireValue: String
    get() = name.lowercase()

private val KeyEvent.wireValue: String
    get() = name.lowercase()
