package dev.typetype.android.data.network

import dev.typetype.android.domain.server.Server
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

data class CurrentServerEndpoint(
    val serverId: String,
    val baseUrl: String,
) {
    fun owns(url: HttpUrl): Boolean {
        val serverUrl = baseUrl.toHttpUrlOrNull() ?: return false
        return serverUrl.scheme == url.scheme &&
            serverUrl.host == url.host &&
            serverUrl.port == url.port
    }

    companion object {
        fun from(server: Server): CurrentServerEndpoint = CurrentServerEndpoint(
            serverId = server.id,
            baseUrl = server.baseUrl,
        )
    }
}
