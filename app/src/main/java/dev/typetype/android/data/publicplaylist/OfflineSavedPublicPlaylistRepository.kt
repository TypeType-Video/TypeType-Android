package dev.typetype.android.data.publicplaylist

import dev.typetype.android.data.account.AccountDao
import dev.typetype.android.data.account.AccountScope
import dev.typetype.android.data.account.ActiveAccountScope
import dev.typetype.android.data.network.TypeTypeApiHolder
import dev.typetype.android.data.network.dto.SavePublicPlaylistRequest
import dev.typetype.android.data.network.dto.SavedPublicPlaylistDto
import dev.typetype.android.data.network.requireSuccessfulResponse
import dev.typetype.android.data.database.TypeTypeDatabase
import dev.typetype.android.data.library.sync.LibraryRefreshToken
import dev.typetype.android.data.library.sync.LibrarySyncTracker
import dev.typetype.android.domain.library.LibraryCollection
import dev.typetype.android.domain.publicplaylist.SavedPublicPlaylist
import dev.typetype.android.domain.publicplaylist.SavedPublicPlaylistRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import androidx.room.withTransaction

@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class OfflineSavedPublicPlaylistRepository @Inject constructor(
    private val apiHolder: TypeTypeApiHolder,
    private val activeAccountScope: ActiveAccountScope,
    private val accountDao: AccountDao,
    private val dao: SavedPublicPlaylistDao,
    private val database: TypeTypeDatabase,
    private val syncTracker: LibrarySyncTracker,
) : SavedPublicPlaylistRepository {
    override fun observe(): Flow<List<SavedPublicPlaylist>> = activeAccountScope.observe()
        .flatMapLatest { scope ->
            if (scope == null) flowOf(emptyList())
            else dao.observe(scope.serverId, scope.accountId).map { rows -> rows.map { it.toDomain() } }
        }

    override fun observeCanModify(): Flow<Boolean> = activeAccountScope.observe()
        .flatMapLatest { scope ->
            if (scope == null) {
                flowOf(false)
            } else {
                accountDao.observeForServer(scope.serverId).map { accounts ->
                    accounts.firstOrNull { it.accountId == scope.accountId }?.isGuest == false
                }
            }
        }

    override suspend fun refresh(): Result<Unit> = savedPlaylistResult {
        val scope = activeAccountScope.require()
        val token = syncTracker.begin(scope, LibraryCollection.SavedPlaylists)
        try {
            val rows = if (isGuest(scope)) {
                emptyList()
            } else {
                val api = apiHolder.require(scope)
                val response = withContext(Dispatchers.IO) { api.savedPublicPlaylists() }
                response.requireSuccessfulResponse()
                requireNotNull(response.body()) { "Empty saved playlists body" }
                    .map { it.toEntity(scope) }
            }
            activeAccountScope.verify(scope)
            database.withTransaction {
                if (!syncTracker.isCurrent(token)) throw SupersededSavedPlaylistRefresh()
                dao.replaceAll(scope.serverId, scope.accountId, rows)
                syncTracker.succeed(token)
            }
        } catch (failure: Throwable) {
            if (failure is CancellationException) throw failure
            recordFailure(token, failure)
            throw failure
        }
    }

    override suspend fun save(url: String): Result<SavedPublicPlaylist> = savedPlaylistResult {
        val scope = activeAccountScope.require()
        check(!isGuest(scope)) { "Sign in to save playlists" }
        val api = apiHolder.require(scope)
        val response = withContext(Dispatchers.IO) {
            api.savePublicPlaylist(SavePublicPlaylistRequest(url.trim()))
        }
        response.requireSuccessfulResponse()
        val row = requireNotNull(response.body()) { "Empty saved playlist body" }.toEntity(scope)
        activeAccountScope.verify(scope)
        dao.upsert(row)
        row.toDomain()
    }

    override suspend fun remove(id: String): Result<Unit> = savedPlaylistResult {
        val scope = activeAccountScope.require()
        check(!isGuest(scope)) { "Sign in to edit saved playlists" }
        val api = apiHolder.require(scope)
        val response = withContext(Dispatchers.IO) { api.removeSavedPublicPlaylist(id) }
        if (!response.isSuccessful && response.code() != 404) response.requireSuccessfulResponse()
        activeAccountScope.verify(scope)
        dao.delete(scope.serverId, scope.accountId, id)
    }

    private suspend fun isGuest(scope: AccountScope): Boolean =
        accountDao.get(scope.serverId, scope.accountId)?.isGuest != false

    private suspend fun recordFailure(token: LibraryRefreshToken, failure: Throwable) {
        val current = runCatching { activeAccountScope.verify(token.scope) }.isSuccess
        if (current) syncTracker.fail(token, failure)
    }

    private fun SavedPublicPlaylistDto.toEntity(scope: AccountScope) = SavedPublicPlaylistEntity(
        serverId = scope.serverId,
        accountId = scope.accountId,
        id = id,
        publicPlaylistId = publicPlaylistId,
        url = url,
        title = title,
        thumbnailUrl = thumbnailUrl,
        uploaderName = uploaderName,
        streamCount = streamCount,
        playlistType = playlistType,
        savedAtMillis = savedAt,
    )
}

private class SupersededSavedPlaylistRefresh :
    CancellationException("Saved playlist refresh was superseded")

private suspend fun <T> savedPlaylistResult(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (failure: Throwable) {
    Result.failure(failure)
}
