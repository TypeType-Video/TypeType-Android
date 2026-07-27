package dev.typetype.android.feature.player.host

import javax.inject.Inject
import javax.inject.Singleton
import dev.typetype.android.domain.playback.PlaybackQueueEntry
import dev.typetype.android.domain.playback.PlaybackQueueController
import dev.typetype.android.domain.playback.PlaybackQueueSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class PlayerHostTarget { Hidden, Mini, Expanded }

data class PlayerHostStateSnapshot(
    val videoUrl: String? = null,
    val target: PlayerHostTarget = PlayerHostTarget.Hidden,
    val resumePositionMillis: Long? = null,
    val initialPlayWhenReady: Boolean = true,
    val requestStamp: Long = 0L,
)

@Singleton
class PlayerHostController @Inject constructor(
    private val playbackQueueCoordinator: PlaybackQueueController,
) {

    private val _state = MutableStateFlow(PlayerHostStateSnapshot())
    val state: StateFlow<PlayerHostStateSnapshot> = _state.asStateFlow()

    fun openVideo(url: String) {
        _state.update {
            it.copy(
                videoUrl = url,
                target = PlayerHostTarget.Expanded,
                resumePositionMillis = null,
                initialPlayWhenReady = true,
                requestStamp = it.requestStamp + 1,
            )
        }
        playbackQueueCoordinator.clear()
    }

    fun openQueue(title: String, entries: List<PlaybackQueueEntry>, shuffle: Boolean) {
        playbackQueueCoordinator.start(title, entries, shuffle)
        val firstUrl = playbackQueueCoordinator.state.value.current?.videoUrl ?: return
        _state.update {
            it.copy(
                videoUrl = firstUrl,
                target = PlayerHostTarget.Expanded,
                resumePositionMillis = null,
                initialPlayWhenReady = true,
                requestStamp = it.requestStamp + 1,
            )
        }
    }

    fun restoreVideo(url: String, positionMillis: Long) {
        require(url.isNotBlank())
        require(positionMillis >= 0L)
        _state.update {
            it.copy(
                videoUrl = url,
                target = PlayerHostTarget.Mini,
                resumePositionMillis = positionMillis,
                initialPlayWhenReady = false,
                requestStamp = it.requestStamp + 1,
            )
        }
        playbackQueueCoordinator.clear()
    }

    fun restoreQueue(snapshot: PlaybackQueueSnapshot, positionMillis: Long) {
        val currentUrl = requireNotNull(snapshot.current?.videoUrl)
        require(positionMillis >= 0L)
        playbackQueueCoordinator.restore(snapshot)
        _state.update {
            it.copy(
                videoUrl = currentUrl,
                target = PlayerHostTarget.Mini,
                resumePositionMillis = positionMillis,
                initialPlayWhenReady = false,
                requestStamp = it.requestStamp + 1,
            )
        }
    }

    fun expand() {
        _state.update {
            it.copy(target = PlayerHostTarget.Expanded, requestStamp = it.requestStamp + 1)
        }
    }

    fun minimize() {
        _state.update {
            it.copy(target = PlayerHostTarget.Mini, requestStamp = it.requestStamp + 1)
        }
    }

    fun hide() {
        _state.update {
            it.copy(
                videoUrl = null,
                target = PlayerHostTarget.Hidden,
                resumePositionMillis = null,
                initialPlayWhenReady = true,
                requestStamp = it.requestStamp + 1,
            )
        }
        playbackQueueCoordinator.clear()
    }

}
