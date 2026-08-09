package dev.typetype.android.feature.setup.addserver

data class AddServerState(
    val url: String = "",
    val isConnecting: Boolean = false,
    val errorMessage: String? = null,
    val errorRequestId: String? = null,
    val localNetworkPermissionDenied: Boolean = false,
    val localNetworkPermissionPermanentlyDenied: Boolean = false,
    val resolvedName: String? = null,
    val resolvedTagline: String? = null,
    val resolvedVersion: String? = null,
)
