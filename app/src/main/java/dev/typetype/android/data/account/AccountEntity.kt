package dev.typetype.android.data.account

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import dev.typetype.android.data.network.dto.UserProfile
import dev.typetype.android.data.server.ServerEntity
import dev.typetype.android.domain.account.Account

@Entity(
    tableName = "accounts",
    primaryKeys = ["serverId", "accountId"],
    foreignKeys = [
        ForeignKey(
            entity = ServerEntity::class,
            parentColumns = ["id"],
            childColumns = ["serverId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("serverId")],
)
data class AccountEntity(
    val serverId: String,
    val accountId: String,
    val publicUsername: String?,
    val role: String?,
    val avatarUrl: String?,
    val avatarType: String?,
    val avatarCode: String?,
    val isGuest: Boolean,
    val lastUsedAt: Long,
    @ColumnInfo(defaultValue = "0") val sessionGeneration: Long,
) {
    fun toDomain(): Account = Account(
        serverId = serverId,
        id = accountId,
        publicUsername = publicUsername,
        role = role,
        avatarUrl = avatarUrl,
        avatarType = avatarType,
        avatarCode = avatarCode,
        isGuest = isGuest,
        lastUsedAt = lastUsedAt,
    )

    companion object {
        fun fromProfile(
            serverId: String,
            profile: UserProfile,
            sessionGeneration: Long,
        ): AccountEntity = AccountEntity(
            serverId = serverId,
            accountId = profile.id,
            publicUsername = profile.publicUsername,
            role = profile.role,
            avatarUrl = profile.avatarUrl,
            avatarType = profile.avatarType,
            avatarCode = profile.avatarCode,
            isGuest = profile.id.startsWith("guest:"),
            lastUsedAt = System.currentTimeMillis(),
            sessionGeneration = sessionGeneration,
        )
    }
}
