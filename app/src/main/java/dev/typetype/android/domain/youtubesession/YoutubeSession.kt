package dev.typetype.android.domain.youtubesession

data class YoutubeSession(
    val status: YoutubeSessionStatus,
    val updatedAt: Long,
    val lastUsedAt: Long,
)

enum class YoutubeSessionStatus {
    Disconnected,
    Connected,
    NeedsReconnect,
    Unknown,
}

data class YoutubeRemoteBrowserSession(
    val sessionId: String,
    val webSocketUrl: String,
    val expiresAt: Long,
)
