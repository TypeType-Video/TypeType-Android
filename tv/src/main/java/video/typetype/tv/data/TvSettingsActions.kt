package video.typetype.tv.data

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import video.typetype.sdk.core.TypeTypeResult
import video.typetype.sdk.core.UserSettings

internal suspend fun TvViewModel.loadUserSettings() {
    when (val result = client.settings.get()) {
        is TypeTypeResult.Success -> {
            val services = availableTvServices(mutableState.value.metadata)
            val service = result.value.defaultService.takeIf(services::contains) ?: services.first()
            mutableState.value = mutableState.value.copy(
                settings = result.value,
                selectedService = service,
            )
        }
        is TypeTypeResult.Failure -> mutableState.value = mutableState.value.copy(
            errorMessage = result.error.toUserMessage(),
        )
    }
}

public fun TvViewModel.updateSettings(settings: UserSettings) {
    viewModelScope.launch {
        if (mutableState.value.authStatus == TvAuthStatus.AUTHENTICATED) {
            updateUserSettings(settings, refreshContent = true)
        } else {
            mutableState.value = mutableState.value.copy(settings = settings, errorMessage = null)
            loadHomeContent()
        }
    }
}

internal suspend fun TvViewModel.updateUserSettings(settings: UserSettings, refreshContent: Boolean) {
    val previous = mutableState.value.settings
    mutableState.value = mutableState.value.copy(settings = settings, errorMessage = null)
    when (val result = client.settings.update(settings)) {
        is TypeTypeResult.Success -> {
            mutableState.value = mutableState.value.copy(settings = result.value)
            if (refreshContent) loadHomeContent()
        }
        is TypeTypeResult.Failure -> mutableState.value = mutableState.value.copy(
            settings = previous,
            errorMessage = result.error.toUserMessage(),
        )
    }
}
