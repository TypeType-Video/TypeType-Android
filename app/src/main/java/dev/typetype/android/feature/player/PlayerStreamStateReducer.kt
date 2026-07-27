package dev.typetype.android.feature.player

import dev.typetype.android.feature.player.host.PlayerHostStateSnapshot
import dev.typetype.android.feature.player.error.classifyStreamError
import dev.typetype.android.domain.stream.Stream

internal fun PlayerState.applyStreamUpdate(
    update: PlayerStreamUpdate,
    hostState: PlayerHostStateSnapshot,
): PlayerState = when (update) {
    is PlayerStreamUpdate.PlaybackReady -> copy(
        isLoading = false,
        stream = update.loaded.stream,
        resumeAtMillis = hostState.resumePositionMillis ?: update.loaded.resumeAtMillis,
        initialPlayWhenReady = hostState.initialPlayWhenReady,
    )
    is PlayerStreamUpdate.MetadataEnriched -> copy(
        stream = stream?.withMetadataFrom(update.stream) ?: update.stream,
    )
    is PlayerStreamUpdate.Failed -> copy(
        isLoading = false,
        error = classifyStreamError(update.failure),
    )
}
