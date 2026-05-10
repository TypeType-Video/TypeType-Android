package dev.typetype.android.feature.settings.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.domain.profile.Profile
import dev.typetype.android.domain.profile.ProfileRepository
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileSettingsState(
    val isLoading: Boolean = true,
    val profile: Profile? = null,
    val usernameDraft: String = "",
    val bioDraft: String = "",
    val isSaving: Boolean = false,
    val errorKey: String? = null,
)

sealed interface ProfileSettingsEvent {
    data object Saved : ProfileSettingsEvent
    data class Error(val messageKey: String) : ProfileSettingsEvent
}

@HiltViewModel
class ProfileSettingsViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileSettingsState())
    val state = _state.asStateFlow()

    private val _events = Channel<ProfileSettingsEvent>(Channel.BUFFERED)
    val events: Flow<ProfileSettingsEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            profileRepository.observe().collect { profile ->
                _state.update { current ->
                    current.copy(
                        isLoading = profile == null && current.profile == null,
                        profile = profile,
                        usernameDraft = if (current.profile == null) profile?.publicUsername.orEmpty() else current.usernameDraft,
                        bioDraft = if (current.profile == null) profile?.bio.orEmpty() else current.bioDraft,
                    )
                }
            }
        }
        viewModelScope.launch { profileRepository.refresh() }
    }

    fun setUsernameDraft(value: String) {
        _state.update { it.copy(usernameDraft = value, errorKey = null) }
    }

    fun setBioDraft(value: String) {
        _state.update { it.copy(bioDraft = value, errorKey = null) }
    }

    fun save() {
        val current = _state.value
        if (current.isSaving) return
        val username = current.usernameDraft.trim().takeIf { it.isNotEmpty() }
        val bio = current.bioDraft
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            profileRepository.updateProfile(publicUsername = username, bio = bio)
                .onSuccess {
                    _state.update { it.copy(isSaving = false) }
                    _events.send(ProfileSettingsEvent.Saved)
                }
                .onFailure { error ->
                    val key = error.message.orEmpty()
                    _state.update { it.copy(isSaving = false, errorKey = key) }
                    _events.send(ProfileSettingsEvent.Error(key))
                }
        }
    }

    fun clearAvatar() {
        viewModelScope.launch { profileRepository.clearAvatar() }
    }

    fun setAvatarEmoji(code: String) {
        viewModelScope.launch { profileRepository.setAvatarEmoji(code) }
    }
}
