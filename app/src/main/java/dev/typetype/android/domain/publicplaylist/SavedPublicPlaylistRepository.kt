package dev.typetype.android.domain.publicplaylist

import kotlinx.coroutines.flow.Flow

interface SavedPublicPlaylistRepository {
    fun observe(): Flow<List<SavedPublicPlaylist>>
    fun observeCanModify(): Flow<Boolean>
    suspend fun refresh(): Result<Unit>
    suspend fun save(url: String): Result<SavedPublicPlaylist>
    suspend fun remove(id: String): Result<Unit>
}
