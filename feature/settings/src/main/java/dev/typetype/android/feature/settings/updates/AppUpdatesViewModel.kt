package dev.typetype.android.feature.settings.updates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.data.appupdate.AndroidAppUpdateInstaller
import dev.typetype.android.data.appupdate.AppUpdateInstallLaunch
import dev.typetype.android.domain.appupdate.AppUpdateChannel
import dev.typetype.android.domain.appupdate.AppUpdateCheck
import dev.typetype.android.domain.appupdate.AppUpdateFailure
import dev.typetype.android.domain.appupdate.AppUpdateFailureReason
import dev.typetype.android.domain.appupdate.AppUpdateRepository
import dev.typetype.android.domain.appupdate.AppUpdateSettingsRepository
import dev.typetype.android.domain.version.AppVersion
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@HiltViewModel
class AppUpdatesViewModel @Inject constructor(
    appVersion: AppVersion,
    private val repository: AppUpdateRepository,
    private val settings: AppUpdateSettingsRepository,
    private val installer: AndroidAppUpdateInstaller,
) : ViewModel() {
    val versionName: String = appVersion.name
    private val versionCode: Long = appVersion.code
    private val mutableState = MutableStateFlow(AppUpdateUiState())
    val state: StateFlow<AppUpdateUiState> = mutableState.asStateFlow()
    private var job: Job? = null

    init {
        viewModelScope.launch {
            settings.observeChannel()
                .distinctUntilChanged()
                .collect { channel ->
                    mutableState.value = AppUpdateUiState(
                        channel = channel,
                        status = if (channel == AppUpdateChannel.F_DROID) {
                            AppUpdateUiStatus.MANAGED_BY_F_DROID
                        } else {
                            AppUpdateUiStatus.IDLE
                        },
                    )
                    if (channel != AppUpdateChannel.F_DROID) check(channel)
                }
        }
    }

    fun selectChannel(channel: AppUpdateChannel) {
        viewModelScope.launch { settings.setChannel(channel) }
    }

    fun check() {
        val channel = mutableState.value.channel
        if (channel != AppUpdateChannel.F_DROID) check(channel)
    }

    fun download() {
        val release = mutableState.value.release ?: return
        job?.cancel()
        job = viewModelScope.launch {
            mutableState.value = mutableState.value.copy(
                status = AppUpdateUiStatus.DOWNLOADING,
                downloadedFile = null,
                progress = 0f,
                downloadedBytes = 0,
                totalBytes = release.apkSizeBytes,
                failureReason = null,
            )
            try {
                val file = repository.download(release) { downloaded, total ->
                    mutableState.value = mutableState.value.copy(
                        downloadedBytes = downloaded,
                        totalBytes = total,
                        progress = if (total > 0) (downloaded.toFloat() / total).coerceIn(0f, 1f) else 0f,
                    )
                }
                installer.validate(file, release)
                mutableState.value = mutableState.value.copy(
                    status = AppUpdateUiStatus.READY_TO_INSTALL,
                    downloadedFile = file,
                    progress = 1f,
                    downloadedBytes = file.length(),
                    failureReason = null,
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                fail(failure)
            }
        }
    }

    fun install() {
        val file = mutableState.value.downloadedFile ?: return
        try {
            mutableState.value = mutableState.value.copy(
                status = when (installer.launch(file)) {
                    AppUpdateInstallLaunch.InstallerOpened -> AppUpdateUiStatus.INSTALLER_OPENED
                    AppUpdateInstallLaunch.PermissionRequested -> AppUpdateUiStatus.PERMISSION_REQUESTED
                },
            )
        } catch (failure: Exception) {
            fail(failure)
        }
    }

    private fun check(channel: AppUpdateChannel) {
        job?.cancel()
        job = viewModelScope.launch {
            mutableState.value = AppUpdateUiState(channel = channel, status = AppUpdateUiStatus.CHECKING)
            try {
                val result = repository.check(
                    channel = channel,
                    installedVersionName = versionName,
                    installedVersionCode = versionCode,
                )
                mutableState.value = result.toUiState(channel)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                fail(failure, channel)
            }
        }
    }

    private fun fail(failure: Exception, channel: AppUpdateChannel = mutableState.value.channel) {
        val reason = when (failure) {
            is AppUpdateFailure -> failure.reason
            is IOException -> AppUpdateFailureReason.NETWORK
            else -> AppUpdateFailureReason.INSTALLER_UNAVAILABLE
        }
        mutableState.value = mutableState.value.copy(
            channel = channel,
            status = AppUpdateUiStatus.FAILED,
            failureReason = reason,
        )
    }
}

private fun AppUpdateCheck.toUiState(channel: AppUpdateChannel): AppUpdateUiState = when (this) {
    AppUpdateCheck.ManagedByF_Droid -> AppUpdateUiState(channel, AppUpdateUiStatus.MANAGED_BY_F_DROID)
    AppUpdateCheck.UpToDate -> AppUpdateUiState(channel, AppUpdateUiStatus.UP_TO_DATE)
    is AppUpdateCheck.Available -> AppUpdateUiState(channel, AppUpdateUiStatus.AVAILABLE, release)
    is AppUpdateCheck.NewerBuildInstalled -> AppUpdateUiState(channel, AppUpdateUiStatus.NEWER_BUILD_INSTALLED, release)
}
