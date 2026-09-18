package dev.typetype.android.feature.subscriptions

import dev.typetype.android.domain.feed.Video

internal fun mergeSubscriptionFirstPage(
    loadedVideos: List<Video>,
    firstPage: List<Video>,
): List<Video> {
    val refreshedUrls = firstPage.mapTo(mutableSetOf()) { video -> video.url }
    return (firstPage + loadedVideos.filterNot { video -> video.url in refreshedUrls })
        .distinctBy { video -> video.url }
}
