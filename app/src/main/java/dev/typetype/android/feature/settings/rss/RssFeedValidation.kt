package dev.typetype.android.feature.settings.rss

import dev.typetype.android.R
import dev.typetype.android.domain.rss.RssFeedScope

internal fun RssFeedEditorState.validationError(): Int? = when {
    name.trim().length !in 1..100 -> R.string.rss_error_name
    serviceIds.isEmpty() -> R.string.rss_error_services
    !includeVideos && !includeShorts && !includeLive && !includeUpcoming ->
        R.string.rss_error_content_types
    scope == RssFeedScope.Channels && channelUrls.size !in 1..100 ->
        R.string.rss_error_channels
    else -> null
}
