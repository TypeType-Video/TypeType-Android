package dev.typetype.android.feature.search

import dev.typetype.android.domain.search.SearchPage

internal fun SearchPage.nextSearchCursor(requestedCursor: String? = null): String? {
    if (videos.isEmpty() && channels.isEmpty() && playlists.isEmpty()) return null
    return nextPage?.takeUnless { it == requestedCursor }
}
