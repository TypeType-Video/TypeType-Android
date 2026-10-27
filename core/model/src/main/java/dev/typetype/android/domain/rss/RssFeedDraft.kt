package dev.typetype.android.domain.rss

data class RssFeedDraft(
    val name: String,
    val scope: RssFeedScope = RssFeedScope.All,
    val channelUrls: Set<String> = emptySet(),
    val serviceIds: Set<Int> = setOf(0, 5, 6),
    val includeVideos: Boolean = true,
    val includeShorts: Boolean = true,
    val includeLive: Boolean = true,
    val includeUpcoming: Boolean = true,
)

data class RssFeedSecret(
    val feed: RssFeed,
    val url: String,
)
