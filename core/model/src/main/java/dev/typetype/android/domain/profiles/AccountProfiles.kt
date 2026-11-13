package dev.typetype.android.domain.profiles

data class AccountProfile(
    val id: String,
    val name: String,
    val isActive: Boolean,
    val isDefault: Boolean,
    val avatarUrl: String?,
)

data class AccountProfiles(
    val profiles: List<AccountProfile>,
    val activeProfileId: String,
    val defaultProfileId: String,
)

interface AccountProfilesRepository {
    suspend fun profiles(): Result<AccountProfiles>
    suspend fun create(name: String): Result<Unit>
    suspend fun rename(profileId: String, name: String): Result<Unit>
    suspend fun setDefault(profileId: String): Result<Unit>
    suspend fun delete(profileId: String): Result<Unit>
    suspend fun switchTo(profileId: String): Result<Unit>
}
