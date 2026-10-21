package dev.typetype.android.feature.settings.about

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.BuildConfig
import dev.typetype.android.data.appupdate.AndroidAppUpdateInstaller
import dev.typetype.android.data.appupdate.AppUpdateInstallLaunch
import dev.typetype.android.domain.appupdate.AppUpdateChannel
import dev.typetype.android.domain.appupdate.AppUpdateCheck
import dev.typetype.android.domain.appupdate.AppUpdateFailure
import dev.typetype.android.domain.appupdate.AppUpdateFailureReason
import dev.typetype.android.domain.appupdate.AppUpdateRepository
import dev.typetype.android.domain.appupdate.AppUpdateSettingsRepository
import dev.typetype.android.domain.profile.ProfileRepository
import dev.typetype.android.domain.server.ServerRepository
import dev.typetype.android.domain.version.ComponentVersions
import dev.typetype.android.domain.version.ComponentVersionsRepository
import javax.inject.Inject
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AboutState(
    val serverBaseUrl: String = "",
    val publicUsername: String = "",
    val role: String = "",
    val componentVersions: ComponentVersions? = null,
    val isLoadingVersions: Boolean = false,
    val appUpdate: AppUpdateUiState = AppUpdateUiState(),
)

private data class VersionLoadState(
    val versions: ComponentVersions? = null,
    val isLoading: Boolean = false,
)

@HiltViewModel
class AboutViewModel @Inject constructor(
    serverRepository: ServerRepository,
    profileRepository: ProfileRepository,
    private val componentVersionsRepository: ComponentVersionsRepository,
    private val appUpdateRepository: AppUpdateRepository,
    private val appUpdateSettingsRepository: AppUpdateSettingsRepository,
    private val appUpdateInstaller: AndroidAppUpdateInstaller,
) : ViewModel() {
    private val versionLoadState = MutableStateFlow(VersionLoadState())
    private val appUpdateState = MutableStateFlow(AppUpdateUiState())
    private var versionRefreshJob: Job? = null
    private var appUpdateJob: Job? = null

    val state = combine(
        serverRepository.observeCurrentServer(),
        profileRepository.observe(),
        versionLoadState,
        appUpdateState,
    ) { server, profile, versions, appUpdate ->
        AboutState(
            serverBaseUrl = server?.baseUrl.orEmpty(),
            publicUsername = profile?.publicUsername.orEmpty(),
            role = profile?.role.orEmpty(),
            componentVersions = versions.versions,
            isLoadingVersions = versions.isLoading,
            appUpdate = appUpdate,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AboutState())

    init {
        viewModelScope.launch { profileRepository.refresh() }
        refreshVersions()
        viewModelScope.launch {
            appUpdateSettingsRepository.observeChannel()
                .distinctUntilChanged()
                .collect { channel ->
                    appUpdateState.value = AppUpdateUiState(
                        channel = channel,
                        status = if (channel == AppUpdateChannel.F_DROID) {
                            AppUpdateUiStatus.MANAGED_BY_F_DROID
                        } else {
                            AppUpdateUiStatus.IDLE
                        },
                    )
                    if (channel != AppUpdateChannel.F_DROID) checkAppUpdate(channel)
                }
        }
    }

    fun refreshVersions() {
        if (versionRefreshJob?.isActive == true) return
        versionRefreshJob = viewModelScope.launch {
            versionLoadState.value = versionLoadState.value.copy(isLoading = true)
            versionLoadState.value = componentVersionsRepository.fetch().fold(
                onSuccess = { VersionLoadState(versions = it) },
                onFailure = { versionLoadState.value.copy(isLoading = false) },
            )
        }
    }

    fun setAppUpdateChannel(channel: AppUpdateChannel) {
        viewModelScope.launch { appUpdateSettingsRepository.setChannel(channel) }
    }

    fun checkAppUpdate() {
        val channel = appUpdateState.value.channel
        if (channel != AppUpdateChannel.F_DROID) checkAppUpdate(channel)
    }

    fun downloadAppUpdate() {
        val release = appUpdateState.value.release ?: return
        appUpdateJob?.cancel()
        appUpdateJob = viewModelScope.launch {
            appUpdateState.value = appUpdateState.value.copy(
                status = AppUpdateUiStatus.DOWNLOADING,
                downloadedFile = null,
                progress = 0f,
                downloadedBytes = 0,
                totalBytes = release.apkSizeBytes,
                failureReason = null,
            )
            try {
                val file = appUpdateRepository.download(release) { downloaded, total ->
                    appUpdateState.value = appUpdateState.value.copy(
                        downloadedBytes = downloaded,
                        totalBytes = total,
                        progress = if (total > 0) (downloaded.toFloat() / total).coerceIn(0f, 1f) else 0f,
                    )
                }
                appUpdateInstaller.validate(file, release)
                appUpdateState.value = appUpdateState.value.copy(
                    status = AppUpdateUiStatus.READY_TO_INSTALL,
                    downloadedFile = file,
                    progress = 1f,
                    downloadedBytes = file.length(),
                    failureReason = null,
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                failAppUpdate(failure)
            }
        }
    }

    fun installAppUpdate() {
        val file = appUpdateState.value.downloadedFile ?: return
        try {
            appUpdateState.value = appUpdateState.value.copy(
                status = when (appUpdateInstaller.launch(file)) {
                    AppUpdateInstallLaunch.InstallerOpened -> AppUpdateUiStatus.INSTALLER_OPENED
                    AppUpdateInstallLaunch.PermissionRequested -> AppUpdateUiStatus.PERMISSION_REQUESTED
                },
            )
        } catch (failure: Exception) {
            failAppUpdate(failure)
        }
    }

    private fun checkAppUpdate(channel: AppUpdateChannel) {
        appUpdateJob?.cancel()
        appUpdateJob = viewModelScope.launch {
            appUpdateState.value = AppUpdateUiState(channel = channel, status = AppUpdateUiStatus.CHECKING)
            try {
                val check = appUpdateRepository.check(
                    channel = channel,
                    installedVersionName = BuildConfig.VERSION_NAME,
                    installedVersionCode = BuildConfig.VERSION_CODE.toLong(),
                )
                appUpdateState.value = when (check) {
                    AppUpdateCheck.ManagedByF_Droid -> AppUpdateUiState(
                        channel = channel,
                        status = AppUpdateUiStatus.MANAGED_BY_F_DROID,
                    )
                    AppUpdateCheck.UpToDate -> AppUpdateUiState(
                        channel = channel,
                        status = AppUpdateUiStatus.UP_TO_DATE,
                    )
                    is AppUpdateCheck.Available -> AppUpdateUiState(
                        channel = channel,
                        status = AppUpdateUiStatus.AVAILABLE,
                        release = check.release,
                    )
                    is AppUpdateCheck.NewerBuildInstalled -> AppUpdateUiState(
                        channel = channel,
                        status = AppUpdateUiStatus.NEWER_BUILD_INSTALLED,
                        release = check.release,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                failAppUpdate(failure, channel)
            }
        }
    }

    private fun failAppUpdate(failure: Exception, channel: AppUpdateChannel = appUpdateState.value.channel) {
        val reason = when (failure) {
            is AppUpdateFailure -> failure.reason
            is IOException -> AppUpdateFailureReason.NETWORK
            else -> AppUpdateFailureReason.INSTALLER_UNAVAILABLE
        }
        appUpdateState.value = appUpdateState.value.copy(
            channel = channel,
            status = AppUpdateUiStatus.FAILED,
            failureReason = reason,
        )
    }
}
