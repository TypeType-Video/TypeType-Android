package dev.typetype.android.feature.setup.addserver

sealed interface AddServerAction {
    data class OnUrlChange(val url: String) : AddServerAction
    data class OnConnectRequested(val permissionRequired: Boolean) : AddServerAction
    data object OnLocalNetworkPermissionGranted : AddServerAction
    data class OnLocalNetworkPermissionDenied(val permanently: Boolean) : AddServerAction
    data object OnBackClick : AddServerAction
}
