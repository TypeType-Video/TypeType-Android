package dev.typetype.android.domain.setup

import dev.typetype.android.domain.server.Server

interface SetupRepository {
    suspend fun probeServer(rawUrl: String): Result<ProbeResult>
    suspend fun persistServer(server: Server, makeCurrent: Boolean = true)
}

data class ProbeResult(
    val normalizedUrl: String,
    val derivedDisplayName: String,
)
