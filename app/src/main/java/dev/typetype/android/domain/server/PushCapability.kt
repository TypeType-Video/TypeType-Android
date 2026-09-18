package dev.typetype.android.domain.server

data class PushCapability(
    val enabled: Boolean = false,
    val provider: String = "unifiedpush",
    val eventTypes: List<String> = emptyList(),
    val maxDevicesPerAccount: Int = 0,
)
