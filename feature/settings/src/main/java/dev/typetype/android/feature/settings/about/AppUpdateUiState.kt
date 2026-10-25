package dev.typetype.android.feature.settings.about

import dev.typetype.android.domain.appupdate.AppUpdateChannel
import dev.typetype.android.domain.appupdate.AppUpdateFailureReason
import dev.typetype.android.domain.appupdate.AppUpdateRelease
import java.io.File

enum class AppUpdateUiStatus {
    IDLE,
    CHECKING,
    MANAGED_BY_F_DROID,
    UP_TO_DATE,
    AVAILABLE,
    NEWER_BUILD_INSTALLED,
    DOWNLOADING,
    READY_TO_INSTALL,
    INSTALLER_OPENED,
    PERMISSION_REQUESTED,
    FAILED,
}

data class AppUpdateUiState(
    val channel: AppUpdateChannel = AppUpdateChannel.GITHUB_STABLE,
    val status: AppUpdateUiStatus = AppUpdateUiStatus.IDLE,
    val release: AppUpdateRelease? = null,
    val downloadedFile: File? = null,
    val progress: Float = 0f,
    val downloadedBytes: Long = 0,
    val totalBytes: Long = 0,
    val failureReason: AppUpdateFailureReason? = null,
)
