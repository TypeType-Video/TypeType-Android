package dev.typetype.android.data.subscriptions

import dev.typetype.android.core.error.suspendRunCatching
import dev.typetype.android.data.account.ActiveAccountScope
import dev.typetype.android.data.network.TypeTypeApi
import dev.typetype.android.data.network.TypeTypeApiHolder
import dev.typetype.android.data.network.dto.SubscriptionGroupDto
import dev.typetype.android.data.network.dto.SubscriptionGroupMembershipRequest
import dev.typetype.android.data.network.dto.SubscriptionGroupNameRequest
import dev.typetype.android.data.network.requireSuccessfulResponse
import dev.typetype.android.domain.subscriptions.SubscriptionGroup
import dev.typetype.android.domain.subscriptions.SubscriptionGroupsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

@Singleton
class RemoteSubscriptionGroupsRepository @Inject constructor(
    private val apiHolder: TypeTypeApiHolder,
    private val activeAccountScope: ActiveAccountScope,
) : SubscriptionGroupsRepository {
    override suspend fun groups(): Result<List<SubscriptionGroup>> = call { api ->
        api.subscriptionGroups().bodyOrFail("Empty subscription groups body")
            .map(SubscriptionGroupDto::toDomain)
            .sortedBy { it.name.lowercase() }
    }

    override suspend fun create(name: String): Result<SubscriptionGroup> = call { api ->
        api.createSubscriptionGroup(SubscriptionGroupNameRequest(name.trim()))
            .bodyOrFail("Empty subscription group body")
            .toDomain()
    }

    override suspend fun rename(groupId: String, name: String): Result<Unit> = call { api ->
        api.renameSubscriptionGroup(groupId, SubscriptionGroupNameRequest(name.trim())).requireSuccessfulResponse()
    }

    override suspend fun delete(groupId: String): Result<Unit> = call { api ->
        api.deleteSubscriptionGroup(groupId).requireSuccessfulResponse()
    }

    override suspend fun groupIdsOf(channelUrl: String): Result<Set<String>> = call { api ->
        api.lookupSubscriptionGroupMemberships(SubscriptionGroupMembershipRequest(channelUrl))
            .bodyOrFail("Empty membership lookup body")
            .flatMap { it.groupIds }
            .toSet()
    }

    override suspend fun setMember(groupId: String, channelUrl: String, member: Boolean): Result<Unit> =
        call { api ->
            val response = if (member) {
                api.addSubscriptionGroupChannel(groupId, SubscriptionGroupMembershipRequest(channelUrl))
            } else {
                api.removeSubscriptionGroupChannel(groupId, channelUrl)
            }
            response.requireSuccessfulResponse()
        }

    private suspend fun <T> call(block: suspend (TypeTypeApi) -> T): Result<T> = suspendRunCatching {
        val scope = activeAccountScope.require()
        val value = withContext(Dispatchers.IO) { block(apiHolder.require(scope)) }
        activeAccountScope.verify(scope)
        value
    }
}

private fun <T> Response<T>.bodyOrFail(message: String): T {
    requireSuccessfulResponse()
    return body() ?: error(message)
}

private fun SubscriptionGroupDto.toDomain() = SubscriptionGroup(id = id, name = name, channelCount = channelCount)
