package dev.typetype.android.data.profile

import dev.typetype.android.data.network.TypeTypeApiHolder
import dev.typetype.android.data.network.dto.AvatarEmojiRequest
import dev.typetype.android.data.network.dto.ProfileUpdateRequest
import dev.typetype.android.data.network.dto.UserProfile
import dev.typetype.android.data.network.extractServerErrorMessage
import dev.typetype.android.domain.profile.Profile
import dev.typetype.android.domain.profile.ProfileRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

@Singleton
class RemoteProfileRepository @Inject constructor(
    private val apiHolder: TypeTypeApiHolder,
) : ProfileRepository {

    private val state = MutableStateFlow<Profile?>(null)

    override fun observe(): Flow<Profile?> = state.asStateFlow()

    override suspend fun refresh(): Result<Unit> = runCatching {
        val response = withContext(Dispatchers.IO) { apiHolder.require().me() }
        if (!response.isSuccessful) error(extractServerErrorMessage(response))
        state.value = response.body()?.toDomain()
    }

    override suspend fun updateProfile(publicUsername: String?, bio: String?): Result<Unit> = runCatching {
        val response = withContext(Dispatchers.IO) {
            apiHolder.require().updateProfile(
                ProfileUpdateRequest(publicUsername = publicUsername, bio = bio),
            )
        }
        if (!response.isSuccessful) error(extractServerErrorMessage(response))
        refresh()
    }

    override suspend fun setAvatarEmoji(code: String): Result<Unit> = runCatching {
        val response = withContext(Dispatchers.IO) {
            apiHolder.require().setAvatarEmoji(AvatarEmojiRequest(code = code))
        }
        if (!response.isSuccessful) error(extractServerErrorMessage(response))
        refresh()
    }

    override suspend fun clearAvatar(): Result<Unit> = runCatching {
        val response = withContext(Dispatchers.IO) { apiHolder.require().clearAvatar() }
        if (!response.isSuccessful) error(extractServerErrorMessage(response))
        refresh()
    }

    private fun UserProfile.toDomain(): Profile = Profile(
        id = id,
        role = role.orEmpty(),
        publicUsername = publicUsername.orEmpty(),
        bio = bio.orEmpty(),
        avatarUrl = avatarUrl.orEmpty(),
        avatarType = avatarType.orEmpty(),
        avatarCode = avatarCode.orEmpty(),
    )
}
