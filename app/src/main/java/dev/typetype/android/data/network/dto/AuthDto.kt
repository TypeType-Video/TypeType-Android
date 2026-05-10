package dev.typetype.android.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val identifier: String? = null,
    val email: String? = null,
    val password: String,
)

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String,
)

@Serializable
data class RefreshRequest(
    val token: String? = null,
)

@Serializable
data class SessionResponse(
    val accessToken: String,
)

@Serializable
data class GuestResponse(
    val token: String,
)

@Serializable
data class UserProfile(
    val id: String,
    val role: String? = null,
    val publicUsername: String? = null,
    val bio: String? = null,
    val avatarUrl: String? = null,
    val avatarType: String? = null,
    val avatarCode: String? = null,
)

@Serializable
data class ServerError(
    val error: String,
)

@Serializable
data class ProfileUpdateRequest(
    val publicUsername: String? = null,
    val bio: String? = null,
)

@Serializable
data class AvatarEmojiRequest(
    val code: String,
)
