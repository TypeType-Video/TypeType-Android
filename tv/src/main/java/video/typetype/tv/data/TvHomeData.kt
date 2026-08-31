package video.typetype.tv.data

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import video.typetype.sdk.core.ChannelRequest
import video.typetype.sdk.core.ServiceId
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
    val channelVideosDeferred = async { loadFeaturedChannelVideos(service) }
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
    val channelVideos = channelVideosDeferred.await()
    val shortsResult = shortsDeferred.await()
    val subscriptionsResult = subscriptionsDeferred.await()
    val groupsResult = groupsDeferred.await()
    val feedResult = feedDeferred.await()
    val errors = listOfNotNull(
        metadataResult, homeResult, trendingResult, shortsResult,
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
    val serverHome = (homeResult as? TypeTypeResult.Success)?.value?.items?.visibleWith(settings)
        ?: if (settings.hideHomeRecommendations) emptyList() else current.home
    val featuredHome = channelVideos.plus(serverHome).distinctBy { it.serviceId to it.id.value }.take(24)
    mutableState.value = current.copy(
        metadata = (metadataResult as? TypeTypeResult.Success)?.value ?: current.metadata,
        home = featuredHome,
        trending = (trendingResult as? TypeTypeResult.Success)?.value?.visibleWith(settings) ?: current.trending,
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

private suspend fun TvViewModel.loadFeaturedChannelVideos(service: ServiceId): List<video.typetype.sdk.core.Video> {
    if (service != ServiceId.YOUTUBE) return emptyList()
    return FEATURED_CHANNEL_URLS.flatMap { channelUrl ->
        when (val result = client.catalog.channel(ChannelRequest(channelUrl))) {
            is TypeTypeResult.Success -> result.value.videos
                .filter { it.durationSeconds > 0L && !it.isLive && !it.isLiveContent && !it.isShortFormContent }
            is TypeTypeResult.Failure -> emptyList()
        }
    }.shuffled().distinctBy { it.id.value }.take(MAX_FEATURED_CHANNEL_VIDEOS)
}

private fun List<video.typetype.sdk.core.Video>.visibleWith(
    settings: video.typetype.sdk.core.UserSettings,
): List<video.typetype.sdk.core.Video> = filter { video ->
    (!settings.hideSubscriptionLiveStreams || !video.isLive) &&
        (!settings.hideMembersOnlyContent || !video.requiresMembership)
}

private val FEATURED_CHANNEL_URLS = listOf(
    "https://www.youtube.com/@MrBeast",
    "https://www.youtube.com/@cyprien",
    "https://www.youtube.com/@Squeezie",
    "https://www.youtube.com/@markiplier",
)

private const val MAX_FEATURED_CHANNEL_VIDEOS = 24
