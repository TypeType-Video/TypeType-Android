package dev.typetype.android.data.library

import dev.typetype.android.data.library.local.VideoMetaDao
import dev.typetype.android.data.library.local.VideoMetaEntity
import dev.typetype.android.domain.library.VideoMeta
import dev.typetype.android.domain.library.VideoMetaRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@Singleton
class RoomVideoMetaRepository @Inject constructor(
    private val dao: VideoMetaDao,
) : VideoMetaRepository {

    override fun observeForUrls(urls: List<String>): Flow<Map<String, VideoMeta>> {
        if (urls.isEmpty()) return flowOf(emptyMap())
        return dao.observeForUrls(urls).map { rows ->
            rows.associate { row ->
                row.videoUrl to VideoMeta(
                    videoUrl = row.videoUrl,
                    channelName = row.channelName,
                    channelUrl = row.channelUrl,
                    channelAvatarUrl = row.channelAvatarUrl,
                    viewCount = row.viewCount,
                )
            }
        }
    }

    override suspend fun put(meta: VideoMeta) {
        dao.upsertAll(listOf(meta.toEntity()))
    }

    override suspend fun putAll(metas: List<VideoMeta>) {
        if (metas.isEmpty()) return
        dao.upsertAll(metas.map { it.toEntity() })
    }

    private fun VideoMeta.toEntity(): VideoMetaEntity = VideoMetaEntity(
        videoUrl = videoUrl,
        channelName = channelName,
        channelUrl = channelUrl,
        channelAvatarUrl = channelAvatarUrl,
        viewCount = viewCount,
        updatedAtMillis = System.currentTimeMillis(),
    )
}
