package dev.typetype.android.data.library.sync

import dev.typetype.android.core.error.CodedFailure
import dev.typetype.android.data.library.LibraryNetworkSource
import dev.typetype.android.data.subscriptions.normalizeChannelUrl
import javax.inject.Inject

class LibraryMutationRemote @Inject constructor(
    private val network: LibraryNetworkSource,
) {
    suspend fun reconcile(entry: LibraryMutationEntity) {
        val kind = entry.kindOrThrow()
        if (kind == LibraryMutationKind.Subscription) {
            reconcileSubscription(entry)
            return
        }
        if (isAlreadyApplied(entry, kind)) return
        try {
            if (entry.desiredPresent) add(entry, kind) else remove(entry, kind)
        } catch (failure: Throwable) {
            if (!entry.desiredPresent && (failure as? CodedFailure)?.statusCode == 404) return
            throw failure
        }
    }

    private suspend fun reconcileSubscription(entry: LibraryMutationEntity) {
        val existing = network.fetchSubscriptions(entry.scope()).firstOrNull {
            normalizeChannelUrl(it.channelUrl) == normalizeChannelUrl(entry.targetId)
        }
        if (entry.desiredPresent) {
            if (existing != null) return
            network.postSubscription(entry.scope(), entry.targetId, entry.title, entry.thumbnailUrl)
        } else if (existing != null) {
            try {
                network.deleteSubscription(entry.scope(), existing.channelUrl)
            } catch (failure: Throwable) {
                if ((failure as? CodedFailure)?.statusCode != 404) throw failure
            }
        }
    }

    private suspend fun isAlreadyApplied(
        entry: LibraryMutationEntity,
        kind: LibraryMutationKind,
    ): Boolean {
        val present = when (kind) {
            LibraryMutationKind.Favorite -> network.fetchFavorites(entry.scope())
                .any { it.videoUrl == entry.targetId }
            LibraryMutationKind.WatchLater -> network.fetchWatchLater(entry.scope())
                .any { it.url == entry.targetId }
            LibraryMutationKind.Subscription -> error("Subscription reconciliation is handled separately")
            LibraryMutationKind.PlaylistVideo -> network.fetchPlaylist(
                entry.scope(),
                requireNotNull(entry.parentId),
            ).second.any { it.url == entry.targetId }
        }
        return present == entry.desiredPresent
    }

    private suspend fun add(entry: LibraryMutationEntity, kind: LibraryMutationKind) {
        when (kind) {
            LibraryMutationKind.Favorite -> network.postFavorite(entry.scope(), entry.targetId)
            LibraryMutationKind.WatchLater -> network.postWatchLater(
                scope = entry.scope(),
                url = entry.targetId,
                title = entry.title,
                thumbnail = entry.thumbnailUrl,
                duration = entry.durationSeconds,
                channelName = entry.channelName,
                channelUrl = entry.channelUrl,
                channelAvatar = entry.channelAvatarUrl,
                viewCount = entry.viewCount,
            )
            LibraryMutationKind.Subscription -> network.postSubscription(
                entry.scope(),
                entry.targetId,
                entry.title,
                entry.thumbnailUrl,
            )
            LibraryMutationKind.PlaylistVideo -> network.postAddVideoToPlaylist(
                scope = entry.scope(),
                playlistId = requireNotNull(entry.parentId),
                url = entry.targetId,
                title = entry.title,
                thumbnail = entry.thumbnailUrl,
                duration = entry.durationSeconds,
                channelName = entry.channelName,
                channelUrl = entry.channelUrl,
                channelAvatar = entry.channelAvatarUrl,
                viewCount = entry.viewCount,
            )
        }
    }

    private suspend fun remove(entry: LibraryMutationEntity, kind: LibraryMutationKind) {
        when (kind) {
            LibraryMutationKind.Favorite -> network.deleteFavorite(entry.scope(), entry.targetId)
            LibraryMutationKind.WatchLater -> network.deleteWatchLater(entry.scope(), entry.targetId)
            LibraryMutationKind.Subscription -> network.deleteSubscription(entry.scope(), entry.targetId)
            LibraryMutationKind.PlaylistVideo -> network.deleteVideoFromPlaylist(
                entry.scope(),
                requireNotNull(entry.parentId),
                entry.targetId,
            )
        }
    }
}

internal fun LibraryMutationEntity.kindOrThrow(): LibraryMutationKind =
    LibraryMutationKind.entries.firstOrNull { it.storageKey == kind }
        ?: error("Unsupported library mutation kind")
