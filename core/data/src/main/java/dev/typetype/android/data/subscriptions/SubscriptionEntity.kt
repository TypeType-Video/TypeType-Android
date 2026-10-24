package dev.typetype.android.data.subscriptions

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import dev.typetype.android.data.account.AccountEntity

@Entity(
    tableName = "subscriptions",
    primaryKeys = ["serverId", "accountId", "channelUrl"],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["serverId", "accountId"],
            childColumns = ["serverId", "accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("serverId", "accountId")],
)
data class SubscriptionEntity(
    val serverId: String,
    val accountId: String,
    val channelUrl: String,
    val name: String,
    val avatarUrl: String,
    val subscribedAtMillis: Long,
)
