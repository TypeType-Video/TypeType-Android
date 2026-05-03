package dev.typetype.android.feature.setup.addserver

sealed interface AddServerEvent {
    data class NavigateToLogin(
        val serverId: String,
        val guestAllowed: Boolean,
        val registrationAllowed: Boolean,
    ) : AddServerEvent
    data object NavigateBack : AddServerEvent
}
