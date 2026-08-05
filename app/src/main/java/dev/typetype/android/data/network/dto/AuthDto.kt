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
data class RegisterStatusResponse(
    val allowRegistration: Boolean,
    val bootstrapAvailable: Boolean,
    val localLoginEnabled: Boolean = true,
)

@Serializable
data class RefreshRequest(
    val token: String? = null,
)

@Serializable
data class ResetPasswordRequest(
    val resetToken: String,
    val newPassword: String,
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
data class OidcStartResponse(
    val authorizationUrl: String,
)

@Serializable
data class OidcStatusResponse(
    val enabled: Boolean,
    val providerName: String? = null,
    val localLoginEnabled: Boolean,
    val autoRedirect: Boolean,
)

@Serializable
data class OidcCallbackRequest(
    val code: String,
    val state: String,
    val redirectUri: String,
)

@Serializable
data class OidcCallbackResponse(
    val accessToken: String,
    val returnTo: String,
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
