package dev.typetype.android.domain.rss

data class RssFeed(
    val id: String,
    val name: String,
    val scope: RssFeedScope,
    val channelUrls: List<String>,
    val serviceIds: Set<Int>,
    val includeVideos: Boolean,
    val includeShorts: Boolean,
    val includeLive: Boolean,
    val includeUpcoming: Boolean,
    val enabled: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val lastUsedAt: Long?,
)

enum class RssFeedScope(val wireValue: String) {
    All("all"),
    Channels("channels");

    companion object {
        fun fromWireValue(value: String): RssFeedScope =
            entries.firstOrNull { it.wireValue == value } ?: All
    }
}
