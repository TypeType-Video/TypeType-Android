package video.typetype.tv.data

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import video.typetype.sdk.core.ProfileUpdateRequest
import video.typetype.sdk.core.TypeTypeResult

public fun TvViewModel.updateProfile(publicUsername: String?, bio: String?) {
    runProfileAction {
        client.profile.update(
            ProfileUpdateRequest(publicUsername.normalizedProfileValue(), bio.normalizedProfileValue()),
        )
    }
}

public fun TvViewModel.setEmojiAvatar(code: String) {
    val normalized = code.trim()
    if (normalized.isEmpty()) return
    runProfileAction { client.profile.setEmojiAvatar(normalized) }
}

public fun TvViewModel.clearAvatar() {
    runProfileAction { client.profile.clearAvatar() }
}

private fun TvViewModel.runProfileAction(action: suspend () -> TypeTypeResult<Unit>) {
    if (mutableState.value.isActionInProgress) return
    viewModelScope.launch {
        mutableState.value = mutableState.value.copy(isActionInProgress = true, errorMessage = null)
        when (val result = action()) {
            is TypeTypeResult.Success -> {
                loadProfile()
                mutableState.value = mutableState.value.copy(isActionInProgress = false)
            }
            is TypeTypeResult.Failure -> mutableState.value = mutableState.value.copy(
                isActionInProgress = false,
                errorMessage = result.error.toUserMessage(),
            )
        }
    }
}

private fun String?.normalizedProfileValue(): String? = this?.trim()?.takeIf(String::isNotEmpty)
