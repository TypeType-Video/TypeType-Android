package dev.typetype.android.feature.player.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import dev.typetype.android.domain.stream.Chapter
import dev.typetype.android.domain.stream.SponsorCategory
import dev.typetype.android.feature.player.SponsorBlockPlaybackPolicy

@Composable
internal fun rememberPlaybackChapters(
    streamChapters: List<Chapter>,
    policy: SponsorBlockPlaybackPolicy,
): List<Chapter> {
    val categoryLabels = SponsorCategory.entries.associateWith {
        stringResource(it.labelResource())
    }
    return remember(streamChapters, policy, categoryLabels) {
        playbackChapters(streamChapters, policy, categoryLabels::getValue)
    }
}

internal fun playbackChapters(
    streamChapters: List<Chapter>,
    policy: SponsorBlockPlaybackPolicy,
    categoryLabel: (SponsorCategory) -> String,
): List<Chapter> = when {
    streamChapters.isNotEmpty() -> streamChapters
    !policy.showChapters -> emptyList()
    else -> policy.visibleSegments
        .sortedBy { it.startMs }
        .distinctBy { it.startMs }
        .map { segment ->
            Chapter(
                title = categoryLabel(segment.category),
                startMs = segment.startMs,
                previewUrl = null,
            )
        }
}
