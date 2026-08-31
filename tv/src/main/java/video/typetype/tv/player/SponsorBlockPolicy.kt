package video.typetype.tv.player

import video.typetype.sdk.core.SponsorBlockMode
import video.typetype.sdk.core.SponsorBlockSegment
import video.typetype.sdk.core.StreamDetails
import video.typetype.sdk.core.UserSettings

internal data class SponsorBlockCategory(
    val id: String,
    val label: String,
    val defaultMode: SponsorBlockMode,
)

internal val SPONSOR_BLOCK_CATEGORIES: List<SponsorBlockCategory> = listOf(
    SponsorBlockCategory("sponsor", "Sponsored message", SponsorBlockMode.AutoSkip),
    SponsorBlockCategory("selfpromo", "Self-promotion", SponsorBlockMode.AutoSkip),
    SponsorBlockCategory("exclusive_access", "Exclusive access", SponsorBlockMode.MarkOnly),
    SponsorBlockCategory("interaction", "Interaction reminder", SponsorBlockMode.AutoSkip),
    SponsorBlockCategory("poi_highlight", "Highlight", SponsorBlockMode.MarkOnly),
    SponsorBlockCategory("intro", "Intermission or intro", SponsorBlockMode.AutoSkip),
    SponsorBlockCategory("outro", "Endcards or credits", SponsorBlockMode.AutoSkip),
    SponsorBlockCategory("preview", "Preview or recap", SponsorBlockMode.AutoSkip),
    SponsorBlockCategory("filler", "Tangents or jokes", SponsorBlockMode.AutoSkip),
    SponsorBlockCategory("chapter", "Chapter", SponsorBlockMode.MarkOnly),
    SponsorBlockCategory("music_offtopic", "Music: non-music", SponsorBlockMode.AutoSkip),
)

internal data class TvSponsorBlockSegment(
    val startMilliseconds: Long,
    val endMilliseconds: Long,
    val category: String,
    val label: String,
    val sourceAction: String,
) {
    val durationMilliseconds: Long get() = endMilliseconds - startMilliseconds
}

internal data class SponsorBlockPolicy(
    val visibleSegments: List<TvSponsorBlockSegment>,
    val autoSkipSegments: List<TvSponsorBlockSegment>,
    val manualSkipSegments: List<TvSponsorBlockSegment>,
    val showCurrentSegment: Boolean,
    val showChapters: Boolean,
    val muteInsteadOfSkip: Boolean,
) {
    fun activeSegment(positionMilliseconds: Long): TvSponsorBlockSegment? =
        visibleSegments.firstOrNull { positionMilliseconds in it.startMilliseconds until it.endMilliseconds }

    fun canManuallySkip(segment: TvSponsorBlockSegment): Boolean =
        segment in manualSkipSegments || segment !in autoSkipSegments || muteInsteadOfSkip

    internal companion object {
        fun create(stream: StreamDetails, settings: UserSettings): SponsorBlockPolicy {
            if (settings.sponsorBlockMode == SponsorBlockMode.Disabled) return EMPTY
            val durationMilliseconds = stream.durationSeconds.coerceAtLeast(0L) * 1_000L
            val normalized = stream.sponsorBlockSegments.mapNotNull {
                it.normalize(durationMilliseconds)
            }
            val visible = normalized.filter { segment ->
                val mode = settings.modeFor(segment.category)
                mode != SponsorBlockMode.Disabled &&
                    segment.durationMilliseconds >= settings.sponsorBlockMinimumDuration.coerceAtLeast(0) * 1_000L &&
                    (settings.sponsorBlockShowFullVideoLabels || !segment.isFullVideo(durationMilliseconds))
            }
            val automatic = if (settings.sponsorBlockMode == SponsorBlockMode.AutoSkip) {
                visible.filter { segment ->
                    settings.modeFor(segment.category) == SponsorBlockMode.AutoSkip &&
                        segment.sourceAction == "skip" &&
                        !(settings.sponsorBlockManualSkipOnFullVideo && segment.isFullVideo(durationMilliseconds)) &&
                        (segment.category != "music_offtopic" ||
                            !settings.sponsorBlockSkipNonMusicOnlyOnMusicVideos || stream.category.equals("music", true))
                }
            } else emptyList()
            val manual = if (settings.sponsorBlockManualSkipOnFullVideo) {
                visible.filter { it.isFullVideo(durationMilliseconds) }
            } else emptyList()
            return SponsorBlockPolicy(
                visibleSegments = visible,
                autoSkipSegments = automatic,
                manualSkipSegments = manual,
                showCurrentSegment = settings.sponsorBlockShowCurrentSegment,
                showChapters = settings.sponsorBlockShowChapters,
                muteInsteadOfSkip = settings.sponsorBlockMuteInsteadOfSkip,
            )
        }

        val EMPTY: SponsorBlockPolicy = SponsorBlockPolicy(emptyList(), emptyList(), emptyList(), false, false, false)
    }
}

private fun UserSettings.modeFor(category: String): SponsorBlockMode =
    sponsorBlockCategoryActions[category]
        ?: SPONSOR_BLOCK_CATEGORIES.firstOrNull { it.id == category }?.defaultMode
        ?: SponsorBlockMode.MarkOnly

private fun SponsorBlockSegment.normalize(durationMilliseconds: Long): TvSponsorBlockSegment? {
    val valuesAreMilliseconds = durationMilliseconds > 0L && endTimeSeconds > durationMilliseconds / 1_000.0 + 30.0
    val multiplier = if (valuesAreMilliseconds) 1.0 else 1_000.0
    val start = (startTimeSeconds * multiplier).toLong().coerceAtLeast(0L)
    val end = (endTimeSeconds * multiplier).toLong().coerceAtMost(durationMilliseconds.takeIf { it > 0L } ?: Long.MAX_VALUE)
    if (end <= start) return null
    return TvSponsorBlockSegment(
        startMilliseconds = start,
        endMilliseconds = end,
        category = category,
        label = SPONSOR_BLOCK_CATEGORIES.firstOrNull { it.id == category }?.label ?: category,
        sourceAction = action,
    )
}

private fun TvSponsorBlockSegment.isFullVideo(durationMilliseconds: Long): Boolean =
    durationMilliseconds > 0L && startMilliseconds <= 5_000L && endMilliseconds >= durationMilliseconds * 9L / 10L

internal fun SponsorBlockPolicy.skipTarget(segment: TvSponsorBlockSegment, durationMilliseconds: Long): Long =
    if (durationMilliseconds > 0L && segment.endMilliseconds >= durationMilliseconds - 750L) {
        (durationMilliseconds - 350L).coerceAtLeast(0L)
    } else segment.endMilliseconds.coerceAtLeast(0L)
