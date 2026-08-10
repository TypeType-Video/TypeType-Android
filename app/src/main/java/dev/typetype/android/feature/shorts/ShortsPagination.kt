package dev.typetype.android.feature.shorts

import dev.typetype.android.domain.feed.ShortsContinuation
import dev.typetype.android.domain.feed.ShortsPage
import dev.typetype.android.domain.feed.Video
import dev.typetype.android.domain.feed.shortIdentity

internal data class ShortsPageMerge(
    val additions: List<Video>,
    val continuation: ShortsContinuation?,
)

internal fun mergeShortsPage(
    knownIdentities: MutableSet<String>,
    requestedContinuation: ShortsContinuation,
    page: ShortsPage,
): ShortsPageMerge {
    val additions = page.videos.filter { knownIdentities.add(it.shortIdentity()) }
    return ShortsPageMerge(
        additions = additions,
        continuation = page.continuation.takeUnless { it == requestedContinuation },
    )
}
