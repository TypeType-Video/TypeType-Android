package dev.typetype.android.domain.profile

import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun observe(): Flow<Profile?>
    suspend fun refresh(): Result<Unit>
    suspend fun updateProfile(publicUsername: String?, bio: String?): Result<Unit>
    suspend fun uploadCustomAvatar(upload: AvatarUpload): Result<Unit>
    suspend fun setAvatarEmoji(code: String): Result<Unit>
    suspend fun clearAvatar(): Result<Unit>
    suspend fun getAccountIdentity(): Result<AccountIdentity>
    suspend fun updateAccountIdentity(
        email: String,
        name: String,
        currentPassword: String,
    ): Result<Unit>
}
