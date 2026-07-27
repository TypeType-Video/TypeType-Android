package dev.typetype.android.feature.setup.addserver

sealed interface AddServerAction {
    data class OnUrlChange(val url: String) : AddServerAction
    data object OnConnectClick : AddServerAction
    data object OnLocalNetworkPermissionDenied : AddServerAction
    data object OnBackClick : AddServerAction
}
