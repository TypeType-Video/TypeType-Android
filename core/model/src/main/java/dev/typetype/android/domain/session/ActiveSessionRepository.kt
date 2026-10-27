package dev.typetype.android.domain.session

import dev.typetype.android.domain.stream.StreamRequestScope
import kotlinx.coroutines.flow.Flow

data class ActivePlaybackSnapshot(
    val videoUrl: String,
    val title: String,
    val thumbnailUrl: String?,
    val channelName: String?,
    val positionMillis: Long,
    val durationMillis: Long?,
    val isPaused: Boolean,
)

interface ActiveSessionRepository {
    fun observeDeviceName(): Flow<String>
    suspend fun setDeviceName(name: String)
    suspend fun reportActivity(): Result<Unit>
    suspend fun reportPlaybackStart(
        requestScope: StreamRequestScope,
        snapshot: ActivePlaybackSnapshot,
    ): Result<Unit>
    suspend fun reportPlaybackProgress(
        requestScope: StreamRequestScope,
        snapshot: ActivePlaybackSnapshot,
    ): Result<Unit>
    suspend fun reportPlaybackStop(requestScope: StreamRequestScope): Result<Unit>
}
