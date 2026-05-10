package dev.typetype.android.domain.actions

import kotlinx.coroutines.flow.Flow

data class BlockedItem(
    val url: String,
    val name: String,
    val thumbnailUrl: String,
    val blockedAt: Long,
)

interface VideoActionsRepository {
    fun observeBlockedVideoUrls(): Flow<Set<String>>
    fun observeBlockedChannelUrls(): Flow<Set<String>>

    fun observeBlockedVideos(): Flow<List<BlockedItem>>
    fun observeBlockedChannels(): Flow<List<BlockedItem>>

    suspend fun refreshBlocked(): Result<Unit>

    suspend fun blockVideo(videoUrl: String): Result<Unit>
    suspend fun unblockVideo(videoUrl: String): Result<Unit>
    suspend fun blockChannel(
        channelUrl: String,
        channelName: String? = null,
        avatarUrl: String? = null,
    ): Result<Unit>
    suspend fun unblockChannel(channelUrl: String): Result<Unit>
}
