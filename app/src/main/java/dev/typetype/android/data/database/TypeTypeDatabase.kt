package dev.typetype.android.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.typetype.android.data.server.ServerDao
import dev.typetype.android.data.server.ServerEntity

@Database(entities = [ServerEntity::class], version = 1, exportSchema = true)
abstract class TypeTypeDatabase : RoomDatabase() {
    abstract fun serverDao(): ServerDao
}
