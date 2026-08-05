package dev.typetype.android.feature.settings.content

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.R
import dev.typetype.android.core.ui.error.UserErrorMapper
import dev.typetype.android.domain.usersettings.UserSettings
import dev.typetype.android.domain.usersettings.UserSettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ContentSettingsViewModel @Inject constructor(
    private val repository: UserSettingsRepository,
    private val errorMapper: UserErrorMapper,
) : ViewModel() {
    private val feedback = MutableStateFlow(ContentSettingsFeedback())

    val state: StateFlow<ContentSettingsState> = combine(
        repository.observe(),
        feedback,
        UserSettings::toContentSettingsState,
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ContentSettingsState(),
    )

    init {
        refresh()
    }

    fun onAction(action: ContentSettingsAction) {
        when (action) {
            ContentSettingsAction.Retry -> refresh()
            ContentSettingsAction.DismissFailure -> feedback.update {
                it.copy(errorMessage = null, errorRequestId = null)
            }
            is ContentSettingsAction.Update -> update(action)
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            feedback.update { it.copy(isLoading = true, errorMessage = null, errorRequestId = null) }
            repository.refresh().fold(
                onSuccess = { feedback.update { it.copy(isLoading = false) } },
                onFailure = { failure -> showFailure(failure, isLoading = false) },
            )
        }
    }

    private fun update(action: ContentSettingsAction.Update) {
        if (feedback.value.isUpdating) return
        viewModelScope.launch {
            feedback.update { it.copy(isUpdating = true, errorMessage = null, errorRequestId = null) }
            repository.update { it.updatedBy(action) }.fold(
                onSuccess = { feedback.update { it.copy(isUpdating = false) } },
                onFailure = { failure -> showFailure(failure, isUpdating = false) },
            )
        }
    }

    private fun showFailure(
        failure: Throwable,
        isLoading: Boolean = feedback.value.isLoading,
        isUpdating: Boolean = feedback.value.isUpdating,
    ) {
        val details = errorMapper.details(failure, R.string.settings_content_update_failed)
        feedback.update {
            it.copy(
                isLoading = isLoading,
                isUpdating = isUpdating,
                errorMessage = details.message,
                errorRequestId = details.requestId,
            )
        }
    }
}

private data class ContentSettingsFeedback(
    val isLoading: Boolean = true,
    val isUpdating: Boolean = false,
    val errorMessage: String? = null,
    val errorRequestId: String? = null,
)

private fun UserSettings.toContentSettingsState(feedback: ContentSettingsFeedback) =
    ContentSettingsState(
        isLoading = feedback.isLoading,
        isUpdating = feedback.isUpdating,
        hideHomeRecommendations = hideHomeRecommendations,
        hideContinueWatching = hideContinueWatching,
        hideRelatedVideos = hideRelatedVideos,
        hideComments = hideComments,
        hideShorts = hideShorts,
        deArrowEnabled = deArrowEnabled,
        deArrowTitleMode = deArrowTitleMode,
        deArrowThumbnailMode = deArrowThumbnailMode,
        deArrowTrustMode = deArrowTrustMode,
        errorMessage = feedback.errorMessage,
        errorRequestId = feedback.errorRequestId,
    )
