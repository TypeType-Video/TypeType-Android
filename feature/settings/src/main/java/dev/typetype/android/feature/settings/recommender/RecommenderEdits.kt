package dev.typetype.android.feature.settings.recommender

import dev.typetype.android.domain.recommendations.RecommenderPreferences

internal fun RecommenderPreferences.choose(algorithmId: String): RecommenderPreferences =
    copy(algorithmId = algorithmId, parameters = emptyMap())

internal fun RecommenderPreferences.togglePinned(id: String): RecommenderPreferences =
    copy(pinnedGroups = pinnedGroups.toggle(id), mutedGroups = mutedGroups - id)

internal fun RecommenderPreferences.toggleMuted(id: String): RecommenderPreferences =
    copy(mutedGroups = mutedGroups.toggle(id), pinnedGroups = pinnedGroups - id)

internal fun RecommenderPreferences.toggleIgnored(id: String): RecommenderPreferences =
    copy(ignoredSignals = ignoredSignals.toggle(id))

internal fun RecommenderPreferences.toggleUse(categoryId: String): RecommenderPreferences =
    copy(unusedData = unusedData.toggle(categoryId))

internal fun RecommenderPreferences.toggleExcluded(itemId: String): RecommenderPreferences =
    copy(excludedItems = excludedItems.toggle(itemId))

internal fun RecommenderPreferences.withRetention(categoryId: String, days: Int?): RecommenderPreferences =
    copy(retentionDays = if (days == null) retentionDays - categoryId else retentionDays + (categoryId to days))

internal fun RecommenderPreferences.addInterest(text: String): RecommenderPreferences? {
    val subject = text.trim().replace(WHITESPACE, " ").take(MAX_INTEREST_LENGTH)
    if (subject.isEmpty() || customInterests.size >= MAX_CUSTOM_INTERESTS) return null
    if (customInterests.any { it.equals(subject, ignoreCase = true) }) return null
    return copy(customInterests = customInterests + subject)
}

internal fun RecommenderPreferences.removeInterest(text: String): RecommenderPreferences =
    copy(customInterests = customInterests.filterNot { it.equals(text, ignoreCase = true) })

private fun List<String>.toggle(value: String): List<String> = if (value in this) this - value else this + value

private val WHITESPACE = Regex("\\s+")
