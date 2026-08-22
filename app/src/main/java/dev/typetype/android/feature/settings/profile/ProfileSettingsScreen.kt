package dev.typetype.android.feature.settings.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.LocalAppSnackbarHost
import dev.typetype.android.core.ui.components.RequestIdRow
import dev.typetype.android.core.ui.share.LocalServerBaseUrl
import dev.typetype.android.feature.settings.SettingsDetailTopBar

@Composable
fun ProfileSettingsRoute(
    onNavigateBack: () -> Unit,
    onResetPassword: () -> Unit,
    viewModel: ProfileSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(viewModel::uploadAvatar) }
    val snackbarHost = LocalAppSnackbarHost.current
    val savedMessage = stringResource(R.string.settings_profile_save_success)
    val avatarSavedMessage = stringResource(R.string.settings_profile_avatar_save_success)
    val identitySavedMessage = stringResource(R.string.settings_profile_account_save_success)
    val baseUrl = LocalServerBaseUrl.current
    LaunchedEffect(viewModel, snackbarHost) {
        if (snackbarHost == null) return@LaunchedEffect
        viewModel.events.collect { event ->
            when (event) {
                ProfileSettingsEvent.ProfileSaved ->
                    snackbarHost.showSnackbar(savedMessage, duration = SnackbarDuration.Short)
                ProfileSettingsEvent.AvatarSaved ->
                    snackbarHost.showSnackbar(avatarSavedMessage, duration = SnackbarDuration.Short)
                ProfileSettingsEvent.IdentitySaved ->
                    snackbarHost.showSnackbar(identitySavedMessage, duration = SnackbarDuration.Short)
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().imePadding(),
        ) {
            SettingsDetailTopBar(
                title = stringResource(R.string.settings_profile_title),
                onNavigateBack = onNavigateBack,
            )
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item { AvatarHeader(profile = state.profile, baseUrl = baseUrl) }
                if (state.isLoading) {
                    item {
                        CircularProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                } else if (state.profile == null) {
                    item {
                        ProfileLoadFailure(
                            requestId = state.profileErrorRequestId,
                            onRetry = viewModel::retryProfile,
                        )
                    }
                } else if (state.isGuest) {
                    item { GuestProfileNotice() }
                } else {
                    item {
                        ProfileCustomAvatarSection(
                            isSaving = state.isAvatarSaving,
                            errorKey = state.avatarErrorKey,
                            errorRequestId = state.avatarErrorRequestId,
                            onSelectFile = { avatarPicker.launch(supportedAvatarMimeTypes()) },
                        )
                    }
                    item {
                        EmojiPicker(
                            baseUrl = baseUrl,
                            selectedCode = state.profile?.avatarCode.orEmpty(),
                            onSelect = viewModel::setAvatarEmoji,
                            onClear = viewModel::clearAvatar,
                        )
                    }
                    item {
                        LabeledField(
                            label = stringResource(R.string.settings_profile_username_label),
                            helper = stringResource(R.string.settings_profile_username_helper),
                            value = state.usernameDraft,
                            onValueChange = viewModel::setUsernameDraft,
                            singleLine = true,
                            errorMessage = profileErrorMessage(state.profileErrorKey),
                        )
                    }
                    state.profileErrorRequestId?.let { requestId ->
                        item { RequestIdRow(requestId = requestId) }
                    }
                    item {
                        LabeledField(
                            label = stringResource(R.string.settings_profile_bio_label),
                            helper = stringResource(R.string.settings_profile_bio_helper),
                            value = state.bioDraft,
                            onValueChange = viewModel::setBioDraft,
                            singleLine = false,
                        )
                    }
                    item {
                        Button(
                            onClick = viewModel::save,
                            enabled = !state.isSaving,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.action_save),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                ),
                            )
                        }
                    }
                    item {
                        ProfileAccountIdentitySection(
                            state = state,
                            onEmailChange = viewModel::setEmailDraft,
                            onNameChange = viewModel::setNameDraft,
                            onPasswordChange = viewModel::setCurrentPasswordDraft,
                            onSave = viewModel::saveIdentity,
                            onResetPassword = onResetPassword,
                            onRetry = viewModel::retryIdentity,
                        )
                    }
                }
                item {
                    val role = state.profile?.role.orEmpty()
                    if (role.isNotBlank()) {
                        InfoRow(
                            label = stringResource(R.string.settings_profile_role),
                            value = role.replaceFirstChar { it.titlecase() },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun profileErrorMessage(key: String?): String? {
    if (key.isNullOrBlank()) return null
    return when (key) {
        "USERNAME_INVALID_LENGTH" -> stringResource(R.string.settings_profile_username_invalid_length)
        "USERNAME_INVALID_FORMAT" -> stringResource(R.string.settings_profile_username_invalid_format)
        "USERNAME_TAKEN" -> stringResource(R.string.settings_profile_username_taken)
        "BIO_TOO_LONG" -> stringResource(R.string.settings_profile_bio_too_long)
        else -> stringResource(R.string.settings_profile_save_failed)
    }
}

@Composable
private fun GuestProfileNotice() {
    Text(
        text = stringResource(R.string.settings_profile_guest_notice),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
    )
}

@Composable
private fun ProfileLoadFailure(
    requestId: String?,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_profile_load_failed),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
        requestId?.let { RequestIdRow(requestId = it) }
        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_retry))
        }
    }
}
