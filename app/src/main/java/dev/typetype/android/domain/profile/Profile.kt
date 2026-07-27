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

data class AccountIdentity(
    val email: String,
    val name: String,
    val managedByOidc: Boolean,
)

data class AvatarUpload(
    val bytes: ByteArray,
    val mediaType: String,
)
