package dev.typetype.android.feature.subscriptions

import dev.typetype.android.domain.feed.SubscriptionsPage

internal suspend fun rebuildSubscriptionGeneration(
    firstPage: SubscriptionsPage,
    targetVideoCount: Int,
    loadContinuation: suspend (cursor: String, generation: Long) -> Result<SubscriptionsPage>,
): Result<SubscriptionsPage> {
    val seenUrls = mutableSetOf<String>()
    val videos = firstPage.videos.filter { seenUrls.add(it.url) }.toMutableList()
    var nextCursor = firstPage.nextCursor
    var latestPage = firstPage

    while (videos.size < targetVideoCount && nextCursor != null) {
        val requestedCursor = nextCursor
        val page = loadContinuation(requestedCursor, firstPage.generation).getOrElse {
            return Result.failure(it)
        }
        if (page.generation != firstPage.generation) {
            return Result.failure(SubscriptionGenerationChangedException())
        }
        page.videos.filterTo(videos) { seenUrls.add(it.url) }
        latestPage = page
        nextCursor = page.nextCursor
        if (nextCursor == requestedCursor) {
            return Result.failure(SubscriptionCursorDidNotAdvanceException())
        }
    }

    return Result.success(
        firstPage.copy(
            videos = videos,
            nextCursor = nextCursor,
            refreshing = latestPage.refreshing,
        ),
    )
}

private class SubscriptionGenerationChangedException :
    IllegalStateException("Subscription feed generation changed while rebuilding")

private class SubscriptionCursorDidNotAdvanceException :
    IllegalStateException("Subscription feed cursor did not advance")
