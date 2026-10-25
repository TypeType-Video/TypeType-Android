package dev.typetype.android.domain.appupdate

sealed interface AppUpdateCheck {
    data object ManagedByF_Droid : AppUpdateCheck
    data object UpToDate : AppUpdateCheck
    data class Available(val release: AppUpdateRelease) : AppUpdateCheck
    data class NewerBuildInstalled(val release: AppUpdateRelease) : AppUpdateCheck
}
