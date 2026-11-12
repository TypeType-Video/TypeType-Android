package dev.typetype.android.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class AccountProfileDto(
    val id: String,
    val name: String,
    val isActive: Boolean = false,
    val isDefault: Boolean = false,
    val avatarUrl: String? = null,
)

@Serializable
data class AccountProfilesResponse(
    val profiles: List<AccountProfileDto> = emptyList(),
    val activeProfileId: String,
    val defaultProfileId: String,
)

@Serializable
data class ProfileNameRequest(val name: String)

@Serializable
data class ProfileSwitchResponse(
    val accessToken: String,
    val profile: AccountProfileDto,
)
