package dev.typetype.android.domain.server

data class Server(
    val id: String,
    val baseUrl: String,
    val displayName: String,
    val addedAt: Long,
)
