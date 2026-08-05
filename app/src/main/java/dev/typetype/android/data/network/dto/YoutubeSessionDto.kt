package dev.typetype.android.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class YoutubeSessionStatusResponse(
    val status: String,
    val updatedAt: Long,
    val lastUsedAt: Long,
)

@Serializable
data class YoutubeRemoteBrowserStartRequest(
    val returnTo: String? = null,
)

@Serializable
data class YoutubeRemoteBrowserStartResponse(
    val sessionId: String,
    val wsUrl: String,
    val expiresAt: Long,
)
