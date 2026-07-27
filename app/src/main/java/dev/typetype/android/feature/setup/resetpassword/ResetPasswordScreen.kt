package dev.typetype.android.feature.setup.resetpassword

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.RequestIdRow
import dev.typetype.android.core.ui.components.TypeTypeCard
import dev.typetype.android.core.ui.components.TypeTypePrimaryButton
import dev.typetype.android.core.ui.components.TypeTypeSecondaryButton
import dev.typetype.android.core.ui.components.TypeTypeTextField

@Composable
fun ResetPasswordRoute(
    onNavigateBack: () -> Unit,
    viewModel: ResetPasswordViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ResetPasswordScreen(
        state = state,
        onNavigateBack = onNavigateBack,
        onResetTokenChange = viewModel::setResetToken,
        onNewPasswordChange = viewModel::setNewPassword,
        onSubmit = viewModel::submit,
    )
}

@Composable
fun ResetPasswordScreen(
    state: ResetPasswordState,
    onNavigateBack: () -> Unit,
    onResetTokenChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.settings_back),
                )
            }
            Text(
                text = stringResource(R.string.reset_password_title),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            Text(
                text = stringResource(R.string.reset_password_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TypeTypeCard {
                TypeTypeTextField(
                    value = state.resetToken,
                    onValueChange = onResetTokenChange,
                    placeholder = stringResource(R.string.reset_password_token),
                    enabled = !state.isSubmitting,
                )
                TypeTypeTextField(
                    value = state.newPassword,
                    onValueChange = onNewPasswordChange,
                    placeholder = stringResource(R.string.reset_password_new_password),
                    enabled = !state.isSubmitting,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                )
                TypeTypePrimaryButton(
                    text = stringResource(R.string.reset_password_submit),
                    onClick = onSubmit,
                    enabled = state.canSubmit,
                    isLoading = state.isSubmitting,
                )
            }
            if (state.isComplete) {
                Text(
                    text = stringResource(R.string.reset_password_complete),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                TypeTypeSecondaryButton(
                    text = stringResource(R.string.reset_password_back_to_login),
                    onClick = onNavigateBack,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (state.errorKey != null) {
                Text(
                    text = stringResource(R.string.reset_password_invalid),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                state.errorRequestId?.let { RequestIdRow(requestId = it) }
            }
        }
    }
}
