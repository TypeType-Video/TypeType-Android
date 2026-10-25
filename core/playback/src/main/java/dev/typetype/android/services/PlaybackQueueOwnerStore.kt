package dev.typetype.android.services

import dev.typetype.android.data.account.AccountScope
import dev.typetype.android.domain.playback.PlaybackQueueSnapshot
import dev.typetype.android.domain.playback.PlaybackQueueState

internal class PlaybackQueueOwnerStore(
    private val persistence: PlaybackQueuePersistence,
) {
    var owner: AccountScope? = null
        private set

    fun restore(snapshot: PlaybackQueueSnapshot) {
        owner = AccountScope(snapshot.serverId, snapshot.accountId)
        persistence.save(snapshot)
    }

    fun replace(nextOwner: AccountScope) {
        val previousOwner = owner
        owner = nextOwner
        if (previousOwner != null && previousOwner != nextOwner) {
            clear(previousOwner, retainOwner = true)
        }
    }

    fun save(state: PlaybackQueueState) {
        val currentOwner = owner ?: return
        val current = state.takeIf(PlaybackQueueState::isActive) ?: return
        persistence.save(
            PlaybackQueueSnapshot(
                serverId = currentOwner.serverId,
                accountId = currentOwner.accountId,
                title = current.title,
                entries = current.entries,
                currentIndex = current.currentIndex,
                repeatMode = current.repeatMode,
                updatedAtMillis = System.currentTimeMillis(),
            ),
        )
    }

    fun clear(
        targetOwner: AccountScope? = owner,
        retainOwner: Boolean = false,
    ) {
        val currentOwner = targetOwner ?: return
        if (!retainOwner && owner == currentOwner) owner = null
        persistence.clear(currentOwner)
    }
}
