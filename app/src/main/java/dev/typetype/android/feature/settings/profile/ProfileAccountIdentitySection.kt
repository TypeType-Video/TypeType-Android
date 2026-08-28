package dev.typetype.android.feature.settings.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.RequestIdRow
import dev.typetype.android.core.ui.components.TypeTypeSecondaryButton

@Composable
fun ProfileAccountIdentitySection(
    state: ProfileSettingsState,
    onEmailChange: (String) -> Unit,
    onNameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSave: () -> Unit,
    onResetPassword: () -> Unit,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_profile_account_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (state.isIdentityLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            IdentityFields(
                state = state,
                onEmailChange = onEmailChange,
                onNameChange = onNameChange,
                onPasswordChange = onPasswordChange,
                onSave = onSave,
            )
            if (state.identity != null) {
                TypeTypeSecondaryButton(
                    text = stringResource(R.string.reset_password_title),
                    onClick = onResetPassword,
                )
            }
        }
        identityErrorMessage(state.identityErrorKey)?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        state.identityErrorRequestId?.let { RequestIdRow(requestId = it) }
        if (state.identity == null && state.identityErrorKey != null && !state.isIdentityLoading) {
            TypeTypeSecondaryButton(
                text = stringResource(R.string.action_retry),
                onClick = onRetry,
            )
        }
    }
}

@Composable
private fun IdentityFields(
    state: ProfileSettingsState,
    onEmailChange: (String) -> Unit,
    onNameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    val managedByOidc = state.identity?.managedByOidc == true
    if (managedByOidc) {
        Text(
            text = stringResource(R.string.settings_profile_account_oidc_managed),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    OutlinedTextField(
        value = state.nameDraft,
        onValueChange = onNameChange,
        label = { Text(stringResource(R.string.settings_profile_account_name)) },
        enabled = !managedByOidc && state.identity != null,
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = state.emailDraft,
        onValueChange = onEmailChange,
        label = { Text(stringResource(R.string.settings_profile_account_email)) },
        enabled = !managedByOidc && state.identity != null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    if (!managedByOidc && state.identity != null) {
        OutlinedTextField(
            value = state.currentPasswordDraft,
            onValueChange = onPasswordChange,
            label = {
                Text(stringResource(R.string.settings_profile_account_current_password))
            },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = onSave,
            enabled = state.canSaveIdentity,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.isIdentitySaving) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(end = 10.dp).size(18.dp),
                    strokeWidth = 2.dp,
                )
            }
            Text(stringResource(R.string.settings_profile_account_save))
        }
    }
}

@Composable
private fun identityErrorMessage(key: String?): String? = when (key) {
    null -> null
    "IDENTITY_INVALID" -> stringResource(R.string.settings_profile_account_invalid)
    "CURRENT_PASSWORD_INVALID" -> stringResource(R.string.settings_profile_account_password_invalid)
    "IDENTITY_PROVIDER_MANAGED" -> stringResource(R.string.settings_profile_account_oidc_managed)
    "EMAIL_TAKEN" -> stringResource(R.string.settings_profile_account_email_taken)
    else -> stringResource(R.string.settings_profile_account_load_failed)
}
