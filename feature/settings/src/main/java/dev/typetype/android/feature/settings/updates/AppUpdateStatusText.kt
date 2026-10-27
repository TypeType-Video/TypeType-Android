package dev.typetype.android.feature.settings.updates

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.typetype.android.R
import dev.typetype.android.domain.appupdate.AppUpdateFailureReason

@Composable
internal fun AppUpdateUiState.statusText(): String {
    val version = release?.versionName.orEmpty()
    return when (status) {
        AppUpdateUiStatus.IDLE,
        AppUpdateUiStatus.CHECKING,
        -> stringResource(R.string.updates_status_checking)
        AppUpdateUiStatus.MANAGED_BY_F_DROID -> stringResource(R.string.updates_status_fdroid)
        AppUpdateUiStatus.UP_TO_DATE -> stringResource(R.string.updates_status_up_to_date)
        AppUpdateUiStatus.AVAILABLE -> stringResource(R.string.updates_status_available, version)
        AppUpdateUiStatus.NEWER_BUILD_INSTALLED -> stringResource(R.string.updates_status_newer_build)
        AppUpdateUiStatus.DOWNLOADING -> stringResource(R.string.updates_status_downloading, version)
        AppUpdateUiStatus.READY_TO_INSTALL -> stringResource(R.string.updates_status_ready, version)
        AppUpdateUiStatus.INSTALLER_OPENED -> stringResource(R.string.updates_status_installer_opened)
        AppUpdateUiStatus.PERMISSION_REQUESTED -> stringResource(R.string.updates_status_permission)
        AppUpdateUiStatus.FAILED -> stringResource(failureReason?.messageRes() ?: R.string.updates_status_failed)
    }
}

private fun AppUpdateFailureReason.messageRes(): Int = when (this) {
    AppUpdateFailureReason.NETWORK -> R.string.updates_failed_network
    AppUpdateFailureReason.INVALID_RELEASE -> R.string.updates_failed_release
    AppUpdateFailureReason.INVALID_CHECKSUM -> R.string.updates_failed_checksum
    AppUpdateFailureReason.INVALID_PACKAGE -> R.string.updates_failed_package
    AppUpdateFailureReason.DOWNGRADE_BLOCKED -> R.string.updates_failed_downgrade
    AppUpdateFailureReason.SIGNATURE_MISMATCH -> R.string.updates_failed_signature
    AppUpdateFailureReason.INSTALLER_UNAVAILABLE -> R.string.updates_failed_installer
}
