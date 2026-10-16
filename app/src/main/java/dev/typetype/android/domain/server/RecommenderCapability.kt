package dev.typetype.android.domain.server

data class RecommenderCapability(
    val installed: Boolean = false,
    val enabled: Boolean = false,
    val experimental: Boolean = false,
) {
    val available: Boolean
        get() = installed && enabled
}
