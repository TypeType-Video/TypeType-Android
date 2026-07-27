package dev.typetype.android.data.server

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.typetype.android.domain.server.Server
import dev.typetype.android.domain.server.ServerRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

@Singleton
class RoomServerRepository @Inject constructor(
    private val dao: ServerDao,
    private val preferences: DataStore<Preferences>,
) : ServerRepository {

    override fun observeServers(): Flow<List<Server>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override fun observeCurrentServer(): Flow<Server?> =
        preferences.data
            .map { it[CURRENT_SERVER_ID_KEY] }
            .flatMapLatest { id ->
                if (id == null) flowOf(null) else dao.observeById(id).map { it?.toDomain() }
            }

    override suspend fun getServer(id: String): Server? = dao.getById(id)?.toDomain()

    override suspend fun addServer(server: Server) =
        dao.upsert(ServerEntity.fromDomain(server))

    override suspend fun deleteServer(id: String) {
        dao.deleteById(id)
        val selectedId = preferences.data.map { it[CURRENT_SERVER_ID_KEY] }.first()
        if (selectedId == id) {
            preferences.edit { it.remove(CURRENT_SERVER_ID_KEY) }
        }
    }

    override suspend fun setCurrentServer(id: String) {
        preferences.edit { it[CURRENT_SERVER_ID_KEY] = id }
    }

    override suspend fun clearCurrentServer() {
        preferences.edit { it.remove(CURRENT_SERVER_ID_KEY) }
    }

    private companion object {
        val CURRENT_SERVER_ID_KEY = stringPreferencesKey("current_server_id")
    }
}
