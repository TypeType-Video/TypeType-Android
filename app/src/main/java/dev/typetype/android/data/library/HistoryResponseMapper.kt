package dev.typetype.android.data.library

import dev.typetype.android.data.account.AccountScope
import dev.typetype.android.data.library.local.HistoryEntity
import dev.typetype.android.data.network.dto.HistoryItemDto

internal data class HistoryPage(
    val rows: List<HistoryEntity>,
    val offset: Int,
    val receivedCount: Int,
    val totalCount: Int,
) {
    val nextOffset: Int = minOf(totalCount, offset + receivedCount)
    val hasMore: Boolean = receivedCount > 0 && nextOffset < totalCount
}

internal fun HistoryItemDto.toPostedHistoryEntity(scope: AccountScope): HistoryEntity? =
    takeIf { it.id.isNotBlank() }?.toHistoryEntity(scope)

internal fun HistoryItemDto.toHistoryEntity(scope: AccountScope) = HistoryEntity(
    serverId = scope.serverId,
    accountId = scope.accountId,
    id = id.ifBlank { url },
    url = url,
    title = title,
    thumbnailUrl = thumbnail,
    channelName = channelName,
    channelUrl = channelUrl,
    channelAvatarUrl = channelAvatar,
    durationSeconds = duration,
    progressSeconds = progress,
    watchedAtMillis = watchedAt,
)
