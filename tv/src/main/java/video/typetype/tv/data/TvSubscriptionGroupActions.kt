package video.typetype.tv.data

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import video.typetype.sdk.core.Subscription
import video.typetype.sdk.core.SubscriptionGroup
import video.typetype.sdk.core.TypeTypeResult

public fun TvViewModel.selectSubscriptionGroup(groupId: String?) {
    if (groupId == mutableState.value.selectedSubscriptionGroupId) return
    val previous = mutableState.value.selectedSubscriptionGroupId
    mutableState.value = mutableState.value.copy(
        selectedSubscriptionGroupId = groupId,
        isActionInProgress = true,
        errorMessage = null,
    )
    viewModelScope.launch {
        when (val result = client.subscriptions.feed(groupId = groupId, limit = 24)) {
            is TypeTypeResult.Success -> mutableState.value = mutableState.value.copy(
                subscriptionFeed = result.value.items,
                isActionInProgress = false,
            )
            is TypeTypeResult.Failure -> mutableState.value = mutableState.value.copy(
                selectedSubscriptionGroupId = previous,
                isActionInProgress = false,
                errorMessage = result.error.toUserMessage(),
            )
        }
    }
}

public fun TvViewModel.createSubscriptionGroup(name: String) {
    if (name.isBlank() || !beginAuthenticatedAction()) return
    viewModelScope.launch {
        when (val result = client.subscriptions.createGroup(name.trim())) {
            is TypeTypeResult.Success -> refreshSubscriptionGroups(result.value.id)
            is TypeTypeResult.Failure -> finishAction(result.error.toUserMessage())
        }
    }
}

public fun TvViewModel.renameSubscriptionGroup(group: SubscriptionGroup, name: String) {
    if (name.isBlank() || !beginAuthenticatedAction()) return
    viewModelScope.launch {
        when (val result = client.subscriptions.renameGroup(group.id, name.trim())) {
            is TypeTypeResult.Success -> refreshSubscriptionGroups(mutableState.value.selectedSubscriptionGroupId)
            is TypeTypeResult.Failure -> finishAction(result.error.toUserMessage())
        }
    }
}

public fun TvViewModel.deleteSubscriptionGroup(group: SubscriptionGroup) {
    if (!beginAuthenticatedAction()) return
    viewModelScope.launch {
        when (val result = client.subscriptions.deleteGroup(group.id)) {
            is TypeTypeResult.Success -> refreshSubscriptionGroups(
                mutableState.value.selectedSubscriptionGroupId.takeUnless { it == group.id },
            )
            is TypeTypeResult.Failure -> finishAction(result.error.toUserMessage())
        }
    }
}

public fun TvViewModel.toggleSubscriptionGroupChannel(group: SubscriptionGroup, subscription: Subscription) {
    if (!beginAuthenticatedAction()) return
    viewModelScope.launch {
        val result = if (group.id in subscription.groupIds) {
            client.subscriptions.removeFromGroup(group.id, listOf(subscription.channelUrl))
        } else {
            client.subscriptions.addToGroup(group.id, listOf(subscription.channelUrl))
        }
        when (result) {
            is TypeTypeResult.Success -> refreshSubscriptionGroups(mutableState.value.selectedSubscriptionGroupId)
            is TypeTypeResult.Failure -> finishAction(result.error.toUserMessage())
        }
    }
}

private suspend fun TvViewModel.refreshSubscriptionGroups(selectedGroupId: String?): Unit = coroutineScope {
    val groupsDeferred = async { client.subscriptions.groups() }
    val subscriptionsDeferred = async { client.subscriptions.groupMemberships() }
    val feedDeferred = async { client.subscriptions.feed(groupId = selectedGroupId, limit = 24) }
    val groups = groupsDeferred.await()
    val subscriptions = subscriptionsDeferred.await()
    val feed = feedDeferred.await()
    val error = listOf(groups, subscriptions, feed)
        .mapNotNull { (it as? TypeTypeResult.Failure)?.error }
        .firstOrNull()
    if (error != null) {
        finishAction(error.toUserMessage())
        return@coroutineScope
    }
    if (groups is TypeTypeResult.Success &&
        subscriptions is TypeTypeResult.Success &&
        feed is TypeTypeResult.Success
    ) {
        mutableState.value = mutableState.value.copy(
            subscriptionGroups = groups.value,
            subscriptions = subscriptions.value,
            subscriptionFeed = feed.value.items,
            selectedSubscriptionGroupId = selectedGroupId,
            isActionInProgress = false,
            errorMessage = null,
        )
    }
}
