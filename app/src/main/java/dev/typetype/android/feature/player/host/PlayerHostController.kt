package dev.typetype.android.feature.player.host

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class PlayerHostTarget { Hidden, Mini, Expanded }

data class PlayerHostStateSnapshot(
    val videoUrl: String? = null,
    val target: PlayerHostTarget = PlayerHostTarget.Hidden,
    val requestStamp: Long = 0L,
)

/**
 * Singleton state holder for the floating player overlay.
 *
 * Replaces the old PlayerRoute navigation: callers invoke openVideo(url) and the
 * PlayerHost composable observes [state] to expand/collapse/hide the player.
 *
 * The PlayerHost composable owns the AnchoredDraggable; it animates to match
 * [PlayerHostStateSnapshot.target] every time [requestStamp] advances. The user
 * may also drag the player to update [target] back via [onAnchorSettled].
 */
@Singleton
class PlayerHostController @Inject constructor() {

    private val _state = MutableStateFlow(PlayerHostStateSnapshot())
    val state: StateFlow<PlayerHostStateSnapshot> = _state.asStateFlow()

    fun openVideo(url: String) {
        _state.update {
            it.copy(
                videoUrl = url,
                target = PlayerHostTarget.Expanded,
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
                requestStamp = it.requestStamp + 1,
            )
        }
    }

    /** Called by the host after the user finishes a drag and the anchor settles. */
    fun onAnchorSettled(target: PlayerHostTarget) {
        _state.update { current ->
            val nextVideoUrl = if (target == PlayerHostTarget.Hidden) null else current.videoUrl
            current.copy(videoUrl = nextVideoUrl, target = target)
        }
    }
}
