package dev.typetype.android.feature.setup.addserver

sealed interface AddServerEvent {
    data class NavigateToLogin(val serverId: String) : AddServerEvent
    data object RequestLocalNetworkPermission : AddServerEvent
    data object NavigateBack : AddServerEvent
}
