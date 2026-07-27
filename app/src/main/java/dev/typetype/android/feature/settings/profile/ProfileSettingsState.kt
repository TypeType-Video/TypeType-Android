package dev.typetype.android.feature.settings.profile

import dev.typetype.android.domain.profile.AccountIdentity
import dev.typetype.android.domain.profile.Profile

data class ProfileSettingsState(
    val isLoading: Boolean = true,
    val profile: Profile? = null,
    val usernameDraft: String = "",
    val bioDraft: String = "",
    val isSaving: Boolean = false,
    val profileErrorKey: String? = null,
    val profileErrorRequestId: String? = null,
    val isAvatarSaving: Boolean = false,
    val avatarErrorKey: String? = null,
    val avatarErrorRequestId: String? = null,
    val identity: AccountIdentity? = null,
    val isIdentityLoading: Boolean = false,
    val emailDraft: String = "",
    val nameDraft: String = "",
    val currentPasswordDraft: String = "",
    val isIdentitySaving: Boolean = false,
    val identityErrorKey: String? = null,
    val identityErrorRequestId: String? = null,
) {
    val isGuest: Boolean
        get() = profile?.id?.startsWith("guest:") == true

    val canSaveIdentity: Boolean
        get() = identity != null &&
            !identity.managedByOidc &&
            !isIdentitySaving &&
            emailDraft.isNotBlank() &&
            nameDraft.isNotBlank() &&
            currentPasswordDraft.isNotBlank()
}

sealed interface ProfileSettingsEvent {
    data object ProfileSaved : ProfileSettingsEvent
    data object AvatarSaved : ProfileSettingsEvent
    data object IdentitySaved : ProfileSettingsEvent
}
