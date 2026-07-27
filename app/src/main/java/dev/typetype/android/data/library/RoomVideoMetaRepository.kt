package dev.typetype.android.data.library

import dev.typetype.android.data.account.AccountScope
import dev.typetype.android.data.account.ActiveAccountScope
import dev.typetype.android.data.library.local.VideoMetaDao
import dev.typetype.android.data.library.local.VideoMetaEntity
import dev.typetype.android.domain.library.VideoMeta
import dev.typetype.android.domain.library.VideoMetaRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class RoomVideoMetaRepository @Inject constructor(
    private val dao: VideoMetaDao,
    private val activeAccountScope: ActiveAccountScope,
) : VideoMetaRepository {

    override fun observeForUrls(urls: List<String>): Flow<Map<String, VideoMeta>> {
        if (urls.isEmpty()) return flowOf(emptyMap())
        return activeAccountScope.observe().flatMapLatest { scope ->
            if (scope == null) flowOf(emptyList())
            else dao.observeForUrls(scope.serverId, scope.accountId, urls)
        }.map { rows ->
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
        val scope = activeAccountScope.require()
        dao.upsertAll(listOf(meta.toEntity(scope)))
    }

    override suspend fun putAll(metas: List<VideoMeta>) {
        if (metas.isEmpty()) return
        val scope = activeAccountScope.require()
        dao.upsertAll(metas.map { it.toEntity(scope) })
    }

    private fun VideoMeta.toEntity(scope: AccountScope): VideoMetaEntity = VideoMetaEntity(
        serverId = scope.serverId,
        accountId = scope.accountId,
        videoUrl = videoUrl,
        channelName = channelName,
        channelUrl = channelUrl,
        channelAvatarUrl = channelAvatarUrl,
        viewCount = viewCount,
        updatedAtMillis = System.currentTimeMillis(),
    )
}
