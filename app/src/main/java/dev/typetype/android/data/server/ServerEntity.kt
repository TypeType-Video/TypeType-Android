package dev.typetype.android.data.server

import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.typetype.android.domain.server.Server

@Entity(tableName = "servers")
data class ServerEntity(
    @PrimaryKey val id: String,
    val baseUrl: String,
    val displayName: String,
    val addedAt: Long,
) {
    fun toDomain(): Server = Server(
        id = id,
        baseUrl = baseUrl,
        displayName = displayName,
        addedAt = addedAt,
    )

    companion object {
        fun fromDomain(server: Server): ServerEntity = ServerEntity(
            id = server.id,
            baseUrl = server.baseUrl,
            displayName = server.displayName,
            addedAt = server.addedAt,
        )
    }
}
