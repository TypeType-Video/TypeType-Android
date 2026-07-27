package dev.typetype.android.feature.settings.profile

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.typetype.android.R
import dev.typetype.android.core.ui.components.LocalAppSnackbarHost
import dev.typetype.android.core.ui.components.RequestIdRow
import dev.typetype.android.core.ui.share.LocalServerBaseUrl

@Composable
fun ProfileSettingsRoute(
    onNavigateBack: () -> Unit,
    viewModel: ProfileSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHost = LocalAppSnackbarHost.current
    val savedMessage = stringResource(R.string.settings_profile_save_success)
    val baseUrl = LocalServerBaseUrl.current
    LaunchedEffect(viewModel, snackbarHost) {
        if (snackbarHost == null) return@LaunchedEffect
        viewModel.events.collect { event ->
            when (event) {
                ProfileSettingsEvent.Saved ->
                    snackbarHost.showSnackbar(savedMessage, duration = SnackbarDuration.Short)
                is ProfileSettingsEvent.Error -> Unit
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .imePadding(),
        ) {
            TopBar(onNavigateBack = onNavigateBack)
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item { AvatarHeader(profile = state.profile, baseUrl = baseUrl) }
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
                        errorMessage = errorMessageFor(state.errorKey),
                    )
                }
                state.errorRequestId?.let { requestId ->
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
                    val role = state.profile?.role.orEmpty()
                    if (role.isNotBlank()) {
                        InfoRow(
                            label = stringResource(R.string.settings_profile_role),
                            value = role.replaceFirstChar { it.titlecase() },
                        )
                    }
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
            }
        }
    }
}

@Composable
private fun errorMessageFor(key: String?): String? {
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
private fun TopBar(onNavigateBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onNavigateBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.settings_back),
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
        Text(
            text = stringResource(R.string.settings_profile_title),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.3).sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}
