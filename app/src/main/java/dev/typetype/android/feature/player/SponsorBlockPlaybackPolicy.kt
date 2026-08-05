package dev.typetype.android.feature.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import dev.typetype.android.domain.stream.SponsorAction
import dev.typetype.android.domain.stream.SponsorBlockSegment
import dev.typetype.android.domain.stream.SponsorCategory
import dev.typetype.android.domain.stream.Stream
import dev.typetype.android.domain.usersettings.SponsorBlockMode
import dev.typetype.android.domain.usersettings.UserSettings

internal data class SponsorBlockPlaybackPolicy(
    val visibleSegments: List<SponsorBlockSegment> = emptyList(),
    val automaticSegments: List<SponsorBlockSegment> = emptyList(),
    val manualSegments: List<SponsorBlockSegment> = emptyList(),
    val muteInsteadOfSkip: Boolean = false,
    val showCurrentSegment: Boolean = false,
    val showChapters: Boolean = false,
) {
    fun canManuallySkip(segment: SponsorBlockSegment): Boolean =
        segment in manualSegments || segment !in automaticSegments || muteInsteadOfSkip
}

internal fun Stream.sponsorBlockPlaybackPolicy(
    settings: UserSettings,
): SponsorBlockPlaybackPolicy {
    if (settings.sponsorBlockMode == SponsorBlockMode.Disabled) {
        return SponsorBlockPlaybackPolicy()
    }
    val durationMs = durationSeconds.coerceAtLeast(0L) * 1_000L
    val minimumDurationMs = settings.sponsorBlockMinimumDuration.coerceAtLeast(0) * 1_000L
    val visible = sponsorBlockSegments.filter { segment ->
        val action = settings.sponsorBlockCategoryActions[segment.category.key]
            ?: SponsorBlockMode.MarkOnly
        action != SponsorBlockMode.Disabled &&
            segment.endMs - segment.startMs >= minimumDurationMs &&
            (settings.sponsorBlockShowFullVideoLabels || !segment.isFullVideo(durationMs))
    }
    val automatic = if (settings.sponsorBlockMode == SponsorBlockMode.AutoSkip) {
        visible.filter { segment ->
            segment.action == SponsorAction.Skip &&
                settings.sponsorBlockCategoryActions[segment.category.key] ==
                SponsorBlockMode.AutoSkip &&
                (!settings.sponsorBlockManualSkipOnFullVideo || !segment.isFullVideo(durationMs)) &&
                segment.appliesToCategory(settings, category)
        }
    } else {
        emptyList()
    }
    val manual = if (settings.sponsorBlockManualSkipOnFullVideo) {
        visible.filter { it.isFullVideo(durationMs) }
    } else {
        emptyList()
    }
    return SponsorBlockPlaybackPolicy(
        visibleSegments = visible,
        automaticSegments = automatic,
        manualSegments = manual,
        muteInsteadOfSkip = settings.sponsorBlockMuteInsteadOfSkip,
        showCurrentSegment = settings.sponsorBlockShowCurrentSegment,
        showChapters = settings.sponsorBlockShowChapters,
    )
}

@Composable
internal fun rememberSponsorBlockPlaybackPolicy(
    stream: Stream,
    settings: UserSettings,
): SponsorBlockPlaybackPolicy = remember(stream, settings) {
    stream.sponsorBlockPlaybackPolicy(settings)
}

private fun SponsorBlockSegment.isFullVideo(durationMs: Long): Boolean =
    durationMs > 0L && startMs <= 5_000L && endMs >= durationMs * 9L / 10L

private fun SponsorBlockSegment.appliesToCategory(
    settings: UserSettings,
    streamCategory: String?,
): Boolean = category != SponsorCategory.MusicOffTopic ||
    !settings.sponsorBlockSkipNonMusicOnlyOnMusicVideos ||
    streamCategory.equals("music", ignoreCase = true)
