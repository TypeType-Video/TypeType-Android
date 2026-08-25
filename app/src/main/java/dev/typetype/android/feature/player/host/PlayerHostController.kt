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

enum class PlayerHostTarget { Hidden, Mini, Expanded, Embedded }

data class PlayerHostStateSnapshot(
    val videoUrl: String? = null,
    val target: PlayerHostTarget = PlayerHostTarget.Hidden,
    val expandedReturnTarget: PlayerHostTarget = PlayerHostTarget.Mini,
    val resumePositionMillis: Long? = null,
    val initialPlayWhenReady: Boolean = true,
    val requestStamp: Long = 0L,
    val playbackClearRequestStamp: Long? = null,
    val embeddedReturnTarget: PlayerHostTarget = PlayerHostTarget.Hidden,
    val embeddedReturnVideoUrl: String? = null,
    val embeddedReturnPositionMillis: Long? = null,
    val embeddedReturnPlayWhenReady: Boolean = false,
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
                expandedReturnTarget = PlayerHostTarget.Mini,
                resumePositionMillis = null,
                initialPlayWhenReady = true,
                requestStamp = it.requestStamp + 1,
                playbackClearRequestStamp = null,
                embeddedReturnTarget = PlayerHostTarget.Hidden,
                embeddedReturnVideoUrl = null,
                embeddedReturnPositionMillis = null,
                embeddedReturnPlayWhenReady = false,
            )
        }
        playbackQueueCoordinator.clear()
    }

    fun continueWithVideo(url: String) {
        _state.update {
            it.copy(
                videoUrl = url,
                resumePositionMillis = null,
                initialPlayWhenReady = true,
                requestStamp = it.requestStamp + 1,
                playbackClearRequestStamp = null,
                embeddedReturnTarget = PlayerHostTarget.Hidden,
                embeddedReturnVideoUrl = null,
                embeddedReturnPositionMillis = null,
                embeddedReturnPlayWhenReady = false,
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
                expandedReturnTarget = PlayerHostTarget.Mini,
                resumePositionMillis = null,
                initialPlayWhenReady = true,
                requestStamp = it.requestStamp + 1,
                playbackClearRequestStamp = null,
                embeddedReturnTarget = PlayerHostTarget.Hidden,
                embeddedReturnVideoUrl = null,
                embeddedReturnPositionMillis = null,
                embeddedReturnPlayWhenReady = false,
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
                expandedReturnTarget = PlayerHostTarget.Mini,
                resumePositionMillis = positionMillis,
                initialPlayWhenReady = false,
                requestStamp = it.requestStamp + 1,
                playbackClearRequestStamp = null,
                embeddedReturnTarget = PlayerHostTarget.Hidden,
                embeddedReturnVideoUrl = null,
                embeddedReturnPositionMillis = null,
                embeddedReturnPlayWhenReady = false,
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
                expandedReturnTarget = PlayerHostTarget.Mini,
                resumePositionMillis = positionMillis,
                initialPlayWhenReady = false,
                requestStamp = it.requestStamp + 1,
                playbackClearRequestStamp = null,
                embeddedReturnTarget = PlayerHostTarget.Hidden,
                embeddedReturnVideoUrl = null,
                embeddedReturnPositionMillis = null,
                embeddedReturnPlayWhenReady = false,
            )
        }
    }

    fun openEmbeddedVideo(
        url: String,
        autoplay: Boolean,
        returnPositionMillis: Long? = null,
        returnPlayWhenReady: Boolean = false,
    ) {
        require(url.isNotBlank())
        val current = _state.value
        if (
            current.videoUrl == url &&
            current.target == PlayerHostTarget.Embedded &&
            current.initialPlayWhenReady == autoplay
        ) return
        _state.update {
            val returnTarget = when (it.target) {
                PlayerHostTarget.Mini, PlayerHostTarget.Expanded -> it.target
                else -> PlayerHostTarget.Hidden
            }
            it.copy(
                videoUrl = url,
                target = PlayerHostTarget.Embedded,
                expandedReturnTarget = PlayerHostTarget.Embedded,
                resumePositionMillis = null,
                initialPlayWhenReady = autoplay,
                requestStamp = it.requestStamp + 1,
                playbackClearRequestStamp = null,
                embeddedReturnTarget = if (it.target == PlayerHostTarget.Embedded) {
                    it.embeddedReturnTarget
                } else {
                    returnTarget
                },
                embeddedReturnVideoUrl = if (it.target == PlayerHostTarget.Embedded) {
                    it.embeddedReturnVideoUrl
                } else {
                    it.videoUrl
                },
                embeddedReturnPositionMillis = if (it.target == PlayerHostTarget.Embedded) {
                    it.embeddedReturnPositionMillis
                } else {
                    returnPositionMillis
                },
                embeddedReturnPlayWhenReady = if (it.target == PlayerHostTarget.Embedded) {
                    it.embeddedReturnPlayWhenReady
                } else {
                    returnPlayWhenReady
                },
            )
        }
    }

    fun closeEmbeddedPlayback() {
        _state.update { current ->
            if (current.target != PlayerHostTarget.Embedded) {
                current
            } else if (current.embeddedReturnTarget == PlayerHostTarget.Hidden) {
                val requestStamp = current.requestStamp + 1
                current.copy(
                    videoUrl = null,
                    target = PlayerHostTarget.Hidden,
                    expandedReturnTarget = PlayerHostTarget.Mini,
                    resumePositionMillis = null,
                    initialPlayWhenReady = true,
                    requestStamp = requestStamp,
                    playbackClearRequestStamp = requestStamp,
                    embeddedReturnTarget = PlayerHostTarget.Hidden,
                    embeddedReturnVideoUrl = null,
                    embeddedReturnPositionMillis = null,
                    embeddedReturnPlayWhenReady = false,
                )
            } else {
                current.copy(
                    videoUrl = current.embeddedReturnVideoUrl,
                    target = current.embeddedReturnTarget,
                    expandedReturnTarget = PlayerHostTarget.Mini,
                    resumePositionMillis = current.embeddedReturnPositionMillis,
                    initialPlayWhenReady = current.embeddedReturnPlayWhenReady,
                    requestStamp = current.requestStamp + 1,
                    embeddedReturnTarget = PlayerHostTarget.Hidden,
                    embeddedReturnVideoUrl = null,
                    embeddedReturnPositionMillis = null,
                    embeddedReturnPlayWhenReady = false,
                )
            }
        }
        if (_state.value.target == PlayerHostTarget.Hidden) {
            playbackQueueCoordinator.clear()
        }
    }

    fun expand() {
        _state.update {
            it.copy(
                target = PlayerHostTarget.Expanded,
                expandedReturnTarget = when (it.target) {
                    PlayerHostTarget.Embedded -> PlayerHostTarget.Embedded
                    PlayerHostTarget.Mini -> PlayerHostTarget.Mini
                    else -> it.expandedReturnTarget
                },
                requestStamp = it.requestStamp + 1,
            )
        }
    }

    fun collapseExpanded() {
        _state.update {
            if (it.target != PlayerHostTarget.Expanded) it else it.copy(
                target = it.expandedReturnTarget,
                requestStamp = it.requestStamp + 1,
            )
        }
    }

    fun minimize() {
        _state.update {
            it.copy(target = PlayerHostTarget.Mini, requestStamp = it.requestStamp + 1)
        }
    }

    fun hide() {
        _state.update {
            val requestStamp = it.requestStamp + 1
            it.copy(
                videoUrl = null,
                target = PlayerHostTarget.Hidden,
                expandedReturnTarget = PlayerHostTarget.Mini,
                resumePositionMillis = null,
                initialPlayWhenReady = true,
                requestStamp = requestStamp,
                playbackClearRequestStamp = requestStamp,
                embeddedReturnTarget = PlayerHostTarget.Hidden,
                embeddedReturnVideoUrl = null,
                embeddedReturnPositionMillis = null,
                embeddedReturnPlayWhenReady = false,
            )
        }
        playbackQueueCoordinator.clear()
    }

    fun acknowledgePlaybackClear(requestStamp: Long) {
        _state.update {
            if (it.playbackClearRequestStamp == requestStamp) {
                it.copy(playbackClearRequestStamp = null)
            } else {
                it
            }
        }
    }

}
