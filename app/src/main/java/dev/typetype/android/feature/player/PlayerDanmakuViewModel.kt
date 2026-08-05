package dev.typetype.android.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.domain.comments.BulletCommentsRepository
import dev.typetype.android.domain.preferences.AppPreferences
import dev.typetype.android.domain.preferences.PreferencesRepository
import dev.typetype.android.domain.usersettings.UserSettings
import dev.typetype.android.domain.usersettings.UserSettingsRepository
import dev.typetype.android.feature.player.host.PlayerHostController
import dev.typetype.android.services.PlaybackQueueCoordinator
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlayerDanmakuViewModel @Inject constructor(
    playerHostController: PlayerHostController,
    playbackQueueCoordinator: PlaybackQueueCoordinator,
    private val preferencesRepository: PreferencesRepository,
    userSettingsRepository: UserSettingsRepository,
    private val bulletCommentsRepository: BulletCommentsRepository,
) : ViewModel() {
    private val videoUrl = combine(
        playerHostController.state.map { it.videoUrl },
        playbackQueueCoordinator.state,
    ) { hostUrl, queue -> queue.current?.videoUrl ?: hostUrl }
        .distinctUntilChanged()

    private val mutableState = MutableStateFlow(PlayerDanmakuState())
    val state = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                videoUrl,
                preferencesRepository.observe(),
                userSettingsRepository.observe(),
                ::DanmakuRequest,
            ).distinctUntilChanged().collectLatest { request ->
                load(request)
            }
        }
    }

    fun onAction(action: PlayerDanmakuAction) {
        viewModelScope.launch {
            when (action) {
                is PlayerDanmakuAction.SetEnabled ->
                    preferencesRepository.setDanmakuEnabled(action.enabled)
                is PlayerDanmakuAction.SetSpeed ->
                    preferencesRepository.setDanmakuSpeed(action.speed)
                is PlayerDanmakuAction.SetSize ->
                    preferencesRepository.setDanmakuSize(action.size)
            }
        }
    }

    private suspend fun load(request: DanmakuRequest) {
        val supported = request.videoUrl?.let(::supportsServerBulletComments) == true
        val available = supported && !request.settings.hideComments
        val enabled = request.preferences.danmakuEnabled
        val baseState = PlayerDanmakuState(
            supported = supported,
            available = available,
            enabled = enabled,
            speed = request.preferences.danmakuSpeed,
            size = request.preferences.danmakuSize,
            isLoading = available && enabled,
        )
        mutableState.value = baseState
        if (!available || !enabled) return

        bulletCommentsRepository.load(requireNotNull(request.videoUrl)).fold(
            onSuccess = { comments ->
                mutableState.update { current ->
                    if (current.matches(baseState)) {
                        current.copy(isLoading = false, comments = comments)
                    } else {
                        current
                    }
                }
            },
            onFailure = {
                mutableState.update { current ->
                    if (current.matches(baseState)) {
                        current.copy(isLoading = false, loadFailed = true)
                    } else {
                        current
                    }
                }
            },
        )
    }
}

private data class DanmakuRequest(
    val videoUrl: String?,
    val preferences: AppPreferences,
    val settings: UserSettings,
)

private fun PlayerDanmakuState.matches(other: PlayerDanmakuState): Boolean =
    supported == other.supported &&
        available == other.available &&
        enabled == other.enabled &&
        speed == other.speed &&
        size == other.size
