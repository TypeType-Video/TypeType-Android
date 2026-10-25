package dev.typetype.android.feature.settings.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.core.error.CodedFailure
import dev.typetype.android.domain.profile.ProfileRepository
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class ProfileSettingsViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val avatarSelectionReader: AvatarSelectionReader,
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileSettingsState())
    val state = _state.asStateFlow()

    private val _events = Channel<ProfileSettingsEvent>(Channel.BUFFERED)
    val events: Flow<ProfileSettingsEvent> = _events.receiveAsFlow()
    private var identityAccountId: String? = null

    init {
        viewModelScope.launch {
            profileRepository.observe().collect { profile ->
                val accountChanged = profile?.id != _state.value.profile?.id
                if (accountChanged) identityAccountId = null
                _state.update { current ->
                    current.copy(
                        isLoading = profile == null && current.profile == null,
                        profile = profile,
                        usernameDraft = if (accountChanged) profile?.publicUsername.orEmpty() else current.usernameDraft,
                        bioDraft = if (accountChanged) profile?.bio.orEmpty() else current.bioDraft,
                        identity = if (accountChanged) null else current.identity,
                        emailDraft = if (accountChanged) "" else current.emailDraft,
                        nameDraft = if (accountChanged) "" else current.nameDraft,
                        currentPasswordDraft = if (accountChanged) "" else current.currentPasswordDraft,
                    )
                }
                if (profile != null && !profile.id.startsWith("guest:") &&
                    identityAccountId != profile.id
                ) {
                    identityAccountId = profile.id
                    loadIdentity(profile.id)
                }
            }
        }
        viewModelScope.launch { profileRepository.refresh().onFailure(::showProfileFailure) }
    }

    fun setUsernameDraft(value: String) {
        _state.update {
            it.copy(
                usernameDraft = value,
                profileErrorKey = null,
                profileErrorRequestId = null,
            )
        }
    }

    fun setBioDraft(value: String) {
        _state.update {
            it.copy(bioDraft = value, profileErrorKey = null, profileErrorRequestId = null)
        }
    }

    fun save() {
        val current = _state.value
        if (current.isSaving || current.isGuest) return
        val username = current.usernameDraft.trim().takeIf { it.isNotEmpty() }
        val bio = current.bioDraft
        viewModelScope.launch {
            _state.update {
                it.copy(isSaving = true, profileErrorKey = null, profileErrorRequestId = null)
            }
            profileRepository.updateProfile(publicUsername = username, bio = bio)
                .onSuccess {
                    _state.update { it.copy(isSaving = false) }
                    _events.send(ProfileSettingsEvent.ProfileSaved)
                }
                .onFailure { error ->
                    showProfileFailure(error)
                    _state.update { it.copy(isSaving = false) }
                }
        }
    }

    fun retryProfile() {
        if (_state.value.isLoading) return
        _state.update {
            it.copy(isLoading = true, profileErrorKey = null, profileErrorRequestId = null)
        }
        viewModelScope.launch { profileRepository.refresh().onFailure(::showProfileFailure) }
    }

    fun clearAvatar() {
        updateAvatar { profileRepository.clearAvatar() }
    }

    fun setAvatarEmoji(code: String) {
        updateAvatar { profileRepository.setAvatarEmoji(code) }
    }

    fun uploadAvatar(uri: Uri) {
        updateAvatar {
            val upload = withContext(Dispatchers.IO) { avatarSelectionReader.read(uri) }
            profileRepository.uploadCustomAvatar(upload)
        }
    }

    fun setEmailDraft(value: String) {
        _state.update {
            it.copy(emailDraft = value, identityErrorKey = null, identityErrorRequestId = null)
        }
    }

    fun setNameDraft(value: String) {
        _state.update {
            it.copy(nameDraft = value, identityErrorKey = null, identityErrorRequestId = null)
        }
    }

    fun setCurrentPasswordDraft(value: String) {
        _state.update {
            it.copy(
                currentPasswordDraft = value,
                identityErrorKey = null,
                identityErrorRequestId = null,
            )
        }
    }

    fun saveIdentity() {
        val current = _state.value
        if (!current.canSaveIdentity) return
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isIdentitySaving = true,
                    identityErrorKey = null,
                    identityErrorRequestId = null,
                )
            }
            profileRepository.updateAccountIdentity(
                email = current.emailDraft.trim(),
                name = current.nameDraft.trim(),
                currentPassword = current.currentPasswordDraft,
            ).onSuccess {
                _state.update {
                    it.copy(
                        isIdentitySaving = false,
                        identity = it.identity?.copy(
                            email = current.emailDraft.trim(),
                            name = current.nameDraft.trim(),
                        ),
                        currentPasswordDraft = "",
                    )
                }
                _events.send(ProfileSettingsEvent.IdentitySaved)
            }.onFailure { error ->
                showIdentityFailure(error)
                _state.update { it.copy(isIdentitySaving = false) }
            }
        }
    }

    fun retryIdentity() {
        val profileId = _state.value.profile?.id ?: return
        if (profileId.startsWith("guest:") || _state.value.isIdentityLoading) return
        identityAccountId = profileId
        loadIdentity(profileId)
    }

    private fun loadIdentity(profileId: String) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isIdentityLoading = true,
                    identityErrorKey = null,
                    identityErrorRequestId = null,
                )
            }
            profileRepository.getAccountIdentity()
                .onSuccess { identity ->
                    if (_state.value.profile?.id != profileId) return@onSuccess
                    _state.update {
                        it.copy(
                            isIdentityLoading = false,
                            identity = identity,
                            emailDraft = identity.email,
                            nameDraft = identity.name,
                            currentPasswordDraft = "",
                        )
                    }
                }
                .onFailure { error ->
                    if (_state.value.profile?.id != profileId) return@onFailure
                    showIdentityFailure(error)
                    _state.update { it.copy(isIdentityLoading = false) }
                }
        }
    }

    private fun updateAvatar(action: suspend () -> Result<Unit>) {
        val current = _state.value
        if (current.isAvatarSaving || current.isGuest) return
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isAvatarSaving = true,
                    avatarErrorKey = null,
                    avatarErrorRequestId = null,
                )
            }
            action()
                .onSuccess {
                    _state.update { it.copy(isAvatarSaving = false) }
                    _events.send(ProfileSettingsEvent.AvatarSaved)
                }
                .onFailure { error ->
                    showAvatarFailure(error)
                    _state.update { it.copy(isAvatarSaving = false) }
                }
        }
    }

    private fun showProfileFailure(error: Throwable) {
        val coded = error as? CodedFailure
        _state.update {
            it.copy(
                isLoading = false,
                profileErrorKey = coded?.failureCode ?: error.message.orEmpty(),
                profileErrorRequestId = coded?.requestId,
            )
        }
    }

    private fun showAvatarFailure(error: Throwable) {
        val coded = error as? CodedFailure
        _state.update {
            it.copy(
                avatarErrorKey = coded?.failureCode ?: error.message.orEmpty(),
                avatarErrorRequestId = coded?.requestId,
            )
        }
    }

    private fun showIdentityFailure(error: Throwable) {
        val coded = error as? CodedFailure
        _state.update {
            it.copy(
                identityErrorKey = coded?.failureCode ?: error.message.orEmpty(),
                identityErrorRequestId = coded?.requestId,
            )
        }
    }
}
