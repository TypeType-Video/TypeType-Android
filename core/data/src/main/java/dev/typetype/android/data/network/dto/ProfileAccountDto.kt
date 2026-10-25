package dev.typetype.android.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class CustomAvatarDto(
    val avatarUrl: String,
    val mediaType: String,
    val size: Int,
)

@Serializable
data class AccountIdentityDto(
    val email: String,
    val name: String,
    val managedByOidc: Boolean,
)

@Serializable
data class AccountIdentityUpdateRequest(
    val email: String,
    val name: String,
    val currentPassword: String,
)
