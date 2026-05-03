package dev.typetype.android.domain.server

import kotlinx.coroutines.flow.Flow

interface ServerRepository {
    fun observeServers(): Flow<List<Server>>
    fun observeCurrentServer(): Flow<Server?>
    suspend fun getServer(id: String): Server?
    suspend fun addServer(server: Server)
    suspend fun deleteServer(id: String)
    suspend fun setCurrentServer(id: String)
    suspend fun clearCurrentServer()
}
