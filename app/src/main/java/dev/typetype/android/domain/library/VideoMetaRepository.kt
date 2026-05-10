package dev.typetype.android.domain.library

import kotlinx.coroutines.flow.Flow

data class VideoMeta(
    val videoUrl: String,
    val channelName: String,
    val channelUrl: String,
    val channelAvatarUrl: String,
    val viewCount: Long,
)

interface VideoMetaRepository {
    fun observeForUrls(urls: List<String>): Flow<Map<String, VideoMeta>>
    suspend fun put(meta: VideoMeta)
    suspend fun putAll(metas: List<VideoMeta>)
}
