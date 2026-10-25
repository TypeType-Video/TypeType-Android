package dev.typetype.android.domain.server

interface ServerCapabilitiesRepository {
    suspend fun refresh(serverId: String): Result<Server>
}
