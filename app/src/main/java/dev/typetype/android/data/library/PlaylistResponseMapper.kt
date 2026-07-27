package dev.typetype.android.data.library

import dev.typetype.android.data.account.AccountScope
import dev.typetype.android.data.library.local.PlaylistEntity
import dev.typetype.android.data.library.local.PlaylistVideoEntity
import dev.typetype.android.data.network.dto.PlaylistDto

internal fun PlaylistDto.toPlaylistEntity(scope: AccountScope) = PlaylistEntity(
    cacheKey = PlaylistEntity.cacheKey(scope, id),
    serverId = scope.serverId,
    accountId = scope.accountId,
    id = id,
    name = name,
    description = description,
    createdAtMillis = createdAt,
    videoCount = maxOf(videoCount, videos.size),
)

internal fun PlaylistDto.toVideoEntities(scope: AccountScope): List<PlaylistVideoEntity> =
    videos.distinctBy { it.url }.map { video ->
        PlaylistVideoEntity(
            playlistCacheKey = PlaylistEntity.cacheKey(scope, id),
            playlistId = id,
            id = video.id,
            url = video.url,
            title = video.title,
            thumbnailUrl = video.thumbnail,
            durationSeconds = video.duration,
            position = video.position,
            channelName = video.channelName,
            channelUrl = video.channelUrl,
            channelAvatarUrl = video.channelAvatar,
            viewCount = video.viewCount,
        )
    }
