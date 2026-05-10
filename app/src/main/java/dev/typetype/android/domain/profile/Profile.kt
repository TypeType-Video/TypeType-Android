package dev.typetype.android.domain.profile

data class Profile(
    val id: String,
    val role: String = "",
    val publicUsername: String = "",
    val bio: String = "",
    val avatarUrl: String = "",
    val avatarType: String = "",
    val avatarCode: String = "",
)
