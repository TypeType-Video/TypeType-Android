package dev.typetype.android.domain.setup

import dev.typetype.android.domain.server.Server

interface SetupRepository {
    suspend fun probeServer(
        rawUrl: String,
        allowLocalCleartext: Boolean = false,
    ): Result<ProbeResult>
    suspend fun persistServer(server: Server, makeCurrent: Boolean = true)
}

data class ProbeResult(
    val normalizedUrl: String,
    val name: String,
    val tagline: String?,
    val version: String,
    val revision: String,
    val apiVersion: Int,
    val registrationAllowed: Boolean,
    val guestAllowed: Boolean,
    val supportedServices: List<Int>,
    val minAndroidClientVersion: String?,
    val logoUrl: String?,
    val bannerUrl: String?,
    val localLoginEnabled: Boolean,
    val oidcEnabled: Boolean,
    val oidcProviderName: String?,
    val oidcAutoRedirect: Boolean,
    val youtubeRemoteLoginEnabled: Boolean,
    val youtubeRemoteLoginReady: Boolean,
    val youtubeRemoteLoginUnavailableReason: String?,
)
