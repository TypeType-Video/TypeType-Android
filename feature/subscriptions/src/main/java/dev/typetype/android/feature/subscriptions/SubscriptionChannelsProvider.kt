package dev.typetype.android.feature.subscriptions

import dev.typetype.android.domain.actions.VideoActionsRepository
import dev.typetype.android.domain.actions.BlockedItem
import dev.typetype.android.domain.subscriptions.SubscriptionSummary
import dev.typetype.android.domain.subscriptions.SubscriptionsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class SubscriptionChannelsProvider @Inject constructor(
    private val subscriptionsRepository: SubscriptionsRepository,
    videoActionsRepository: VideoActionsRepository,
) {
    val channels: Flow<List<SubscriptionSummary>> = combine(
        subscriptionsRepository.observeSubscriptions(),
        videoActionsRepository.observeBlockedChannels(),
    ) { subscriptions, blocked ->
        visibleSubscriptionChannels(subscriptions, blocked)
    }

    suspend fun refresh(): Result<Unit> = subscriptionsRepository.refresh()
}

internal fun visibleSubscriptionChannels(
    subscriptions: List<SubscriptionSummary>,
    blocked: List<BlockedItem>,
): List<SubscriptionSummary> = subscriptions
    .filterNot { subscription ->
        blocked.any { item ->
            item.url == subscription.channelUrl ||
                item.name.isNotBlank() && item.name.equals(subscription.name, ignoreCase = true)
        }
    }
    .sortedBy { it.name.lowercase() }
