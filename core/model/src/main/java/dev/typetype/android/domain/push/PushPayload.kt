package dev.typetype.android.domain.push

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class PushPayload(
    val version: Int = 1,
    val eventType: String,
    val serviceId: Int,
    val serviceName: String,
    val eventId: String,
    val videoId: String,
    val videoUrl: String,
    val channelId: String,
    val channelName: String,
    val channelAvatarUrl: String,
    val instanceId: String,
    val accountId: String,
    val publishedAt: Long,
    val title: String,
)

fun parsePushPayload(content: ByteArray): PushPayload? = runCatching {
    payloadJson.decodeFromString(PushPayload.serializer(), content.decodeToString())
}.getOrNull()

private val payloadJson = Json {
    ignoreUnknownKeys = true
}
