package dev.typetype.android.domain.account

data class Account(
    val serverId: String,
    val id: String,
    val publicUsername: String?,
    val role: String?,
    val avatarUrl: String?,
    val avatarType: String?,
    val avatarCode: String?,
    val isGuest: Boolean,
    val lastUsedAt: Long,
)
