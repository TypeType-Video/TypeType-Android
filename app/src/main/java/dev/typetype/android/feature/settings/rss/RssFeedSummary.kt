package dev.typetype.android.feature.settings.rss

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.typetype.android.R
import dev.typetype.android.domain.rss.RssFeed

@Composable
internal fun RssFeedSummary(feed: RssFeed) {
    val serviceNames = mapOf(
        0 to stringResource(R.string.settings_default_service_youtube),
        5 to stringResource(R.string.settings_default_service_bilibili),
        6 to stringResource(R.string.settings_default_service_niconico),
    )
    val unknownService = stringResource(R.string.rss_service_unknown)
    val services = feed.serviceIds.sorted().joinToString(", ") {
        serviceNames[it] ?: unknownService
    }
    val videos = stringResource(R.string.rss_include_videos)
    val shorts = stringResource(R.string.rss_include_shorts)
    val live = stringResource(R.string.rss_include_live)
    val upcoming = stringResource(R.string.rss_include_upcoming)
    val contentTypes = listOfNotNull(
        videos.takeIf { feed.includeVideos },
        shorts.takeIf { feed.includeShorts },
        live.takeIf { feed.includeLive },
        upcoming.takeIf { feed.includeUpcoming },
    ).joinToString(", ")
    Text(
        text = stringResource(R.string.rss_feed_services, services),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        text = stringResource(R.string.rss_feed_content, contentTypes),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
