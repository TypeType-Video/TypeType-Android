package video.typetype.tv.player

import android.content.Intent

internal fun Intent.putSponsorBlockPolicy(policy: SponsorBlockPolicy): Intent {
    val visible = policy.visibleSegments
    putExtra(SPONSOR_STARTS, visible.map(TvSponsorBlockSegment::startMilliseconds).toLongArray())
    putExtra(SPONSOR_ENDS, visible.map(TvSponsorBlockSegment::endMilliseconds).toLongArray())
    putExtra(SPONSOR_CATEGORIES, visible.map(TvSponsorBlockSegment::category).toTypedArray())
    putExtra(SPONSOR_LABELS, visible.map(TvSponsorBlockSegment::label).toTypedArray())
    putExtra(SPONSOR_ACTIONS, visible.map(TvSponsorBlockSegment::sourceAction).toTypedArray())
    putExtra(SPONSOR_AUTO, visible.indices.filter { visible[it] in policy.autoSkipSegments }.toIntArray())
    putExtra(SPONSOR_MANUAL, visible.indices.filter { visible[it] in policy.manualSkipSegments }.toIntArray())
    putExtra(SPONSOR_SHOW_CURRENT, policy.showCurrentSegment)
    putExtra(SPONSOR_SHOW_CHAPTERS, policy.showChapters)
    putExtra(SPONSOR_MUTE, policy.muteInsteadOfSkip)
    return this
}

internal fun Intent.toSponsorBlockPolicy(): SponsorBlockPolicy {
    val starts = getLongArrayExtra(SPONSOR_STARTS) ?: return SponsorBlockPolicy.EMPTY
    val ends = getLongArrayExtra(SPONSOR_ENDS) ?: return SponsorBlockPolicy.EMPTY
    val categories = getStringArrayExtra(SPONSOR_CATEGORIES) ?: return SponsorBlockPolicy.EMPTY
    val labels = getStringArrayExtra(SPONSOR_LABELS) ?: return SponsorBlockPolicy.EMPTY
    val actions = getStringArrayExtra(SPONSOR_ACTIONS) ?: return SponsorBlockPolicy.EMPTY
    if (listOf(ends.size, categories.size, labels.size, actions.size).any { it != starts.size }) {
        return SponsorBlockPolicy.EMPTY
    }
    val visible = starts.indices.map { index ->
        TvSponsorBlockSegment(starts[index], ends[index], categories[index], labels[index], actions[index])
    }
    val automatic = (getIntArrayExtra(SPONSOR_AUTO) ?: intArrayOf()).map(visible::getOrNull).filterNotNull()
    val manual = (getIntArrayExtra(SPONSOR_MANUAL) ?: intArrayOf()).map(visible::getOrNull).filterNotNull()
    return SponsorBlockPolicy(
        visibleSegments = visible,
        autoSkipSegments = automatic,
        manualSkipSegments = manual,
        showCurrentSegment = getBooleanExtra(SPONSOR_SHOW_CURRENT, false),
        showChapters = getBooleanExtra(SPONSOR_SHOW_CHAPTERS, false),
        muteInsteadOfSkip = getBooleanExtra(SPONSOR_MUTE, false),
    )
}

private const val SPONSOR_STARTS = "sponsor_starts"
private const val SPONSOR_ENDS = "sponsor_ends"
private const val SPONSOR_CATEGORIES = "sponsor_categories"
private const val SPONSOR_LABELS = "sponsor_labels"
private const val SPONSOR_ACTIONS = "sponsor_actions"
private const val SPONSOR_AUTO = "sponsor_auto"
private const val SPONSOR_MANUAL = "sponsor_manual"
private const val SPONSOR_SHOW_CURRENT = "sponsor_show_current"
private const val SPONSOR_SHOW_CHAPTERS = "sponsor_show_chapters"
private const val SPONSOR_MUTE = "sponsor_mute"
