package dev.typetype.android.feature.player

import dev.typetype.android.domain.playback.PlaybackQueueState
import dev.typetype.android.feature.player.host.PlayerHostController
import dev.typetype.android.services.PlaybackQueueCoordinator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

internal data class PlayerNavigationControllerState(
    val queue: PlaybackQueueState,
    val availability: PlayerNavigationAvailability,
)

internal class PlayerNavigationController(
    private val hostController: PlayerHostController,
    private val queueCoordinator: PlaybackQueueCoordinator,
) {
    private val relatedVideoCount = MutableStateFlow(0)

    val state: Flow<PlayerNavigationControllerState> = combine(
        queueCoordinator.state,
        hostController.state,
        relatedVideoCount,
    ) { queue, host, relatedCount ->
        PlayerNavigationControllerState(
            queue = queue,
            availability = playerNavigationAvailability(
                queue = queue,
                relatedVideoCount = relatedCount,
                hasPreviousHistory = host.normalPlaybackHistory.isNotEmpty(),
            ),
        )
    }.distinctUntilChanged()

    fun setRelatedVideoCount(count: Int) {
        relatedVideoCount.value = count.coerceAtLeast(0)
    }

    fun playPrevious() {
        if (queueCoordinator.state.value.isActive) {
            queueCoordinator.playPrevious()
        } else {
            hostController.goToPreviousVideo()
        }
    }

    fun playNext(relatedVideoUrl: String?) {
        if (queueCoordinator.state.value.isActive) {
            queueCoordinator.advanceToNext()
        } else {
            relatedVideoUrl?.let(hostController::continueWithVideo)
        }
    }
}
