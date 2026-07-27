package dev.typetype.android.data.profile

import dev.typetype.android.data.account.AccountScopedValue
import dev.typetype.android.data.account.AccountDao
import dev.typetype.android.data.account.AccountEntity
import dev.typetype.android.data.account.ActiveAccountScope
import dev.typetype.android.data.network.TypeTypeApiHolder
import dev.typetype.android.data.network.dto.AvatarEmojiRequest
import dev.typetype.android.data.network.dto.ProfileUpdateRequest
import dev.typetype.android.data.network.dto.UserProfile
import dev.typetype.android.data.network.requireSuccessfulResponse
import dev.typetype.android.domain.profile.Profile
import dev.typetype.android.domain.profile.ProfileRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

@Singleton
class RemoteProfileRepository @Inject constructor(
    private val apiHolder: TypeTypeApiHolder,
    private val activeAccountScope: ActiveAccountScope,
    private val accountDao: AccountDao,
) : ProfileRepository {

    private val state = MutableStateFlow<AccountScopedValue<Profile>?>(null)

    override fun observe(): Flow<Profile?> = combine(activeAccountScope.observe(), state) { scope, cached ->
        cached?.value?.takeIf { scope == cached.scope }
    }

    override suspend fun refresh(): Result<Unit> = runCatching {
        val scope = activeAccountScope.require()
        val response = withContext(Dispatchers.IO) { apiHolder.require(scope).me() }
        response.requireSuccessfulResponse()
        val body = response.body() ?: error("The instance returned an empty profile")
        val profile = body.toDomain()
        activeAccountScope.verify(scope)
        val saved = accountDao.get(scope.serverId, scope.accountId)
        if (saved != null) {
            accountDao.upsert(
                AccountEntity.fromProfile(
                    serverId = scope.serverId,
                    profile = body,
                    sessionGeneration = saved.sessionGeneration,
                ),
            )
        }
        state.value = AccountScopedValue(scope, profile)
    }

    override suspend fun updateProfile(publicUsername: String?, bio: String?): Result<Unit> = runCatching {
        val scope = activeAccountScope.require()
        val response = withContext(Dispatchers.IO) {
            apiHolder.require(scope).updateProfile(
                ProfileUpdateRequest(publicUsername = publicUsername, bio = bio),
            )
        }
        response.requireSuccessfulResponse()
        activeAccountScope.verify(scope)
        refresh()
    }

    override suspend fun setAvatarEmoji(code: String): Result<Unit> = runCatching {
        val scope = activeAccountScope.require()
        val response = withContext(Dispatchers.IO) {
            apiHolder.require(scope).setAvatarEmoji(AvatarEmojiRequest(code = code))
        }
        response.requireSuccessfulResponse()
        activeAccountScope.verify(scope)
        refresh()
    }

    override suspend fun clearAvatar(): Result<Unit> = runCatching {
        val scope = activeAccountScope.require()
        val response = withContext(Dispatchers.IO) { apiHolder.require(scope).clearAvatar() }
        response.requireSuccessfulResponse()
        activeAccountScope.verify(scope)
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
