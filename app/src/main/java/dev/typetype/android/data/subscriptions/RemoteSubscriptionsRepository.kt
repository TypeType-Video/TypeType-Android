package dev.typetype.android.data.subscriptions

import dev.typetype.android.data.network.TypeTypeApiHolder
import dev.typetype.android.data.network.dto.SubscriptionItemDto
import dev.typetype.android.data.network.extractServerErrorMessage
import dev.typetype.android.domain.subscriptions.SubscriptionSummary
import dev.typetype.android.domain.subscriptions.SubscriptionsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

@Singleton
class RemoteSubscriptionsRepository @Inject constructor(
    private val apiHolder: TypeTypeApiHolder,
) : SubscriptionsRepository {

    private val state = MutableStateFlow<Set<String>>(emptySet())

    override fun observeSubscribedChannelUrls(): Flow<Set<String>> = state.asStateFlow()

    override suspend fun refresh(): Result<Unit> = runCatching {
        val response = withContext(Dispatchers.IO) { apiHolder.require().subscriptions() }
        if (!response.isSuccessful) error(extractServerErrorMessage(response))
        state.value = response.body().orEmpty().map { it.channelUrl }.toSet()
    }

    override suspend fun listSubscriptions(): Result<List<SubscriptionSummary>> = runCatching {
        val response = withContext(Dispatchers.IO) { apiHolder.require().subscriptions() }
        if (!response.isSuccessful) error(extractServerErrorMessage(response))
        response.body().orEmpty().map { SubscriptionSummary(channelUrl = it.channelUrl) }
    }

    override suspend fun subscribe(
        channelUrl: String,
        name: String,
        avatarUrl: String,
    ): Result<Unit> = runCatching {
        val response = withContext(Dispatchers.IO) {
            apiHolder.require().subscribe(
                SubscriptionItemDto(channelUrl = channelUrl, name = name, avatarUrl = avatarUrl),
            )
        }
        if (!response.isSuccessful) error(extractServerErrorMessage(response))
        state.update { it + channelUrl }
    }

    override suspend fun unsubscribe(channelUrl: String): Result<Unit> = runCatching {
        val response = withContext(Dispatchers.IO) { apiHolder.require().unsubscribe(channelUrl) }
        if (!response.isSuccessful) error(extractServerErrorMessage(response))
        state.update { it - channelUrl }
    }

    override suspend fun unsubscribeAll(): Result<Unit> = runCatching {
        val subscriptions = listSubscriptions().getOrThrow()
        for (subscription in subscriptions) {
            unsubscribe(subscription.channelUrl).getOrThrow()
        }
        state.value = emptySet()
    }
}
