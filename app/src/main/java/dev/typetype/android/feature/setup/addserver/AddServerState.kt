package dev.typetype.android.feature.setup.addserver

data class AddServerState(
    val url: String = "",
    val isConnecting: Boolean = false,
    val errorMessage: String? = null,
)
