package dev.typetype.android.feature.settings.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.typetype.android.R
import dev.typetype.android.domain.appupdate.AppUpdateChannel
import dev.typetype.android.domain.appupdate.AppUpdateFailureReason

@Composable
internal fun AboutAppUpdatesSection(
    state: AppUpdateUiState,
    onChannelChanged: (AppUpdateChannel) -> Unit,
    onCheck: () -> Unit,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.about_section_app_updates),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp),
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            UpdateChannelRow(
                channel = AppUpdateChannel.F_DROID,
                selected = state.channel == AppUpdateChannel.F_DROID,
                label = stringResource(R.string.about_update_channel_fdroid),
                enabled = state.status != AppUpdateUiStatus.DOWNLOADING,
                onClick = onChannelChanged,
            )
            UpdateChannelRow(
                channel = AppUpdateChannel.GITHUB_STABLE,
                selected = state.channel == AppUpdateChannel.GITHUB_STABLE,
                label = stringResource(R.string.about_update_channel_github_stable),
                enabled = state.status != AppUpdateUiStatus.DOWNLOADING,
                onClick = onChannelChanged,
            )
            UpdateChannelRow(
                channel = AppUpdateChannel.GITHUB_BETA,
                selected = state.channel == AppUpdateChannel.GITHUB_BETA,
                label = stringResource(R.string.about_update_channel_github_beta),
                enabled = state.status != AppUpdateUiStatus.DOWNLOADING,
                onClick = onChannelChanged,
            )
        }
        if (state.channel == AppUpdateChannel.GITHUB_BETA) {
            Text(
                text = stringResource(R.string.about_update_beta_warning),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        UpdateStatus(state)
        state.release?.let { release ->
            Text(
                text = stringResource(R.string.about_update_release, release.versionName),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            if (release.releaseNotes.isNotBlank()) {
                Text(
                    text = release.releaseNotes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
        if (state.status == AppUpdateUiStatus.DOWNLOADING) {
            LinearProgressIndicator(
                progress = { state.progress },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            Text(
                text = stringResource(R.string.about_update_progress, (state.progress * 100).toInt()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (state.channel != AppUpdateChannel.F_DROID) {
                Button(
                    onClick = onCheck,
                    enabled = state.status != AppUpdateUiStatus.CHECKING &&
                        state.status != AppUpdateUiStatus.DOWNLOADING,
                ) {
                    Text(stringResource(R.string.about_update_check))
                }
            }
            if (state.status == AppUpdateUiStatus.AVAILABLE) {
                Button(onClick = onDownload) {
                    Text(stringResource(R.string.about_update_download))
                }
            }
            if (
                state.status == AppUpdateUiStatus.READY_TO_INSTALL ||
                state.status == AppUpdateUiStatus.PERMISSION_REQUESTED
            ) {
                Button(onClick = onInstall) {
                    Text(stringResource(R.string.about_update_install))
                }
            }
        }
    }
}

@Composable
private fun UpdateChannelRow(
    channel: AppUpdateChannel,
    selected: Boolean,
    label: String,
    enabled: Boolean,
    onClick: (AppUpdateChannel) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = { onClick(channel) },
            )
            .padding(horizontal = 12.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null, enabled = enabled)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun UpdateStatus(state: AppUpdateUiState) {
    val text = when (state.status) {
        AppUpdateUiStatus.IDLE -> null
        AppUpdateUiStatus.CHECKING -> stringResource(R.string.about_update_checking)
        AppUpdateUiStatus.MANAGED_BY_F_DROID -> stringResource(R.string.about_update_managed_by_fdroid)
        AppUpdateUiStatus.UP_TO_DATE -> stringResource(R.string.about_update_up_to_date)
        AppUpdateUiStatus.AVAILABLE -> stringResource(R.string.about_update_available)
        AppUpdateUiStatus.NEWER_BUILD_INSTALLED ->
            stringResource(R.string.about_update_newer_build_installed)
        AppUpdateUiStatus.DOWNLOADING -> stringResource(R.string.about_update_downloading)
        AppUpdateUiStatus.READY_TO_INSTALL -> stringResource(R.string.about_update_ready_to_install)
        AppUpdateUiStatus.INSTALLER_OPENED -> stringResource(R.string.about_update_installer_opened)
        AppUpdateUiStatus.PERMISSION_REQUESTED -> stringResource(R.string.about_update_permission_requested)
        AppUpdateUiStatus.FAILED -> state.failureReason?.let(::failureMessage)
            ?.let { stringResource(it) }
            ?: stringResource(R.string.about_update_failed)
    }
    text?.let {
        Text(
            text = it,
            style = MaterialTheme.typography.bodySmall,
            color = if (state.status == AppUpdateUiStatus.FAILED) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}

private fun failureMessage(reason: AppUpdateFailureReason): Int = when (reason) {
    AppUpdateFailureReason.NETWORK -> R.string.about_update_failed_network
    AppUpdateFailureReason.INVALID_RELEASE -> R.string.about_update_failed_release
    AppUpdateFailureReason.INVALID_CHECKSUM -> R.string.about_update_failed_checksum
    AppUpdateFailureReason.INVALID_PACKAGE -> R.string.about_update_failed_package
    AppUpdateFailureReason.DOWNGRADE_BLOCKED -> R.string.about_update_failed_downgrade
    AppUpdateFailureReason.SIGNATURE_MISMATCH -> R.string.about_update_failed_signature
    AppUpdateFailureReason.INSTALLER_UNAVAILABLE -> R.string.about_update_failed_installer
}
