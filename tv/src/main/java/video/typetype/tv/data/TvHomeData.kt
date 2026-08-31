package video.typetype.tv.data

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import video.typetype.sdk.core.SearchRequest
import video.typetype.sdk.core.TypeTypeError
import video.typetype.sdk.core.TypeTypeResult

internal suspend fun TvViewModel.loadAuthenticatedContent(): Unit = coroutineScope {
    loadUserSettings()
    launch { loadHomeContent() }
    launch { loadLibraryContent(showLoading = false) }
    launch { loadProfile() }
}

internal suspend fun TvViewModel.loadProfile() {
    when (val result = client.profile.profile()) {
        is TypeTypeResult.Success -> mutableState.value = mutableState.value.copy(profile = result.value)
        is TypeTypeResult.Failure -> mutableState.value = mutableState.value.copy(
            errorMessage = result.error.toUserMessage(),
        )
    }
}

internal suspend fun TvViewModel.loadHomeContent(): Unit = coroutineScope {
    val snapshot = mutableState.value
    val service = snapshot.selectedService
    val metadataDeferred = async {
        if (snapshot.metadata == null) client.instance.metadata() else null
    }
    val settings = snapshot.settings
    val homeDeferred = async {
        if (settings.hideHomeRecommendations) null else client.recommendations.home(service, limit = 24)
    }
    val trendingDeferred = async { client.catalog.trending(service) }
    val bunnyDeferred = async { client.catalog.search(SearchRequest("Big Buck Bunny", service)) }
    val shortsDeferred = async {
        if (settings.hideShorts) null else client.recommendations.shorts(service, limit = 20)
    }
    val authenticated = snapshot.authStatus == TvAuthStatus.AUTHENTICATED
    val subscriptionsDeferred = async { if (authenticated) client.subscriptions.groupMemberships() else null }
    val groupsDeferred = async { if (authenticated) client.subscriptions.groups() else null }
    val feedDeferred = async {
        if (authenticated) client.subscriptions.feed(groupId = snapshot.selectedSubscriptionGroupId, limit = 24) else null
    }

    val metadataResult = metadataDeferred.await()
    val homeResult = homeDeferred.await()
    val trendingResult = trendingDeferred.await()
    val bunnyResult = bunnyDeferred.await()
    val shortsResult = shortsDeferred.await()
    val subscriptionsResult = subscriptionsDeferred.await()
    val groupsResult = groupsDeferred.await()
    val feedResult = feedDeferred.await()
    val errors = listOfNotNull(
        metadataResult, homeResult, trendingResult, bunnyResult, shortsResult,
        subscriptionsResult, groupsResult, feedResult,
    ).mapNotNull { (it as? TypeTypeResult.Failure)?.error }
    if (errors.firstOrNull() is TypeTypeError.Authentication) {
        client.sessions.clear()
        mutableState.value = TvAppState(
            authStatus = TvAuthStatus.SIGNED_OUT,
            metadata = mutableState.value.metadata,
            isLoading = false,
            errorMessage = errors.first().toUserMessage(),
        )
        return@coroutineScope
    }
    val current = mutableState.value
    val bunnyPage = bunnyResult as? TypeTypeResult.Success
    val bunny = bunnyPage?.value?.videos
        ?.firstOrNull { it.title.contains("big buck bunny", ignoreCase = true) }
    mutableState.value = current.copy(
        metadata = (metadataResult as? TypeTypeResult.Success)?.value ?: current.metadata,
        home = (homeResult as? TypeTypeResult.Success)?.value?.items?.visibleWith(settings)
            ?: if (settings.hideHomeRecommendations) emptyList() else current.home,
        trending = (trendingResult as? TypeTypeResult.Success)?.value?.visibleWith(settings) ?: current.trending,
        bigBuckBunny = if (bunnyPage != null) bunny else current.bigBuckBunny,
        shorts = (shortsResult as? TypeTypeResult.Success)?.value?.items?.visibleWith(settings)
            ?: if (settings.hideShorts) emptyList() else current.shorts,
        subscriptions = (subscriptionsResult as? TypeTypeResult.Success)?.value ?: current.subscriptions,
        subscriptionGroups = (groupsResult as? TypeTypeResult.Success)?.value ?: current.subscriptionGroups,
        subscriptionFeed = (feedResult as? TypeTypeResult.Success)?.value?.items?.visibleWith(settings)
            ?: current.subscriptionFeed,
        isLoading = false,
        errorMessage = errors.firstOrNull()?.toUserMessage(),
    )
}

private fun List<video.typetype.sdk.core.Video>.visibleWith(
    settings: video.typetype.sdk.core.UserSettings,
): List<video.typetype.sdk.core.Video> = filter { video ->
    (!settings.hideSubscriptionLiveStreams || !video.isLive) &&
        (!settings.hideMembersOnlyContent || !video.requiresMembership)
}
