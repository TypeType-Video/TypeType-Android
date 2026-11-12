package dev.typetype.android.data.allowlist

import dev.typetype.android.core.error.suspendRunCatching
import dev.typetype.android.data.account.ActiveAccountScope
import dev.typetype.android.data.network.TypeTypeApiHolder
import dev.typetype.android.data.network.dto.AllowedChannelDto
import dev.typetype.android.data.network.requireSuccessfulResponse
import dev.typetype.android.domain.allowlist.AllowedChannelsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class RemoteAllowedChannelsRepository @Inject constructor(
    private val apiHolder: TypeTypeApiHolder,
    private val activeAccountScope: ActiveAccountScope,
) : AllowedChannelsRepository {
    override suspend fun allowedChannelUrls(): Result<Set<String>> = suspendRunCatching {
        val scope = activeAccountScope.require()
        val response = withContext(Dispatchers.IO) { apiHolder.require(scope).allowedChannels() }
        response.requireSuccessfulResponse()
        activeAccountScope.verify(scope)
        response.body().orEmpty().mapTo(HashSet()) { it.url }
    }

    // The server only honours "global" for admins and moderators, who manage the instance family list.
    override suspend fun allowForEveryone(channelUrl: String, name: String, avatarUrl: String?): Result<Unit> =
        suspendRunCatching {
            val scope = activeAccountScope.require()
            val body = AllowedChannelDto(url = channelUrl, name = name, thumbnailUrl = avatarUrl, global = true)
            val response = withContext(Dispatchers.IO) { apiHolder.require(scope).allowChannel(body) }
            response.requireSuccessfulResponse()
            activeAccountScope.verify(scope)
        }
}
