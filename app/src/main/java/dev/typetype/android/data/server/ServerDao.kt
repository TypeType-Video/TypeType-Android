package dev.typetype.android.data.server

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ServerDao {
    @Query("SELECT * FROM servers ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<ServerEntity>>

    @Query("SELECT * FROM servers WHERE id = :id")
    suspend fun getById(id: String): ServerEntity?

    @Query("SELECT * FROM servers WHERE id = :id")
    fun observeById(id: String): Flow<ServerEntity?>

    @Upsert
    suspend fun upsert(server: ServerEntity)

    @Query("DELETE FROM servers WHERE id = :id")
    suspend fun deleteById(id: String)
}
