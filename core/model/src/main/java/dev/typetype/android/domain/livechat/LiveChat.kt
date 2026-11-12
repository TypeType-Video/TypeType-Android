package dev.typetype.android.domain.livechat

import kotlinx.coroutines.flow.Flow

data class LiveChatMessage(
    val id: String,
    val text: String,
    val authorName: String?,
    val authorAvatarUrl: String?,
    val moderator: Boolean,
)

sealed interface LiveChatEvent {
    data object Connected : LiveChatEvent
    data class Message(val message: LiveChatMessage) : LiveChatEvent
    data object Ended : LiveChatEvent
}

class LiveChatRequestException(val status: Int, message: String) : Exception(message)

interface LiveChatRepository {
    fun connect(videoUrl: String): Flow<LiveChatEvent>
}
