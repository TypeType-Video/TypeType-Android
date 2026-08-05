package dev.typetype.android.core.ui.branding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.domain.branding.DeArrowItem
import dev.typetype.android.domain.branding.DeArrowRepository
import dev.typetype.android.domain.usersettings.UserSettings
import dev.typetype.android.domain.usersettings.UserSettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class DeArrowBrandingViewModel @Inject constructor(
    userSettingsRepository: UserSettingsRepository,
    private val deArrowRepository: DeArrowRepository,
) : ViewModel() {
    val settings: StateFlow<UserSettings> = userSettingsRepository.observe()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = UserSettings(),
        )

    suspend fun load(sourceUrl: String, durationSeconds: Long): Result<DeArrowItem?> =
        deArrowRepository.load(sourceUrl, durationSeconds)
}
