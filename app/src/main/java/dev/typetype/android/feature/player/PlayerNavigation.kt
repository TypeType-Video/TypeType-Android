package dev.typetype.android.feature.player

import dev.typetype.android.domain.playback.PlaybackQueueState

internal data class PlayerNavigationAvailability(
    val previous: Boolean,
    val next: Boolean,
)

internal data class PlayerNavigationControls(
    val availability: PlayerNavigationAvailability,
    val onPrevious: () -> Unit,
    val onNext: () -> Unit,
) {
    companion object {
        val Disabled = PlayerNavigationControls(
            availability = PlayerNavigationAvailability(previous = false, next = false),
            onPrevious = {},
            onNext = {},
        )
    }
}

internal fun playerNavigationAvailability(
    queue: PlaybackQueueState,
    relatedVideoCount: Int,
    hasPreviousHistory: Boolean,
): PlayerNavigationAvailability {
    if (queue.isActive) {
        return PlayerNavigationAvailability(
            previous = queue.previous != null,
            next = queue.next != null,
        )
    }
    return PlayerNavigationAvailability(
        previous = hasPreviousHistory,
        next = relatedVideoCount > 0,
    )
}
