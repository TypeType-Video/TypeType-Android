package dev.typetype.android.feature.settings.rss

import dev.typetype.android.domain.rss.RssFeed
import dev.typetype.android.domain.rss.RssFeedDraft
import dev.typetype.android.domain.rss.RssFeedScope
import dev.typetype.android.domain.server.RssCapability
import dev.typetype.android.domain.subscriptions.SubscriptionSummary

data class RssFeedsState(
    val capability: RssCapability = RssCapability(),
    val availableServiceIds: Set<Int> = emptySet(),
    val feeds: List<RssFeed> = emptyList(),
    val subscriptions: List<SubscriptionSummary> = emptyList(),
    val isLoading: Boolean = true,
    val hasLoadedFeeds: Boolean = false,
    val isMutating: Boolean = false,
    val editor: RssFeedEditorState? = null,
    val regeneratingFeedId: String? = null,
    val deletingFeedId: String? = null,
    val secret: RssFeedSecretState? = null,
    val errorMessage: String? = null,
    val errorRequestId: String? = null,
) {
    val canCreate: Boolean
        get() = capability.enabled && hasLoadedFeeds && feeds.size < capability.maxFeedsPerUser
}

data class RssFeedSecretState(
    val feedName: String,
    val url: String,
)

data class RssFeedEditorState(
    val feedId: String? = null,
    val name: String = "",
    val scope: RssFeedScope = RssFeedScope.All,
    val channelUrls: Set<String> = emptySet(),
    val serviceIds: Set<Int> = emptySet(),
    val includeVideos: Boolean = true,
    val includeShorts: Boolean = true,
    val includeLive: Boolean = true,
    val includeUpcoming: Boolean = true,
) {
    fun toDraft() = RssFeedDraft(
        name = name,
        scope = scope,
        channelUrls = channelUrls,
        serviceIds = serviceIds,
        includeVideos = includeVideos,
        includeShorts = includeShorts,
        includeLive = includeLive,
        includeUpcoming = includeUpcoming,
    )
}

internal fun RssFeed.toEditorState() = RssFeedEditorState(
    feedId = id,
    name = name,
    scope = scope,
    channelUrls = channelUrls.toSet(),
    serviceIds = serviceIds,
    includeVideos = includeVideos,
    includeShorts = includeShorts,
    includeLive = includeLive,
    includeUpcoming = includeUpcoming,
)
