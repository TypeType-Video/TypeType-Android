package dev.typetype.android.feature.setup.resetpassword

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.core.error.CodedFailure
import dev.typetype.android.core.ui.navigation.ResetPasswordRoute
import dev.typetype.android.domain.auth.AuthRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ResetPasswordViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val serverId = savedStateHandle.toRoute<ResetPasswordRoute>().serverId
    private val _state = MutableStateFlow(ResetPasswordState())
    val state = _state.asStateFlow()

    fun setResetToken(value: String) {
        _state.update {
            it.copy(resetToken = value, isComplete = false, errorKey = null, errorRequestId = null)
        }
    }

    fun setNewPassword(value: String) {
        _state.update {
            it.copy(
                newPassword = value,
                isComplete = false,
                errorKey = null,
                errorRequestId = null,
            )
        }
    }

    fun submit() {
        val current = _state.value
        if (!current.canSubmit) return
        _state.update {
            it.copy(isSubmitting = true, isComplete = false, errorKey = null, errorRequestId = null)
        }
        viewModelScope.launch {
            authRepository.resetPassword(
                serverId = serverId,
                resetToken = current.resetToken.trim(),
                newPassword = current.newPassword,
            ).onSuccess {
                _state.value = ResetPasswordState(isComplete = true)
            }.onFailure { error ->
                val coded = error as? CodedFailure
                _state.update {
                    it.copy(
                        isSubmitting = false,
                        errorKey = coded?.failureCode ?: "RESET_PASSWORD_ERROR",
                        errorRequestId = coded?.requestId,
                    )
                }
            }
        }
    }
}
